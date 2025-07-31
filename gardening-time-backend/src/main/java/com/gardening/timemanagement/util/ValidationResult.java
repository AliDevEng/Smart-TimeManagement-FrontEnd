package com.gardening.timemanagement.util;


import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Centraliserad ValidationResult-klass för hela enterprise-systemet.
 *
 * Denna klass representerar resultatet av alla typer av validering i systemet,
 * från grundläggande input-validering till komplexa business rule evaluations.
 *
 * Design-principer:
 * - Immutable value object för thread safety
 * - Rich information för användargränssnitt och debugging
 * - Konsistent interface för alla validering-scenarios
 * - Stöd för både framgång, misslyckande och varningar
 *
 * Används av:
 * - WorkDayValidationUtils för komplex business logic
 * - EmployeeValidationUtils för medarbetarregler
 * - TaskValidationUtils för uppdragsvalidering
 * - Alla service-lager för input-validering
 */
public final class ValidationResult {

    private final boolean isValid;
    private final String message;
    private final boolean hasWarning;
    private final String warningMessage;

    /**
     * Privat konstruktor för att säkerställa kontrollerad instansering.
     * Använd factory methods för att skapa ValidationResult-objekt.
     */
    private ValidationResult(boolean isValid, String message, boolean hasWarning, String warningMessage) {
        this.isValid = isValid;
        this.message = message != null ? message.trim() : "";
        this.hasWarning = hasWarning;
        this.warningMessage = warningMessage != null ? warningMessage.trim() : null;
    }

    // =================================================================
    // FACTORY METHODS - Standard sätt att skapa ValidationResult
    // =================================================================

    /**
     * Skapar ett framgångsrikt validering-resultat utan meddelande.
     * Används för enkel validering som passerade utan kommentarer.
     *
     * @return ValidationResult som indikerar framgång
     */
    public static ValidationResult valid() {
        return new ValidationResult(true, null, false, null);
    }

    /**
     * Skapar ett framgångsrikt validering-resultat med bekräftelse-meddelande.
     * Användbart när du vill ge positiv feedback om vad som validerades.
     *
     * @param message Bekräftelse-meddelande som förklarar framgången
     * @return ValidationResult som indikerar framgång med kontext
     */
    public static ValidationResult valid(String message) {
        return new ValidationResult(true, message, false, null);
    }

    /**
     * Skapar ett misslyckat validering-resultat med fel-meddelande.
     * Används när validering misslyckades och operationen inte bör fortsätta.
     *
     * @param message Fel-meddelande som förklarar vad som gick fel
     * @return ValidationResult som indikerar misslyckande
     */
    public static ValidationResult invalid(String message) {
        return new ValidationResult(false, message, false, null);
    }

    /**
     * Skapar ett framgångsrikt resultat med varning.
     * Används när validering tekniskt lyckades men det finns potentiella problem
     * som användaren bör vara medveten om.
     *
     * @param warningMessage Varning som beskriver potentiella problem
     * @return ValidationResult som är giltigt men med varningar
     */
    public static ValidationResult validWithWarning(String warningMessage) {
        return new ValidationResult(true, "Giltig med varningar", true, warningMessage);
    }

    /**
     * Skapar ett framgångsrikt resultat med både bekräftelse och varning.
     * Använd när du vill både bekräfta framgång och varna för potentiella problem.
     *
     * @param confirmationMessage Bekräftelse av vad som validerades
     * @param warningMessage Varning om potentiella problem
     * @return ValidationResult med både bekräftelse och varning
     */
    public static ValidationResult validWithWarning(String confirmationMessage, String warningMessage) {
        return new ValidationResult(true, confirmationMessage, true, warningMessage);
    }

    // =================================================================
    // ACCESSOR METHODS - För att läsa validation-resultat
    // =================================================================

    /**
     * Kontrollerar om valideringen lyckades.
     *
     * @return true om validering var framgångsrik, false om den misslyckades
     */
    public boolean isValid() {
        return isValid;
    }

    /**
     * Hämtar huvud-meddelandet från valideringen.
     * Detta kan vara antingen ett bekräftelse-meddelande eller fel-meddelande.
     *
     * @return Optional med meddelandet, eller empty() om inget meddelande finns
     */
    public Optional<String> getMessage() {
        return message != null && !message.isEmpty() ? Optional.of(message) : Optional.empty();
    }

    /**
     * Kontrollerar om detta giltiga resultat innehåller varningar.
     * Endast relevant när isValid() returnerar true.
     *
     * @return true om resultatet har varningar, false annars
     */
    public boolean hasWarning() {
        return hasWarning;
    }

