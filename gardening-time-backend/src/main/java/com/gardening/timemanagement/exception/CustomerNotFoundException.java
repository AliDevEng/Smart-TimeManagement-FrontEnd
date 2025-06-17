// src/main/java/com/gardening/timemanagement/exception/CustomerNotFoundException.java
package com.gardening.timemanagement.exception;


public class CustomerNotFoundException extends RuntimeException {


    public CustomerNotFoundException(String message) {
        super(message);
    }


    public CustomerNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }


    public CustomerNotFoundException(Long customerId) {
        super("Kund med ID " + customerId + " finns inte");
    }


    public CustomerNotFoundException(String customerName, boolean isNameBased) {
        super("Kund med namnet '" + customerName + "' finns inte");
    }
}