package org.djezzy.pfe.event;

public record CvUploadedEvent(
        Long cvId,
        Long evaluationId
) {
}

