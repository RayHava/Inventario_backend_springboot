package com.example.inventario.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record VentaResponseDTO(

        @Schema(example = "1")
        Long id,

        @Schema(example = "2026-11-17T12:30:00")
        LocalDateTime fecha,

        @Schema(example = "2501.00")
        BigDecimal total,

        // Productos que se vendieron en esta venta
        List<DetalleVentaResponseDTO> productos
) {}
