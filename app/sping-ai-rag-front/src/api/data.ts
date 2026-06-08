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
  id: number;
  fileId: number;
  documentId: string;
  source: string;
  chunkIndex: number;
  chunkCount: number;
  chunkSize: number;
  preview: string;
  content: string;
  metadata: Record<string, unknown>;
}
