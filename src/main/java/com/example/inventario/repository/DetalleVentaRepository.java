package com.example.inventario.repository;

import com.example.inventario.model.DetalleVenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DetalleVentaRepository extends JpaRepository<DetalleVenta, Long> {

    // ¿El producto aparece en alguna venta? (para no permitir borrarlo)
    boolean existsByProductoId(Long productoId);
}
