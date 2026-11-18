package com.example.inventario.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record VentaRequestDTO(

        @NotEmpty(message = "La venta debe incluir al menos un producto")
        List<@Valid DetalleVentaRequestDTO> productos
) {}
