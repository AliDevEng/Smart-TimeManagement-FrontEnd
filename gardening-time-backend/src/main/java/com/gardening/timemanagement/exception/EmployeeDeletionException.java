package com.gardening.timemanagement.exception;

/**
 * Exception som kastas när en medarbetare inte kan tas bort eller inaktiveras.
 *
 * Denna exception används när:
 * - Medarbetaren har framtida arbetsdagar registrerade
 * - Medarbetaren har pågående uppdrag som inte kan avbrytas
 * - Affärsregler förhindrar borttagning/inaktivering
 *
 * Kommer att mappas till HTTP 409 Conflict i Controller-lagret.
 */
public class EmployeeDeletionException extends RuntimeException {

    public EmployeeDeletionException(String message) {
        super(message);
    }

    public EmployeeDeletionException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Bekvämlighets-konstruktor för medarbetare med aktiva kopplingar.
     */
    public EmployeeDeletionException(String employeeName, int activeConnections, String connectionType) {
        super("Medarbetaren '" + employeeName + "' kan inte tas bort eftersom " +
                "den har " + activeConnections + " aktiva " + connectionType);
    }
}