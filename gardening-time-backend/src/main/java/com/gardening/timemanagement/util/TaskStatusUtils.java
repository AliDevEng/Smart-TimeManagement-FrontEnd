package com.gardening.timemanagement.util;

import com.gardening.timemanagement.entity.Task.TaskStatus;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;


public class TaskStatusUtils {

    /**
     * Mappning mellan enum-värden och användarmeddelanden på svenska.
     * Detta möjliggör användarvänliga meddelanden i vårt API utan att
     * exponera interna tekniska enum-namn.
     */
    private static final Map<TaskStatus, String> STATUS_DISPLAY_NAMES = Map.of(
            TaskStatus.ACTIVE, "Pågående",
            TaskStatus.COMPLETED, "Avslutad",
            TaskStatus.CANCELLED, "Avbruten"
    );



    private static final Map<TaskStatus, String> STATUS_DESCRIPTIONS = Map.of(
            TaskStatus.ACTIVE, "Uppdraget pågår och kan ta emot ny arbetstidsregistrering",
            TaskStatus.COMPLETED, "Uppdraget är slutfört och avslutat enligt plan",
            TaskStatus.CANCELLED, "Uppdraget har avbrutits innan slutförande"
    );



    private static final Map<TaskStatus, Set<TaskStatus>> VALID_TRANSITIONS = Map.of(
            TaskStatus.ACTIVE, Set.of(TaskStatus.COMPLETED, TaskStatus.CANCELLED),
            TaskStatus.COMPLETED, Set.of(TaskStatus.CANCELLED),
            TaskStatus.CANCELLED, Set.of() // Ingen övergång tillåten från CANCELLED
    );


    public static Optional<TaskStatus> fromString(String statusString) {
        if (statusString == null || statusString.trim().isEmpty()) {
            return Optional.empty();
        }

        String cleanStatus = statusString.trim().toUpperCase();

        // Först: försök direkt enum-matchning
        try {
            return Optional.of(TaskStatus.valueOf(cleanStatus));
        } catch (IllegalArgumentException e) {
            // Inget direkt match - försök svenska beskrivningar
        }

        // Andra försöket: matcha mot svenska displaynamn
        String lowerCaseInput = statusString.trim().toLowerCase();
        return STATUS_DISPLAY_NAMES.entrySet().stream()
                .filter(entry -> entry.getValue().toLowerCase().equals(lowerCaseInput))
                .map(Map.Entry::getKey)
                .findFirst();
    }


    public static String getDisplayName(TaskStatus status) {
        if (status == null) {
            return "Okänd status";
        }
        return STATUS_DISPLAY_NAMES.getOrDefault(status, status.name());
    }



    public static String getDescription(TaskStatus status) {
        if (status == null) {
            return "Ingen status angiven";
        }
        return STATUS_DESCRIPTIONS.getOrDefault(status, "Ingen beskrivning tillgänglig");
    }



    public static boolean isValidTransition(TaskStatus currentStatus, TaskStatus newStatus) {
        if (currentStatus == null || newStatus == null) {
            return false;
        }

        // Om status inte ändras är det alltid giltigt
        if (currentStatus == newStatus) {
            return true;
        }

        // Kontrollera om övergången finns i vår state machine
        Set<TaskStatus> allowedTransitions = VALID_TRANSITIONS.get(currentStatus);
        return allowedTransitions != null && allowedTransitions.contains(newStatus);
    }



    public static Set<TaskStatus> getValidTransitions(TaskStatus currentStatus) {
        if (currentStatus == null) {
            return Set.of();
        }
        return VALID_TRANSITIONS.getOrDefault(currentStatus, Set.of());
    }

    /**
     * Skapar ett detaljerat felmeddelande för ogiltig statusövergång.
     * Ger användaren specifik information om vad som gick fel och vad som är tillåtet.
     *
     * @param currentStatus Nuvarande status
     * @param attemptedStatus Status som försöktes sättas
     * @return Detaljerat felmeddelande
     */
    public static String createTransitionErrorMessage(TaskStatus currentStatus, TaskStatus attemptedStatus) {
        if (currentStatus == null) {
            return "Kan inte ändra status: nuvarande status är okänd";
        }

        if (attemptedStatus == null) {
            return "Kan inte ändra status: ny status är okänd";
        }

        Set<TaskStatus> validTransitions = getValidTransitions(currentStatus);

        if (validTransitions.isEmpty()) {
            return String.format("Uppdrag med status '%s' kan inte ändra status - statusen är slutgiltig",
                    getDisplayName(currentStatus));
        }

        String validOptions = validTransitions.stream()
                .map(TaskStatusUtils::getDisplayName)
                .collect(Collectors.joining(", "));

        return String.format("Ogiltig statusändring från '%s' till '%s'. Tillåtna övergångar: %s",
                getDisplayName(currentStatus),
                getDisplayName(attemptedStatus),
                validOptions);
    }



    public static List<TaskStatus> getAllStatuses() {
        return Arrays.asList(TaskStatus.values());
    }



    public static boolean isValidStatus(String statusString) {
        return fromString(statusString).isPresent();
    }



    public static Map<String, String> getStatusSummary() {
        return STATUS_DISPLAY_NAMES.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> entry.getKey().name(),
                        entry -> entry.getValue() + ": " + STATUS_DESCRIPTIONS.get(entry.getKey())
                ));
    }



    public static boolean canAcceptWorkTime(TaskStatus status) {
        return TaskStatus.ACTIVE.equals(status);
    }



    public static boolean isFinalized(TaskStatus status) {
        return TaskStatus.COMPLETED.equals(status) || TaskStatus.CANCELLED.equals(status);
    }
}