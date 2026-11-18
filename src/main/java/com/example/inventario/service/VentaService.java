package com.example.inventario.service;

import com.example.inventario.dto.DetalleVentaRequestDTO;
import com.example.inventario.dto.DetalleVentaResponseDTO;
import com.example.inventario.dto.VentaRequestDTO;
import com.example.inventario.dto.VentaResponseDTO;
import com.example.inventario.exception.BusinessRuleException;
import com.example.inventario.exception.ResourceNotAvailable;
import com.example.inventario.model.DetalleVenta;
import com.example.inventario.model.Producto;
import com.example.inventario.model.Venta;
import com.example.inventario.repository.ProductoRepository;
import com.example.inventario.repository.VentaRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class VentaService {

    private final VentaRepository ventaRepository;
    private final ProductoRepository productoRepository;

    public VentaService(VentaRepository ventaRepository, ProductoRepository productoRepository) {
        this.ventaRepository = ventaRepository;
        this.productoRepository = productoRepository;
    }

    @Transactional(readOnly = true)
    public List<VentaResponseDTO> findAll() {
        return ventaRepository.findAll(Sort.by(Sort.Direction.DESC, "id"))
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public VentaResponseDTO obtenerPorId(Long id) {
        return toResponseDTO(buscarEntidadPorId(id));
    }

    /**
     * Registra una venta: valida stock, descuenta el stock de cada producto
     * y guarda la venta con sus renglones. Todo en una sola transacción:
     * si un producto falla, no se descuenta nada.
     */
    @Transactional
    public VentaResponseDTO saveVenta(VentaRequestDTO requestDTO) {
        Venta venta = new Venta();
        venta.setFecha(LocalDateTime.now());

        aplicarDetalles(venta, requestDTO);

        Venta ventaGuardada = ventaRepository.save(venta);
        return toResponseDTO(ventaGuardada);
    }

    /**
     * Modifica los productos de una venta: primero devuelve al inventario
     * lo que se había vendido y después aplica los nuevos renglones.
     */
    @Transactional
    public VentaResponseDTO updateVenta(Long id, VentaRequestDTO requestDTO) {
        Venta venta = buscarEntidadPorId(id);

        devolverStock(venta);
        venta.limpiarDetalles();

        aplicarDetalles(venta, requestDTO);

        Venta ventaActualizada = ventaRepository.save(venta);
        return toResponseDTO(ventaActualizada);
    }

    /**
     * Elimina (cancela) una venta y regresa el stock de sus productos.
     */
    @Transactional
    public void deleteVenta(Long id) {
        Venta venta = buscarEntidadPorId(id);
        devolverStock(venta);
        ventaRepository.delete(venta);
    }

    // --- Métodos auxiliares privados ---

    private void aplicarDetalles(Venta venta, VentaRequestDTO requestDTO) {
        // Si el mismo producto viene repetido en la petición, se suman sus cantidades
        Map<Long, Integer> cantidadesPorProducto = new LinkedHashMap<>();
        for (DetalleVentaRequestDTO item : requestDTO.productos()) {
            cantidadesPorProducto.merge(item.productoId(), item.cantidad(), Integer::sum);
        }

        BigDecimal total = BigDecimal.ZERO;

        for (Map.Entry<Long, Integer> entry : cantidadesPorProducto.entrySet()) {
            Long productoId = entry.getKey();
            int cantidad = entry.getValue();

            Producto producto = productoRepository.findById(productoId)
                    .orElseThrow(() -> new ResourceNotAvailable(
                            "No se encontró el producto con ID: " + productoId
                    ));

            int stockActual = producto.getStock() == null ? 0 : producto.getStock();
            if (stockActual < cantidad) {
                throw new BusinessRuleException(
                        "Stock insuficiente para '" + producto.getNombre() + "'. Disponible: "
                                + stockActual + ", solicitado: " + cantidad
                );
            }

            // Reducir stock
            producto.setStock(stockActual - cantidad);

            BigDecimal subtotal = producto.getPrecio().multiply(BigDecimal.valueOf(cantidad));

            DetalleVenta detalle = new DetalleVenta();
            detalle.setProducto(producto);
            detalle.setCantidad(cantidad);
            detalle.setPrecioUnitario(producto.getPrecio());
            detalle.setSubtotal(subtotal);
            venta.agregarDetalle(detalle);

            total = total.add(subtotal);
        }

        venta.setTotal(total);
    }

    private void devolverStock(Venta venta) {
        for (DetalleVenta detalle : venta.getDetalles()) {
            Producto producto = detalle.getProducto();
            int stockActual = producto.getStock() == null ? 0 : producto.getStock();
            producto.setStock(stockActual + detalle.getCantidad());
        }
    }

    private Venta buscarEntidadPorId(Long id) {
        return ventaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotAvailable(
                        "No se encontró la venta con ID: " + id
                ));
    }

    private VentaResponseDTO toResponseDTO(Venta venta) {
        List<DetalleVentaResponseDTO> productos = venta.getDetalles()
                .stream()
                .map(d -> new DetalleVentaResponseDTO(
                        d.getProducto().getId(),
                        d.getProducto().getNombre(),
                        d.getCantidad(),
                        d.getPrecioUnitario(),
                        d.getSubtotal()
                ))
                .toList();

        return new VentaResponseDTO(
                venta.getId(),
                venta.getFecha(),
                venta.getTotal(),
                productos
        );
    }
}
