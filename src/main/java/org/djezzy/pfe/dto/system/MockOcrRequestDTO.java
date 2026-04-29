package org.djezzy.pfe.dto.system;

import org.djezzy.pfe.dto.auth.*;
import org.djezzy.pfe.dto.job.*;
import org.djezzy.pfe.dto.evaluation.*;
import org.djezzy.pfe.dto.system.*;

import jakarta.validation.constraints.NotBlank;

public record MockOcrRequestDTO(
        Integer page,
        @NotBlank String imageBase64
) {
}




