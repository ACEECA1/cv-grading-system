package org.djezzy.pfe.service.workflow;

import java.util.Map;

public interface WorkflowProcessorService {
    void processWorkflow(Map<String, Object> payload);
}

