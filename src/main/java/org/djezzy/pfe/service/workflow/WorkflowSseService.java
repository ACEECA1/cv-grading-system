package org.djezzy.pfe.service.workflow;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class WorkflowSseService {

    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long evaluationId) {
        SseEmitter emitter = new SseEmitter(600000L);
        emitters.put(evaluationId, emitter);

        emitter.onCompletion(() -> {
            log.debug("SSE Emitter completed for evaluationId: {}", evaluationId);
            emitters.remove(evaluationId);
        });
        emitter.onTimeout(() -> {
            log.debug("SSE Emitter timed out for evaluationId: {}", evaluationId);
            emitter.complete();
            emitters.remove(evaluationId);
        });
        emitter.onError((e) -> {
            log.debug("SSE Emitter error for evaluationId: {}", evaluationId, e);
            emitters.remove(evaluationId);
        });

        sendProgress(evaluationId, 5, "Connection established");

        return emitter;
    }

    public void sendProgress(Long evaluationId, int progress, String message) {
        SseEmitter emitter = emitters.get(evaluationId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("progress")
                        .data(Map.of("progress", progress, "message", message)));
            } catch (IOException e) {
                log.error("Error sending SSE to evaluationId: {}", evaluationId, e);
                emitters.remove(evaluationId);
            }
        }
    }

    public void complete(Long evaluationId) {
        SseEmitter emitter = emitters.get(evaluationId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("progress")
                        .data(Map.of("progress", 100, "message", "Complete")));
                emitter.complete();
            } catch (IOException e) {
                log.error("Error completing SSE for evaluationId: {}", evaluationId, e);
            } finally {
                emitters.remove(evaluationId);
            }
        }
    }

    public void fail(Long evaluationId, String errorMessage) {
        SseEmitter emitter = emitters.get(evaluationId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data(Map.of("message", errorMessage)));
                emitter.completeWithError(new RuntimeException(errorMessage));
            } catch (IOException e) {
                log.error("Error failing SSE for evaluationId: {}", evaluationId, e);
            } finally {
                emitters.remove(evaluationId);
            }
        }
    }
}
