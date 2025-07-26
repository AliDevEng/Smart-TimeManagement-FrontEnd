package com.gardening.timemanagement.util;

import com.gardening.timemanagement.entity.*;
import java.time.LocalDate;
import java.time.DayOfWeek;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Optional;
import java.util.stream.Collectors;


public final class WorkDayValidationUtils {

    // =================================================================
    // AFFÄRSREGELKONSTANTER - KONFIGURERBARA PARAMETRAR
    // =================================================================

    // Temporal constraints för arbetsdagar
    public static final int MAX_HISTORICAL_DAYS = 90;        // Hur långt tillbaka vi tillåter registrering
    public static final int MAX_FUTURE_DAYS = 365;           // Hur långt framåt vi tillåter planering
    public static final int MAX_EMPLOYEES_PER_WORKDAY = 20;  // Kapacitetsbegränsning för team size
    public static final int MAX_EQUIPMENT_ITEMS_PER_DAY = 15; // Praktisk gräns för utrustning

    // Arbetstidsregler för aggregerad validering
    public static final double MAX_TOTAL_HOURS_PER_WORKDAY = 200.0;  // Total för alla medarbetare
    public static final double MIN_AVERAGE_HOURS_PER_EMPLOYEE = 1.0;  // Minimum för att vara meningsfullt
    public static final double MAX_AVERAGE_HOURS_PER_EMPLOYEE = 16.0; // Maximum för rimlighet

    // Helgdagar och speciella datum (förenklad för exempel - i verkligheten från databas/config)
    private static final Set<String> SWEDISH_HOLIDAYS = Set.of(
            "01-01", "01-06", "05-01", "06-06", "12-24", "12-25", "12-26", "12-31"
    );

    // Privat konstruktor - utility-klass
    private WorkDayValidationUtils() {
        throw new UnsupportedOperationException("WorkDayValidationUtils är en utility-klass");
    }

    // =================================================================
    // 1. GRUNDLÄGGANDE TEMPORAL VALIDERING - STATELESS
    // =================================================================

    /**
     * Validerar arbetsdagens grundläggande temporala logik.
     *
     * Kontrollerar att datumet är rimligt i förhållande till nuvarande datum
     * och följer affärsregler för när arbetsdagar kan registreras eller planeras.
     *
     * @param workDayDate Datum för arbetsdagen
     * @param allowFutureDates Om framtida datum ska tillåtas (för planering vs registrering)
     * @return ValidationResult med resultat och förklaring
     */
    public static ValidationResult validateWorkDayDate(LocalDate workDayDate, boolean allowFutureDates) {
        if (workDayDate == null) {
            return ValidationResult.invalid("Arbetsdagens datum måste anges");
        }

        LocalDate today = LocalDate.now();

        // Kontrollera historiska gränser - för gamla datum kan skapa dataintegritets-problem
        LocalDate earliestAllowed = today.minusDays(MAX_HISTORICAL_DAYS);
        if (workDayDate.isBefore(earliestAllowed)) {
            return ValidationResult.invalid(
                    String.format("Arbetsdagar kan inte registreras längre tillbaka än %d dagar (före %s)",
                            MAX_HISTORICAL_DAYS, earliestAllowed)
            );
        }

        // Kontrollera framtida begränsningar
        if (workDayDate.isAfter(today)) {
            if (!allowFutureDates) {
                return ValidationResult.invalid("Arbetsdagar kan inte registreras för framtida datum");
            }

            LocalDate latestAllowed = today.plusDays(MAX_FUTURE_DAYS);
            if (workDayDate.isAfter(latestAllowed)) {
                return ValidationResult.invalid(
                        String.format("Arbetsdagar kan inte planeras längre fram än %d dagar (efter %s)",
                                MAX_FUTURE_DAYS, latestAllowed)
                );
            }
        }

        return ValidationResult.valid();
    }

