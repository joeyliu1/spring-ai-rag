package com.lss.springairag.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "lss.rag.chunk")
public class RagChunkProperties {

    /**
     * Target chunk size in characters. Keep this close to the amount of text that
     * can answer one focused question without mixing too many topics.
     */
    private int chunkSize = 1200;

    /**
     * Character overlap between adjacent chunks to avoid losing answers that cross
     * a chunk boundary.
     */
    private int overlapSize = 200;

    /**
     * Very small fragments are merged into nearby chunks when possible.
     */
    private int minChunkSize = 80;

    /**
     * Guardrail for unexpectedly huge files.
     */
    private int maxChunks = 10000;
}
