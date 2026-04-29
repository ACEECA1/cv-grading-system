package org.djezzy.pfe.dto.system;

import org.djezzy.pfe.dto.auth.*;
import org.djezzy.pfe.dto.job.*;
import org.djezzy.pfe.dto.evaluation.*;
import org.djezzy.pfe.dto.system.*;

public record DashboardStatsDTO(
        long totalCvsProcessed,
        double averageMatchScore,
        long pendingHrApprovals
) {
}




