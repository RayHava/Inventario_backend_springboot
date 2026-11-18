package com.example.inventario.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

public record DetalleVentaResponseDTO(

        @Schema(example = "1")
        Long productoId,

        @Schema(example = "Teclado Mecánico RGB")
        String nombreProducto,

        @Schema(example = "2")
        Integer cantidad,

        @Schema(example = "1250.50")
        BigDecimal precioUnitario,

        @Schema(example = "2501.00")
        BigDecimal subtotal
) {}
