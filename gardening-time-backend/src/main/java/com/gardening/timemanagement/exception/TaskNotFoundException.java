package com.gardening.timemanagement.exception;

// Exception som kastas när ett uppdrag inte kan hittas i systemet.

public class TaskNotFoundException extends RuntimeException {

    // Det ID eller uppdragsnummer som inte kunde hittas
    private final String searchCriteria;

    // Typ av sökning som misslyckades (ID, nummer, etc.)
    private final String searchType;

    public TaskNotFoundException (String message) {
        super(message);
        this.searchCriteria = null;
        this.searchType = "unknown";

    }

    public TaskNotFoundException (String message, Throwable cause) {
        super(message, cause);
        this.searchCriteria = null;
        this.searchType = "unknown";
    }

    public TaskNotFoundException(Long taskId) {
        super("Uppdrag med ID " + taskId + " finns inte");
        this.searchCriteria = taskId != null ? taskId.toString() : "null";
        this.searchType = "ID";
    }



    public TaskNotFoundException(String searchType, String searchCriteria, String customMessage) {
        super(customMessage != null ? customMessage :
                "Uppdrag med " + searchType + " '" + searchCriteria + "' finns inte");
        this.searchCriteria = searchCriteria;
        this.searchType = searchType;
    }

    public static TaskNotFoundException forUpdate(Long taskId) {
        return new TaskNotFoundException(
                "ID",
                taskId != null ? taskId.toString() : "null",
                "Kan inte uppdatera uppdrag med ID " + taskId + " - uppdraget finns inte"
        );
    }

    public static TaskNotFoundException forDeletion(Long taskId) {
        return new TaskNotFoundException(
                "ID",
                taskId != null ? taskId.toString() : "null",
                "Kan inte ta bort uppdrag med ID " + taskId + " - uppdraget finns inte"
        );
    }

    public static TaskNotFoundException forStatusChange(Long taskId) {
        return new TaskNotFoundException(
                "ID",
                taskId != null ? taskId.toString() : "null",
                "Kan inte ändra status för uppdrag med ID " + taskId + " - uppdraget finns inte"
        );
    }

    public static TaskNotFoundException forNumber(String taskNumber, String context) {
        String message = context != null ?
                context + " - uppdrag med nummer '" + taskNumber + "' finns inte" :
                "Uppdrag med nummer '" + taskNumber + "' finns inte";
        return new TaskNotFoundException("nummer", taskNumber, message);
    }

    // Getters för debugging och loggning

    public String getSearchCriteria() {
        return searchCriteria;
    }

    public String getSearchType() {
        return searchType;
    }

    /**
     * Returnerar en strukturerad representation av felet för loggning.
     * Inkluderar både sökkriterierna och context för debugging.
     */
    public String getStructuredMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("TaskNotFoundException: ");
        sb.append("SearchType=").append(searchType);
        sb.append(", SearchCriteria=").append(searchCriteria);
        sb.append(", Message=").append(getMessage());
        return sb.toString();
    }

    /**
     * Kontrollerar om detta fel beror på ogiltigt ID format.
     * Användbart för att skilja mellan "finns inte" och "ogiltigt format".
     */
    public boolean isInvalidIdFormat() {
        return "ID".equals(searchType) && searchCriteria != null &&
                (searchCriteria.equals("null") || searchCriteria.trim().isEmpty());
    }

    /**
     * Skapar en användarvänlig förklaringstext för detta fel.
     * Kan användas i API-responses för att hjälpa användare förstå vad som gick fel.
     */
    public String getUserFriendlyExplanation() {
        if (isInvalidIdFormat()) {
            return "Ogiltigt uppdrag-ID. Kontrollera att du anger ett giltigt nummer.";
        }

        return switch (searchType) {
            case "ID" -> "Det angivna uppdrag-ID:t (" + searchCriteria +
                    ") finns inte i systemet. Kontrollera att numret är korrekt.";
            case "nummer" -> "Uppdragsnumret '" + searchCriteria +
                    "' finns inte. Kontrollera stavningen eller sök efter liknande uppdrag.";
            default -> "Det sökta uppdraget kunde inte hittas. Kontrollera dina sökkriterier.";
        };
    }

    @Override
    public String toString() {
        return "TaskNotFoundException{" +
                "searchCriteria='" + searchCriteria + '\'' +
                ", searchType='" + searchType + '\'' +
                ", message='" + getMessage() + '\'' +
                '}';
    }
}
