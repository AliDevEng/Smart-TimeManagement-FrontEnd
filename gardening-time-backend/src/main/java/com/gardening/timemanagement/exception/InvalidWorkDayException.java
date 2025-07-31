package com.gardening.timemanagement.exception;

import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;


public class InvalidWorkDayException extends RuntimeException {

    // =================================================================
    // ERROR CATEGORIZATION - STRUCTURED ERROR CLASSIFICATION
    // =================================================================

    /**
     * Enum som kategorizes different types av WorkDay validation errors.
     *
     * Denna categorization enables intelligent error handling där different
     * error types kan trigger different response strategies throughout
     * application stack - från service layer error recovery till
     * user interface error display patterns.
     */
    public enum ErrorCategory {
        /**
         * Input validation errors - malformed eller obviously invalid data.
         * These errors typically indicate client-side validation failures
         * eller API misuse och ska result i HTTP 400 Bad Request responses.
         */
        VALIDATION_ERROR("Validation Error", "Provided data is invalid or malformed"),

        /**
         * Business rule violations - data is technically valid men violates
         * domain-specific business rules. These errors indicate legitimate
         * business constraints och ska be presented med actionable guidance.
         */
        BUSINESS_RULE_VIOLATION("Business Rule Violation", "Operation violates business rules"),

        /**
         * Resource conflicts - operation conflicts med existing data eller
         * resource availability. These errors often indicate timing issues
         * eller concurrent access conflicts som might be resolvable.
         */
        RESOURCE_CONFLICT("Resource Conflict", "Operation conflicts with existing resources"),

        /**
         * State transition errors - attempting illegal transitions mellan
         * different WorkDay states. These errors indicate workflow violations
         * som require understanding av business process constraints.
         */
        ILLEGAL_STATE_TRANSITION("Illegal State Transition", "Requested operation is not allowed in current state"),

        /**
         * Authorization errors - operation requires permissions som current
         * user eller context doesn't possess. These errors indicate security
         * eller role-based access control violations.
         */
        AUTHORIZATION_ERROR("Authorization Error", "Insufficient permissions for requested operation"),

        /**
         * System errors - technical problems som prevent operation completion.
         * These errors typically indicate infrastructure issues eller
         * unexpected system conditions requiring administrative attention.
         */
        SYSTEM_ERROR("System Error", "Technical problem prevents operation completion");

        private final String displayName;
        private final String description;

        ErrorCategory(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }

        /**
         * Determines om denna error category indicates en recoverable condition.
         * Recoverable errors can potentially be resolved genom user action eller
         * system retry, while non-recoverable errors require administrative intervention.
         */
        public boolean isRecoverable() {
            return this != SYSTEM_ERROR && this != AUTHORIZATION_ERROR;
        }

        /**
         * Determines recommended HTTP status code för denna error category.
         * This helps med consistent API error response mapping.
         */
        public int getRecommendedHttpStatus() {
            return switch (this) {
                case VALIDATION_ERROR -> 400; // Bad Request
                case BUSINESS_RULE_VIOLATION -> 422; // Unprocessable Entity
                case RESOURCE_CONFLICT -> 409; // Conflict
                case ILLEGAL_STATE_TRANSITION -> 409; // Conflict
                case AUTHORIZATION_ERROR -> 403; // Forbidden
                case SYSTEM_ERROR -> 500; // Internal Server Error
            };
        }
    }

    // =================================================================
    // ERROR CONTEXT FIELDS - RICH DIAGNOSTIC INFORMATION
    // =================================================================

    /**
     * Primary error category för structured error handling.
     */
    private final ErrorCategory category;

    /**
     * Short, user-friendly error code för programmatic identification.
     * This enables client applications att handle specific error types
     * programmatically while providing consistent error identification.
     */
    private final String errorCode;

    /**
     * Detailed technical description för developers och logging systems.
     * This provides additional context beyond the primary exception message
     * för effective debugging och troubleshooting.
     */
    private final String technicalDetails;

    /**
     * User-actionable suggestions för error resolution.
     * These suggestions help users understand how they might resolve
     * the problem independently, improving overall user experience.
     */
    private final List<String> resolutionSuggestions;

    /**
     * Contextual information about när och where error occurred.
     * This metadata helps med debugging och error pattern analysis.
     */
    private final WorkDayErrorContext errorContext;

    // =================================================================
    // CONSTRUCTOR OVERLOADS - FLEXIBLE ERROR CREATION
    // =================================================================

