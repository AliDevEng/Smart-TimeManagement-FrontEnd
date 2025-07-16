package com.gardening.timemanagement.exception;

import com.gardening.timemanagement.entity.Task.TaskStatus;
import com.gardening.timemanagement.util.TaskStatusUtils;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Exception som kastas när en ogiltig statusövergång försöks på ett uppdrag.
 *
 * Denna exception representerar en av de mest avancerade typerna av affärsregelfel
 * eftersom den måste hantera state machine-logik, användarintention, och komplexa
 * scenarion där samma operation kan vara giltig eller ogiltig beroende på context.
 *
 * Användningsområden:
 * - Försök att övergå från COMPLETED till ACTIVE
 * - Försök att övergå från CANCELLED till något annat tillstånd
 * - Försök att sätta COMPLETED utan att ha startat uppdraget
 * - Automatiska övergångar som bryter affärsregler
 *
 * Kommer att mappas till HTTP 409 Conflict i Controller-lagret eftersom det
 * representerar en konflikt med nuvarande resurstillstånd.
 *
 */
public class InvalidTaskTransitionException extends RuntimeException {

    /**
     * Uppdragets ID för referens och loggning.
     */
    private final Long taskId;

    /**
     * Uppdragsnummer för användarvänlig identifiering.
     */
    private final String taskNumber;

    /**
     * Den status som uppdraget hade innan övergången försöktes.
     */
    private final TaskStatus currentStatus;

    /**
     * Den status som användaren försökte sätta.
     */
    private final TaskStatus attemptedStatus;

    /**
     * De statusövergångar som faktiskt är tillåtna från current status.
     * Används för att ge konstruktiv feedback till användaren.
     */
    private final Set<TaskStatus> allowedTransitions;

    /**
     * Specifik anledning till varför övergången inte är tillåten.
     * Kan vara null för generella affärsregelöverträdelser.
     */
    private final String specificReason;

    /**
     * Standard konstruktor för enkla transition-fel.
     */
    public InvalidTaskTransitionException(TaskStatus currentStatus, TaskStatus attemptedStatus) {
        super(TaskStatusUtils.createTransitionErrorMessage(currentStatus, attemptedStatus));
        this.taskId = null;
        this.taskNumber = null;
        this.currentStatus = currentStatus;
        this.attemptedStatus = attemptedStatus;
        this.allowedTransitions = TaskStatusUtils.getValidTransitions(currentStatus);
        this.specificReason = null;
    }

    /**
     * Fullständig konstruktor med all kontextinformation.
     * Detta är den mest användbara konstruktorn för verkliga scenario.
     */
    public InvalidTaskTransitionException(Long taskId, String taskNumber,
                                          TaskStatus currentStatus, TaskStatus attemptedStatus,
                                          String specificReason) {
        super(createDetailedErrorMessage(taskId, taskNumber, currentStatus, attemptedStatus, specificReason));
        this.taskId = taskId;
        this.taskNumber = taskNumber;
        this.currentStatus = currentStatus;
        this.attemptedStatus = attemptedStatus;
        this.allowedTransitions = TaskStatusUtils.getValidTransitions(currentStatus);
        this.specificReason = specificReason;
    }

    /**
     * Konstruktor för när vi har Task-objektets information.
     */
    public InvalidTaskTransitionException(Long taskId, String taskNumber,
                                          TaskStatus currentStatus, TaskStatus attemptedStatus) {
        this(taskId, taskNumber, currentStatus, attemptedStatus, null);
    }

    /**
     * Factory method för vanliga scenario: försök att återaktivera avslutat uppdrag.
     */
    public static InvalidTaskTransitionException cannotReactivateCompleted(Long taskId, String taskNumber) {
        return new InvalidTaskTransitionException(
                taskId, taskNumber, TaskStatus.COMPLETED, TaskStatus.ACTIVE,
                "Avslutade uppdrag kan inte återaktiveras. Skapa ett nytt uppdrag istället."
        );
    }

    /**
     * Factory method för vanliga scenario: försök att ändra status på avbrutet uppdrag.
     */
    public static InvalidTaskTransitionException cannotChangeFromCancelled(Long taskId, String taskNumber,
                                                                           TaskStatus attemptedStatus) {
        return new InvalidTaskTransitionException(
                taskId, taskNumber, TaskStatus.CANCELLED, attemptedStatus,
                "Avbrutna uppdrag kan inte ändra status. Status CANCELLED är slutgiltig."
        );
    }

