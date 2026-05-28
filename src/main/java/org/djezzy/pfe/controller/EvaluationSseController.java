package org.djezzy.pfe.controller;

import lombok.RequiredArgsConstructor;
import org.djezzy.pfe.service.workflow.WorkflowSseService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/evaluations")
@RequiredArgsConstructor
public class EvaluationSseController {

    private final WorkflowSseService workflowSseService;

    @GetMapping(path = "/{id}/progress", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeToProgress(@PathVariable Long id) {
        return workflowSseService.subscribe(id);
    }
}