    /**
     * Minimal constructor för simple error scenarios.
     *
     * @param message Human-readable error description
     */
    public InvalidWorkDayException(String message) {
        super(message);
        this.category = ErrorCategory.VALIDATION_ERROR; // Default fallback
        this.errorCode = generateDefaultErrorCode(category);
        this.technicalDetails = null;
        this.resolutionSuggestions = Collections.emptyList();
        this.errorContext = new WorkDayErrorContext();
    }

    /**
     * Constructor för categorized errors med technical context.
     *
     * @param message Human-readable error description
     * @param technicalDetails Additional technical information för debugging
     */
    public InvalidWorkDayException(String message, String technicalDetails) {
        super(message);
        this.category = ErrorCategory.VALIDATION_ERROR;
        this.errorCode = generateDefaultErrorCode(category);
        this.technicalDetails = technicalDetails;
        this.resolutionSuggestions = Collections.emptyList();
        this.errorContext = new WorkDayErrorContext();
    }

    /**
     * Constructor för errors med nested cause tracking.
     *
     * @param message Human-readable error description
     * @param technicalDetails Technical context för debugging
     * @param cause Underlying exception som triggered this error
     */
    public InvalidWorkDayException(String message, String technicalDetails, Throwable cause) {
        super(message, cause);
        this.category = ErrorCategory.SYSTEM_ERROR; // Nested causes often indicate system issues
        this.errorCode = generateDefaultErrorCode(category);
        this.technicalDetails = technicalDetails;
        this.resolutionSuggestions = Collections.emptyList();
        this.errorContext = new WorkDayErrorContext();
    }

    /**
     * Full-featured constructor för comprehensive error reporting.
     *
     * This constructor enables creation av highly informative errors med
     * all available context för optimal error handling och user guidance.
     *
     * @param category Error category för structured handling
     * @param errorCode Programmatic error identifier
     * @param message Human-readable error description
     * @param technicalDetails Technical context för debugging
     * @param resolutionSuggestions User-actionable resolution guidance
     * @param errorContext Contextual information about error occurrence
     */
    public InvalidWorkDayException(ErrorCategory category, String errorCode, String message,
                                   String technicalDetails, List<String> resolutionSuggestions,
                                   WorkDayErrorContext errorContext) {
        super(message);
        this.category = category != null ? category : ErrorCategory.VALIDATION_ERROR;
        this.errorCode = errorCode != null ? errorCode : generateDefaultErrorCode(this.category);
        this.technicalDetails = technicalDetails;
        this.resolutionSuggestions = resolutionSuggestions != null ?
                new ArrayList<>(resolutionSuggestions) : Collections.emptyList();
        this.errorContext = errorContext != null ? errorContext : new WorkDayErrorContext();
    }

    /**
     * Constructor med cause tracking för full-featured errors.
     */
    public InvalidWorkDayException(ErrorCategory category, String errorCode, String message,
                                   String technicalDetails, List<String> resolutionSuggestions,
                                   WorkDayErrorContext errorContext, Throwable cause) {
        super(message, cause);
        this.category = category != null ? category : ErrorCategory.SYSTEM_ERROR;
        this.errorCode = errorCode != null ? errorCode : generateDefaultErrorCode(this.category);
        this.technicalDetails = technicalDetails;
        this.resolutionSuggestions = resolutionSuggestions != null ?
                new ArrayList<>(resolutionSuggestions) : Collections.emptyList();
        this.errorContext = errorContext != null ? errorContext : new WorkDayErrorContext();
    }

    // =================================================================
    // FACTORY METHODS - CONVENIENT ERROR CREATION PATTERNS
    // =================================================================

    /**
     * Factory method för validation errors med automatic categorization.
     *
     * @param fieldName Name av field som failed validation
     * @param fieldValue Value som was invalid
     * @param validationRule Rule som was violated
     * @return Properly categorized validation error
     */
    public static InvalidWorkDayException validationError(String fieldName, Object fieldValue,
                                                          String validationRule) {
        String message = String.format("Validation failed för field '%s'", fieldName);
        String technicalDetails = String.format("Field '%s' med value '%s' violated rule: %s",
                fieldName, fieldValue, validationRule);

        List<String> suggestions = List.of(
                "Kontrollera att värdet följer required format och constraints",
                "Se API documentation för valid värden för detta fält"
        );

        WorkDayErrorContext context = new WorkDayErrorContext()
                .withField(fieldName)
                .withInvalidValue(String.valueOf(fieldValue));

        return new InvalidWorkDayException(
                ErrorCategory.VALIDATION_ERROR,
                "VALIDATION_FAILED",
                message,
                technicalDetails,
                suggestions,
                context
        );
    }

