package com.gardening.timemanagement.util;

import com.gardening.timemanagement.entity.Employee;
import com.gardening.timemanagement.entity.EmployeeTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.regex.Pattern;
import java.util.Optional;

/**
 * Centraliserad utility-klass för all Employee-relaterad validering.
 *
 * Denna klass samlar all affärslogik och valideringsregler för medarbetare
 * på ett ställe för att säkerställa konsistens och enkelhet att underhålla.
 *
 * Strukturerad i fyra huvudområden:
 * 1. Grundläggande identitetsvalidering
 * 2. Affärsregelvalidering
 * 3. Arbetstidsvalidering
 * 4. Statistisk validering
 */
public final class EmployeeValidationUtils {

    // =================================================================
    // KONSTANTER FÖR AFFÄRSREGLER
    // =================================================================

    // Grundläggande identitetsregler
    public static final int MIN_NAME_LENGTH = 2;
    public static final int MAX_NAME_LENGTH = 255;

    // Svenska telefonnummer regex - stöder mobil och fast telefoni
    private static final Pattern SWEDISH_PHONE_PATTERN = Pattern.compile(
            "^(\\+46|0)[1-9]\\d{7,9}$"
    );

    // Arbetstidsregler för rimlighetskontroller
    public static final double MIN_REASONABLE_DAILY_HOURS = 0.25; // 15 minuter minimum
    public static final double MAX_REASONABLE_DAILY_HOURS = 16.0;  // 16 timmar maximum
    public static final double STANDARD_WORK_DAY_HOURS = 8.0;
    public static final int MAX_LUNCH_MINUTES = 120; // 2 timmar max lunch
    public static final double MAX_DRIVE_TIME_HOURS = 12.0; // Max 12h körtid per dag

    // Statistiska begränsningar för prestanda
    public static final int MAX_REPORT_PERIOD_DAYS = 366; // Max 1 år för rapporter
    public static final int MAX_HISTORICAL_YEARS = 5;     // Max 5 år bakåt i tiden

    // Privat konstruktor - detta är en utility-klass
    private EmployeeValidationUtils() {
        throw new UnsupportedOperationException("EmployeeValidationUtils är en utility-klass");
    }

    // =================================================================
    // 1. GRUNDLÄGGANDE IDENTITETSVALIDERING
    // =================================================================

    /**
     * Validerar medarbetarens namn enligt affärsregler.
     *
     * Kontrollerar längd, tecken och format för att säkerställa
     * att namnet är användbart i rapporter och kommunikation.
     *
     * @param name Namnet att validera
     * @return ValidationResult med resultat och eventuellt felmeddelande
     */
    public static ValidationResult validateEmployeeName(String name) {
        if (name == null) {
            return ValidationResult.invalid("Medarbetarens namn får inte vara null");
        }

        String trimmedName = name.trim();

        if (trimmedName.isEmpty()) {
            return ValidationResult.invalid("Medarbetarens namn får inte vara tomt");
        }

        if (trimmedName.length() < MIN_NAME_LENGTH) {
            return ValidationResult.invalid(
                    String.format("Medarbetarens namn måste vara minst %d tecken långt", MIN_NAME_LENGTH)
            );
        }

        if (trimmedName.length() > MAX_NAME_LENGTH) {
            return ValidationResult.invalid(
                    String.format("Medarbetarens namn får inte vara längre än %d tecken", MAX_NAME_LENGTH)
            );
        }

        // Kontrollera att namnet innehåller åtminstone en bokstav
        if (!trimmedName.matches(".*[a-zA-ZåäöÅÄÖ].*")) {
            return ValidationResult.invalid("Medarbetarens namn måste innehålla minst en bokstav");
        }

        // Kontrollera att namnet inte bara består av specialtecken
        if (trimmedName.matches("^[^a-zA-ZåäöÅÄÖ0-9\\s]+$")) {
            return ValidationResult.invalid("Medarbetarens namn kan inte bara bestå av specialtecken");
        }

        return ValidationResult.valid();
    }

