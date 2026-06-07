package com.lss.springairag.service;

import com.alibaba.fastjson2.JSON;
import com.lss.springairag.pojo.dto.RagEvaluationRequest;
import com.lss.springairag.pojo.vo.RagEvaluationResult;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class RagEvaluationService {

    private static final int DEFAULT_TOP_K = 5;
    private static final double DEFAULT_SIMILARITY_THRESHOLD = 0.1d;
    private static final int MAX_TOP_K = 50;
    private static final int PREVIEW_LENGTH = 240;

    private final VectorStore vectorStore;

    public RagEvaluationService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public RagEvaluationResult evaluate(RagEvaluationRequest request) {
        int topK = normalizeTopK(request.getTopK());
        double similarityThreshold = normalizeSimilarityThreshold(request.getSimilarityThreshold());
        List<String> sources = normalizeList(request.getSources());
        List<String> expectedSources = normalizeList(request.getExpectedSources());
        List<String> expectedKeywords = normalizeList(request.getExpectedKeywords());

        SearchRequest.Builder searchRequestBuilder = SearchRequest.builder()
                .query(request.getQuestion())
                .topK(topK)
                .similarityThreshold(similarityThreshold);

        if (!sources.isEmpty()) {
            searchRequestBuilder.filterExpression("source in " + JSON.toJSONString(sources));
        }

        List<Document> documents = vectorStore.similaritySearch(searchRequestBuilder.build());
        if (documents == null) {
            documents = Collections.emptyList();
        }

        List<RagEvaluationResult.RetrievedChunk> chunks = new ArrayList<>(documents.size());
        Set<String> matchedSourceSet = new HashSet<>();
        Set<String> matchedKeywordSet = new HashSet<>();

        for (int i = 0; i < documents.size(); i++) {
            Document document = documents.get(i);
            Map<String, Object> metadata = document.getMetadata();
            String source = metadataValue(metadata, "source");
            boolean sourceMatched = !expectedSources.isEmpty() && expectedSources.contains(source);
            List<String> matchedKeywords = findMatchedKeywords(document.getText(), expectedKeywords);

            if (sourceMatched) {
                matchedSourceSet.add(source);
            }
            if (!matchedKeywords.isEmpty()) {
                matchedKeywordSet.addAll(matchedKeywords);
            }

            chunks.add(RagEvaluationResult.RetrievedChunk.builder()
                    .rank(i + 1)
                    .id(document.getId())
                    .score(document.getScore())
                    .source(source)
                    .chunkIndex(metadataInteger(metadata, "chunk_index"))
                    .chunkCount(metadataInteger(metadata, "chunk_count"))
                    .preview(preview(document.getText()))
                    .sourceMatched(sourceMatched)
                    .matchedKeywords(matchedKeywords)
                    .metadata(metadata)
                    .build());
        }

        int sourceHitCount = matchedSourceSet.size();
        int keywordHitCount = matchedKeywordSet.size();
        double sourceHitRate = expectedSources.isEmpty() ? 0d : divide(sourceHitCount, expectedSources.size());
        double keywordHitRate = expectedKeywords.isEmpty() ? 0d : divide(matchedKeywordSet.size(), expectedKeywords.size());
        boolean passed = (expectedSources.isEmpty() || sourceHitCount > 0)
                && (expectedKeywords.isEmpty() || matchedKeywordSet.size() == expectedKeywords.size());

        return RagEvaluationResult.builder()
                .question(request.getQuestion())
                .topK(topK)
                .similarityThreshold(similarityThreshold)
                .retrievedCount(documents.size())
                .sourceHitCount(sourceHitCount)
                .keywordHitCount(keywordHitCount)
                .sourceHitRate(sourceHitRate)
                .keywordHitRate(keywordHitRate)
                .passed(passed)
                .expectedSources(expectedSources)
                .expectedKeywords(expectedKeywords)
                .retrievedChunks(chunks)
                .build();
    }

    private int normalizeTopK(Integer topK) {
        if (topK == null || topK <= 0) {
            return DEFAULT_TOP_K;
        }
        return Math.min(topK, MAX_TOP_K);
    }

    private double normalizeSimilarityThreshold(Double similarityThreshold) {
        if (similarityThreshold == null || similarityThreshold < 0 || similarityThreshold > 1) {
            return DEFAULT_SIMILARITY_THRESHOLD;
        }
        return similarityThreshold;
    }

    private List<String> normalizeList(List<String> values) {
        if (CollectionUtils.isEmpty(values)) {
            return Collections.emptyList();
        }
        return values.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
    }

    private List<String> findMatchedKeywords(String text, List<String> expectedKeywords) {
        if (!StringUtils.hasText(text) || expectedKeywords.isEmpty()) {
            return Collections.emptyList();
        }
        String normalizedText = text.toLowerCase(Locale.ROOT);
        return expectedKeywords.stream()
                .filter(keyword -> normalizedText.contains(keyword.toLowerCase(Locale.ROOT)))
                .toList();
    }

    private String preview(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String compactText = text.replaceAll("\\s+", " ").trim();
        if (compactText.length() <= PREVIEW_LENGTH) {
            return compactText;
        }
        return compactText.substring(0, PREVIEW_LENGTH) + "...";
    }

    private String metadataValue(Map<String, Object> metadata, String key) {
        if (metadata == null || metadata.get(key) == null) {
            return null;
        }
        return metadata.get(key).toString();
    }

    private Integer metadataInteger(Map<String, Object> metadata, String key) {
        if (metadata == null || metadata.get(key) == null) {
            return null;
        }
        Object value = metadata.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private double divide(int numerator, int denominator) {
        if (denominator == 0) {
            return 0d;
        }
        return numerator * 1.0d / denominator;
    }
}
