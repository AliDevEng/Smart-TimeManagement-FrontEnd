package com.gardening.timemanagement.exception;

public class DuplicateCustomerException extends RuntimeException {

    public DuplicateCustomerException(String message) {
        super(message);
    }

    // Denna konstruktor är användbar när dubblette konflikten upptäcks som
    // ett resultat av en databasoperation som själv kastade en exception.

    public DuplicateCustomerException(String message, Throwable cause) {
        super(message, cause);
    }

    // Exempel på användning:
    //     throw new DuplicateCustomerException("telefonnummer", "070-1234567");
    //     Resulterar i: "En kund med telefonnummer '070-1234567' finns redan"

    public DuplicateCustomerException(String fieldName, String fieldValue) {
        super("En kund med " + fieldName + " '" + fieldValue + "' finns redan");
    }

    // Denna konstruktor är användbar när konflikten involverar flera fält
    // eller när du vill ge extra kontext om vad användaren bör göra för att lösa konflikten.

    public DuplicateCustomerException(String fieldName, String fieldValue, String additionalContext) {
        super("En kund med " + fieldName + " '" + fieldValue + "' finns redan. " + additionalContext);
    }


    // Denna konstruktor är speciellt designad för situationer där en
    // uppdateringsoperation skulle skapa en konflikt med en annan befintlig
    // kund. Den skapar ett meddelande som tydligt förklarar att konflikten
    // uppstår med en annan kund, inte samma kund som uppdateras.

    public DuplicateCustomerException(Long customerId, String conflictingField, String conflictingValue) {
        super("Kunde inte uppdatera kund med ID " + customerId +
                ": En annan kund med " + conflictingField + " '" + conflictingValue + "' finns redan");
    }
}
