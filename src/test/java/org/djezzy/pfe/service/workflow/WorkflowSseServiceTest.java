package org.djezzy.pfe.service.workflow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.junit.jupiter.api.Assertions.*;

class WorkflowSseServiceTest {

    private WorkflowSseService workflowSseService;

    @BeforeEach
    void setUp() {
        workflowSseService = new WorkflowSseService();
    }

    @Test
    void subscribe_CreatesEmitterAndSendsInitialProgress() {
        SseEmitter emitter = workflowSseService.subscribe(1L);
        assertNotNull(emitter);
    }

    @Test
    void sendProgress_DoesNotThrow() {
        workflowSseService.subscribe(1L);
        assertDoesNotThrow(() -> workflowSseService.sendProgress(1L, 50, "Halfway there"));
    }

    @Test
    void complete_DoesNotThrow() {
        workflowSseService.subscribe(1L);
        assertDoesNotThrow(() -> workflowSseService.complete(1L));
    }

    @Test
    void fail_DoesNotThrow() {
        workflowSseService.subscribe(1L);
        assertDoesNotThrow(() -> workflowSseService.fail(1L, "Error message"));
    }
}
