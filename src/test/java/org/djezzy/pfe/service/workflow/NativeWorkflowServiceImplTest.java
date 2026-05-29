package org.djezzy.pfe.service.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.djezzy.pfe.config.AppProperties;
import org.djezzy.pfe.dao.evaluation.CVDAO;
import org.djezzy.pfe.dao.evaluation.CandidateEvaluationDAO;
import org.djezzy.pfe.dao.job.JobOfferDAO;
import org.djezzy.pfe.model.evaluation.CV;
import org.djezzy.pfe.model.evaluation.CandidateEvaluation;
import org.djezzy.pfe.model.job.JobOffer;
import org.djezzy.pfe.service.job.LlmParsingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NativeWorkflowServiceImplTest {

    @Mock
    private JobOfferDAO jobOfferDAO;

    @Mock
    private CandidateEvaluationDAO candidateEvaluationDAO;

    @Mock
    private CVDAO cvdao;

    @Mock
    private LlmParsingService llmParsingService;

    @Mock
    private WorkflowSseService sseService;

    @Mock
    private AppProperties appProperties;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private java.util.concurrent.Executor workflowProcessorExecutor;

    @InjectMocks
    private NativeWorkflowServiceImpl nativeWorkflowService;

    @Test
    void processWorkflow_SucceedsSynchronouslyIfExecutionFails() {
        assertNotNull(nativeWorkflowService);
        nativeWorkflowService.processWorkflow(java.util.Map.of("evaluationId", 1L, "cv_text", "Sample CV", "job_description", "Sample JD"));
    }
}
