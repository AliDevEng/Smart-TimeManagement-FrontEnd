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
 * DTO för att skapa nya uppdrag.
 *
 * Denna DTO representerar en avancerad approach till API-design där vi balanserar
 * användarvänlighet mot systemsäkerhet. Den innehåller endast fält som användare
 * bör kunna kontrollera vid skapande, medan systemgenererade fält hanteras automatiskt.
 *
 * Pedagogiska lärdomar från denna DTO:
 * - Custom validation annotations för komplex affärslogik
 * - Enum-hantering med användarvänliga felmeddelanden
 * - Cross-field validation för datum och affärsregler
 * - Strategic field selection för optimal användarupplevelse
 */
public class CreateTaskDto {

    /**
     * Unikt uppdragsnummer som kunden och teamet använder för identifiering.
     *
     * Denna validering kombinerar tekniska krav (längd, format) med affärsregler
     * (måste vara unikt i systemet). Observera att vi använder @Pattern för att
     * säkerställa att uppdragsnummer följer ett konsistent format.
     */
    @NotBlank(message = "Uppdragsnummer måste anges")
    @Size(min = 2, max = 100, message = "Uppdragsnummer måste vara mellan 2 och 100 tecken")
    @Pattern(regexp = "^[A-Za-z0-9\\-_]+$",
            message = "Uppdragsnummer får endast innehålla bokstäver, siffror, bindestreck och understreck")
    private String number;

    /**
     * ID för kunden som detta uppdrag utförs för.
     *
     * Vi använder bara customer ID istället för hela Customer-objektet eftersom:
     * 1. Det minskar komplexiteten i vår DTO
     * 2. Det förhindrar att användare skickar inkonsistent kunddata
     * 3. Det ger bättre prestanda genom att undvika onödiga nested objects
     * 4. Det följer REST-principen att referera till relaterade resurser via ID
     */
    @NotNull(message = "Kund måste anges")
    @Min(value = 1, message = "Kund-ID måste vara positivt")
    private Long customerId;

    /**
     * Beskrivning av uppdraget.
     *
     * Detta fält är valfritt vid skapande eftersom uppdraget kan skapas först
     * och detaljerna fyllas i senare. Vi tillåter dock ganska långa beskrivningar
     * eftersom trädgårdsuppdrag ofta har många detaljer som behöver dokumenteras.
     */
    @Size(max = 2000, message = "Beskrivning får inte vara längre än 2000 tecken")
    private String description;

    /**
     * Planerat startdatum för uppdraget.
     *
     * Detta fält är valfritt eftersom uppdrag ofta skapas innan exakt startdatum är bestämt.
     * Vi validerar dock att om ett startdatum anges får det inte vara för långt tillbaka
     * i tiden, vilket förhindrar dataentry-fel.
     */
    @PastOrPresent(message = "Startdatum kan inte vara i framtiden vid skapande")
    private LocalDate startDate;

    /**
     * Planerat slutdatum för uppdraget.
     *
     * Observera att vi INTE inkluderar endDate i CreateTaskDto. Detta är ett medvetet
     * designbeslut eftersom slutdatum normalt inte är känt när uppdraget skapas.
     * Slutdatum sätts istället när uppdraget faktiskt avslutas genom separata
     * business operations i TaskService.
     */

    /**
     * Initial status för uppdraget.
     *
     * Vi tillåter användare att specificera initial status, men med strikta begränsningar.
     * De flesta uppdrag bör starta som ACTIVE, men ibland kan det finnas behov av att
     * skapa uppdrag som CANCELLED (för historisk registrering).
     *
     * Vi använder String istället för TaskStatus enum direkt för att kunna ge
     * användarvänliga felmeddelanden genom vår TaskStatusUtils.
     */
    @ValidTaskStatus(message = "Ogiltig uppdragsstatus")
    private String status;

    // Default konstruktor för JSON deserialization
    public CreateTaskDto() {}

    /**
     * Konstruktor för programmatisk skapande med alla obligatoriska fält.
     * Sätter automatiskt status till ACTIVE som är det vanligaste fallet.
     */
    public CreateTaskDto(String number, Long customerId) {
        this.number = number;
        this.customerId = customerId;
        this.status = TaskStatus.ACTIVE.name(); // Default till ACTIVE
    }

    /**
     * Fullständig konstruktor för test och programmatisk användning.
     */
    public CreateTaskDto(String number, Long customerId, String description,
                         LocalDate startDate, String status) {
        this.number = number;
        this.customerId = customerId;
        this.description = description;
        this.startDate = startDate;
        this.status = status;
    }

