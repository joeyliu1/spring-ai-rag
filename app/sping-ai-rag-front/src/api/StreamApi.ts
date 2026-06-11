import { fetchEventSource } from "@microsoft/fetch-event-source";
import {BASE_URL} from "@/http/config.ts";
import service from "@/http";

type ResultCallBack = (e: any | null) => void;

export interface RagStreamOptions {
    topK?: number;
    similarityThreshold?: number;
    rerank?: boolean;
}

const BaseUrl = BASE_URL;
export const postStreamChat = (
    author: string,
    onMessage: ResultCallBack,
    onError: ResultCallBack,
    onClose: ResultCallBack
) => {
    const ctrl = new AbortController();
    fetchEventSource(BaseUrl + "/post-chat", {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify({
            author: author,
        }),
        signal: ctrl.signal,
        onmessage: onMessage,
        onerror: (err: any) => {
            onError(err);
        },
        onclose: () => {
            onClose(null);
        },
        onopen: async (response: any) => {
            if (response.ok) {
                return;
            } else if (
                response.status >= 400 &&
                response.status < 500 &&
                response.status !== 429
            ) {
                // 不再抛出FatalError，防止自动重试
                // 直接调用onError回调
                onError(new Error(`HTTP ${response.status}: ${response.statusText}`));
                // 返回一个永远不会resolve的Promise，终止连接
                return new Promise(() => {});
            } else {
                // 不再抛出RetriableError，防止自动重试
                // 直接调用onError回调
                onError(new Error(`HTTP ${response.status}: ${response.statusText}`));
                // 返回一个永远不会resolve的Promise，终止连接
                return new Promise(() => {});
            }
        },
    });
};

export const getStreamChat = (
    message: string,
    url: string = "/chat/stream",
    onMessage: ResultCallBack,
    onError: ResultCallBack,
    onClose: ResultCallBack,
    sources?: string[],
    options?: RagStreamOptions
) => {
    const ctrl = new AbortController();
    
    // 统一使用POST请求发送数据
    const formData = new FormData();
    formData.append('message', message);
    if (sources && sources.length > 0) {
        sources.forEach(source => {
            formData.append('sources', source);
        });
    }
    if (options?.topK !== undefined) {
        formData.append('topK', String(options.topK));
    }
    if (options?.similarityThreshold !== undefined) {
        formData.append('similarityThreshold', String(options.similarityThreshold));
    }
    if (options?.rerank !== undefined) {
        formData.append('rerank', String(options.rerank));
    }
    
    fetchEventSource(service.defaults.baseURL + url, {
        method: "POST",
        headers: {
            "Authorization": `Bearer ${localStorage.getItem("token") || ""}`
        },
        body: formData,
        signal: ctrl.signal,
        onmessage: onMessage,
        onerror: (err: any) => {
            onError(err);
        },
        onclose: () => {
            onClose(null);
        },
        onopen: async (response: any) => {
            if (response.ok) {
                return;
            } 
            else if (response.status === 401) {
                // 处理401未授权错误
                import('@/api/authUtils').then(module => {
                module.default();
                });
            }
            else{
                // 不再抛出RetriableError，防止自动重试
                // 直接调用onError回调
                onError(new Error(`HTTP ${response.status}: ${response.statusText}`));
                // 返回一个永远不会resolve的Promise，终止连接
                return new Promise(() => {});
            }
        },
    });
};

// 专门用于POST请求的流式聊天函数
export const postStreamChatWithSources = (
    message: string,
    sources: string[],
    url: string = "/ai/rag",
    onMessage: ResultCallBack,
    onError: ResultCallBack,
    onClose: ResultCallBack,
) => {
    const ctrl = new AbortController();
    
    const formData = new FormData();
    formData.append('message', message);
    sources.forEach(source => {
        formData.append('sources', source);
    });
    
    fetchEventSource(service.defaults.baseURL + url, {
        method: "POST",
        headers: {
            "Authorization": `Bearer ${localStorage.getItem("token") || ""}`
        },
        body: formData,
        signal: ctrl.signal,
        onmessage: onMessage,
        onerror: (err: any) => {
            onError(err);
        },
        onclose: () => {
            onClose(null);
        },
        onopen: async (response: any) => {
            if (response.ok) {
                return;
            } 
            else if (response.status === 401) {
                // 处理401未授权错误
                import('@/api/authUtils').then(module => {
                module.default();
                });
            }
            else{
                // 不再抛出RetriableError，防止自动重试
                // 直接调用onError回调
                onError(new Error(`HTTP ${response.status}: ${response.statusText}`));
                // 返回一个永远不会resolve的Promise，终止连接
                return new Promise(() => {});
            }
        },
    });
};
