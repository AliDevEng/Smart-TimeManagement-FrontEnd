package com.gardening.timemanagement.dto.request;

import com.gardening.timemanagement.entity.Task.TaskStatus;
import com.gardening.timemanagement.util.TaskStatusUtils;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import jakarta.validation.constraints.*;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.LocalDate;

/**
 * DTO för att uppdatera befintliga uppdrag.
 *
 * Denna DTO representerar en avancerad approach till update-operationer där vi måste
 * balansera flexibilitet mot säkerhet. Till skillnad från CreateTaskDto innehåller
 * denna alla fält som Optional eftersom användare bara ska behöva ange de fält
 * de faktiskt vill ändra.
 *
 * Pedagogiska lärdomar från denna DTO:
 * - Partial update patterns för RESTful APIs
 * - Conditional validation baserat på context
 * - Business rule enforcement vid uppdateringar
 * - Strategic null-handling för optional fields
 *
 * Designfilosofi: Alla fält är Optional, men om de anges måste de vara giltiga.
 * Null värden betyder "ändra inte detta fält", tomma/ogiltiga värden ger valideringsfel.
 */
public class UpdateTaskDto {

    /**
     * Nytt uppdragsnummer (optional).
     *
     * Om det anges valideras det enligt samma regler som för CreateTaskDto,
     * men systemet måste också kontrollera att det nya numret inte redan används
     * av ett annat uppdrag.
     */
    @Size(min = 2, max = 100, message = "Uppdragsnummer måste vara mellan 2 och 100 tecken")
    @Pattern(regexp = "^[A-Za-z0-9\\-_]+$",
            message = "Uppdragsnummer får endast innehålla bokstäver, siffror, bindestreck och understreck")
    private String number;

    /**
     * Ny kund för uppdraget (optional).
     *
     * Observera att vi använder customer ID istället för hela Customer-objektet,
     * precis som i CreateTaskDto. Detta förhindrar komplex nested validation
     * och ger bättre prestanda.
     *
     * Affärsregler kan begränsa när customer får ändras - till exempel kanske
     * avslutade uppdrag inte får byta kund.
     */
    @Min(value = 1, message = "Kund-ID måste vara positivt")
    private Long customerId;

    /**
     * Ny beskrivning (optional).
     *
     * En tom sträng tolkas som att beskrivningen ska tas bort (sättas till null),
     * medan null betyder att beskrivningen inte ska ändras alls.
     */
    @Size(max = 2000, message = "Beskrivning får inte vara längre än 2000 tecken")
    private String description;

    /**
     * Nytt startdatum (optional).
     *
     * Affärsregler kan begränsa när startdatum får ändras. Till exempel:
     * - Kan inte sätta startdatum för avslutade uppdrag
     * - Startdatum kan inte vara efter slutdatum
     * - Vissa statusar kräver att startdatum finns
     */
    @PastOrPresent(message = "Startdatum kan inte vara i framtiden")
    private LocalDate startDate;

    /**
     * Nytt slutdatum (optional).
     *
     * Slutdatum hanteras normalt automatiskt när uppdrag avslutas,
     * men ibland behöver det justeras manuellt av affärsskäl.
     *
     * Affärsregler:
     * - Slutdatum kan inte vara före startdatum
     * - Bara vissa roller får sätta slutdatum manuellt
     * - Aktiva uppdrag bör normalt inte ha slutdatum
     */
    private LocalDate endDate;

    /**
     * Ny status (optional).
     *
     * Detta är det mest komplexa fältet eftersom statusändringar måste följa
     * strikt state machine-logik. Vi använder String istället för TaskStatus enum
     * för att kunna ge användarvänliga felmeddelanden.
     *
     * Alla statusövergångar valideras mot TaskStatusUtils.isValidTransition().
     */
    @ValidTaskStatusForUpdate(message = "Ogiltig uppdragsstatus för uppdatering")
    private String status;

    // Default konstruktor för JSON deserialization
    public UpdateTaskDto() {}

    /**
     * Convenience-konstruktor för programmatisk skapande.
     * Användbar i tester och när vi vet exakt vilka fält som ska uppdateras.
     */
    public UpdateTaskDto(String number, Long customerId, String description,
                         LocalDate startDate, String status) {
        this.number = number;
        this.customerId = customerId;
        this.description = description;
        this.startDate = startDate;
        this.status = status;
    }

    // Getters och setters med intelligent null-hantering

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number != null ? number.trim() : null;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description != null ? description.trim() : null;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getStatus() {
        return status;
    }

