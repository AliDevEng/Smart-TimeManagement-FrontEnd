// src/main/java/com/gardening/timemanagement/exception/DuplicateEmployeeException.java
package com.gardening.timemanagement.exception;

/**
 * Exception som kastas när en medarbetare med samma data redan existerar.
 *
 * Denna exception används när:
 * - En medarbetare med samma namn redan finns
 * - En medarbetare med samma telefonnummer redan finns
 * - Vid uppdatering: nya värden går till konflikt med befintlig medarbetare
 *
 * Kommer att mappas till HTTP 409 Conflict i Controller-lagret.
 */

public class DuplicateEmployeeException extends RuntimeException {

    public DuplicateEmployeeException(String message) {
        super(message);
    }

    public DuplicateEmployeeException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Bekvämlighets-konstruktor för namn-konflikter.
     */
    public DuplicateEmployeeException(String fieldName, String fieldValue) {
        super("En medarbetare med " + fieldName + " '" + fieldValue + "' finns redan");
    }
}