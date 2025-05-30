package com.gardening.timemanagement.exception;


/**
 * Exception som kastas när en medarbetare inte kan hittas.
 *
 * Denna exception används när:
 * - Vi söker efter en medarbetare med ett ID som inte existerar
 * - Vi försöker hämta en medarbetare som har tagits bort
 *
 * Kommer att mappas till HTTP 404 Not Found i Controller-lagret.
 */

public class EmployeeNotFoundException extends RuntimeException {

    public EmployeeNotFoundException(String message) {
        super(message);
    }

    public EmployeeNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public EmployeeNotFoundException(Long employeeId) {
        super("Medarbetare med ID " + employeeId + " finns inte");
    }

}