    /**
     * Intelligent setter för status som normaliserar input.
     */
    public void setStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            this.status = null; // Betyder "ändra inte status"
        } else {
            // Försök normalisera genom TaskStatusUtils
            var parsedStatus = TaskStatusUtils.fromString(status);
            this.status = parsedStatus.map(TaskStatus::name).orElse(status.trim());
        }
    }

    /**
     * Convenience-metod för att få status som enum.
     * Returnerar null om status inte ska ändras eller är ogiltig.
     */
    public TaskStatus getStatusAsEnum() {
        return status != null ? TaskStatusUtils.fromString(status).orElse(null) : null;
    }

    /**
     * Kontrollerar om denna DTO faktiskt innehåller några ändringar.
     * Användbart för att undvika onödiga databasoperationer.
     */
    public boolean hasAnyChanges() {
        return number != null ||
                customerId != null ||
                description != null ||
                startDate != null ||
                endDate != null ||
                status != null;
    }

    /**
     * Kontrollerar om beskrivningen ska rensas (sättas till null).
     * Detta skiljer mellan "ändra inte" (null) och "ta bort" (tom sträng).
     */
    public boolean shouldClearDescription() {
        return description != null && description.trim().isEmpty();
    }

    /**
     * Returnerar en lista av fält som kommer att ändras.
     * Användbart för audit logs och användarbekräftelse.
     */
    public java.util.List<String> getChangedFields() {
        java.util.List<String> changes = new java.util.ArrayList<>();

        if (number != null) changes.add("uppdragsnummer");
        if (customerId != null) changes.add("kund");
        if (description != null) {
            changes.add(shouldClearDescription() ? "beskrivning (rensa)" : "beskrivning");
        }
        if (startDate != null) changes.add("startdatum");
        if (endDate != null) changes.add("slutdatum");
        if (status != null) changes.add("status");

        return changes;
    }

    /**
     * Skapar en användarvänlig sammanfattning av ändringarna.
     */
    public String getChangesSummary() {
        java.util.List<String> changes = getChangedFields();

        if (changes.isEmpty()) {
            return "Inga ändringar";
        } else if (changes.size() == 1) {
            return "Ändrar " + changes.get(0);
        } else {
            String lastChange = changes.get(changes.size() - 1);
            String otherChanges = String.join(", ", changes.subList(0, changes.size() - 1));
            return "Ändrar " + otherChanges + " och " + lastChange;
        }
    }

    /**
     * Validerar att datum-kombinationen är logisk.
     * Detta är cross-field validation som inte kan hanteras av enkla annotations.
     */
    public boolean hasValidDateCombination() {
        if (startDate != null && endDate != null) {
            return !startDate.isAfter(endDate);
        }
        return true; // Om bara ett datum anges är det OK
    }

    /**
     * Returnerar felmeddelande om datum-kombinationen är ogiltig.
     */
    public String getDateValidationError() {
        if (!hasValidDateCombination()) {
            return "Startdatum (" + startDate + ") kan inte vara efter slutdatum (" + endDate + ")";
        }
        return null;
    }

    @Override
    public String toString() {
        return "UpdateTaskDto{" +
                "number='" + number + '\'' +
                ", customerId=" + customerId +
                ", description='" + (description != null ? description.substring(0, Math.min(50, description.length())) + "..." : "null") + '\'' +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", status='" + status + '\'' +
                ", changes=" + getChangedFields() +
                '}';
    }
}

/**
 * Custom validation annotation för TaskStatus i update-context.
 *
 * Detta är en specialiserad version av vår ValidTaskStatus annotation
 * som är anpassad för update-operationer där null betyder "ändra inte".
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidTaskStatusForUpdateValidator.class)
@interface ValidTaskStatusForUpdate {
    String message() default "Ogiltig uppdragsstatus för uppdatering";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

/**
 * Validator för status-uppdateringar.
 *
 * Denna validator förstår att null betyder "ändra inte status" och
 * validerar bara non-null värden.
 */
class ValidTaskStatusForUpdateValidator implements ConstraintValidator<ValidTaskStatusForUpdate, String> {

    @Override
    public void initialize(ValidTaskStatusForUpdate constraintAnnotation) {
        // Ingen speciell initialisering behövs
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // null är alltid giltigt för updates (betyder "ändra inte")
        if (value == null) {
            return true;
        }

        // Tom sträng är också OK (kan betyda "rensa status" i vissa contexts)
        if (value.trim().isEmpty()) {
            return true;
        }

        // Använd vår TaskStatusUtils för faktisk validering
        boolean isValid = TaskStatusUtils.isValidStatus(value);

        if (!isValid) {
            // Skapa specifikt felmeddelande för update-context
            String validStatuses = String.join(", ",
                    TaskStatusUtils.getAllStatuses().stream()
                            .map(TaskStatusUtils::getDisplayName)
                            .toArray(String[]::new));

            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                            "Ogiltig status för uppdatering: '" + value + "'. Giltiga värden: " + validStatuses +
                                    ". Lämna tomt för att inte ändra status.")
                    .addConstraintViolation();
        }

        return isValid;
    }
}