    /**
     * Validerar svenskt telefonnummer.
     *
     * Stöder både mobilnummer och fasta nummer i svenska format.
     * Accepterar olika format men normaliserar för lagring.
     *
     * @param phone Telefonnumret att validera (kan vara null för valfria fält)
     * @return ValidationResult med resultat och normaliserat nummer om giltigt
     */
    public static ValidationResult validatePhoneNumber(String phone) {
        // Null eller tom sträng är OK - telefonnummer är valfritt
        if (phone == null || phone.trim().isEmpty()) {
            return ValidationResult.valid();
        }

        // Normalisera numret - ta bort mellanslag, bindestreck och parenteser
        String cleanPhone = phone.trim()
                .replaceAll("[\\s\\-()\\.]", "");

        if (!SWEDISH_PHONE_PATTERN.matcher(cleanPhone).matches()) {
            return ValidationResult.invalid(
                    "Ogiltigt telefonnummer. Använd svenskt format (t.ex. 070-1234567 eller +46701234567)"
            );
        }

        // Returnera det normaliserade numret för lagring
        return ValidationResult.validWithValue(normalizePhoneNumber(cleanPhone));
    }

    /**
     * Normaliserar telefonnummer till enhetligt format för lagring.
     * Konverterar alla nummer till format med +46 prefix.
     */
    private static String normalizePhoneNumber(String cleanPhone) {
        if (cleanPhone.startsWith("+46")) {
            return cleanPhone;
        } else if (cleanPhone.startsWith("0")) {
            return "+46" + cleanPhone.substring(1);
        }
        return cleanPhone;
    }

    // =================================================================
    // 2. AFFÄRSREGELVALIDERING
    // =================================================================

    /**
     * Kontrollerar om en medarbetare kan tilldelas nytt arbete.
     *
     * En medarbetare kan bara tilldelas arbete om de är:
     * - Aktiva i systemet
     * - Inte blockerade av administrativa skäl
     *
     * @param employee Medarbetaren att kontrollera
     * @return ValidationResult med resultat och förklaring
     */
    public static ValidationResult validateCanAssignWork(Employee employee) {
        if (employee == null) {
            return ValidationResult.invalid("Medarbetare kan inte vara null");
        }

        if (!Boolean.TRUE.equals(employee.getIsActive())) {
            return ValidationResult.invalid(
                    String.format("Medarbetaren '%s' är inaktiv och kan inte tilldelas nytt arbete",
                            employee.getName())
            );
        }

        return ValidationResult.valid();
    }

    /**
     * Validerar om en medarbetare kan inaktiveras.
     *
     * Kontrollerar affärsregler för inaktivering som att medarbetaren
     * inte får ha pågående eller framtida arbete registrerat.
     *
     * @param employee Medarbetaren som ska inaktiveras
     * @param hasFutureWork Om medarbetaren har framtida arbete (från service layer)
     * @return ValidationResult med resultat
     */
    public static ValidationResult validateCanDeactivate(Employee employee, boolean hasFutureWork) {
        if (employee == null) {
            return ValidationResult.invalid("Medarbetare kan inte vara null");
        }

        if (!Boolean.TRUE.equals(employee.getIsActive())) {
            return ValidationResult.invalid(
                    String.format("Medarbetaren '%s' är redan inaktiv", employee.getName())
            );
        }

        if (hasFutureWork) {
            return ValidationResult.invalid(
                    String.format("Medarbetaren '%s' kan inte inaktiveras eftersom den har framtida arbete registrerat. Ta bort dessa registreringar först.",
                            employee.getName())
            );
        }

        return ValidationResult.valid();
    }

    // =================================================================
    // 3. ARBETSTIDSVALIDERING - DEN MEST KOMPLEXA DELEN
    // =================================================================

    /**
     * Omfattande validering av arbetstidsregistrering.
     *
     * Detta är kärnan i arbetstidslogiken och kontrollerar:
     * - Grundläggande tidslogik (start före slut)
     * - Lunchtidsrimklighet
     * - Körtidslogik för förare
     * - Rimlighetskontroller för arbetstid
     *
     * @param employeeTime Arbetstidsregistreringen att validera
     * @return ValidationResult med detaljerat resultat
     */
    public static ValidationResult validateWorkTimeEntry(EmployeeTime employeeTime) {
        if (employeeTime == null) {
            return ValidationResult.invalid("Arbetstidsregistrering kan inte vara null");
        }

        // Validera grundläggande tidslogik
        ValidationResult basicTimeResult = validateBasicTimeLogic(employeeTime);
        if (!basicTimeResult.isValid()) {
            return basicTimeResult;
        }

        // Validera lunchtid
        ValidationResult lunchResult = validateLunchTime(employeeTime);
        if (!lunchResult.isValid()) {
            return lunchResult;
        }

        // Validera körtid om förare
        ValidationResult driveTimeResult = validateDriveTime(employeeTime);
        if (!driveTimeResult.isValid()) {
            return driveTimeResult;
        }

        // Validera total arbetstid för rimlighet
        ValidationResult totalTimeResult = validateTotalWorkTime(employeeTime);
        if (!totalTimeResult.isValid()) {
            return totalTimeResult;
        }

        return ValidationResult.valid();
    }