    /**
     * Kontrollerar om ett datum är en helgdag eller weekend.
     *
     * Denna information kan användas för varningar men blockerar inte nödvändivis
     * skapandet av arbetsdagar eftersom vissa verksamheter arbetar på helger.
     *
     * @param date Datum att kontrollera
     * @return WorkDayDateInfo med information om datumet
     */
    public static WorkDayDateInfo analyzeDateCharacteristics(LocalDate date) {
        if (date == null) {
            return new WorkDayDateInfo(false, false, false, "Ogiltigt datum");
        }

        boolean isWeekend = date.getDayOfWeek() == DayOfWeek.SATURDAY ||
                date.getDayOfWeek() == DayOfWeek.SUNDAY;

        String monthDay = String.format("%02d-%02d", date.getMonthValue(), date.getDayOfMonth());
        boolean isHoliday = SWEDISH_HOLIDAYS.contains(monthDay);

        boolean isWorkDay = !isWeekend && !isHoliday;

        String description = buildDateDescription(date, isWeekend, isHoliday, isWorkDay);

        return new WorkDayDateInfo(isWeekend, isHoliday, isWorkDay, description);
    }

    private static String buildDateDescription(LocalDate date, boolean isWeekend,
                                               boolean isHoliday, boolean isWorkDay) {
        if (isHoliday) {
            return "Helgdag - kontrollera om arbete verkligen utfördes";
        } else if (isWeekend) {
            return "Helg - kontrollera om övertidsersättning gäller";
        } else if (isWorkDay) {
            return "Ordinarie arbetsdag";
        } else {
            return "Kontrollera datum";
        }
    }

    // =================================================================
    // 2. CROSS-ENTITY VALIDERING - KRÄVER KONTEXT
    // =================================================================

    /**
     * Validerar att det valda uppdraget är lämpligt för en ny arbetsdag.
     *
     * Denna metod kräver Task-kontext eftersom den måste kontrollera status
     * och andra affärsregler som inte är tillgängliga i WorkDay-entiteten själv.
     *
     * @param task Uppdraget som arbetsdagen ska kopplas till
     * @param workDayDate Datum för arbetsdagen
     * @return ValidationResult med detaljerat resultat
     */
    public static ValidationResult validateTaskSuitability(Task task, LocalDate workDayDate) {
        if (task == null) {
            return ValidationResult.invalid("Uppdrag måste anges för arbetsdagen");
        }

        // Kontrollera att uppdraget kan ta emot arbetstid
        if (!task.canAcceptWorkTime()) {
            return ValidationResult.invalid(
                    String.format("Uppdraget '%s' kan inte ta emot ny arbetstid (status: %s)",
                            task.getNumber(), task.getStatusDisplayName())
            );
        }

        // Kontrollera temporal konsistens med uppdraget
        if (task.getStartDate() != null && workDayDate.isBefore(task.getStartDate())) {
            return ValidationResult.invalid(
                    String.format("Arbetsdag %s kan inte vara före uppdragets startdatum %s",
                            workDayDate, task.getStartDate())
            );
        }

        if (task.getEndDate() != null && workDayDate.isAfter(task.getEndDate())) {
            return ValidationResult.invalid(
                    String.format("Arbetsdag %s kan inte vara efter uppdragets slutdatum %s",
                            workDayDate, task.getEndDate())
            );
        }

        return ValidationResult.valid();
    }

    /**
     * Validerar att alla medarbetare i listan kan tilldelas denna arbetsdag.
     *
     * Kontrollerar både grundläggande geschäftsregler (aktiv status) och
     * konflikter (dubbletter i listan, redan registrerad samma dag).
     *
     * @param employees Lista över medarbetare att validera
     * @param workDayDate Datum för arbetsdagen
     * @param existingWorkDays Befintliga arbetsdagar för samma datum (för konfliktdetektering)
     * @return ValidationResult med detaljerat resultat
     */
    public static ValidationResult validateEmployeeAssignments(
            List<Employee> employees,
            LocalDate workDayDate,
            List<WorkDay> existingWorkDays) {

        if (employees == null || employees.isEmpty()) {
            return ValidationResult.invalid("Minst en medarbetare måste tilldelas arbetsdagen");
        }

        if (employees.size() > MAX_EMPLOYEES_PER_WORKDAY) {
            return ValidationResult.invalid(
                    String.format("För många medarbetare (%d). Maximum %d medarbetare per arbetsdag",
                            employees.size(), MAX_EMPLOYEES_PER_WORKDAY)
            );
        }

        // Kontrollera dubbletter i input-listan
        Set<Long> employeeIds = new HashSet<>();
        for (Employee employee : employees) {
            if (employee == null) {
                return ValidationResult.invalid("Medarbetarlistan innehåller null-värden");
            }

            if (!employeeIds.add(employee.getId())) {
                return ValidationResult.invalid(
                        String.format("Medarbetaren '%s' är listad flera gånger", employee.getName())
                );
            }
        }

        // Kontrollera varje medarbetares lämplighet
        for (Employee employee : employees) {
            ValidationResult employeeResult = validateSingleEmployeeAssignment(
                    employee, workDayDate, existingWorkDays);
            if (!employeeResult.isValid()) {
                return employeeResult;
            }
        }

        return ValidationResult.valid();
    }

