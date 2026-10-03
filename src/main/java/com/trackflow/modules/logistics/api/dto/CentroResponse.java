package com.trackflow.modules.logistics.api.dto;

import com.trackflow.modules.logistics.domain.Centro;
import com.trackflow.modules.logistics.domain.TipoCentro;
import com.trackflow.shared.geografia.Ciudad;

public record CentroResponse(Long id, String name, String type, Long cityId, String cityName,
        String label) {

    public static CentroResponse from(Centro centro, Ciudad ciudad) {
        return new CentroResponse(
                centro.getId(),
                centro.getName(),
                tipoEnIngles(centro.getType()),
                ciudad.id(),
                ciudad.etiqueta(),
                centro.getName() + " · " + ciudad.etiqueta());
    }

    /** El enum se persiste en español; el contrato de la API va en inglés. */
    static String tipoEnIngles(TipoCentro tipo) {
        return switch (tipo) {
            case CENTRO_DISTRIBUCION -> "DISTRIBUTION_CENTER";
            case PUNTO_RECOLECCION -> "PICKUP_POINT";
            case OFICINA -> "OFFICE";
        };
    }
}
