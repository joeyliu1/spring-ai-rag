package com.lss.springairag.controller;

import com.alibaba.cloud.ai.document.DocumentWithScore;
import com.alibaba.cloud.ai.model.RerankRequest;
import com.alibaba.cloud.ai.model.RerankResponse;
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
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
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
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@RestController
@RequestMapping(ApplicationConstant.API_VERSION + "/ai")
public class AIRagController {

    private static final int DEFAULT_RAG_TOP_K = 5;
    private static final int DEFAULT_RAG_PREFETCH_TOP_K = 20;
    private static final int MAX_RAG_TOP_K = 20;
    private static final int MAX_RAG_PREFETCH_TOP_K = 100;
    private static final int STREAM_AUDIT_BUFFER_SIZE = 80;
    private static final double DEFAULT_SIMILARITY_THRESHOLD = 0.1d;

    ChatClient chatClient;

    VectorStore vectorStore;

    @Autowired
    private SensitiveAuditService sensitiveAuditService;

    @Autowired
    private WordFrequencyService wordFrequencyService;

    @Autowired
    private DashScopeRerankModel dashScopeRerankModel;

    private static final String DEFAULT_SYSTEM_PROMPT = """
            你是"LSS"知识库系统的对话助手，请以乐于助人的方式进行对话，
            {rag_message}
            今天的日期：{current_data}
            """;


