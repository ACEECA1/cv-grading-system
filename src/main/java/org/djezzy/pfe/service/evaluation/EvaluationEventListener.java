package org.djezzy.pfe.service.evaluation;

import lombok.RequiredArgsConstructor;
import org.djezzy.pfe.event.CvUploadedEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class EvaluationEventListener {
    private final AsyncWorkflowService asyncWorkflowService;

    @Async("applicationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCvUploaded(CvUploadedEvent event) {
        asyncWorkflowService.processCvAndSendForEvaluation(event.cvId(), event.evaluationId());
    }
}


