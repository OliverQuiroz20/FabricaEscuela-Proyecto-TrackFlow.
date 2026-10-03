package com.trackflow.bootstrap;

import com.trackflow.shared.geografia.CatalogoDeCiudades;
import com.trackflow.shared.geografia.Ciudad;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Alimenta el autocompletado de ciudad al registrar un envío: se escribe parte del
 * nombre y se escoge de la lista, en lugar de teclearlo y que "Bogotá" y "Bogota
 * D.C." acaben siendo dos ciudades.
 *
 * Vive en bootstrap por la misma razón que la reconstrucción de proyecciones: el
 * catálogo es dato de referencia transversal y ningún módulo de negocio es su dueño.
 */
@RestController
@RequestMapping("/api/cities")
public class CatalogoCiudadesController {

    private static final int LIMITE = 10;

    private final CatalogoDeCiudades ciudades;

    public CatalogoCiudadesController(CatalogoDeCiudades ciudades) {
        this.ciudades = ciudades;
    }

    public record CiudadResponse(Long id, String name, String department, String label) {

        static CiudadResponse from(Ciudad ciudad) {
            return new CiudadResponse(ciudad.id(), ciudad.nombre(), ciudad.departamento(), ciudad.etiqueta());
        }
    }

    @GetMapping
    public List<CiudadResponse> buscar(
            @RequestParam("q") @NotBlank(message = "indique el texto a buscar") String q) {
        return ciudades.buscar(q, LIMITE).stream()
                .map(CiudadResponse::from)
                .toList();
    }
}
