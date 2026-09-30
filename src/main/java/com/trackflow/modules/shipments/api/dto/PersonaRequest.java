package com.trackflow.modules.shipments.api.dto;

import com.trackflow.modules.shipments.application.DatosPersona;
import com.trackflow.modules.shipments.domain.TipoDocumento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PersonaRequest(
        @NotBlank(message = "el nombre completo es obligatorio") String fullName,
        @NotNull(message = "el tipo de documento es obligatorio (CC, CE, TI, PP o NIT)")
        TipoDocumento documentType,
        @NotBlank(message = "el número de documento es obligatorio") String documentNumber,
        @NotBlank(message = "el teléfono es obligatorio") String phone,
        @NotBlank(message = "la dirección es obligatoria") String address,
        @NotNull(message = "la ciudad es obligatoria: use el id que devuelve GET /api/cities")
        Long cityId) {

    public DatosPersona toDatos() {
        return new DatosPersona(fullName, documentType, documentNumber, phone, address, cityId);
    }
}
