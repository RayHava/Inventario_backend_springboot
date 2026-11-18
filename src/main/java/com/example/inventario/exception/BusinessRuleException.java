package com.example.inventario.exception;

/**
 * Se lanza cuando la petición es válida en formato pero rompe una regla del negocio:
 * stock insuficiente, borrar un producto que ya tiene ventas, etc. -> HTTP 409
 */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