    /**
     * Factory method för business rule violations.
     *
     * @param ruleName Name av business rule som was violated
     * @param ruleDescription Human-readable description av the rule
     * @param violationDetails Specific details about how rule was violated
     * @return Properly categorized business rule violation error
     */
    public static InvalidWorkDayException businessRuleViolation(String ruleName,
                                                                String ruleDescription,
                                                                String violationDetails) {
        String message = String.format("Business rule violation: %s", ruleName);
        String technicalDetails = String.format("Rule '%s' (%s) was violated: %s",
                ruleName, ruleDescription, violationDetails);

        List<String> suggestions = List.of(
                "Kontrollera att your request följer company policies",
                "Kontakta administrator om du tror denna rule ska be relaxed",
                "Se business rule documentation för details"
        );

        WorkDayErrorContext context = new WorkDayErrorContext()
                .withBusinessRule(ruleName)
                .withRuleDescription(ruleDescription);

        return new InvalidWorkDayException(
                ErrorCategory.BUSINESS_RULE_VIOLATION,
                "BUSINESS_RULE_VIOLATION",
                message,
                technicalDetails,
                suggestions,
                context
        );
    }

    /**
     * Factory method för resource conflict errors.
     *
     * @param resourceType Type av resource som is conflicted
     * @param resourceId Identifier för the conflicted resource
     * @param conflictReason Reason för the conflict
     * @return Properly categorized resource conflict error
     */
    public static InvalidWorkDayException resourceConflict(String resourceType, String resourceId,
                                                           String conflictReason) {
        String message = String.format("%s conflict detected", resourceType);
        String technicalDetails = String.format("%s '%s' is not available: %s",
                resourceType, resourceId, conflictReason);

        List<String> suggestions = List.of(
                "Kontrollera att specified " + resourceType.toLowerCase() + " är available",
                "Välj different " + resourceType.toLowerCase() + " eller different date",
                "Check för existing bookings eller conflicts"
        );

        WorkDayErrorContext context = new WorkDayErrorContext()
                .withResourceType(resourceType)
                .withResourceId(resourceId)
                .withConflictReason(conflictReason);

        return new InvalidWorkDayException(
                ErrorCategory.RESOURCE_CONFLICT,
                "RESOURCE_CONFLICT",
                message,
                technicalDetails,
                suggestions,
                context
        );
    }

    /**
     * Factory method för illegal state transition errors.
     *
     * @param currentState Current state av the WorkDay
     * @param attemptedTransition Transition som was attempted
     * @param reason Reason varför transition är illegal
     * @return Properly categorized state transition error
     */
    public static InvalidWorkDayException illegalStateTransition(String currentState,
                                                                 String attemptedTransition,
                                                                 String reason) {
        String message = String.format("Illegal state transition från '%s'", currentState);
        String technicalDetails = String.format("Cannot perform '%s' from state '%s': %s",
                attemptedTransition, currentState, reason);

        List<String> suggestions = List.of(
                "Kontrollera current state av WorkDay innan attempting operation",
                "Se workflow documentation för valid state transitions",
                "Kontakta administrator om denna transition ska be allowed"
        );

        WorkDayErrorContext context = new WorkDayErrorContext()
                .withCurrentState(currentState)
                .withAttemptedTransition(attemptedTransition)
                .withTransitionBlockReason(reason);

        return new InvalidWorkDayException(
                ErrorCategory.ILLEGAL_STATE_TRANSITION,
                "ILLEGAL_STATE_TRANSITION",
                message,
                technicalDetails,
                suggestions,
                context
        );
    }

    // =================================================================
    // ACCESSOR METHODS - ERROR INFORMATION RETRIEVAL
    // =================================================================

    public ErrorCategory getCategory() { return category; }
    public String getErrorCode() { return errorCode; }
    public String getTechnicalDetails() { return technicalDetails; }
    public List<String> getResolutionSuggestions() { return new ArrayList<>(resolutionSuggestions); }
    public WorkDayErrorContext getErrorContext() { return errorContext; }

    /**
     * Determines om denna error is recoverable genom user action.
     */
    public boolean isRecoverable() {
        return category.isRecoverable();
    }

