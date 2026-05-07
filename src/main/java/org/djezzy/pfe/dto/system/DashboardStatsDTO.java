package org.djezzy.pfe.dto.system;


public record DashboardStatsDTO(
        long totalCvsProcessed,
        double averageMatchScore,
        long pendingHrApprovals
) {
}




