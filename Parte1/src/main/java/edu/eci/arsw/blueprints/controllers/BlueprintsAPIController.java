package edu.eci.arsw.blueprints.controllers;

import edu.eci.arsw.blueprints.dto.ApiResponse;
import edu.eci.arsw.blueprints.model.Blueprint;
import edu.eci.arsw.blueprints.model.Point;
import edu.eci.arsw.blueprints.persistence.BlueprintNotFoundException;
import edu.eci.arsw.blueprints.persistence.BlueprintPersistenceException;
import edu.eci.arsw.blueprints.services.BlueprintsServices;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/blueprints")
@Tag(name = "Blueprints", description = "Consulta y registro de planos (blueprints) y sus puntos")
public class BlueprintsAPIController {

    private final BlueprintsServices services;

    public BlueprintsAPIController(BlueprintsServices services) { this.services = services; }

    @Operation(summary = "Listar todos los blueprints")
    @ApiResponses(@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Consulta exitosa"))
    @GetMapping
    public ResponseEntity<ApiResponse<Set<Blueprint>>> getAll() {
        return respond(HttpStatus.OK, services.getAllBlueprints());
    }

    @Operation(summary = "Listar los blueprints de un autor")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Consulta exitosa"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "El autor no tiene blueprints")
    })
    @GetMapping("/{author}")
    public ResponseEntity<ApiResponse<Set<Blueprint>>> byAuthor(
            @Parameter(description = "Autor del blueprint", example = "john") @PathVariable String author)
            throws BlueprintNotFoundException {
        return respond(HttpStatus.OK, services.getBlueprintsByAuthor(author));
    }

    @Operation(summary = "Obtener un blueprint por autor y nombre",
            description = "El resultado pasa por el filtro activo (identity, redundancy o undersampling).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Consulta exitosa"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "El blueprint no existe")
    })
    @GetMapping("/{author}/{bpname}")
    public ResponseEntity<ApiResponse<Blueprint>> byAuthorAndName(
            @Parameter(description = "Autor del blueprint", example = "john") @PathVariable String author,
            @Parameter(description = "Nombre del blueprint", example = "house") @PathVariable String bpname)
            throws BlueprintNotFoundException {
        return respond(HttpStatus.OK, services.getBlueprint(author, bpname));
    }

    @Operation(summary = "Crear un blueprint")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Blueprint creado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos invalidos o el blueprint ya existe")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<Blueprint>> add(@Valid @RequestBody NewBlueprintRequest req)
            throws BlueprintPersistenceException {
        Blueprint bp = new Blueprint(req.author(), req.name(), req.points());
        services.addNewBlueprint(bp);
        return respond(HttpStatus.CREATED, bp);
    }

    @Operation(summary = "Agregar un punto a un blueprint existente")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "202", description = "Punto agregado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "El blueprint no existe")
    })
    @PutMapping("/{author}/{bpname}/points")
    public ResponseEntity<ApiResponse<Point>> addPoint(
            @Parameter(description = "Autor del blueprint", example = "john") @PathVariable String author,
            @Parameter(description = "Nombre del blueprint", example = "house") @PathVariable String bpname,
            @RequestBody Point point) throws BlueprintNotFoundException {
        services.addPoint(author, bpname, point.x(), point.y());
        return respond(HttpStatus.ACCEPTED, point);
    }

    private static <T> ResponseEntity<ApiResponse<T>> respond(HttpStatus status, T data) {
        return ResponseEntity.status(status).body(ApiResponse.of(status, data));
    }

    public record NewBlueprintRequest(
            @NotBlank String author,
            @NotBlank String name,
            @Valid List<Point> points
    ) { }
}