    /**
     * Validerar en enskild medarbetares lämplighet för arbetsdagen.
     */
    private static ValidationResult validateSingleEmployeeAssignment(
            Employee employee,
            LocalDate workDayDate,
            List<WorkDay> existingWorkDays) {

        // Kontrollera att medarbetaren kan tilldelas arbete
        if (!employee.canBeAssignedToWork()) {
            return ValidationResult.invalid(
                    String.format("Medarbetaren '%s' är inaktiv och kan inte tilldelas arbete",
                            employee.getName())
            );
        }

        // Kontrollera konflikter med befintliga arbetsdagar samma datum
        if (existingWorkDays != null) {
            for (WorkDay existingWorkDay : existingWorkDays) {
                if (existingWorkDay.getDate().equals(workDayDate) &&
                        existingWorkDay.hasEmployee(employee)) {

                    return ValidationResult.invalid(
                            String.format("Medarbetaren '%s' är redan registrerad för arbetsdag %s på uppdrag '%s'",
                                    employee.getName(), workDayDate,
                                    existingWorkDay.getTask().getNumber())
                    );
                }
            }
        }

        return ValidationResult.valid();
    }

    // =================================================================
    // 3. UTRUSTNINGSVALIDERING - RESOURCE MANAGEMENT
    // =================================================================

    /**
     * Validerar utrustningsanvändning för arbetsdagen.
     *
     * Kontrollerar tillgänglighet, kapacitet och affärsregler för utrustning.
     * Denna validering är särskilt viktig för kostnadskontroll och resursplanering.
     *
     * @param equipmentList Lista över utrustning att validera
     * @param workDayDate Datum för användning
     * @param existingEquipmentUsage Befintlig utrustningsanvändning samma datum
     * @return ValidationResult med detaljerat resultat
     */
    public static ValidationResult validateEquipmentUsage(
            List<Equipment> equipmentList,
            LocalDate workDayDate,
            List<WorkDayEquipment> existingEquipmentUsage) {

        // Tom lista är OK - utrustning är valfritt
        if (equipmentList == null || equipmentList.isEmpty()) {
            return ValidationResult.valid();
        }

        if (equipmentList.size() > MAX_EQUIPMENT_ITEMS_PER_DAY) {
            return ValidationResult.invalid(
                    String.format("För mycket utrustning (%d objekt). Maximum %d objekt per arbetsdag",
                            equipmentList.size(), MAX_EQUIPMENT_ITEMS_PER_DAY)
            );
        }

        // Kontrollera varje utrustningsobjekt
        for (Equipment equipment : equipmentList) {
            if (equipment == null) {
                return ValidationResult.invalid("Utrustningslistan innehåller null-värden");
            }

            // Kontrollera att utrustningen är tillgänglig för bokning
            if (!equipment.isAvailableForBooking()) {
                return ValidationResult.invalid(
                        String.format("Utrustningen '%s' är inte tillgänglig för bokning (inaktiv)",
                                equipment.getName())
                );
            }
        }

        // Kontrollera dubbletter i samma arbetsdag
        Set<Long> equipmentIds = new HashSet<>();
        for (Equipment equipment : equipmentList) {
            if (!equipmentIds.add(equipment.getId())) {
                return ValidationResult.invalid(
                        String.format("Utrustningen '%s' är listad flera gånger för samma arbetsdag",
                                equipment.getName())
                );
            }
        }

        return ValidationResult.valid();
    }

    // =================================================================
    // 4. AGGREGERAD ARBETSTIDSVALIDERING - BUSINESS INTELLIGENCE
    // =================================================================