    /**
     * Factory method för scenario där uppdrag försöks avslutas utan att ha startats.
     */
    public static InvalidTaskTransitionException cannotCompleteUnstarted(Long taskId, String taskNumber) {
        return new InvalidTaskTransitionException(
                taskId, taskNumber, TaskStatus.ACTIVE, TaskStatus.COMPLETED,
                "Uppdraget kan inte avslutas eftersom det aldrig har startats. " +
                        "Sätt ett startdatum först eller avbryt uppdraget istället."
        );
    }

    /**
     * Factory method för scenario där automatiska systemoperationer försöker ogiltiga övergångar.
     */
    public static InvalidTaskTransitionException fromAutomaticOperation(Long taskId, String taskNumber,
                                                                        TaskStatus currentStatus,
                                                                        String operationDescription) {
        return new InvalidTaskTransitionException(
                taskId, taskNumber, currentStatus, null,
                "Automatisk operation '" + operationDescription + "' kan inte utföras på uppdrag med status '" +
                        TaskStatusUtils.getDisplayName(currentStatus) + "'"
        );
    }

    /**
     * Factory method för scenario där datum-konflikter förhindrar statusändringar.
     */
    public static InvalidTaskTransitionException dueToDatesConflict(Long taskId, String taskNumber,
                                                                    TaskStatus currentStatus,
                                                                    TaskStatus attemptedStatus,
                                                                    String dateIssue) {
        return new InvalidTaskTransitionException(
                taskId, taskNumber, currentStatus, attemptedStatus,
                "Statusändring blockerad på grund av datumkonflikt: " + dateIssue
        );
    }

    /**
     * Factory method för scenario där relationer till andra entiteter förhindrar övergången.
     */
    public static InvalidTaskTransitionException dueToActiveRelations(Long taskId, String taskNumber,
                                                                      TaskStatus currentStatus,
                                                                      TaskStatus attemptedStatus,
                                                                      String relationDescription) {
        return new InvalidTaskTransitionException(
                taskId, taskNumber, currentStatus, attemptedStatus,
                "Statusändring blockerad på grund av aktiva relationer: " + relationDescription
        );
    }

    /**
     * Skapar ett detaljerat felmeddelande som inkluderar all tillgänglig context.
     */
    private static String createDetailedErrorMessage(Long taskId, String taskNumber,
                                                     TaskStatus currentStatus, TaskStatus attemptedStatus,
                                                     String specificReason) {
        StringBuilder message = new StringBuilder();

        // Uppdragsidentifiering
        if (taskNumber != null) {
            message.append("Uppdrag '").append(taskNumber).append("'");
        } else if (taskId != null) {
            message.append("Uppdrag med ID ").append(taskId);
        } else {
            message.append("Uppdraget");
        }

        // Aktuell status
        if (currentStatus != null) {
            message.append(" har status '").append(TaskStatusUtils.getDisplayName(currentStatus)).append("'");
        }

        // Vad som försöktes
        if (attemptedStatus != null) {
            message.append(" och kan inte ändras till '").append(TaskStatusUtils.getDisplayName(attemptedStatus)).append("'");
        }

        // Specifik anledning om tillgänglig
        if (specificReason != null && !specificReason.trim().isEmpty()) {
            message.append(". ").append(specificReason);
        }

        // Lägg till information om tillåtna övergångar
        if (currentStatus != null) {
            Set<TaskStatus> allowed = TaskStatusUtils.getValidTransitions(currentStatus);
            if (!allowed.isEmpty()) {
                String allowedNames = allowed.stream()
                        .map(TaskStatusUtils::getDisplayName)
                        .collect(Collectors.joining(", "));
                message.append(" Tillåtna statusändringar: ").append(allowedNames);
            } else {
                message.append(" Inga statusändringar är tillåtna från denna status");
            }
        }

        return message.toString();
    }

    // Getters för rich exception information

    public Long getTaskId() {
        return taskId;
    }

    public String getTaskNumber() {
        return taskNumber;
    }

    public TaskStatus getCurrentStatus() {
        return currentStatus;
    }

    public TaskStatus getAttemptedStatus() {
        return attemptedStatus;
    }

    public Set<TaskStatus> getAllowedTransitions() {
        return allowedTransitions;
    }

    public String getSpecificReason() {
        return specificReason;
    }