    /**
     * Validerar grundläggande tidslogik - start och sluttider.
     */
    private static ValidationResult validateBasicTimeLogic(EmployeeTime employeeTime) {
        LocalTime startTime = employeeTime.getStartTime();
        LocalTime endTime = employeeTime.getEndTime();

        if (startTime == null) {
            return ValidationResult.invalid("Starttid måste anges");
        }

        if (endTime == null) {
            return ValidationResult.invalid("Sluttid måste anges");
        }

        if (!endTime.isAfter(startTime)) {
            return ValidationResult.invalid("Sluttid måste vara efter starttid");
        }

        // Kontrollera att arbetstiden inte är orimligt kort
        long workMinutes = ChronoUnit.MINUTES.between(startTime, endTime);
        if (workMinutes < 15) { // Minimum 15 minuter
            return ValidationResult.invalid("Arbetstid kan inte vara kortare än 15 minuter");
        }

        return ValidationResult.valid();
    }

    /**
     * Validerar lunchtid enligt affärsregler.
     */
    private static ValidationResult validateLunchTime(EmployeeTime employeeTime) {
        Integer lunchMinutes = employeeTime.getLunchMinutes();

        if (lunchMinutes == null) {
            return ValidationResult.invalid("Lunchtid måste anges (kan vara 0)");
        }

        if (lunchMinutes < 0) {
            return ValidationResult.invalid("Lunchtid kan inte vara negativ");
        }

        if (lunchMinutes > MAX_LUNCH_MINUTES) {
            return ValidationResult.invalid(
                    String.format("Lunchtid kan inte vara längre än %d minuter", MAX_LUNCH_MINUTES)
            );
        }

        // Kontrollera att lunchtid inte är längre än total arbetstid
        long totalWorkMinutes = ChronoUnit.MINUTES.between(
                employeeTime.getStartTime(), employeeTime.getEndTime()
        );

        if (lunchMinutes >= totalWorkMinutes) {
            return ValidationResult.invalid("Lunchtid kan inte vara längre än eller lika med total arbetstid");
        }

        return ValidationResult.valid();
    }

    /**
     * Validerar körtid för förare.
     */
    private static ValidationResult validateDriveTime(EmployeeTime employeeTime) {
        Boolean isDriver = employeeTime.getIsDriver();

        // Om inte förare ska körtid vara 0 eller null
        if (!Boolean.TRUE.equals(isDriver)) {
            if (employeeTime.getDriveTimeHours() != null &&
                    employeeTime.getDriveTimeHours().doubleValue() > 0) {
                return ValidationResult.invalid("Endast förare kan registrera körtid");
            }
            return ValidationResult.valid();
        }

        // Om förare - validera körtid
        if (employeeTime.getDriveTimeHours() == null) {
            return ValidationResult.invalid("Förare måste ha körtid angiven (kan vara 0)");
        }

        double driveHours = employeeTime.getDriveTimeHours().doubleValue();

        if (driveHours < 0) {
            return ValidationResult.invalid("Körtid kan inte vara negativ");
        }

        if (driveHours > MAX_DRIVE_TIME_HOURS) {
            return ValidationResult.invalid(
                    String.format("Körtid kan inte vara längre än %.1f timmar per dag", MAX_DRIVE_TIME_HOURS)
            );
        }

        return ValidationResult.valid();
    }