    /**
     * Gets recommended HTTP status code för API responses.
     */
    public int getRecommendedHttpStatus() {
        return category.getRecommendedHttpStatus();
    }

    /**
     * Generates comprehensive error information för logging systems.
     */
    public String getComprehensiveErrorInfo() {
        StringBuilder info = new StringBuilder();
        info.append("InvalidWorkDayException Details:\n");
        info.append("Category: ").append(category.getDisplayName()).append("\n");
        info.append("Error Code: ").append(errorCode).append("\n");
        info.append("Message: ").append(getMessage()).append("\n");

        if (technicalDetails != null) {
            info.append("Technical Details: ").append(technicalDetails).append("\n");
        }

        if (!resolutionSuggestions.isEmpty()) {
            info.append("Resolution Suggestions:\n");
            for (int i = 0; i < resolutionSuggestions.size(); i++) {
                info.append("  ").append(i + 1).append(". ").append(resolutionSuggestions.get(i)).append("\n");
            }
        }

        if (errorContext.hasContextualInformation()) {
            info.append("Context: ").append(errorContext.toString()).append("\n");
        }

        if (getCause() != null) {
            info.append("Caused by: ").append(getCause().toString()).append("\n");
        }

        return info.toString();
    }

    /**
     * Generates user-friendly error message för UI display.
     *
     * This method creates error messages som are appropriate för end-user
     * consumption, avoiding technical jargon while providing actionable guidance.
     */
    public String getUserFriendlyMessage() {
        StringBuilder userMessage = new StringBuilder();
        userMessage.append(getMessage());

        if (!resolutionSuggestions.isEmpty()) {
            userMessage.append("\n\nFörslag för lösning:");
            for (String suggestion : resolutionSuggestions) {
                userMessage.append("\n• ").append(suggestion);
            }
        }

        return userMessage.toString();
    }

    // =================================================================
    // HELPER METHODS - INTERNAL UTILITIES
    // =================================================================

    /**
     * Generates default error code baserat på category.
     */
    private static String generateDefaultErrorCode(ErrorCategory category) {
        return "WORKDAY_" + category.name();
    }

    // =================================================================
    // ERROR CONTEXT CLASS - STRUCTURED CONTEXTUAL INFORMATION
    // =================================================================

    /**
     * Immutable context class som carries structured information about
     * när och where an error occurred för enhanced debugging och analysis.
     *
     * This class uses builder pattern för fluent context construction
     * och provides type-safe access till contextual error information.
     */
    public static class WorkDayErrorContext {
        private final java.util.Map<String, Object> contextData;
        private final java.time.LocalDateTime errorTimestamp;

        public WorkDayErrorContext() {
            this.contextData = new java.util.HashMap<>();
            this.errorTimestamp = java.time.LocalDateTime.now();
        }

        private WorkDayErrorContext(java.util.Map<String, Object> contextData) {
            this.contextData = new java.util.HashMap<>(contextData);
            this.errorTimestamp = java.time.LocalDateTime.now();
        }

        // Builder-style methods för fluent context construction
        public WorkDayErrorContext withWorkDayId(Long workDayId) {
            java.util.Map<String, Object> newData = new java.util.HashMap<>(contextData);
            newData.put("workDayId", workDayId);
            return new WorkDayErrorContext(newData);
        }

        public WorkDayErrorContext withDate(LocalDate date) {
            java.util.Map<String, Object> newData = new java.util.HashMap<>(contextData);
            newData.put("date", date);
            return new WorkDayErrorContext(newData);
        }

        public WorkDayErrorContext withTaskId(Long taskId) {
            java.util.Map<String, Object> newData = new java.util.HashMap<>(contextData);
            newData.put("taskId", taskId);
            return new WorkDayErrorContext(newData);
        }

        public WorkDayErrorContext withEmployeeId(Long employeeId) {
            java.util.Map<String, Object> newData = new java.util.HashMap<>(contextData);
            newData.put("employeeId", employeeId);
            return new WorkDayErrorContext(newData);
        }

        public WorkDayErrorContext withField(String fieldName) {
            java.util.Map<String, Object> newData = new java.util.HashMap<>(contextData);
            newData.put("field", fieldName);
            return new WorkDayErrorContext(newData);
        }

        public WorkDayErrorContext withInvalidValue(String value) {
            java.util.Map<String, Object> newData = new java.util.HashMap<>(contextData);
            newData.put("invalidValue", value);
            return new WorkDayErrorContext(newData);
        }