    /**
     * Returnerar en användarvänlig förklaring av problemet och möjliga lösningar.
     */
    public String getUserFriendlyExplanation() {
        StringBuilder explanation = new StringBuilder();

        if (currentStatus == TaskStatus.CANCELLED) {
            explanation.append("Avbrutna uppdrag kan inte ändra status eftersom de är slutgiltiga. ");
            explanation.append("Om du behöver fortsätta arbetet, skapa ett nytt uppdrag istället.");
        } else if (currentStatus == TaskStatus.COMPLETED && attemptedStatus == TaskStatus.ACTIVE) {
            explanation.append("Avslutade uppdrag kan normalt inte återaktiveras. ");
            explanation.append("Om arbetet behöver fortsätta, överväg att skapa ett uppföljande uppdrag.");
        } else if (allowedTransitions != null && !allowedTransitions.isEmpty()) {
            explanation.append("Från status '").append(TaskStatusUtils.getDisplayName(currentStatus))
                    .append("' kan uppdraget bara ändras till: ");
            String options = allowedTransitions.stream()
                    .map(status -> "'" + TaskStatusUtils.getDisplayName(status) + "'")
                    .collect(Collectors.joining(" eller "));
            explanation.append(options);
        } else {
            explanation.append("Detta uppdrag är i ett slutgiltigt tillstånd och kan inte ändra status.");
        }

        return explanation.toString();
    }

    /**
     * Returnerar förslag på vad användaren kan göra istället.
     */
    public String getSuggestedActions() {
        if (currentStatus == TaskStatus.CANCELLED) {
            return "Skapa ett nytt uppdrag om arbetet behöver fortsätta.";
        } else if (currentStatus == TaskStatus.COMPLETED && attemptedStatus == TaskStatus.ACTIVE) {
            return "Skapa ett uppföljande uppdrag eller kontakta administratör för specialhantering.";
        } else if (allowedTransitions != null && !allowedTransitions.isEmpty()) {
            return "Välj en av de tillåtna statusövergångarna eller kontakta administratör.";
        } else {
            return "Kontakta administratör om denna statusändring är affärskritisk.";
        }
    }

    /**
     * Kontrollerar om detta fel kan lösas genom användaråtgärder.
     */
    public boolean isUserResolvable() {
        return allowedTransitions != null && !allowedTransitions.isEmpty();
    }

    /**
     * Returnerar en strukturerad representation för API-responses.
     */
    public TransitionErrorDetails getErrorDetails() {
        return new TransitionErrorDetails(
                taskId,
                taskNumber,
                currentStatus != null ? currentStatus.name() : null,
                attemptedStatus != null ? attemptedStatus.name() : null,
                allowedTransitions != null ?
                        allowedTransitions.stream().map(TaskStatus::name).collect(Collectors.toSet()) : null,
                specificReason,
                getUserFriendlyExplanation(),
                getSuggestedActions()
        );
    }

    @Override
    public String toString() {
        return "InvalidTaskTransitionException{" +
                "taskId=" + taskId +
                ", taskNumber='" + taskNumber + '\'' +
                ", currentStatus=" + currentStatus +
                ", attemptedStatus=" + attemptedStatus +
                ", allowedTransitions=" + allowedTransitions +
                ", specificReason='" + specificReason + '\'' +
                '}';
    }

    /**
     * Data class för strukturerade feldetaljer i API-responses.
     * Ger frontend all information den behöver för att hantera felet elegant.
     */
    public static class TransitionErrorDetails {
        private final Long taskId;
        private final String taskNumber;
        private final String currentStatus;
        private final String attemptedStatus;
        private final Set<String> allowedTransitions;
        private final String specificReason;
        private final String explanation;
        private final String suggestedActions;

        public TransitionErrorDetails(Long taskId, String taskNumber, String currentStatus,
                                      String attemptedStatus, Set<String> allowedTransitions,
                                      String specificReason, String explanation, String suggestedActions) {
            this.taskId = taskId;
            this.taskNumber = taskNumber;
            this.currentStatus = currentStatus;
            this.attemptedStatus = attemptedStatus;
            this.allowedTransitions = allowedTransitions;
            this.specificReason = specificReason;
            this.explanation = explanation;
            this.suggestedActions = suggestedActions;
        }

        // Getters
        public Long getTaskId() { return taskId; }
        public String getTaskNumber() { return taskNumber; }
        public String getCurrentStatus() { return currentStatus; }
        public String getAttemptedStatus() { return attemptedStatus; }
        public Set<String> getAllowedTransitions() { return allowedTransitions; }
        public String getSpecificReason() { return specificReason; }
        public String getExplanation() { return explanation; }
        public String getSuggestedActions() { return suggestedActions; }
    }
}