    /**
     * Hämtar varnings-meddelandet om det finns.
     * Endast relevant när hasWarning() returnerar true.
     *
     * @return Optional med varnings-meddelandet, eller empty() om ingen varning
     */
    public Optional<String> getWarningMessage() {
        return warningMessage != null && !warningMessage.isEmpty() ?
                Optional.of(warningMessage) : Optional.empty();
    }

    /**
     * Kontrollerar om resultatet är perfekt (giltigt utan varningar).
     * Användbart för att skilja mellan "tekniskt OK" och "helt OK".
     *
     * @return true endast om både giltigt och utan varningar
     */
    public boolean isPerfect() {
        return isValid && !hasWarning;
    }

    // =================================================================
    // UTILITY METHODS - För avancerad hantering av resultat
    // =================================================================

    /**
     * Kombinerar detta ValidationResult med ett annat.
     * Användbart när flera validering-steg ska kombineras till ett slutresultat.
     *
     * Kombinerings-regler:
     * - Resultat är giltigt endast om båda är giltiga
     * - Meddelanden kombineras med semikolon-separation
     * - Varningar från båda bevaras
     *
     * @param other Det andra ValidationResult att kombinera med
     * @return Nytt ValidationResult som representerar kombinationen
     */
    public ValidationResult combine(ValidationResult other) {
        if (other == null) {
            return this;
        }

        boolean combinedValid = this.isValid && other.isValid;

        // Kombinera huvud-meddelanden
        StringBuilder combinedMessage = new StringBuilder();
        if (this.message != null && !this.message.trim().isEmpty()) {
            combinedMessage.append(this.message);
        }
        if (other.message != null && !other.message.trim().isEmpty()) {
            if (combinedMessage.length() > 0) {
                combinedMessage.append("; ");
            }
            combinedMessage.append(other.message);
        }

        // Kombinera varningar
        boolean combinedHasWarning = this.hasWarning || other.hasWarning;
        StringBuilder combinedWarning = new StringBuilder();

        if (this.hasWarning && this.warningMessage != null) {
            combinedWarning.append(this.warningMessage);
        }
        if (other.hasWarning && other.warningMessage != null) {
            if (combinedWarning.length() > 0) {
                combinedWarning.append("; ");
            }
            combinedWarning.append(other.warningMessage);
        }

        return new ValidationResult(
                combinedValid,
                combinedMessage.toString(),
                combinedHasWarning,
                combinedWarning.length() > 0 ? combinedWarning.toString() : null
        );
    }

    /**
     * Hämtar alla meddelanden (både huvud och varningar) som en lista.
     * Perfekt för användargränssnitt som vill visa all relevant information.
     *
     * @return Lista med alla meddelanden, kan vara tom men aldrig null
     */
    public List<String> getAllMessages() {
        List<String> messages = new ArrayList<>();

        if (message != null && !message.trim().isEmpty()) {
            messages.add(message);
        }

        if (hasWarning && warningMessage != null && !warningMessage.trim().isEmpty()) {
            messages.add("VARNING: " + warningMessage);
        }

        return messages;
    }

    /**
     * Skapar en detaljerad beskrivning av validation-resultatet.
     * Användbar för logging, debugging och detaljerad error reporting.
     *
     * @return Formaterad sträng med komplett validation-information
     */
    public String getDetailedDescription() {
        StringBuilder description = new StringBuilder();

        description.append("Status: ").append(isValid ? "GILTIG" : "OGILTIG");

        if (hasWarning) {
            description.append(" (med varningar)");
        }

        if (message != null && !message.trim().isEmpty()) {
            description.append("\nMeddelande: ").append(message);
        }

        if (hasWarning && warningMessage != null && !warningMessage.trim().isEmpty()) {
            description.append("\nVarning: ").append(warningMessage);
        }

        return description.toString();
    }

    // =================================================================
    // STANDARD OBJECT METHODS
    // =================================================================

    @Override
    public String toString() {
        return "ValidationResult{" +
                "isValid=" + isValid +
                ", message='" + message + '\'' +
                ", hasWarning=" + hasWarning +
                ", warningMessage='" + warningMessage + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ValidationResult that = (ValidationResult) o;

        if (isValid != that.isValid) return false;
        if (hasWarning != that.hasWarning) return false;
        if (message != null ? !message.equals(that.message) : that.message != null) return false;
        return warningMessage != null ? warningMessage.equals(that.warningMessage) : that.warningMessage == null;
    }

    @Override
    public int hashCode() {
        int result = (isValid ? 1 : 0);
        result = 31 * result + (message != null ? message.hashCode() : 0);
        result = 31 * result + (hasWarning ? 1 : 0);
        result = 31 * result + (warningMessage != null ? warningMessage.hashCode() : 0);
        return result;
    }
}