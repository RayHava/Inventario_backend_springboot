package com.example.inventario.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record DetalleVentaRequestDTO(

        @NotNull(message = "El ID del producto es obligatorio")
        @Schema(example = "1")
        Long productoId,

        @NotNull(message = "La cantidad es obligatoria")
        @Positive(message = "La cantidad debe ser mayor a 0")
        @Schema(example = "2")
        Integer cantidad
) {}
