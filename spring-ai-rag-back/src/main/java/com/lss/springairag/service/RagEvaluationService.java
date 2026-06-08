package com.lss.springairag.service;

import com.alibaba.fastjson2.JSON;
import com.lss.springairag.context.BaseContext;
import com.lss.springairag.pojo.dto.RagEvaluationBatchRequest;
import com.lss.springairag.pojo.dto.RagEvaluationRequest;
import com.lss.springairag.pojo.vo.RagEvaluationBatchResult;
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
import java.util.Objects;
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
        return evaluateWithOptions(request, topK, similarityThreshold);
    }

    public RagEvaluationBatchResult evaluateBatch(RagEvaluationBatchRequest request) {
        List<RagEvaluationRequest> cases = normalizeCases(request.getCases());
        List<Integer> topKValues = normalizeTopKValues(request.getTopKValues());
        List<Double> similarityThresholds = normalizeSimilarityThresholds(request.getSimilarityThresholds());
        List<RagEvaluationBatchResult.StrategyResult> strategies = new ArrayList<>();

        for (Integer topK : topKValues) {
            for (Double similarityThreshold : similarityThresholds) {
                List<RagEvaluationResult> results = cases.stream()
                        .map(testCase -> evaluateWithOptions(testCase, topK, similarityThreshold))
                        .toList();
                strategies.add(buildStrategyResult(topK, similarityThreshold, results));
            }
        }

        return RagEvaluationBatchResult.builder()
                .caseCount(cases.size())
                .strategyCount(strategies.size())
                .strategies(strategies)
                .build();
    }

    private RagEvaluationResult evaluateWithOptions(RagEvaluationRequest request, int topK, double similarityThreshold) {
        List<String> sources = normalizeList(request.getSources());
        List<String> expectedSources = normalizeList(request.getExpectedSources());
        List<String> expectedKeywords = normalizeList(request.getExpectedKeywords());

        SearchRequest.Builder searchRequestBuilder = SearchRequest.builder()
                .query(request.getQuestion())
                .topK(topK)
                .similarityThreshold(similarityThreshold);

        searchRequestBuilder.filterExpression(buildFilterExpression(sources));

        List<Document> documents = vectorStore.similaritySearch(searchRequestBuilder.build());
        if (documents == null) {
            documents = Collections.emptyList();
        }

        List<RagEvaluationResult.RetrievedChunk> chunks = new ArrayList<>(documents.size());
        Set<String> matchedSourceSet = new HashSet<>();
        Set<String> matchedKeywordSet = new HashSet<>();
        double scoreSum = 0d;
        int scoreCount = 0;
        Double maxScore = null;
        Double minScore = null;

        for (int i = 0; i < documents.size(); i++) {
            Document document = documents.get(i);
            Map<String, Object> metadata = document.getMetadata();
            String source = metadataValue(metadata, "source");
            Double score = document.getScore();
            boolean sourceMatched = !expectedSources.isEmpty() && expectedSources.contains(source);
            List<String> matchedKeywords = findMatchedKeywords(document.getText(), expectedKeywords);

            if (score != null) {
                scoreSum += score;
                scoreCount++;
                maxScore = maxScore == null ? score : Math.max(maxScore, score);
                minScore = minScore == null ? score : Math.min(minScore, score);
            }
            if (sourceMatched) {
                matchedSourceSet.add(source);
            }
            if (!matchedKeywords.isEmpty()) {
                matchedKeywordSet.addAll(matchedKeywords);
            }

            chunks.add(RagEvaluationResult.RetrievedChunk.builder()
                    .rank(i + 1)
                    .id(document.getId())
                    .score(score)
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
                .averageScore(scoreCount == 0 ? 0d : scoreSum / scoreCount)
                .maxScore(maxScore)
                .minScore(minScore)
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

    private RagEvaluationBatchResult.StrategyResult buildStrategyResult(int topK,
                                                                        double similarityThreshold,
                                                                        List<RagEvaluationResult> results) {
        int totalCases = results.size();
        int passedCases = (int) results.stream().filter(RagEvaluationResult::isPassed).count();
        double averageRetrievedCount = results.stream()
                .mapToInt(RagEvaluationResult::getRetrievedCount)
                .average()
                .orElse(0d);
        double averageScore = results.stream()
                .mapToDouble(RagEvaluationResult::getAverageScore)
                .average()
                .orElse(0d);
        double averageSourceHitRate = results.stream()
                .mapToDouble(RagEvaluationResult::getSourceHitRate)
                .average()
                .orElse(0d);
        double averageKeywordHitRate = results.stream()
                .mapToDouble(RagEvaluationResult::getKeywordHitRate)
                .average()
                .orElse(0d);
        double recallAtK = divide((int) results.stream().filter(this::hasExpectedHit).count(), totalCases);
        double meanReciprocalRank = results.stream()
                .mapToDouble(this::reciprocalRank)
                .average()
                .orElse(0d);

        return RagEvaluationBatchResult.StrategyResult.builder()
                .topK(topK)
                .similarityThreshold(similarityThreshold)
                .totalCases(totalCases)
                .passedCases(passedCases)
                .passRate(divide(passedCases, totalCases))
                .averageRetrievedCount(averageRetrievedCount)
                .averageScore(averageScore)
                .averageSourceHitRate(averageSourceHitRate)
                .averageKeywordHitRate(averageKeywordHitRate)
                .recallAtK(recallAtK)
                .meanReciprocalRank(meanReciprocalRank)
                .results(results)
                .build();
    }

    private boolean hasExpectedHit(RagEvaluationResult result) {
        return result.getRetrievedChunks().stream().anyMatch(chunk ->
                chunk.isSourceMatched() || !chunk.getMatchedKeywords().isEmpty());
    }

    private double reciprocalRank(RagEvaluationResult result) {
        return result.getRetrievedChunks().stream()
                .filter(chunk -> chunk.isSourceMatched() || !chunk.getMatchedKeywords().isEmpty())
                .findFirst()
                .map(chunk -> 1.0d / chunk.getRank())
                .orElse(0d);
    }

    private String buildFilterExpression(List<String> sources) {
        Long userId = BaseContext.getCurrentId();
        String ownerFilter = "owner_user_id == " + userId;
        if (sources.isEmpty()) {
            return ownerFilter;
        }
        return ownerFilter + " && source in " + JSON.toJSONString(sources);
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

    private List<RagEvaluationRequest> normalizeCases(List<RagEvaluationRequest> cases) {
        if (CollectionUtils.isEmpty(cases)) {
            return Collections.emptyList();
        }
        return cases.stream()
                .filter(Objects::nonNull)
                .filter(testCase -> StringUtils.hasText(testCase.getQuestion()))
                .toList();
    }

    private List<Integer> normalizeTopKValues(List<Integer> topKValues) {
        if (CollectionUtils.isEmpty(topKValues)) {
            return List.of(DEFAULT_TOP_K);
        }
        List<Integer> normalizedValues = topKValues.stream()
                .filter(Objects::nonNull)
                .map(this::normalizeTopK)
                .distinct()
                .toList();
        return normalizedValues.isEmpty() ? List.of(DEFAULT_TOP_K) : normalizedValues;
    }

    private List<Double> normalizeSimilarityThresholds(List<Double> similarityThresholds) {
        if (CollectionUtils.isEmpty(similarityThresholds)) {
            return List.of(DEFAULT_SIMILARITY_THRESHOLD);
        }
        List<Double> normalizedValues = similarityThresholds.stream()
                .filter(Objects::nonNull)
                .map(this::normalizeSimilarityThreshold)
                .distinct()
                .toList();
        return normalizedValues.isEmpty() ? List.of(DEFAULT_SIMILARITY_THRESHOLD) : normalizedValues;
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
