package com.gardening.timemanagement.exception;

/**
 * Exception som kastas när arbetstidsregistrering är ogiltig.
 *
 * Denna exception används när:
 * - Sluttid är före starttid
 * - Lunchtid är längre än total arbetstid
 * - Arbetstid är orimligt lång eller kort
 * - Medarbetare är redan registrerad för samma arbetsdag
 * - Inaktiv medarbetare försöker registrera arbetstid
 *
 * Kommer att mappas till HTTP 400 Bad Request i Controller-lagret.
 */

public class InvalidWorkTimeException extends RuntimeException {

    public InvalidWorkTimeException(String message) {
        super(message);
    }

    public InvalidWorkTimeException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Bekvämlighets-konstruktor för tidsvaliderings-fel.
     */
    public InvalidWorkTimeException(String field, String actualValue, String expectedCondition) {
        super("Ogiltigt värde för " + field + ": '" + actualValue +
                "'. Förväntat: " + expectedCondition);
    }
}