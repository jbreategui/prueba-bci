package com.brr.customers.api.exception;

public class EmailAlreadyRegisteredException extends RuntimeException {
    public EmailAlreadyRegisteredException() {
        super("El correo ya registrado");
    }
}
