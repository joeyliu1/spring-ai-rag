import service from "@/http";
import { RagApi } from "@/api/common";

type Res = any;

export interface RagEvaluationCase {
  question: string;
  sources?: string[];
  expectedSources?: string[];
  expectedKeywords?: string[];
  topK?: number;
  similarityThreshold?: number;
}

export interface RagEvaluationBatchRequest {
  cases: RagEvaluationCase[];
  topKValues: number[];
  similarityThresholds: number[];
}

export const evaluateRagBatchApi = async (data: RagEvaluationBatchRequest): Promise<Res> => {
  return service.post(RagApi.EvaluateBatch, data);
};