    /**
     * Validerar total arbetstid för rimlighet.
     */
    private static ValidationResult validateTotalWorkTime(EmployeeTime employeeTime) {
        double totalHours = employeeTime.getTotalHours().doubleValue();

        if (totalHours < MIN_REASONABLE_DAILY_HOURS) {
            return ValidationResult.invalid(
                    String.format("Total arbetstid %.2f timmar verkar för kort. Minimum: %.2f timmar",
                            totalHours, MIN_REASONABLE_DAILY_HOURS)
            );
        }

        if (totalHours > MAX_REASONABLE_DAILY_HOURS) {
            return ValidationResult.invalid(
                    String.format("Total arbetstid %.2f timmar verkar för lång. Maximum: %.2f timmar",
                            totalHours, MAX_REASONABLE_DAILY_HOURS)
            );
        }

        return ValidationResult.valid();
    }

    // =================================================================
    // 4. STATISTISK VALIDERING
    // =================================================================

    /**
     * Validerar datumintervall för rapporter och statistik.
     *
     * Säkerställer att rapporter inte spänner över för långa perioder
     * vilket skulle kunna påverka prestanda negativt.
     *
     * @param startDate Startdatum för rapporten
     * @param endDate Slutdatum för rapporten
     * @return ValidationResult med resultat
     */
    public static ValidationResult validateReportDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return ValidationResult.invalid("Start- och slutdatum måste anges för rapporter");
        }

        if (startDate.isAfter(endDate)) {
            return ValidationResult.invalid("Startdatum kan inte vara efter slutdatum");
        }

        // Kontrollera att vi inte går för långt tillbaka i tiden (prestanda)
        LocalDate earliestAllowed = LocalDate.now().minusYears(MAX_HISTORICAL_YEARS);
        if (startDate.isBefore(earliestAllowed)) {
            return ValidationResult.invalid(
                    String.format("Rapporter kan inte sträcka sig längre tillbaka än %d år", MAX_HISTORICAL_YEARS)
            );
        }

        // Kontrollera att intervallet inte är för stort (prestanda)
        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);
        if (daysBetween > MAX_REPORT_PERIOD_DAYS) {
            return ValidationResult.invalid(
                    String.format("Rapportperiod kan inte vara längre än %d dagar", MAX_REPORT_PERIOD_DAYS)
            );
        }

        return ValidationResult.valid();
    }

    /**
     * Kontrollerar om arbetstimmar är inom normal range för övertidsberäkningar.
     *
     * @param hoursInPeriod Antal timmar i perioden
     * @param periodDays Antal dagar i perioden
     * @return ValidationResult med information om arbetstiden verkar rimlig
     */
    public static ValidationResult validateWorkHoursForPeriod(double hoursInPeriod, long periodDays) {
        if (hoursInPeriod < 0) {
            return ValidationResult.invalid("Arbetstimmar kan inte vara negativa");
        }

        if (periodDays <= 0) {
            return ValidationResult.invalid("Period måste vara minst 1 dag");
        }

        double averageHoursPerDay = hoursInPeriod / periodDays;

        // Varna om genomsnittet verkar orimligt
        if (averageHoursPerDay > MAX_REASONABLE_DAILY_HOURS) {
            return ValidationResult.invalid(
                    String.format("Genomsnittlig arbetstid %.2f timmar/dag verkar orimligt hög", averageHoursPerDay)
            );
        }

        return ValidationResult.valid();
    }

    // =================================================================
    // HJÄLPKLASS FÖR VALIDERINGSRESULTAT
    // =================================================================

    /**
     * Immutable klass som representerar resultatet av en validering.
     *
     * Kan vara antingen giltig eller ogiltig, och kan bära med sig
     * ett normaliserat värde eller felmeddelande.
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String message;
        private final String normalizedValue;

        private ValidationResult(boolean valid, String message, String normalizedValue) {
            this.valid = valid;
            this.message = message;
            this.normalizedValue = normalizedValue;
        }

        public static ValidationResult valid() {
            return new ValidationResult(true, null, null);
        }

        public static ValidationResult validWithValue(String normalizedValue) {
            return new ValidationResult(true, null, normalizedValue);
        }

        public static ValidationResult invalid(String message) {
            return new ValidationResult(false, message, null);
        }

        public boolean isValid() {
            return valid;
        }

        public Optional<String> getMessage() {
            return Optional.ofNullable(message);
        }

        public Optional<String> getNormalizedValue() {
            return Optional.ofNullable(normalizedValue);
        }

        @Override
        public String toString() {
            if (valid) {
                return normalizedValue != null ?
                        "Valid (normalized: " + normalizedValue + ")" : "Valid";
            } else {
                return "Invalid: " + message;
            }
        }
    }
}