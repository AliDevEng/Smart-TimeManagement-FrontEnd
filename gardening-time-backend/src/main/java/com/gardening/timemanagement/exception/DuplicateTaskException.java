package com.gardening.timemanagement.exception;

/**
 * Exception som kastas när ett uppdrag med samma data redan existerar.
 *
 * Denna exception används när:
 * - Ett uppdrag med samma uppdragsnummer redan finns
 * - Vid uppdatering: nytt uppdragsnummer konfliktar med befintligt uppdrag
 * - Systemet upptäcker andra former av uppdragsdubbletter
 *
 * Kommer att mappas till HTTP 409 Conflict i Controller-lagret eftersom det
 * representerar en konflikt med befintliga resurser.
 */
public class DuplicateTaskException extends RuntimeException {

    /**
     * Det uppdragsnummer som orsakade konflikten.
     * Lagras för loggning och användarfeedback.
     */
    private final String conflictingTaskNumber;

    /**
     * Typ av konflikt som upptäcktes.
     * Används för specifika felmeddelanden och hantering.
     */
    private final ConflictType conflictType;

    /**
     * ID för det befintliga uppdraget som orsakar konflikten (om känt).
     */
    private final Long existingTaskId;

    /**
     * Enum som definierar olika typer av uppdragskonflikter.
     */
    public enum ConflictType {
        TASK_NUMBER("Uppdragsnummer"),
        CUSTOMER_AND_DESCRIPTION("Kund och beskrivning"),
        BUSINESS_RULE("Affärsregel");

        private final String displayName;

        ConflictType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Standard konstruktor med bara felmeddelande.
     * Används för generella dublett-konflikter.
     */
    public DuplicateTaskException(String message) {
        super(message);
        this.conflictingTaskNumber = null;
        this.conflictType = ConflictType.BUSINESS_RULE;
        this.existingTaskId = null;
    }

    /**
     * Konstruktor med felmeddelande och underlying cause.
     * Användbar när DuplicateTaskException wrappas runt andra exceptions.
     */
    public DuplicateTaskException(String message, Throwable cause) {
        super(message, cause);
        this.conflictingTaskNumber = null;
        this.conflictType = ConflictType.BUSINESS_RULE;
        this.existingTaskId = null;
    }

    /**
     * Huvudkonstruktor med full kontextinformation.
     * Detta är den mest användbara konstruktorn för specifika konflikter.
     */
    public DuplicateTaskException(String conflictingTaskNumber, ConflictType conflictType,
                                  Long existingTaskId, String customMessage) {
        super(customMessage != null ? customMessage :
                createDefaultMessage(conflictingTaskNumber, conflictType));
        this.conflictingTaskNumber = conflictingTaskNumber;
        this.conflictType = conflictType;
        this.existingTaskId = existingTaskId;
    }

    /**
     * Förenklad konstruktor för vanliga task number-konflikter.
     */
    public DuplicateTaskException(String conflictingTaskNumber, ConflictType conflictType) {
        this(conflictingTaskNumber, conflictType, null, null);
    }

    /**
     * Factory method för vanliga scenario: uppdragsnummer-konflikter vid skapande.
     */
    public static DuplicateTaskException forTaskNumber(String taskNumber) {
        return new DuplicateTaskException(
                taskNumber,
                ConflictType.TASK_NUMBER,
                null,
                "Ett uppdrag med nummer '" + taskNumber + "' finns redan i systemet"
        );
    }

    /**
     * Factory method för uppdragsnummer-konflikter vid uppdatering.
     */
    public static DuplicateTaskException forTaskNumberUpdate(String taskNumber, Long existingTaskId) {
        return new DuplicateTaskException(
                taskNumber,
                ConflictType.TASK_NUMBER,
                existingTaskId,
                "Kan inte ändra till uppdragsnummer '" + taskNumber +
                        "' eftersom det redan används av ett annat uppdrag"
        );
    }

    /**
     * Factory method för komplexa affärsregelkonflikter.
     */
    public static DuplicateTaskException forBusinessRule(String taskNumber, String ruleDescription) {
        return new DuplicateTaskException(
                taskNumber,
                ConflictType.BUSINESS_RULE,
                null,
                "Uppdrag '" + taskNumber + "' bryter mot affärsregel: " + ruleDescription
        );
    }

    /**
     * Factory method för kund- och beskrivningskonflikter.
     * Används när samma kund försöker skapa identiska uppdrag.
     */
    public static DuplicateTaskException forCustomerAndDescription(String taskNumber,
                                                                   String customerName) {
        return new DuplicateTaskException(
                taskNumber,
                ConflictType.CUSTOMER_AND_DESCRIPTION,
                null,
                "Kunden '" + customerName + "' har redan ett liknande uppdrag. " +
                        "Kontrollera om '" + taskNumber + "' är ett dublett."
        );
    }

    /**
     * Skapar ett standardfelmeddelande baserat på konflikttyp.
     */
    private static String createDefaultMessage(String taskNumber, ConflictType conflictType) {
        return switch (conflictType) {
            case TASK_NUMBER -> "Uppdragsnummer '" + taskNumber + "' finns redan";
            case CUSTOMER_AND_DESCRIPTION -> "Liknande uppdrag för samma kund finns redan: " + taskNumber;
            case BUSINESS_RULE -> "Uppdrag '" + taskNumber + "' bryter mot systemregler";
        };
    }

