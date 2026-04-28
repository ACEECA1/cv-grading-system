package org.djezzy.pfe.dto;

import jakarta.validation.constraints.NotBlank;

public record MockOcrRequestDTO(
        Integer page,
        @NotBlank String imageBase64
) {
}