    /**
     * Validerar aggregerad arbetstid för hela arbetsdagen.
     *
     * Denna validering tittar på totala arbetstimmar, fördelning per medarbetare,
     * och andra metriker som kan indikera problem med registreringen.
     *
     * @param employeeTimes Lista över alla arbetstidsregistreringar för dagen
     * @return ValidationResult med analys av arbetstidsfördelningen
     */
    public static ValidationResult validateAggregatedWorkTime(List<EmployeeTime> employeeTimes) {
        if (employeeTimes == null || employeeTimes.isEmpty()) {
            return ValidationResult.invalid("Arbetsdagen måste innehålla minst en arbetstidsregistrering");
        }

        // Beräkna totaler och genomsnitt
        double totalHours = employeeTimes.stream()
                .mapToDouble(et -> et.getTotalHours().doubleValue())
                .sum();

        double averageHours = totalHours / employeeTimes.size();

        // Validera totaler
        if (totalHours > MAX_TOTAL_HOURS_PER_WORKDAY) {
            return ValidationResult.invalid(
                    String.format("Total arbetstid %.1f timmar överstiger maximum %s timmar per arbetsdag",
                            totalHours, MAX_TOTAL_HOURS_PER_WORKDAY)
            );
        }

        // Validera genomsnitt
        if (averageHours < MIN_AVERAGE_HOURS_PER_EMPLOYEE) {
            return ValidationResult.invalid(
                    String.format("Genomsnittlig arbetstid %.2f timmar/medarbetare är för låg (minimum %.1f)",
                            averageHours, MIN_AVERAGE_HOURS_PER_EMPLOYEE)
            );
        }

        if (averageHours > MAX_AVERAGE_HOURS_PER_EMPLOYEE) {
            return ValidationResult.invalid(
                    String.format("Genomsnittlig arbetstid %.2f timmar/medarbetare är för hög (maximum %.1f)",
                            averageHours, MAX_AVERAGE_HOURS_PER_EMPLOYEE)
            );
        }

        // Kontrollera för extrema avvikelser i arbetstider
        ValidationResult variationResult = validateWorkTimeVariation(employeeTimes, averageHours);
        if (!variationResult.isValid()) {
            return variationResult;
        }

        return ValidationResult.valid();
    }

    /**
     * Kontrollerar variation i arbetstider för att identifiera potentiella problem.
     */
    private static ValidationResult validateWorkTimeVariation(List<EmployeeTime> employeeTimes,
                                                              double averageHours) {
        // Hitta extrema avvikelser som kan indikera registreringsfel
        for (EmployeeTime employeeTime : employeeTimes) {
            double individualHours = employeeTime.getTotalHours().doubleValue();
            double deviationFromAverage = Math.abs(individualHours - averageHours);

            // Om någon medarbetare har mer än 8 timmars avvikelse från genomsnittet
            if (deviationFromAverage > 8.0 && averageHours > 2.0) {
                return ValidationResult.invalid(
                        String.format("Medarbetaren '%s' har %.1f arbetstimmar vilket avviker extremt från genomsnittet %.1f timmar. Kontrollera registreringen.",
                                employeeTime.getEmployee().getName(), individualHours, averageHours)
                );
            }
        }

        return ValidationResult.valid();
    }

    // =================================================================
    // 5. KOMPLETT WORKDAY VALIDERING - ORCHESTRATION
    // =================================================================

    /**
     * Omfattande validering av en komplett WorkDay med alla dess komponenter.
     *
     * Detta är den primära valideringsmetoden som ska användas innan en WorkDay
     * sparas till databasen. Den koordinerar alla andra valideringsmetoder och
     * ger en holistisk bedömning av arbetsdagens giltighet.
     *
     * @param workDay WorkDay-objektet att validera
     * @param allowFutureDates Om framtida datum tillåts
     * @param existingWorkDays Befintliga arbetsdagar för konfliktdetektering
     * @param existingEquipmentUsage Befintlig utrustningsanvändning
     * @return ComprehensiveValidationResult med detaljerad analys
     */
    public static ComprehensiveValidationResult validateCompleteWorkDay(
            WorkDay workDay,
            boolean allowFutureDates,
            List<WorkDay> existingWorkDays,
            List<WorkDayEquipment> existingEquipmentUsage) {

        if (workDay == null) {
            return ComprehensiveValidationResult.invalid("WorkDay kan inte vara null");
        }

        ComprehensiveValidationResult result = new ComprehensiveValidationResult();

        // 1. Temporal validering
        ValidationResult dateResult = validateWorkDayDate(workDay.getDate(), allowFutureDates);
        result.addValidation("datum", dateResult);

        // 2. Task-validering
        ValidationResult taskResult = validateTaskSuitability(workDay.getTask(), workDay.getDate());
        result.addValidation("uppdrag", taskResult);

        // 3. Employee-validering
        List<Employee> employees = workDay.getEmployeeTimes().stream()
                .map(EmployeeTime::getEmployee)
                .collect(Collectors.toList());

        ValidationResult employeeResult = validateEmployeeAssignments(
                employees, workDay.getDate(), existingWorkDays);
        result.addValidation("medarbetare", employeeResult);

        // 4. Equipment-validering
        List<Equipment> equipment = workDay.getEquipmentUsed().stream()
                .map(WorkDayEquipment::getEquipment)
                .collect(Collectors.toList());

        ValidationResult equipmentResult = validateEquipmentUsage(
                equipment, workDay.getDate(), existingEquipmentUsage);
        result.addValidation("utrustning", equipmentResult);

        // 5. Arbetstids-validering
        ValidationResult workTimeResult = validateAggregatedWorkTime(workDay.getEmployeeTimes());
        result.addValidation("arbetstid", workTimeResult);

        // 6. Datum-analys (informativ, blockerar inte)
        WorkDayDateInfo dateInfo = analyzeDateCharacteristics(workDay.getDate());
        result.setDateInfo(dateInfo);

        return result;
    }

