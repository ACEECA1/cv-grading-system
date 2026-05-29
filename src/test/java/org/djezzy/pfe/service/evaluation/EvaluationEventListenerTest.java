package org.djezzy.pfe.service.evaluation;

import org.djezzy.pfe.event.CvUploadedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EvaluationEventListenerTest {

    @Mock
    private AsyncWorkflowService asyncWorkflowService;

    @InjectMocks
    private EvaluationEventListener evaluationEventListener;

    @Test
    void handleCvUploaded() {
        CvUploadedEvent event = new CvUploadedEvent(1L, 2L);

        evaluationEventListener.handleCvUploaded(event);

        verify(asyncWorkflowService).processCvAndSendForEvaluation(1L, 2L);
    }
}
