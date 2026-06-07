package com.lss.springairag.controller;

import com.alibaba.cloud.ai.advisor.RetrievalRerankAdvisor;
import com.alibaba.cloud.ai.dashscope.rerank.DashScopeRerankModel;
import com.alibaba.fastjson2.JSON;
import com.lss.springairag.advisors.MetadataAwareQuestionAnswerAdvisor;
import com.lss.springairag.annotation.Loggable;
import com.lss.springairag.common.ApplicationConstant;
import com.lss.springairag.context.BaseContext;
import com.lss.springairag.pojo.vo.SensitiveAuditResult;
import com.lss.springairag.service.SensitiveAuditService;
import com.lss.springairag.service.WordFrequencyService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping(ApplicationConstant.API_VERSION + "/ai")
public class AIRagController {


    ChatClient chatClient;

    VectorStore vectorStore;

    @Autowired
    private SensitiveAuditService sensitiveAuditService;

    @Autowired
    private WordFrequencyService wordFrequencyService;

    @Autowired
    private DashScopeRerankModel dashScopeRerankModel;

    private ChatModel chatModel;

    private static final String DEFAULT_SYSTEM_PROMPT = """
            你是"帅帅"知识库系统的对话助手，请以乐于助人的方式进行对话，
            {rag_message}
            今天的日期：{current_data}
            """;


    public AIRagController(ChatModel chatModel, ChatMemory chatMemory,
                           VectorStore vectorStore) {
        this.chatModel = chatModel;
        this.chatClient = ChatClient.builder(chatModel)
                // 隐式
                .defaultSystem(DEFAULT_SYSTEM_PROMPT)
                .defaultSystem(p -> p.param("rag_message", ""))
                .defaultAdvisors(
                        PromptChatMemoryAdvisor.builder(chatMemory).build(),
                        SimpleLoggerAdvisor.builder().build(),
                        new MetadataAwareQuestionAnswerAdvisor()
                )
                .build();
        this.vectorStore = vectorStore;
    }

    @Operation(summary = "rag post", description = "Rag对话接口POST版本")
    @PostMapping(value = "/rag")
    @Loggable
    public Flux<String> generatePost(@RequestParam(value = "sources", required = false) List<String> sources,
                                     @RequestParam(value = "message", defaultValue = "你好") String message) throws IOException {

        SensitiveAuditResult auditResult = sensitiveAuditService.auditInput(message, "rag_chat");
        if (auditResult.isBlocked()) {
            return Flux.just(auditResult.getBlockMessage());
        }
        wordFrequencyService.recordQuestion(message, "rag_chat");

        // 检查是否是聚合查询   router方式
       /*boolean isSql= chatClient.prompt()
                .system("用户的查询是否涉及统计数据、求和、计数、平均值等聚合操作？")
                .user(message)      // 班级这个热词出现了多少次
                .call()
                .entity(Boolean.class); */
        return processNormalRagQuery(sources, message);
    }

