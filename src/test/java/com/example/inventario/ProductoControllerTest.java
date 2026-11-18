package com.example.inventario;

import com.example.inventario.model.Producto;
import com.example.inventario.repository.ProductoRepository;
import com.example.inventario.repository.VentaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class ProductoControllerTest {

    @Autowired private WebApplicationContext context;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private VentaRepository ventaRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        // Base limpia antes de cada test (primero ventas por la llave foránea)
        ventaRepository.deleteAll();
        productoRepository.deleteAll();
    }

    private Producto crearProducto(String nombre, String precio, int stock) {
        Producto p = new Producto();
        p.setNombre(nombre);
        p.setPrecio(new BigDecimal(precio));
        p.setStock(stock);
        return productoRepository.save(p);
    }

    @Test
    @DisplayName("P-01 POST /api/productos crea un producto -> 201")
    void crearProducto() throws Exception {
        String json = """
                {"nombre": "Mouse Inalámbrico", "precio": 350.00, "stock": 20}
                """;

        mockMvc.perform(post("/api/productos").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nombre").value("Mouse Inalámbrico"))
                .andExpect(jsonPath("$.precio").value(350.0))
                .andExpect(jsonPath("$.stock").value(20));

        assertThat(productoRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("P-02 POST /api/productos con datos inválidos -> 400 con detalle por campo")
    void crearProductoInvalido() throws Exception {
        String json = """
                {"nombre": "", "precio": -5, "stock": -1}
                """;

        mockMvc.perform(post("/api/productos").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Error de validación"))
                .andExpect(jsonPath("$.detalles.nombre").exists())
                .andExpect(jsonPath("$.detalles.precio").exists())
                .andExpect(jsonPath("$.detalles.stock").exists());

        assertThat(productoRepository.count()).isZero();
    }

    @Test
    @DisplayName("P-03 POST /api/productos con JSON mal formado -> 400")
    void crearProductoJsonMalFormado() throws Exception {
        mockMvc.perform(post("/api/productos").contentType(MediaType.APPLICATION_JSON).content("{\"nombre\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Cuerpo JSON inválido o mal formado"));
    }

    @Test
    @DisplayName("P-04 GET /api/productos lista los productos -> 200")
    void listarProductos() throws Exception {
        crearProducto("Teclado", "500.00", 10);
        crearProducto("Monitor", "3200.00", 5);

        mockMvc.perform(get("/api/productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].nombre").value("Teclado"));
    }

    @Test
    @DisplayName("P-05 GET /api/productos/{id} existente -> 200")
    void obtenerProducto() throws Exception {
        Producto p = crearProducto("Teclado", "500.00", 10);

        mockMvc.perform(get("/api/productos/{id}", p.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(p.getId()))
                .andExpect(jsonPath("$.nombre").value("Teclado"));
    }

    @Test
    @DisplayName("P-06 GET /api/productos/{id} inexistente -> 404")
    void obtenerProductoInexistente() throws Exception {
        mockMvc.perform(get("/api/productos/{id}", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("No se encontró el producto con ID: 999999"));
    }

    @Test
    @DisplayName("P-07 GET /api/productos/abc (id no numérico) -> 400")
    void obtenerProductoIdInvalido() throws Exception {
        mockMvc.perform(get("/api/productos/abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("P-08 PUT /api/productos/{id} actualiza -> 200")
    void actualizarProducto() throws Exception {
        Producto p = crearProducto("Teclado", "500.00", 10);
        String json = """
                {"nombre": "Teclado RGB", "precio": 650.00, "stock": 8}
                """;

        mockMvc.perform(put("/api/productos/{id}", p.getId()).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Teclado RGB"))
                .andExpect(jsonPath("$.precio").value(650.0))
                .andExpect(jsonPath("$.stock").value(8));

        Producto enBd = productoRepository.findById(p.getId()).orElseThrow();
        assertThat(enBd.getNombre()).isEqualTo("Teclado RGB");
        assertThat(enBd.getStock()).isEqualTo(8);
    }

    @Test
    @DisplayName("P-09 PUT /api/productos/{id} inexistente -> 404")
    void actualizarProductoInexistente() throws Exception {
        String json = """
                {"nombre": "Teclado RGB", "precio": 650.00, "stock": 8}
                """;

        mockMvc.perform(put("/api/productos/{id}", 999999).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("P-10 DELETE /api/productos/{id} -> 204 y después ya no existe")
    void eliminarProducto() throws Exception {
        Producto p = crearProducto("Teclado", "500.00", 10);

        mockMvc.perform(delete("/api/productos/{id}", p.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/productos/{id}", p.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("P-11 DELETE /api/productos/{id} con ventas registradas -> 409")
    void eliminarProductoConVentas() throws Exception {
        Producto p = crearProducto("Teclado", "500.00", 10);
        String venta = """
                {"productos": [{"productoId": %d, "cantidad": 1}]}
                """.formatted(p.getId());
        mockMvc.perform(post("/api/ventas").contentType(MediaType.APPLICATION_JSON).content(venta))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/productos/{id}", p.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        assertThat(productoRepository.existsById(p.getId())).isTrue();
    }
}
