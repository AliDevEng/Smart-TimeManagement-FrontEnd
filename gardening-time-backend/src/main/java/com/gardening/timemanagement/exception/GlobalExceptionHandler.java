package com.gardening.timemanagement.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.validation.FieldError;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;


// @RestControllerAdvice berättar för Spring att denna klass ska hantera
// exceptions från alla @RestController-klasser i applikationen. Det är
// som att sätta upp en "säkerhetsnät" som fångar alla fel innan de når slutanvändaren som mystiska 500-fel.

@RestControllerAdvice
public class GlobalExceptionHandler {


    @ExceptionHandler(CustomerNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCustomerNotFound(CustomerNotFoundException ex) {
        // Skapa en strukturerad felrespons med all nödvändig information
        ErrorResponse errorResponse = new ErrorResponse(
                "CUSTOMER_NOT_FOUND",                    // Unik felkod för programmatisk hantering
                ex.getMessage(),                          // Användarvänligt meddelande från exception:en
                HttpStatus.NOT_FOUND.value(),            // HTTP-statuskod (404)
                LocalDateTime.now()                      // Tidsstämpel för loggning och debugging
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }




    @ExceptionHandler(DuplicateCustomerException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateCustomer(DuplicateCustomerException ex) {
        ErrorResponse errorResponse = new ErrorResponse(
                "DUPLICATE_CUSTOMER",                    // Felkod som frontend kan använda för att visa specifika meddelanden
                ex.getMessage(),                          // Detaljerat meddelande från vår exception
                HttpStatus.CONFLICT.value(),             // HTTP-statuskod (409)
                LocalDateTime.now()
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }




    @ExceptionHandler(CustomerDeletionException.class)
    public ResponseEntity<ErrorResponse> handleCustomerDeletion(CustomerDeletionException ex) {
        ErrorResponse errorResponse = new ErrorResponse(
                "CUSTOMER_DELETION_FORBIDDEN",           // Specifik felkod för borttagningsfel
                ex.getMessage(),                          // Detaljerat meddelande om varför borttagning förhindras
                HttpStatus.CONFLICT.value(),             // HTTP-statuskod (409)
                LocalDateTime.now()
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }




    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> validationErrors = new HashMap<>();

        // Gå igenom alla valideringsfel och samla dem i en Map
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            validationErrors.put(fieldName, errorMessage);
        });

        // Skapa en specialiserad felrespons för valideringsfel
        ValidationErrorResponse errorResponse = new ValidationErrorResponse(
                "VALIDATION_FAILED",
                "Validering misslyckades för " + validationErrors.size() + " fält",
                HttpStatus.BAD_REQUEST.value(),
                LocalDateTime.now(),
                validationErrors
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }




    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        // Logga det fullständiga felet för utvecklare och systemadministratörer
        System.err.println("Oväntat fel i applikationen: " + ex.getClass().getSimpleName());
        System.err.println("Felmeddelande: " + ex.getMessage());
        ex.printStackTrace(); // I produktion skulle detta gå till en professionell loggningslösning

        // Skapa ett säkert, generiskt meddelande för slutanvändaren
        ErrorResponse errorResponse = new ErrorResponse(
                "INTERNAL_SERVER_ERROR",
                "Ett oväntat fel uppstod. Kontakta support om problemet kvarstår.",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                LocalDateTime.now()
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }




    public static class ErrorResponse {
        private String errorCode;
        private String message;
        private int statusCode;
        private LocalDateTime timestamp;

        public ErrorResponse(String errorCode, String message, int statusCode, LocalDateTime timestamp) {
            this.errorCode = errorCode;
            this.message = message;
            this.statusCode = statusCode;
            this.timestamp = timestamp;
        }

        // Getters för JSON serialisering
        public String getErrorCode() { return errorCode; }
        public String getMessage() { return message; }
        public int getStatusCode() { return statusCode; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }



    /**
     * Specialiserad felrespons för valideringsfel.
     *
     * Denna klass utökar grundläggande ErrorResponse med ett fält för
     * detaljerade valideringsfel per fält. Detta låter frontend visa
     * specifika felmeddelanden vid rätt formulärfält.
     */

    public static class ValidationErrorResponse extends ErrorResponse {
        private Map<String, String> validationErrors;

        public ValidationErrorResponse(String errorCode, String message, int statusCode,
                                       LocalDateTime timestamp, Map<String, String> validationErrors) {
            super(errorCode, message, statusCode, timestamp);
            this.validationErrors = validationErrors;
        }

        public Map<String, String> getValidationErrors() { return validationErrors; }
    }
}