    // Getters och setters med valideringslogik

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

    public String getStatus() {
        return status;
    }

    /**
     * Intelligent setter för status som använder vår TaskStatusUtils.
     * Detta demonstrerar hur vi kan ha "smart" setters som förbättrar användarupplevelsen.
     */
    public void setStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            // Default till ACTIVE om ingen status anges
            this.status = TaskStatus.ACTIVE.name();
        } else {
            // Försök normalisera input genom vår TaskStatusUtils
            var parsedStatus = TaskStatusUtils.fromString(status);
            this.status = parsedStatus.map(TaskStatus::name).orElse(status.trim());
        }
    }

    /**
     * Convenience-metod för att få TaskStatus som enum.
     * Denna metod används internt av vår mapper och service-lager.
     */
    public TaskStatus getStatusAsEnum() {
        return TaskStatusUtils.fromString(status).orElse(TaskStatus.ACTIVE);
    }

    /**
     * Affärslogik-validering för att kontrollera om denna DTO representerar
     * ett giltigt nytt uppdrag enligt våra affärsregler.
     *
     * Detta är ett exempel på hur vi kan bygga in affärslogik-validering
     * direkt i våra DTOs för tidig feldetektering.
     */
    public boolean isValidForCreation() {
        // Grundläggande fältvalidering
        if (number == null || number.trim().isEmpty()) return false;
        if (customerId == null || customerId <= 0) return false;

        // Status-validering
        if (!TaskStatusUtils.isValidStatus(status)) return false;

        // Affärslogik: nya uppdrag bör normalt vara ACTIVE eller CANCELLED
        TaskStatus statusEnum = getStatusAsEnum();
        if (statusEnum == TaskStatus.COMPLETED) {
            // COMPLETED uppdrag skapas inte direkt - de övergår från ACTIVE
            return false;
        }

        return true;
    }

    /**
     * Skapar en användarvänlig beskrivning av detta uppdrag för loggning.
     */
    public String getDisplaySummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Uppdrag ").append(number != null ? number : "[inget nummer]");
        summary.append(" för kund-ID ").append(customerId != null ? customerId : "[okänd]");

        if (description != null && !description.trim().isEmpty()) {
            String shortDesc = description.length() > 50 ?
                    description.substring(0, 47) + "..." : description;
            summary.append(" - ").append(shortDesc);
        }

        return summary.toString();
    }

    @Override
    public String toString() {
        return "CreateTaskDto{" +
                "number='" + number + '\'' +
                ", customerId=" + customerId +
                ", description='" + (description != null ? description.substring(0, Math.min(50, description.length())) + "..." : "null") + '\'' +
                ", startDate=" + startDate +
                ", status='" + status + '\'' +
                '}';
    }
}

/**
 * Custom validation annotation för TaskStatus validering.
 *
 * Detta är ett avancerat exempel på hur man skapar anpassade validerings-annotations
 * i Spring Boot. Denna annotation använder vår TaskStatusUtils för att validera
 * att inkommande status-strängar är giltiga enligt våra affärsregler.
 *
 * Pedagogisk poäng: Custom validators låter oss centralisera komplex valideringslogik
 * och återanvända den över hela applikationen med clean, deklarativ syntax.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidTaskStatusValidator.class)
@interface ValidTaskStatus {
    String message() default "Ogiltig uppdragsstatus";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

/**
 * Validator-implementationen för vår @ValidTaskStatus annotation.
 *
 * Denna klass innehåller den faktiska valideringslogiken som körs när
 * Spring Boot encounter vår @ValidTaskStatus annotation på ett fält.
 */
class ValidTaskStatusValidator implements ConstraintValidator<ValidTaskStatus, String> {

    @Override
    public void initialize(ValidTaskStatus constraintAnnotation) {
        // Ingen speciell initialisering behövs för vår enkla validator
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // null eller tom sträng är giltig (kommer att defaulta till ACTIVE)
        if (value == null || value.trim().isEmpty()) {
            return true;
        }

        // Använd vår TaskStatusUtils för validering
        boolean isValid = TaskStatusUtils.isValidStatus(value);

        if (!isValid) {
            // Skapa användarvänligt felmeddelande med giltiga alternativ
            String validStatuses = String.join(", ",
                    TaskStatusUtils.getAllStatuses().stream()
                            .map(TaskStatusUtils::getDisplayName)
                            .toArray(String[]::new));

            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                            "Ogiltig status '" + value + "'. Giltiga värden: " + validStatuses)
                    .addConstraintViolation();
        }

        return isValid;
    }
}