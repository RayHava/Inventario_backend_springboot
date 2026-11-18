package com.example.inventario.repository;

//Spring Data JPA traduce las operaciones al lenguaje de PostgrSQL

import com.example.inventario.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductoRepository extends JpaRepository<Producto,Long> {
}
