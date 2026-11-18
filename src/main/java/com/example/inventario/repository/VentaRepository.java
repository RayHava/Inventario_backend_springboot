package com.example.inventario.repository;

import com.example.inventario.model.Venta;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VentaRepository extends JpaRepository<Venta, Long> {

    // @EntityGraph trae la venta junto con sus detalles y productos en una sola consulta
    // (evita el problema N+1 al listar)
    @Override
    @EntityGraph(attributePaths = {"detalles", "detalles.producto"})
    List<Venta> findAll(Sort sort);

    @Override
    @EntityGraph(attributePaths = {"detalles", "detalles.producto"})
    Optional<Venta> findById(Long id);
}
