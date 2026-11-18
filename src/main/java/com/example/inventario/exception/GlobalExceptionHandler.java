package com.example.inventario.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 1. Errores de validación (@NotBlank, @Positive, etc.) -> HTTP 400
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> manejarValidaciones(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        Map<String, String> erroresCampos = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                erroresCampos.put(error.getField(), error.getDefaultMessage())
        );

        ErrorResponse respuesta = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Error de validación",
                null,
                request.getRequestURI(),
                erroresCampos
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(respuesta);
    }

    // 2. JSON mal formado o tipos de dato incorrectos -> HTTP 400
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> manejarJsonInvalido(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {

        // Solo la causa raíz (mucho más limpia que el mensaje completo de Jackson)
        String causaRaiz = ex.getMostSpecificCause().getMessage();

        ErrorResponse respuesta = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Cuerpo JSON inválido o mal formado",
                causaRaiz,
                request.getRequestURI(),
                null
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(respuesta);
    }

    // 3. Recursos inexistentes -> HTTP 404
    @ExceptionHandler(ResourceNotAvailable.class)
    public ResponseEntity<ErrorResponse> manejarNoEncontrado(
            ResourceNotAvailable ex,
            HttpServletRequest request) {

        ErrorResponse respuesta = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value(),
                "Recurso no encontrado",
                ex.getMessage(),
                request.getRequestURI(),
                null
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(respuesta);
    }

    // 4. Reglas de negocio (stock insuficiente, producto con ventas) -> HTTP 409
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponse> manejarReglaNegocio(
            BusinessRuleException ex,
            HttpServletRequest request) {

        return construir(HttpStatus.CONFLICT, "Conflicto con el estado del recurso", ex.getMessage(), request);
    }

    // 5. Violación de llaves foráneas / restricciones de la BD (respaldo) -> HTTP 409
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> manejarIntegridad(
            DataIntegrityViolationException ex,
            HttpServletRequest request) {

        return construir(HttpStatus.CONFLICT, "Conflicto de integridad de datos",
                "La operación viola una restricción de la base de datos", request);
    }

    // 6. Tipo de dato inválido en la URL (ej. /api/productos/abc) -> HTTP 400
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> manejarTipoInvalido(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {

        return construir(HttpStatus.BAD_REQUEST, "Parámetro inválido",
                "El valor '" + ex.getValue() + "' no es válido para '" + ex.getName() + "'", request);
    }

    // 7. Método HTTP no soportado (ej. PATCH) -> HTTP 405
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> manejarMetodoNoSoportado(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request) {

        return construir(HttpStatus.METHOD_NOT_ALLOWED, "Método no permitido", ex.getMessage(), request);
    }

    // 8. Ruta inexistente -> HTTP 404 (sin esto caería en el 500 genérico)
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> manejarRutaInexistente(
            NoResourceFoundException ex,
            HttpServletRequest request) {

        return construir(HttpStatus.NOT_FOUND, "Ruta no encontrada", null, request);
    }

    // 9. Cualquier otro error no controlado -> HTTP 500 (con log, sin filtrar stacktrace al cliente)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> manejarGenerico(
            Exception ex,
            HttpServletRequest request) {

        log.error("Error no controlado en {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        ErrorResponse respuesta = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Error interno del servidor",
                null,
                request.getRequestURI(),
                null
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(respuesta);
    }

    private ResponseEntity<ErrorResponse> construir(HttpStatus status, String error,
                                                    String mensaje, HttpServletRequest request) {
        ErrorResponse respuesta = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                error,
                mensaje,
                request.getRequestURI(),
                null
        );
        return ResponseEntity.status(status).body(respuesta);
    }
}
