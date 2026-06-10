export interface StoreFile {
  id: number;
  url: string;
  fileName: string;
  vectorId: string[];
  ownerUserId?: number;
  teamId?: number;
  createTime: Date;
  updateTime: Date;
}

export interface KnowledgeFileDetail {
  file: StoreFile;
  vectorCount: number;
  chunkCount: number;
  vectorIds: string[];
}

export interface KnowledgeChunk {
  id?: number;
  fileId?: number;
  documentId: string;
  source: string;
  chunkIndex: number;
  chunkCount: number;
  chunkSize: number;
  preview: string;
  content: string;
  metadata: Record<string, unknown>;
}

export interface KnowledgePreviewFile {
  fileName: string;
  chunkCount: number;
  chunks: KnowledgeChunk[];
}

export interface KnowledgePreviewResult {
  chunkConfig: {
    chunkSize: number;
    overlapSize: number;
    minChunkSize: number;
    maxChunks: number;
    minAllowedChunkSize?: number;
    maxAllowedChunkSize?: number;
    maxAllowedOverlapSize?: number;
    maxAllowedChunks?: number;
  };
  fileCount: number;
  totalChunkCount: number;
  files: KnowledgePreviewFile[];
}
