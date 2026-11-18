package com.example.inventario.service;

import com.example.inventario.dto.ProductoRequestDTO;
import com.example.inventario.dto.ProductoResponseDTO;
import com.example.inventario.exception.BusinessRuleException;
import com.example.inventario.exception.ResourceNotAvailable;
import com.example.inventario.model.Producto;
import com.example.inventario.repository.DetalleVentaRepository;
import com.example.inventario.repository.ProductoRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final DetalleVentaRepository detalleVentaRepository;

    public ProductoService(ProductoRepository productoRepository,
                           DetalleVentaRepository detalleVentaRepository) {
        this.productoRepository = productoRepository;
        this.detalleVentaRepository = detalleVentaRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductoResponseDTO> findAll() {
        return productoRepository.findAll(Sort.by("id"))
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductoResponseDTO obtenerPorId(Long id) {
        Producto producto = buscarEntidadPorId(id);
        return toResponseDTO(producto);
    }

    @Transactional
    public ProductoResponseDTO saveProducto(ProductoRequestDTO requestDTO) {
        Producto nuevoProducto = toEntity(requestDTO);
        Producto productoGuardado = productoRepository.save(nuevoProducto);
        return toResponseDTO(productoGuardado);
    }

    @Transactional
    public ProductoResponseDTO updateProducto(Long id, ProductoRequestDTO requestDTO) {
        Producto productoExistente = buscarEntidadPorId(id);

        productoExistente.setNombre(requestDTO.nombre());
        productoExistente.setPrecio(requestDTO.precio());
        productoExistente.setStock(requestDTO.stock());

        Producto productoActualizado = productoRepository.save(productoExistente);
        return toResponseDTO(productoActualizado);
    }

    @Transactional
    public void deleteProducto(Long id) {
        Producto productoExistente = buscarEntidadPorId(id);

        // No se puede borrar un producto que ya aparece en ventas (rompería el historial)
        if (detalleVentaRepository.existsByProductoId(id)) {
            throw new BusinessRuleException(
                    "No se puede eliminar el producto con ID: " + id + " porque tiene ventas registradas"
            );
        }

        productoRepository.delete(productoExistente);
    }

    // --- Métodos auxiliares privados ---

    private Producto buscarEntidadPorId(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotAvailable(
                        "No se encontró el producto con ID: " + id
                ));
    }

    private Producto toEntity(ProductoRequestDTO dto) {
        Producto producto = new Producto();
        producto.setNombre(dto.nombre());
        producto.setPrecio(dto.precio());
        producto.setStock(dto.stock());
        return producto;
    }

    private ProductoResponseDTO toResponseDTO(Producto producto) {
        return new ProductoResponseDTO(
                producto.getId(),
                producto.getNombre(),
                producto.getPrecio(),
                producto.getStock()
        );
    }
}