    public AIRagController(ChatModel chatModel, ChatMemory chatMemory,
                           VectorStore vectorStore) {
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
    @PostMapping(value = "/rag", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Loggable
    public Flux<String> generatePost(@RequestParam(value = "sources", required = false) List<String> sources,
                                     @RequestParam(value = "message", defaultValue = "你好") String message,
                                     @RequestParam(value = "topK", required = false) Integer topK,
                                     @RequestParam(value = "prefetchTopK", required = false) Integer prefetchTopK,
                                     @RequestParam(value = "similarityThreshold", required = false) Double similarityThreshold,
                                     @RequestParam(value = "rerank", defaultValue = "true") Boolean rerank) throws IOException {

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
        return processNormalRagQuery(sources, message, topK, prefetchTopK, similarityThreshold, rerank);
    }

    /**
     * 处理正常的 RAG 查询
     *
     * @param sources 数据源列表
     * @param message 用户消息
     * @return 响应流
     */
    private Flux<String> processNormalRagQuery(List<String> sources,
                                               String message,
                                               Integer topK,
                                               Integer prefetchTopK,
                                               Double similarityThreshold,
                                               Boolean rerank) {
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
            int normalizedTopK = normalizeTopK(topK);
            int normalizedPrefetchTopK = normalizePrefetchTopK(prefetchTopK, normalizedTopK);
            double normalizedThreshold = normalizeSimilarityThreshold(similarityThreshold);


            SearchRequest searchRequest = SearchRequest.builder()
                    .query(message)
                    .similarityThreshold(normalizedThreshold)
                    .topK(normalizedPrefetchTopK)
                    // source in ['xxx.pdf','xxxx']
                    .filterExpression("owner_user_id == " + userId + " && source in " + JSON.toJSONString(sources))
                    .build();
            sourceDocuments = vectorStore.similaritySearch(searchRequest);
            sourceDocuments = rerankDocuments(message, sourceDocuments, Boolean.TRUE.equals(rerank));
            sourceDocuments = limitDocuments(sourceDocuments, normalizedTopK);
            if (sourceDocuments.isEmpty()) {
                return Flux.just("未在已选择的知识库文件中检索到足够相关的内容，请调整问题、选择更多文件或降低相似度阈值。");
            }

            clientRequestSpec = clientRequestSpec
                    .system(a -> a.param("rag_message", """
                            请严格基于知识库内容回答用户问题，
                            使用知识库片段回答时，请在相关句子后标注 [来源1]、[来源2] 这样的引用编号，
                            不要添加任何知识库之外的信息。如果知识库内容不完整，仅需基于已有信息作答，
                            不要自行补充。
                            """))
                    .user(buildRagUserMessage(message, sourceDocuments));
        }


        Flux<String> content = clientRequestSpec
                .stream()// 流式方式
                .content();

        String citations = buildCitationAppendix(sourceDocuments);
        return auditOutput(content, citations, "rag_chat");
    }

    private Flux<String> auditOutput(Flux<String> content, String citations, String scene) {
        AtomicReference<String> buffer = new AtomicReference<>("");
        AtomicBoolean blocked = new AtomicBoolean(false);
        Flux<String> auditedContent = content.<String>handle((chunk, sink) -> {
            if (blocked.get()) {
                sink.complete();
                return;
            }
            String nextBuffer = buffer.get() + chunk;
            if (!shouldFlushAuditBuffer(nextBuffer)) {
                buffer.set(nextBuffer);
                return;
            }
            SensitiveAuditResult auditResult = sensitiveAuditService.auditOutput(nextBuffer, scene);
            if (auditResult.isBlocked()) {
                blocked.set(true);
                buffer.set("");
                sink.next(auditResult.getBlockMessage());
                sink.complete();
                return;
            }
            buffer.set("");
            sink.next(nextBuffer);
        });
        if (citations.isEmpty()) {
            return auditedContent.concatWith(flushAuditBuffer(buffer, blocked, scene));
        }
        return auditedContent
                .concatWith(flushAuditBuffer(buffer, blocked, scene))
                .concatWith(Flux.defer(() -> {
            if (blocked.get()) {
                return Flux.empty();
            }
            SensitiveAuditResult auditResult = sensitiveAuditService.auditOutput(citations, scene);
            if (auditResult.isBlocked()) {
                return Flux.just(auditResult.getBlockMessage());
            }
            return Flux.just(citations);
        }));
    }

    private Flux<String> flushAuditBuffer(AtomicReference<String> buffer, AtomicBoolean blocked, String scene) {
        return Flux.defer(() -> {
            String remaining = buffer.getAndSet("");
            if (blocked.get() || remaining.isEmpty()) {
                return Flux.empty();
            }
            SensitiveAuditResult auditResult = sensitiveAuditService.auditOutput(remaining, scene);
            if (auditResult.isBlocked()) {
                blocked.set(true);
                return Flux.just(auditResult.getBlockMessage());
            }
            return Flux.just(remaining);
        });
    }

    private boolean shouldFlushAuditBuffer(String buffer) {
        return buffer.length() >= STREAM_AUDIT_BUFFER_SIZE
                || buffer.endsWith("\n")
                || buffer.endsWith("。")
                || buffer.endsWith("！")
                || buffer.endsWith("？")
                || buffer.endsWith(".")
                || buffer.endsWith("!")
                || buffer.endsWith("?");
    }

    private List<Document> rerankDocuments(String message, List<Document> documents, boolean rerank) {
        if (!rerank || documents == null || documents.isEmpty()) {
            return documents == null ? List.of() : documents;
        }
        try {
            RerankResponse response = dashScopeRerankModel.call(new RerankRequest(message, documents));
            if (response == null || response.getResults() == null || response.getResults().isEmpty()) {
                return documents;
            }
            return response.getResults().stream()
                    .filter(result -> result != null && result.getOutput() != null)
                    .sorted(Comparator.comparing(DocumentWithScore::getScore,
                            Comparator.nullsLast(Comparator.reverseOrder())))
                    .map(DocumentWithScore::getOutput)
                    .toList();
        } catch (Exception e) {
            log.warn("RAG rerank failed, fallback to vector search order", e);
            return documents;
        }
    }

    private List<Document> limitDocuments(List<Document> documents, int topK) {
        if (documents == null || documents.isEmpty()) {
            return List.of();
        }
        return documents.stream().limit(topK).toList();
    }

    private String buildRagUserMessage(String message, List<Document> documents) {
        StringBuilder context = new StringBuilder();
        for (int i = 0; i < documents.size(); i++) {
            Document document = documents.get(i);
            context.append("[来源").append(i + 1).append("]\n")
                    .append("文件: ").append(metadataValue(document, "source", "unknown")).append("\n")
                    .append("分块: ").append(metadataValue(document, "chunk_index", "-")).append("\n")
                    .append(document.getText())
                    .append("\n\n");
        }
        return """
                用户问题：
                %s

                知识库上下文如下，回答时只能基于这些内容：
                ---------------------
                %s
                ---------------------
                """.formatted(message, context);
    }

    private int normalizeTopK(Integer topK) {
        if (topK == null || topK <= 0) {
            return DEFAULT_RAG_TOP_K;
        }
        return Math.min(topK, MAX_RAG_TOP_K);
    }

    private int normalizePrefetchTopK(Integer prefetchTopK, int topK) {
        if (prefetchTopK == null || prefetchTopK <= 0) {
            return Math.max(DEFAULT_RAG_PREFETCH_TOP_K, topK);
        }
        return Math.min(Math.max(prefetchTopK, topK), MAX_RAG_PREFETCH_TOP_K);
    }

    private double normalizeSimilarityThreshold(Double similarityThreshold) {
        if (similarityThreshold == null || similarityThreshold < 0 || similarityThreshold > 1) {
            return DEFAULT_SIMILARITY_THRESHOLD;
        }
        return similarityThreshold;
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
