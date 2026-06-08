package com.lss.springairag.service;

import com.lss.springairag.context.BaseContext;
import com.lss.springairag.pojo.dto.RagEvaluationBatchRequest;
import com.lss.springairag.pojo.dto.RagEvaluationRequest;
import com.lss.springairag.pojo.vo.RagEvaluationBatchResult;
import com.lss.springairag.pojo.vo.RagEvaluationResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RagEvaluationServiceTest {

    @Mock
    private VectorStore vectorStore;

    private RagEvaluationService ragEvaluationService;

    @BeforeEach
    void setUp() {
        BaseContext.setCurrentId(7L);
        ragEvaluationService = new RagEvaluationService(vectorStore);
    }

    @AfterEach
    void tearDown() {
        BaseContext.removeCurrentId();
    }

    @Test
    void evaluateFiltersByCurrentUserAndCalculatesScoreStats() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(
                document("doc-1", "Spring AI 支持 RAG 知识库问答", "guide.pdf", 0, 0.92),
                document("doc-2", "Milvus 用于向量检索", "guide.pdf", 1, 0.72)
        ));
        RagEvaluationRequest request = new RagEvaluationRequest();
        request.setQuestion("Spring AI RAG 怎么做");
        request.setSources(List.of("guide.pdf"));
        request.setExpectedSources(List.of("guide.pdf"));
        request.setExpectedKeywords(List.of("Spring AI", "Milvus"));
        request.setTopK(5);
        request.setSimilarityThreshold(0.2);

        RagEvaluationResult result = ragEvaluationService.evaluate(request);

        assertThat(result.isPassed()).isTrue();
        assertThat(result.getRetrievedCount()).isEqualTo(2);
        assertThat(result.getAverageScore()).isCloseTo(0.82, within(0.0001));
        assertThat(result.getMaxScore()).isEqualTo(0.92);
        assertThat(result.getMinScore()).isEqualTo(0.72);
        assertThat(result.getKeywordHitRate()).isEqualTo(1.0);

        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(captor.capture());
        assertThat(captor.getValue().toString()).contains("owner_user_id", "7", "guide.pdf");
    }

    @Test
    void evaluateBatchComparesTopKAndThresholdStrategies() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(
                document("doc-1", "命中 关键词", "case.pdf", 0, 0.8)
        ));
        RagEvaluationRequest testCase = new RagEvaluationRequest();
        testCase.setQuestion("测试问题");
        testCase.setExpectedKeywords(List.of("关键词"));
        RagEvaluationBatchRequest request = new RagEvaluationBatchRequest();
        request.setCases(List.of(testCase));
        request.setTopKValues(List.of(3, 5));
        request.setSimilarityThresholds(List.of(0.1, 0.3));

        RagEvaluationBatchResult result = ragEvaluationService.evaluateBatch(request);

        assertThat(result.getCaseCount()).isEqualTo(1);
        assertThat(result.getStrategyCount()).isEqualTo(4);
        assertThat(result.getStrategies())
                .allSatisfy(strategy -> {
                    assertThat(strategy.getPassRate()).isEqualTo(1.0);
                    assertThat(strategy.getRecallAtK()).isEqualTo(1.0);
                    assertThat(strategy.getMeanReciprocalRank()).isEqualTo(1.0);
                });
    }

    private Document document(String id, String text, String source, int chunkIndex, double score) {
        Document document = new Document(id, text, Map.of(
                "source", source,
                "chunk_index", chunkIndex,
                "chunk_count", 2
        ));
        setScore(document, score);
        return document;
    }

    private void setScore(Document document, double score) {
        try {
            Field scoreField = Document.class.getDeclaredField("score");
            scoreField.setAccessible(true);
            scoreField.set(document, score);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("无法设置测试文档 score", e);
        }
    }
}
