package com.lss.springairag.rag;

import com.lss.springairag.config.RagChunkProperties;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class RecursiveChunkSplitter {

    private static final List<String> SEPARATORS = List.of(
            "\n\n",
            "\n",
            "。", "！", "？",
            ". ", "! ", "? ",
            "；", "; ",
            "，", ", ",
            " "
    );

    private final RagChunkProperties properties;

    public RecursiveChunkSplitter(RagChunkProperties properties) {
        this.properties = properties;
    }

    public List<Document> split(List<Document> documents, String sourceName) {
        List<ChunkData> chunks = new ArrayList<>();
        for (Document document : documents) {
            if (document == null || !StringUtils.hasText(document.getText())) {
                continue;
            }

            Map<String, Object> metadata = new LinkedHashMap<>(document.getMetadata());
            if (StringUtils.hasText(sourceName)) {
                metadata.put("source", sourceName);
            }

            for (String chunkText : splitText(document.getText())) {
                chunks.add(new ChunkData(chunkText, metadata));
                if (chunks.size() >= properties.getMaxChunks()) {
                    return toDocuments(chunks);
                }
            }
        }

        return toDocuments(chunks);
    }

    private List<Document> toDocuments(List<ChunkData> chunks) {
        List<Document> result = new ArrayList<>(chunks.size());
        int chunkCount = chunks.size();
        for (int i = 0; i < chunks.size(); i++) {
            ChunkData chunk = chunks.get(i);
            Map<String, Object> metadata = new LinkedHashMap<>(chunk.metadata());
            metadata.put("chunk_index", i);
            metadata.put("chunk_count", chunkCount);
            metadata.put("chunk_size", chunk.text().length());
            result.add(new Document(chunk.text(), metadata));
        }
        return result;
    }

    private List<String> splitText(String text) {
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n').trim();
        if (!StringUtils.hasText(normalized)) {
            return List.of();
        }

        List<String> units = recursiveSplit(normalized, 0);
        return mergeUnits(units);
    }

    private List<String> recursiveSplit(String text, int separatorIndex) {
        int chunkSize = normalizedChunkSize();
        if (text.length() <= chunkSize) {
            return List.of(text.trim());
        }

        if (separatorIndex >= SEPARATORS.size()) {
            return hardSplit(text);
        }

        String separator = SEPARATORS.get(separatorIndex);
        List<String> pieces = splitBySeparator(text, separator);
        if (pieces.size() == 1) {
            return recursiveSplit(text, separatorIndex + 1);
        }

        List<String> result = new ArrayList<>();
        for (String piece : pieces) {
            String trimmed = piece.trim();
            if (!StringUtils.hasText(trimmed)) {
                continue;
            }
            if (trimmed.length() > chunkSize) {
                result.addAll(recursiveSplit(trimmed, separatorIndex + 1));
            }
            else {
                result.add(trimmed);
            }
        }
        return result;
    }

    private List<String> splitBySeparator(String text, String separator) {
        List<String> result = new ArrayList<>();
        int start = 0;
        int index;
        while ((index = text.indexOf(separator, start)) >= 0) {
            int end = index + separator.length();
            String piece = text.substring(start, shouldKeepSeparator(separator) ? end : index);
            if (StringUtils.hasText(piece)) {
                result.add(piece);
            }
            start = end;
        }
        if (start < text.length()) {
            String tail = text.substring(start);
            if (StringUtils.hasText(tail)) {
                result.add(tail);
            }
        }
        return result;
    }

    private boolean shouldKeepSeparator(String separator) {
        return !" ".equals(separator);
    }

    private List<String> mergeUnits(List<String> units) {
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int chunkSize = normalizedChunkSize();

        for (String unit : units) {
            if (!StringUtils.hasText(unit)) {
                continue;
            }

            String candidate = appendWithSpace(current.toString(), unit);
            if (candidate.length() <= chunkSize) {
                current.setLength(0);
                current.append(candidate);
                continue;
            }

            emitCurrent(chunks, current);
            current.setLength(0);

            String overlap = tailOverlap(chunks.isEmpty() ? "" : chunks.get(chunks.size() - 1));
            String next = appendWithSpace(overlap, unit);
            if (next.length() > chunkSize) {
                current.append(unit);
            }
            else {
                current.append(next);
            }
        }

        emitCurrent(chunks, current);
        mergeSmallTail(chunks);
        return chunks;
    }

    private void emitCurrent(List<String> chunks, StringBuilder current) {
        String chunk = current.toString().trim();
        if (StringUtils.hasText(chunk)) {
            chunks.add(chunk);
        }
    }

    private void mergeSmallTail(List<String> chunks) {
        int minChunkSize = Math.max(1, properties.getMinChunkSize());
        if (chunks.size() < 2) {
            return;
        }

        int lastIndex = chunks.size() - 1;
        String last = chunks.get(lastIndex);
        if (last.length() >= minChunkSize) {
            return;
        }

        String previous = chunks.get(lastIndex - 1);
        String merged = appendWithSpace(previous, last);
        if (merged.length() <= normalizedChunkSize() + normalizedOverlapSize()) {
            chunks.set(lastIndex - 1, merged);
            chunks.remove(lastIndex);
        }
    }

    private List<String> hardSplit(String text) {
        List<String> chunks = new ArrayList<>();
        int chunkSize = normalizedChunkSize();
        int overlapSize = normalizedOverlapSize();
        int start = 0;
        while (start < text.length() && chunks.size() < properties.getMaxChunks()) {
            int end = Math.min(start + chunkSize, text.length());
            String chunk = text.substring(start, end).trim();
            if (StringUtils.hasText(chunk)) {
                chunks.add(chunk);
            }
            if (end >= text.length()) {
                break;
            }
            start = Math.max(end - overlapSize, start + 1);
        }
        return chunks;
    }

    private String appendWithSpace(String left, String right) {
        if (!StringUtils.hasText(left)) {
            return right.trim();
        }
        if (!StringUtils.hasText(right)) {
            return left.trim();
        }
        return left.trim() + "\n" + right.trim();
    }

    private String tailOverlap(String text) {
        int overlapSize = normalizedOverlapSize();
        if (!StringUtils.hasText(text) || overlapSize <= 0) {
            return "";
        }
        if (text.length() <= overlapSize) {
            return text;
        }
        return text.substring(text.length() - overlapSize);
    }

    private int normalizedChunkSize() {
        return Math.max(200, properties.getChunkSize());
    }

    private int normalizedOverlapSize() {
        int chunkSize = normalizedChunkSize();
        return Math.max(0, Math.min(properties.getOverlapSize(), chunkSize / 2));
    }

    private record ChunkData(String text, Map<String, Object> metadata) {
    }
}
