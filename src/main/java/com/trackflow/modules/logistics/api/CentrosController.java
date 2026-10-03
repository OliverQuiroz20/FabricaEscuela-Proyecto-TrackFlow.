package com.trackflow.modules.logistics.api;

import com.trackflow.modules.logistics.api.dto.CentroResponse;
import com.trackflow.modules.logistics.application.CatalogoDeCentros;
import com.trackflow.modules.logistics.domain.Centro;
import com.trackflow.modules.logistics.domain.CentroNoEncontradoException;
import com.trackflow.shared.geografia.CatalogoDeCiudades;
import com.trackflow.shared.geografia.Ciudad;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Alimenta el autocompletado de centro al registrar un evento logístico: se escribe
 * parte del nombre (y opcionalmente se filtra por ciudad) y se escoge de la lista, en
 * lugar de teclearlo. Mismo espíritu que CatalogoCiudadesController para ciudades.
 */
@RestController
@RequestMapping("/api/centers")
public class CentrosController {

    private static final int LIMITE = 20;

    private final CatalogoDeCentros centros;
    private final CatalogoDeCiudades ciudades;

    public CentrosController(CatalogoDeCentros centros, CatalogoDeCiudades ciudades) {
        this.centros = centros;
        this.ciudades = ciudades;
    }

    @GetMapping
    public List<CentroResponse> buscar(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "cityId", required = false) Long ciudadId) {
        return centros.buscar(q, ciudadId, LIMITE).stream()
                .map(this::aRespuesta)
                .toList();
    }

    @GetMapping("/{id}")
    public CentroResponse detalle(@PathVariable Long id) {
        Centro centro = centros.porId(id).orElseThrow(() -> new CentroNoEncontradoException(id));
        return aRespuesta(centro);
    }

    private CentroResponse aRespuesta(Centro centro) {
        Ciudad ciudad = ciudades.exigir(centro.getCityId());
        return CentroResponse.from(centro, ciudad);
    }
}