    // Getters för rich exception information

    public String getConflictingTaskNumber() {
        return conflictingTaskNumber;
    }

    public ConflictType getConflictType() {
        return conflictType;
    }

    public Long getExistingTaskId() {
        return existingTaskId;
    }

    /**
     * Returnerar en användarvänlig förklaring av konflikten.
     */
    public String getUserFriendlyExplanation() {
        return switch (conflictType) {
            case TASK_NUMBER -> "Varje uppdrag måste ha ett unikt uppdragsnummer. " +
                    "Uppdragsnumret '" + conflictingTaskNumber + "' används redan av ett annat uppdrag.";
            case CUSTOMER_AND_DESCRIPTION -> "Det verkar som om denna kund redan har ett liknande uppdrag. " +
                    "Kontrollera om detta är ett dublett innan du fortsätter.";
            case BUSINESS_RULE -> "Detta uppdrag bryter mot systemets affärsregler för att förhindra dubbletter " +
                    "och säkerställa datakonsistens.";
        };
    }

    /**
     * Returnerar förslag på vad användaren kan göra för att lösa konflikten.
     */
    public String getSuggestedActions() {
        return switch (conflictType) {
            case TASK_NUMBER -> "Förslag:\n" +
                    "• Välj ett annat uppdragsnummer\n" +
                    "• Kontrollera om uppdraget '" + conflictingTaskNumber + "' redan finns\n" +
                    "• Lägg till suffix som -2, -B eller liknande för att göra numret unikt";
            case CUSTOMER_AND_DESCRIPTION -> "Förslag:\n" +
                    "• Kontrollera kundens befintliga uppdrag\n" +
                    "• Lägg till mer specifik beskrivning för att skilja uppdragen åt\n" +
                    "• Överväg om detta är en utökning av befintligt uppdrag";
            case BUSINESS_RULE -> "Kontakta systemadministratör för att förstå vilka affärsregler " +
                    "som förhindrar skapandet av detta uppdrag.";
        };
    }

    /**
     * Kontrollerar om detta fel kan lösas genom användaråtgärder.
     */
    public boolean isUserResolvable() {
        return conflictType == ConflictType.TASK_NUMBER ||
                conflictType == ConflictType.CUSTOMER_AND_DESCRIPTION;
    }

    /**
     * Returnerar söktermer som kan hjälpa användaren hitta det befintliga uppdraget.
     */
    public String getSearchSuggestion() {
        if (conflictingTaskNumber != null) {
            return "Sök efter '" + conflictingTaskNumber + "' för att hitta det befintliga uppdraget";
        }
        return "Använd sökfunktionen för att kontrollera befintliga uppdrag";
    }

    /**
     * Returnerar en strukturerad representation för API-responses.
     */
    public DuplicationErrorDetails getErrorDetails() {
        return new DuplicationErrorDetails(
                conflictingTaskNumber,
                conflictType.name(),
                conflictType.getDisplayName(),
                existingTaskId,
                getUserFriendlyExplanation(),
                getSuggestedActions(),
                isUserResolvable(),
                getSearchSuggestion()
        );
    }

    @Override
    public String toString() {
        return "DuplicateTaskException{" +
                "conflictingTaskNumber='" + conflictingTaskNumber + '\'' +
                ", conflictType=" + conflictType +
                ", existingTaskId=" + existingTaskId +
                ", message='" + getMessage() + '\'' +
                '}';
    }

    /**
     * Data class för strukturerade feldetaljer i API-responses.
     * Ger frontend all information den behöver för att hantera dublett-fel elegant.
     */
    public static class DuplicationErrorDetails {
        private final String conflictingTaskNumber;
        private final String conflictType;
        private final String conflictTypeDisplay;
        private final Long existingTaskId;
        private final String explanation;
        private final String suggestedActions;
        private final boolean userResolvable;
        private final String searchSuggestion;

        public DuplicationErrorDetails(String conflictingTaskNumber, String conflictType,
                                       String conflictTypeDisplay, Long existingTaskId,
                                       String explanation, String suggestedActions,
                                       boolean userResolvable, String searchSuggestion) {
            this.conflictingTaskNumber = conflictingTaskNumber;
            this.conflictType = conflictType;
            this.conflictTypeDisplay = conflictTypeDisplay;
            this.existingTaskId = existingTaskId;
            this.explanation = explanation;
            this.suggestedActions = suggestedActions;
            this.userResolvable = userResolvable;
            this.searchSuggestion = searchSuggestion;
        }

        // Getters
        public String getConflictingTaskNumber() { return conflictingTaskNumber; }
        public String getConflictType() { return conflictType; }
        public String getConflictTypeDisplay() { return conflictTypeDisplay; }
        public Long getExistingTaskId() { return existingTaskId; }
        public String getExplanation() { return explanation; }
        public String getSuggestedActions() { return suggestedActions; }
        public boolean isUserResolvable() { return userResolvable; }
        public String getSearchSuggestion() { return searchSuggestion; }
    }
}