    /**
     * 处理正常的 RAG 查询
     *
     * @param sources 数据源列表
     * @param message 用户消息
     * @return 响应流
     */
    private Flux<String> processNormalRagQuery(List<String> sources, String message) {
        Long userId = BaseContext.getCurrentId();
        List<Document> sourceDocuments = List.of();
        ChatClient.ChatClientRequestSpec clientRequestSpec = chatClient.prompt()
                .user(message)
                .system(a -> a.param("current_data", LocalDate.now().toString()))
                // 为什么要存userMessage  为了MetadataAwareQuestionAnswerAdvisor中获取
                .advisors(a -> a.param("userMessage", message))
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, userId));
        // 如果提供了sources参数，使用向量数据库查询
        if (sources != null && !sources.isEmpty()) {


            SearchRequest searchRequest = SearchRequest.builder()
                    .query(message)
                    .similarityThreshold(0.1d).topK(5)
                    // source in ['xxx.pdf','xxxx']
                    .filterExpression("source in " + JSON.toJSONString(sources))
                    .build();
            sourceDocuments = vectorStore.similaritySearch(searchRequest);


            // 增强QuestionAnswerAdvisor  ：
            // 包含:
            // 1. 检索为空时，返回提示
            // 2. 查询重写
            /*Advisor retrievalAugmentationAdvisor = RetrievalAugmentationAdvisor.builder()
                    // 查 = QuestionAnswerAdvisor
                    .documentRetriever(VectorStoreDocumentRetriever.builder()
                            .similarityThreshold(0.10)
                            .vectorStore(vectorStore)
                            .build())
                    // 检索为空时，返回提示
                    .queryAugmenter(ContextualQueryAugmenter.builder()
                            .allowEmptyContext(false)
                            .emptyContextPromptTemplate(PromptTemplate.builder().template("用户查询位于知识库之外。礼貌地告知用户您无法回答").build())
                            .build())
                    .queryTransformers(RewriteQueryTransformer.builder()
                            .chatClientBuilder(ChatClient.builder(chatModel))
                            .build())
                    .build();*/

            // 自行从vectorStore中查询，自行拼接
            /*List<Document> documents = vectorStore.similaritySearch(searchRequestBuilder.build());
            for (Document document : documents){
                String text = document.getText();
                String source = document.getMetadata().get("source").toString();
                message+=text+"文件来源"+source;
            }*/

            // 重排序 2次筛选--->只有前面步骤已经优化完毕
            RetrievalRerankAdvisor retrievalRerankAdvisor =
                    new RetrievalRerankAdvisor(vectorStore, dashScopeRerankModel
                            , SearchRequest.builder().topK(200).build());

            clientRequestSpec = clientRequestSpec
                    .system(a -> a.param("rag_message", """
                            如果涉及RAG，请提供文件来源，我会提供给你文件来源，
                            请严格基于知识库内容回答用户问题，
                            使用知识库片段回答时，请在相关句子后标注 [来源1]、[来源2] 这样的引用编号，
                            不要添加任何知识库之外的信息。如果知识库内容不完整，仅需基于已有信息作答，
                            不要自行补充。
                            """))
                    // filterExpression的param指定方式：
                    //.advisors(advisorSpec -> advisorSpec.param(QuestionAnswerAdvisor.FILTER_EXPRESSION,"source in "+JSON.toJSONString(sources)));
//                    .advisors(retrievalRerankAdvisor)
                    .advisors(QuestionAnswerAdvisor.builder(vectorStore)
                            .searchRequest(searchRequest)
                            .build());
        }


        Flux<String> content = clientRequestSpec
                .stream()// 流式方式
                .content();

        String citations = buildCitationAppendix(sourceDocuments);
        return auditOutput(content, citations, "rag_chat");
    }

    private Flux<String> auditOutput(Flux<String> content, String citations, String scene) {
        return content.collectList()
                .flatMapMany(chunks -> {
                    String response = String.join("", chunks);
                    if (!citations.isEmpty()) {
                        response = response + citations;
                    }
                    SensitiveAuditResult auditResult = sensitiveAuditService.auditOutput(response, scene);
                    if (auditResult.isBlocked()) {
                        return Flux.just(auditResult.getBlockMessage());
                    }
                    return Flux.just(response);
                });
    }

    private String buildCitationAppendix(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return "";
        }
        List<CitationItem> citations = new ArrayList<>();
        Map<String, Integer> seen = new LinkedHashMap<>();
        for (Document document : documents) {
            String source = metadataValue(document, "source", "unknown");
            String chunkIndex = metadataValue(document, "chunk_index", "-");
            String key = source + "#" + chunkIndex;
            if (seen.containsKey(key)) {
                continue;
            }
            seen.put(key, citations.size() + 1);
            citations.add(new CitationItem(
                    citations.size() + 1,
                    source,
                    chunkIndex,
                    document.getScore(),
                    preview(document.getText())
            ));
        }
        if (citations.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder("\n\n---\n\n### 参考来源\n\n");
        for (CitationItem citation : citations) {
            builder.append("- [来源").append(citation.index()).append("] ")
                    .append(citation.source())
                    .append("，分块 ")
                    .append(citation.chunkIndex());
            if (citation.score() != null) {
                builder.append("，相似度 ")
                        .append(String.format(Locale.ROOT, "%.4f", citation.score()));
            }
            builder.append("\n  > ")
                    .append(citation.preview())
                    .append("\n");
        }
        return builder.toString();
    }

    private String metadataValue(Document document, String key, String defaultValue) {
        Object value = document.getMetadata().get(key);
        return value == null ? defaultValue : value.toString();
    }

    private String preview(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String compactText = text.replaceAll("\\s+", " ").trim();
        if (compactText.length() <= 120) {
            return compactText;
        }
        return compactText.substring(0, 120) + "...";
    }

    private record CitationItem(int index, String source, String chunkIndex, Double score, String preview) {
    }


}
