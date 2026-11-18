package com.example.inventario.controller;

import com.example.inventario.dto.ErrorResponseDTO;
import com.example.inventario.dto.VentaRequestDTO;
import com.example.inventario.dto.VentaResponseDTO;
import com.example.inventario.service.VentaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ventas")
@Tag(name = "Ventas", description = "Registro de ventas: muestra los productos vendidos y descuenta el stock")
public class VentaController {

    private final VentaService ventaService;

    public VentaController(VentaService ventaService) {
        this.ventaService = ventaService;
    }

    //LISTAR TODAS LAS VENTAS (con sus productos vendidos)
    @Operation(summary = "Listar todas las ventas",
            description = "Devuelve las ventas (más recientes primero) con el detalle de productos vendidos.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de ventas",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = VentaResponseDTO.class))))
    })
    @GetMapping
    public ResponseEntity<List<VentaResponseDTO>> listarVentas() {
        return ResponseEntity.ok(ventaService.findAll());
    }

    //OBTENER UNA VENTA POR SU id
    @Operation(summary = "Obtener venta por ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Venta encontrada",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = VentaResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "No existe la venta",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<VentaResponseDTO> obtenerVentaPorId(@PathVariable Long id) {
        return ResponseEntity.ok(ventaService.obtenerPorId(id));
    }

    //REGISTRAR UNA VENTA (descuenta stock)
    @Operation(summary = "Registrar una venta",
            description = "Recibe la lista de productos y cantidades, valida el stock, lo descuenta y guarda la venta.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Venta registrada",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = VentaResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos (lista vacía, cantidad <= 0, JSON mal formado)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Algún producto no existe",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "Stock insuficiente",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PostMapping
    public ResponseEntity<VentaResponseDTO> crearVenta(@Valid @RequestBody VentaRequestDTO venta) {
        return new ResponseEntity<>(ventaService.saveVenta(venta), HttpStatus.CREATED);
    }

    //MODIFICAR UNA VENTA (regresa el stock anterior y aplica el nuevo)
    @Operation(summary = "Actualizar una venta",
            description = "Reemplaza los productos de la venta. Devuelve al inventario lo vendido antes y descuenta lo nuevo.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Venta actualizada",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = VentaResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "No existe la venta o algún producto",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "Stock insuficiente",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<VentaResponseDTO> actualizarVenta(@PathVariable Long id,
                                                            @Valid @RequestBody VentaRequestDTO venta) {
        return ResponseEntity.ok(ventaService.updateVenta(id, venta));
    }

    //ELIMINAR (CANCELAR) UNA VENTA (regresa el stock)
    @Operation(summary = "Eliminar una venta", description = "Cancela la venta y devuelve el stock de sus productos.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Venta eliminada", content = @Content),
            @ApiResponse(responseCode = "404", description = "No existe la venta",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarVenta(@PathVariable Long id) {
        ventaService.deleteVenta(id);
        return ResponseEntity.noContent().build();
    }
}
