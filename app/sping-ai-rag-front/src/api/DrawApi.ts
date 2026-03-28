import service from "@/http";
import { DrawApi } from "./common";

export const drawApi = (prompt: string): Promise<Blob> => {
  return service.get(DrawApi.DrawApi + 'image', {
    params: { prompt },
    responseType: 'blob'
  });
};