    // =================================================================
    // HJÄLPKLASSER FÖR RIKA VALIDERINGSRESULTAT
    // =================================================================

    /**
     * Information om ett datums karakteristika.
     */
    public static class WorkDayDateInfo {
        private final boolean isWeekend;
        private final boolean isHoliday;
        private final boolean isRegularWorkDay;
        private final String description;

        public WorkDayDateInfo(boolean isWeekend, boolean isHoliday,
                               boolean isRegularWorkDay, String description) {
            this.isWeekend = isWeekend;
            this.isHoliday = isHoliday;
            this.isRegularWorkDay = isRegularWorkDay;
            this.description = description;
        }

        // Getters
        public boolean isWeekend() { return isWeekend; }
        public boolean isHoliday() { return isHoliday; }
        public boolean isRegularWorkDay() { return isRegularWorkDay; }
        public String getDescription() { return description; }

        public boolean requiresSpecialAttention() {
            return isWeekend || isHoliday;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    /**
     * Omfattande valideringsresultat för kompletta WorkDay-objekt.
     */
    public static class ComprehensiveValidationResult {
        private final java.util.Map<String, ValidationResult> validations;
        private WorkDayDateInfo dateInfo;

        public ComprehensiveValidationResult() {
            this.validations = new java.util.HashMap<>();
        }

        public void addValidation(String category, ValidationResult result) {
            validations.put(category, result);
        }

        public void setDateInfo(WorkDayDateInfo dateInfo) {
            this.dateInfo = dateInfo;
        }

        public boolean isValid() {
            return validations.values().stream().allMatch(ValidationResult::isValid);
        }

        public List<String> getErrorMessages() {
            return validations.entrySet().stream()
                    .filter(entry -> !entry.getValue().isValid())
                    .map(entry -> entry.getKey() + ": " + entry.getValue().getMessage().orElse("Okänt fel"))
                    .collect(Collectors.toList());
        }

        public Optional<WorkDayDateInfo> getDateInfo() {
            return Optional.ofNullable(dateInfo);
        }

        public static ComprehensiveValidationResult invalid(String message) {
            ComprehensiveValidationResult result = new ComprehensiveValidationResult();
            result.addValidation("allmän", ValidationResult.invalid(message));
            return result;
        }

        @Override
        public String toString() {
            if (isValid()) {
                return "Giltig WorkDay" +
                        (dateInfo != null && dateInfo.requiresSpecialAttention() ?
                                " (OBS: " + dateInfo.getDescription() + ")" : "");
            } else {
                return "Ogiltig WorkDay: " + String.join("; ", getErrorMessages());
            }
        }
    }

    // Återanvänd ValidationResult från EmployeeValidationUtils
    // (I verkliga systemet skulle denna vara i en gemensam util-klass)
    public static class ValidationResult {
        private final boolean valid;
        private final String message;

        private ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult invalid(String message) {
            return new ValidationResult(false, message);
        }

        public boolean isValid() {
            return valid;
        }

        public Optional<String> getMessage() {
            return Optional.ofNullable(message);
        }
    }
}