package com.lss.springairag.controller;

import com.lss.springairag.annotation.Loggable;
import com.lss.springairag.common.ApplicationConstant;
import com.lss.springairag.context.BaseContext;
import com.lss.springairag.pojo.vo.SensitiveAuditResult;
import com.lss.springairag.service.SensitiveAuditService;
import com.lss.springairag.service.WordFrequencyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Tag(name = "AiRagController", description = "chat对话接口")
@Slf4j
@RestController
@RequestMapping(ApplicationConstant.API_VERSION + "/chat")
public class ChatController {

    private static final int STREAM_AUDIT_BUFFER_SIZE = 80;

    @Autowired
    private ChatClient chatClient;

    @Autowired
    private SensitiveAuditService sensitiveAuditService;

    @Autowired
    private WordFrequencyService wordFrequencyService;

    public ChatController(ChatClient.Builder builder, ChatMemory chatMemory) {

        this.chatClient = builder
                .defaultSystem("""
                        你是一家名为“Agent创业公司”的知识库系统的客户客服代理。请友好乐于助人，充满喜悦地回复。
                        """)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build()

                )
                .build();
    }

    @Operation(summary = "stream", description = "流式对话接口")
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Loggable("message")
    public Flux<String> streamRagChat(@RequestParam(value = "message", defaultValue = "你好") String message,
                                      @RequestParam(value = "prompt", defaultValue = "你是一名AI助手，致力于帮助人们解决问题.") String prompt) {
        SensitiveAuditResult inputAudit = sensitiveAuditService.auditInput(message, "chat");
        if (inputAudit.isBlocked()) {
            return Flux.just(inputAudit.getBlockMessage());
        }
        wordFrequencyService.recordQuestion(message, "chat");

        Long userId = BaseContext.getCurrentId();
        Flux<String> content = chatClient.prompt()
                .system(prompt)
                .advisors(a -> a
                        .param(ChatMemory.CONVERSATION_ID, userId))
                .user(message)
                .stream()
                .content();
        return auditOutput(content);
    }

    private Flux<String> auditOutput(Flux<String> content) {
        AtomicReference<String> buffer = new AtomicReference<>("");
        AtomicBoolean blocked = new AtomicBoolean(false);
        return content.<String>handle((chunk, sink) -> {
            if (blocked.get()) {
                sink.complete();
                return;
            }
            String nextBuffer = buffer.get() + chunk;
            if (!shouldFlushAuditBuffer(nextBuffer)) {
                buffer.set(nextBuffer);
                return;
            }
            SensitiveAuditResult outputAudit = sensitiveAuditService.auditOutput(nextBuffer, "chat");
            if (outputAudit.isBlocked()) {
                blocked.set(true);
                buffer.set("");
                sink.next(outputAudit.getBlockMessage());
                sink.complete();
                return;
            }
            buffer.set("");
            sink.next(nextBuffer);
        }).concatWith(Flux.defer(() -> {
            String remaining = buffer.getAndSet("");
            if (blocked.get() || remaining.isEmpty()) {
                return Flux.empty();
            }
            SensitiveAuditResult outputAudit = sensitiveAuditService.auditOutput(remaining, "chat");
            if (outputAudit.isBlocked()) {
                blocked.set(true);
                return Flux.just(outputAudit.getBlockMessage());
            }
            return Flux.just(remaining);
        }));
    }

    private boolean shouldFlushAuditBuffer(String buffer) {
        return buffer.length() >= STREAM_AUDIT_BUFFER_SIZE
                || buffer.endsWith("\n")
                || buffer.endsWith("。")
                || buffer.endsWith("！")
                || buffer.endsWith("？")
                || buffer.endsWith(".")
                || buffer.endsWith("!")
                || buffer.endsWith("?");
    }
}
