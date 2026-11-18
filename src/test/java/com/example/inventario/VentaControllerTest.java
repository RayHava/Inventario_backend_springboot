package com.example.inventario;

import com.example.inventario.model.Producto;
import com.example.inventario.repository.ProductoRepository;
import com.example.inventario.repository.VentaRepository;
import com.jayway.jsonpath.JsonPath;
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
class VentaControllerTest {

    @Autowired private WebApplicationContext context;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private VentaRepository ventaRepository;

    private MockMvc mockMvc;
    private Producto teclado;   // precio 500.00, stock 10
    private Producto mouse;     // precio 250.50, stock 3

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        ventaRepository.deleteAll();
        productoRepository.deleteAll();
        teclado = crearProducto("Teclado", "500.00", 10);
        mouse = crearProducto("Mouse", "250.50", 3);
    }

    private Producto crearProducto(String nombre, String precio, int stock) {
        Producto p = new Producto();
        p.setNombre(nombre);
        p.setPrecio(new BigDecimal(precio));
        p.setStock(stock);
        return productoRepository.save(p);
    }

    private int stockDe(Producto p) {
        return productoRepository.findById(p.getId()).orElseThrow().getStock();
    }

    /** Registra una venta y devuelve su id */
    private long registrarVenta(String json) throws Exception {
        String respuesta = mockMvc.perform(post("/api/ventas").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(respuesta, "$.id")).longValue();
    }

    @Test
    @DisplayName("V-01 POST /api/ventas registra la venta, muestra productos vendidos y reduce stock -> 201")
    void crearVenta() throws Exception {
        String json = """
                {"productos": [
                    {"productoId": %d, "cantidad": 2},
                    {"productoId": %d, "cantidad": 1}
                ]}
                """.formatted(teclado.getId(), mouse.getId());

        mockMvc.perform(post("/api/ventas").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.fecha").exists())
                .andExpect(jsonPath("$.total").value(1250.5))           // 2*500 + 1*250.50
                .andExpect(jsonPath("$.productos", hasSize(2)))
                .andExpect(jsonPath("$.productos[0].nombreProducto").value("Teclado"))
                .andExpect(jsonPath("$.productos[0].cantidad").value(2))
                .andExpect(jsonPath("$.productos[0].precioUnitario").value(500.0))
                .andExpect(jsonPath("$.productos[0].subtotal").value(1000.0))
                .andExpect(jsonPath("$.productos[1].nombreProducto").value("Mouse"));

        assertThat(stockDe(teclado)).isEqualTo(8);
        assertThat(stockDe(mouse)).isEqualTo(2);
    }

    @Test
    @DisplayName("V-02 POST /api/ventas con stock insuficiente -> 409 y el stock no cambia")
    void crearVentaSinStock() throws Exception {
        String json = """
                {"productos": [{"productoId": %d, "cantidad": 5}]}
                """.formatted(mouse.getId());

        mockMvc.perform(post("/api/ventas").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Stock insuficiente para 'Mouse'. Disponible: 3, solicitado: 5"));

        assertThat(stockDe(mouse)).isEqualTo(3);
        assertThat(ventaRepository.count()).isZero();
    }

    @Test
    @DisplayName("V-03 La venta es atómica: si un producto no tiene stock, no se descuenta ninguno")
    void ventaAtomica() throws Exception {
        String json = """
                {"productos": [
                    {"productoId": %d, "cantidad": 2},
                    {"productoId": %d, "cantidad": 99}
                ]}
                """.formatted(teclado.getId(), mouse.getId());

        mockMvc.perform(post("/api/ventas").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isConflict());

        assertThat(stockDe(teclado)).isEqualTo(10);   // no se descontó
        assertThat(stockDe(mouse)).isEqualTo(3);
        assertThat(ventaRepository.count()).isZero();
    }

    @Test
    @DisplayName("V-04 POST /api/ventas con producto inexistente -> 404")
    void crearVentaProductoInexistente() throws Exception {
        String json = """
                {"productos": [{"productoId": 999999, "cantidad": 1}]}
                """;

        mockMvc.perform(post("/api/ventas").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("No se encontró el producto con ID: 999999"));
    }

    @Test
    @DisplayName("V-05 POST /api/ventas sin productos -> 400")
    void crearVentaVacia() throws Exception {
        mockMvc.perform(post("/api/ventas").contentType(MediaType.APPLICATION_JSON).content("{\"productos\": []}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.productos").exists());
    }

    @Test
    @DisplayName("V-06 POST /api/ventas con cantidad 0 o sin productoId -> 400")
    void crearVentaCantidadInvalida() throws Exception {
        String json = """
                {"productos": [{"productoId": null, "cantidad": 0}]}
                """;

        mockMvc.perform(post("/api/ventas").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Error de validación"));

        assertThat(ventaRepository.count()).isZero();
    }

    @Test
    @DisplayName("V-07 Producto repetido en la misma venta se agrupa en un solo renglón")
    void productoRepetidoSeAgrupa() throws Exception {
        String json = """
                {"productos": [
                    {"productoId": %d, "cantidad": 1},
                    {"productoId": %d, "cantidad": 2}
                ]}
                """.formatted(teclado.getId(), teclado.getId());

        mockMvc.perform(post("/api/ventas").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productos", hasSize(1)))
                .andExpect(jsonPath("$.productos[0].cantidad").value(3))
                .andExpect(jsonPath("$.total").value(1500.0));

        assertThat(stockDe(teclado)).isEqualTo(7);
    }

    @Test
    @DisplayName("V-08 GET /api/ventas lista las ventas con sus productos -> 200")
    void listarVentas() throws Exception {
        registrarVenta("""
                {"productos": [{"productoId": %d, "cantidad": 1}]}
                """.formatted(teclado.getId()));
        registrarVenta("""
                {"productos": [{"productoId": %d, "cantidad": 1}]}
                """.formatted(mouse.getId()));

        mockMvc.perform(get("/api/ventas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].productos[0].nombreProducto").value("Mouse"))   // más reciente primero
                .andExpect(jsonPath("$[1].productos[0].nombreProducto").value("Teclado"));
    }

    @Test
    @DisplayName("V-09 GET /api/ventas/{id} -> 200; inexistente -> 404")
    void obtenerVenta() throws Exception {
        long id = registrarVenta("""
                {"productos": [{"productoId": %d, "cantidad": 2}]}
                """.formatted(teclado.getId()));

        mockMvc.perform(get("/api/ventas/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.productos[0].cantidad").value(2));

        mockMvc.perform(get("/api/ventas/{id}", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("No se encontró la venta con ID: 999999"));
    }

    @Test
    @DisplayName("V-10 PUT /api/ventas/{id} devuelve el stock anterior y descuenta el nuevo -> 200")
    void actualizarVenta() throws Exception {
        long id = registrarVenta("""
                {"productos": [{"productoId": %d, "cantidad": 4}]}
                """.formatted(teclado.getId()));
        assertThat(stockDe(teclado)).isEqualTo(6);

        String nueva = """
                {"productos": [
                    {"productoId": %d, "cantidad": 1},
                    {"productoId": %d, "cantidad": 2}
                ]}
                """.formatted(teclado.getId(), mouse.getId());

        mockMvc.perform(put("/api/ventas/{id}", id).contentType(MediaType.APPLICATION_JSON).content(nueva))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productos", hasSize(2)))
                .andExpect(jsonPath("$.total").value(1001.0));   // 500 + 2*250.50

        assertThat(stockDe(teclado)).isEqualTo(9);   // 10 - 1
        assertThat(stockDe(mouse)).isEqualTo(1);     // 3 - 2
    }

    @Test
    @DisplayName("V-11 PUT /api/ventas/{id} inexistente -> 404")
    void actualizarVentaInexistente() throws Exception {
        String json = """
                {"productos": [{"productoId": %d, "cantidad": 1}]}
                """.formatted(teclado.getId());

        mockMvc.perform(put("/api/ventas/{id}", 999999).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isNotFound());

        assertThat(stockDe(teclado)).isEqualTo(10);
    }

    @Test
    @DisplayName("V-12 DELETE /api/ventas/{id} cancela la venta y regresa el stock -> 204")
    void eliminarVenta() throws Exception {
        long id = registrarVenta("""
                {"productos": [{"productoId": %d, "cantidad": 3}]}
                """.formatted(mouse.getId()));
        assertThat(stockDe(mouse)).isZero();

        mockMvc.perform(delete("/api/ventas/{id}", id))
                .andExpect(status().isNoContent());

        assertThat(stockDe(mouse)).isEqualTo(3);
        assertThat(ventaRepository.existsById(id)).isFalse();
    }

    @Test
    @DisplayName("V-13 DELETE /api/ventas/{id} inexistente -> 404")
    void eliminarVentaInexistente() throws Exception {
        mockMvc.perform(delete("/api/ventas/{id}", 999999))
                .andExpect(status().isNotFound());
    }
}