        public WorkDayErrorContext withBusinessRule(String ruleName) {
            java.util.Map<String, Object> newData = new java.util.HashMap<>(contextData);
            newData.put("businessRule", ruleName);
            return new WorkDayErrorContext(newData);
        }

        public WorkDayErrorContext withRuleDescription(String description) {
            java.util.Map<String, Object> newData = new java.util.HashMap<>(contextData);
            newData.put("ruleDescription", description);
            return new WorkDayErrorContext(newData);
        }

        public WorkDayErrorContext withResourceType(String resourceType) {
            java.util.Map<String, Object> newData = new java.util.HashMap<>(contextData);
            newData.put("resourceType", resourceType);
            return new WorkDayErrorContext(newData);
        }

        public WorkDayErrorContext withResourceId(String resourceId) {
            java.util.Map<String, Object> newData = new java.util.HashMap<>(contextData);
            newData.put("resourceId", resourceId);
            return new WorkDayErrorContext(newData);
        }

        public WorkDayErrorContext withConflictReason(String reason) {
            java.util.Map<String, Object> newData = new java.util.HashMap<>(contextData);
            newData.put("conflictReason", reason);
            return new WorkDayErrorContext(newData);
        }

        public WorkDayErrorContext withCurrentState(String state) {
            java.util.Map<String, Object> newData = new java.util.HashMap<>(contextData);
            newData.put("currentState", state);
            return new WorkDayErrorContext(newData);
        }

        public WorkDayErrorContext withAttemptedTransition(String transition) {
            java.util.Map<String, Object> newData = new java.util.HashMap<>(contextData);
            newData.put("attemptedTransition", transition);
            return new WorkDayErrorContext(newData);
        }

        public WorkDayErrorContext withTransitionBlockReason(String reason) {
            java.util.Map<String, Object> newData = new java.util.HashMap<>(contextData);
            newData.put("transitionBlockReason", reason);
            return new WorkDayErrorContext(newData);
        }

        // Accessor methods
        public java.time.LocalDateTime getErrorTimestamp() { return errorTimestamp; }

        public boolean hasContextualInformation() {
            return !contextData.isEmpty();
        }

        public Object getContextValue(String key) {
            return contextData.get(key);
        }

        public java.util.Set<String> getContextKeys() {
            return contextData.keySet();
        }

        public String getField() {
            Object field = this.contextData.get("field");
            return field != null ? field.toString() : null;
        }

        @Override
        public String toString() {
            if (contextData.isEmpty()) {
                return "No additional context available";
            }

            StringBuilder context = new StringBuilder();
            context.append("Error occurred at ").append(errorTimestamp).append(" med context: ");
            contextData.forEach((key, value) ->
                    context.append(key).append("=").append(value).append(", "));

            // Remove trailing comma och space
            if (context.length() > 2) {
                context.setLength(context.length() - 2);
            }

            return context.toString();
        }
    }


    /**
     * Returnerar en kort, beskrivande titel för felet.
     * Används av Controller-lagret för strukturerade error responses.
     *
     * @return Kort titel som beskriver error-typen
     */
    public String getTitle() {
        if (this.category != null) {
            return this.category.getDisplayName();
        } else {
            // Fallback för befintlig kod som inte använder categories
            return "WorkDay Validation Error";
        }
    }

    /**
     * Returnerar namnet på det specifika fält som orsakade felet.
     * Detta gör det möjligt för frontend att highlighta specifika formulärfält.
     *
     * @return Fältnamn som orsakade felet, eller null om inte tillgängligt
     */
    public String getField() {
        if (this.errorContext != null) {
            return this.errorContext.getField();
        }
        return null;
    }

    /**
     * Convenience-konstruktor för field-specific validation errors.
     * Denna metod gör det enkelt att skapa exceptions med field-information.
     *
     * @param title Kort titel för felet
     * @param message Detaljerat felmeddelande
     * @param field Namnet på fältet som orsakade felet
     */
    public InvalidWorkDayException(String title, String message, String field) {
        super(message);
        this.category = ErrorCategory.VALIDATION_ERROR;
        this.errorCode = generateDefaultErrorCode(category);
        this.technicalDetails = null;
        this.resolutionSuggestions = Collections.emptyList();
        this.errorContext = new WorkDayErrorContext().withField(field);
    }

    @Override
    public String toString() {
        return String.format("InvalidWorkDayException{category=%s, errorCode='%s', message='%s'}",
                category, errorCode, getMessage());
    }
}