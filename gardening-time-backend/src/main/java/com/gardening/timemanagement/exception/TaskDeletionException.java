package com.gardening.timemanagement.exception;

import java.util.List;
import java.util.Map;

/**
 * Exception som kastas när ett uppdrag inte kan tas bort på grund av aktiva relationer
 * eller affärsregler som förhindrar borttagning.
 *
 * Denna exception hanterar komplexa scenario där uppdrag är inbäddade i systemets
 * relationsstruktur på sätt som gör borttagning problematisk eller omöjlig.
 *
 * Användningsområden:
 * - Uppdrag med registrerade arbetsdagar som måste bevaras för löneberäkningar
 * - Uppdrag med utrustningsanvändning som påverkar kostnadsspårning
 * - Uppdrag som är refererade från fakturor eller ekonomisystem
 * - Uppdrag som är del av större projektstrukturer
 *
 * Kommer att mappas till HTTP 409 Conflict i Controller-lagret eftersom det
 * representerar en konflikt med systemets dataintegritet.
 */
public class TaskDeletionException extends RuntimeException {

    /**
     * ID för uppdraget som inte kan tas bort.
     */
    private final Long taskId;

    /**
     * Uppdragsnummer för användarvänlig identifiering.
     */
    private final String taskNumber;

    /**
     * Kundnamn för kontext i felmeddelanden.
     */
    private final String customerName;

    /**
     * Typ av hinder som förhindrar borttagning.
     * Används för kategorisering och specifik felhantering.
     */
    private final DeletionBlockerType blockerType;

    /**
     * Antal relaterade entiteter som förhindrar borttagning.
     * Till exempel antal arbetsdagar eller utrustningsregistreringar.
     */
    private final int relatedEntitiesCount;

    /**
     * Beskrivning av vad som specifikt förhindrar borttagning.
     */
    private final String blockingReason;

    /**
     * Detaljer om specifika relaterade entiteter för debugging och användarinfo.
     */
    private final Map<String, Object> relatedEntitiesDetails;

    /**
     * Enum som definierar olika typer av borttagningshinder.
     */
    public enum DeletionBlockerType {
        ACTIVE_WORK_DAYS("Aktiva arbetsdagar"),
        EQUIPMENT_USAGE("Utrustningsanvändning"),
        FINANCIAL_RECORDS("Ekonomiska poster"),
        EMPLOYEE_TIME_RECORDS("Arbetstidsregistreringar"),
        SYSTEM_REFERENCES("Systemreferenser"),
        BUSINESS_RULE_VIOLATION("Affärsregelöverträdelse");

        private final String displayName;

        DeletionBlockerType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Standard konstruktor med grundläggande information.
     */
    public TaskDeletionException(String message) {
        super(message);
        this.taskId = null;
        this.taskNumber = null;
        this.customerName = null;
        this.blockerType = DeletionBlockerType.SYSTEM_REFERENCES;
        this.relatedEntitiesCount = 0;
        this.blockingReason = null;
        this.relatedEntitiesDetails = Map.of();
    }

    /**
     * Fullständig konstruktor med all kontextinformation.
     */
    public TaskDeletionException(Long taskId, String taskNumber, String customerName,
                                 DeletionBlockerType blockerType, int relatedEntitiesCount,
                                 String blockingReason, Map<String, Object> relatedEntitiesDetails) {
        super(createDetailedErrorMessage(taskNumber, customerName, blockerType,
                relatedEntitiesCount, blockingReason));
        this.taskId = taskId;
        this.taskNumber = taskNumber;
        this.customerName = customerName;
        this.blockerType = blockerType;
        this.relatedEntitiesCount = relatedEntitiesCount;
        this.blockingReason = blockingReason;
        this.relatedEntitiesDetails = relatedEntitiesDetails != null ? relatedEntitiesDetails : Map.of();
    }

    /**
     * Förenklad konstruktor för vanliga scenario.
     */
    public TaskDeletionException(Long taskId, String taskNumber, String customerName,
                                 DeletionBlockerType blockerType, int relatedEntitiesCount,
                                 String blockingReason) {
        this(taskId, taskNumber, customerName, blockerType, relatedEntitiesCount,
                blockingReason, Map.of());
    }

    /**
     * Factory method för scenario med aktiva arbetsdagar.
     */
    public static TaskDeletionException dueToActiveWorkDays(Long taskId, String taskNumber,
                                                            String customerName, int workDayCount,
                                                            List<String> workDayDates) {
        Map<String, Object> details = Map.of(
                "workDayCount", workDayCount,
                "workDayDates", workDayDates
        );

        String reason = "Uppdraget har " + workDayCount + " registrerade arbetsdagar som innehåller " +
                "arbetstidsdata och utrustningsanvändning som måste bevaras för löneberäkningar " +
                "och kostnadsspårning.";

        return new TaskDeletionException(taskId, taskNumber, customerName,
                DeletionBlockerType.ACTIVE_WORK_DAYS, workDayCount, reason, details);
    }

    /**
     * Factory method för scenario med utrustningsanvändning.
     */
    public static TaskDeletionException dueToEquipmentUsage(Long taskId, String taskNumber,
                                                            String customerName, int equipmentRecords,
                                                            List<String> equipmentNames) {
        Map<String, Object> details = Map.of(
                "equipmentRecordCount", equipmentRecords,
                "equipmentNames", equipmentNames
        );

        String reason = "Uppdraget har " + equipmentRecords + " utrustningsanvändningsregistreringar " +
                "som är kopplade till kostnadsspårning och fakturering. Denna data måste bevaras " +
                "för ekonomisk revision och projektanalys.";

        return new TaskDeletionException(taskId, taskNumber, customerName,
                DeletionBlockerType.EQUIPMENT_USAGE, equipmentRecords, reason, details);
    }

    /**
     * Factory method för scenario med arbetstidsregistreringar.
     */
    public static TaskDeletionException dueToEmployeeTimeRecords(Long taskId, String taskNumber,
                                                                 String customerName, int timeRecords,
                                                                 List<String> employeeNames) {
        Map<String, Object> details = Map.of(
                "timeRecordCount", timeRecords,
                "employeeNames", employeeNames
        );

        String reason = "Uppdraget har " + timeRecords + " arbetstidsregistreringar från " +
                employeeNames.size() + " medarbetare. Denna data är kritisk för löneberäkningar " +
                "och kan inte tas bort enligt arbetsmiljölagstiftning.";

        return new TaskDeletionException(taskId, taskNumber, customerName,
                DeletionBlockerType.EMPLOYEE_TIME_RECORDS, timeRecords, reason, details);
    }

    /**
     * Factory method för scenario med ekonomiska kopplingar.
     */
    public static TaskDeletionException dueToFinancialRecords(Long taskId, String taskNumber,
                                                              String customerName, String financialSystemRef) {
        Map<String, Object> details = Map.of(
                "financialSystemReference", financialSystemRef,
                "hasInvoices", true
        );

        String reason = "Uppdraget är kopplat till ekonomiska poster i faktureringssystemet " +
                "(referens: " + financialSystemRef + "). Borttagning skulle skapa " +
                "datainkonsistens i ekonomirapporter.";

        return new TaskDeletionException(taskId, taskNumber, customerName,
                DeletionBlockerType.FINANCIAL_RECORDS, 1, reason, details);
    }

    /**
     * Factory method för affärsregelöverträdelser.
     */
    public static TaskDeletionException dueToBusinessRule(Long taskId, String taskNumber,
                                                          String customerName, String ruleName,
                                                          String ruleDescription) {
        Map<String, Object> details = Map.of(
                "ruleName", ruleName,
                "ruleDescription", ruleDescription
        );

        String reason = "Borttagning är inte tillåten enligt affärsregel '" + ruleName + "': " + ruleDescription;

        return new TaskDeletionException(taskId, taskNumber, customerName,
                DeletionBlockerType.BUSINESS_RULE_VIOLATION, 1, reason, details);
    }

    /**
     * Skapar ett detaljerat felmeddelande med all tillgänglig kontext.
     */
    private static String createDetailedErrorMessage(String taskNumber, String customerName,
                                                     DeletionBlockerType blockerType, int count,
                                                     String blockingReason) {
        StringBuilder message = new StringBuilder();

        // Uppdragsidentifiering
        if (taskNumber != null) {
            message.append("Uppdrag '").append(taskNumber).append("'");
        } else {
            message.append("Uppdraget");
        }

        // Kundkontext
        if (customerName != null) {
            message.append(" för ").append(customerName);
        }

        // Grundläggande felmeddelande
        message.append(" kan inte tas bort på grund av ");

        // Typ av hinder
        if (blockerType != null) {
            message.append(blockerType.getDisplayName().toLowerCase());
        } else {
            message.append("systemrestriktioner");
        }

        // Antal relaterade entiteter
        if (count > 0) {
            message.append(" (").append(count).append(" poster)");
        }

        // Specifik anledning
        if (blockingReason != null && !blockingReason.trim().isEmpty()) {
            message.append(". ").append(blockingReason);
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

    public String getCustomerName() {
        return customerName;
    }

    public DeletionBlockerType getBlockerType() {
        return blockerType;
    }

    public int getRelatedEntitiesCount() {
        return relatedEntitiesCount;
    }

    public String getBlockingReason() {
        return blockingReason;
    }

    public Map<String, Object> getRelatedEntitiesDetails() {
        return relatedEntitiesDetails;
    }

    /**
     * Returnerar en användarvänlig förklaring av varför borttagning inte är möjlig.
     */
    public String getUserFriendlyExplanation() {
        StringBuilder explanation = new StringBuilder();

        explanation.append("Detta uppdrag kan inte tas bort eftersom det innehåller viktig data ");
        explanation.append("som måste bevaras av juridiska, ekonomiska eller operationella skäl. ");

        switch (blockerType) {
            case ACTIVE_WORK_DAYS:
                explanation.append("Arbetsdagar med tidsregistreringar måste sparas för löneadministration ");
                explanation.append("och arbetsmiljödokumentation enligt lag.");
                break;
            case EMPLOYEE_TIME_RECORDS:
                explanation.append("Arbetstidsregistreringar är skyddade enligt arbetsmiljölagstiftning ");
                explanation.append("och måste bevaras för revision och löneberäkningar.");
                break;
            case EQUIPMENT_USAGE:
                explanation.append("Utrustningsanvändning är kopplad till kostnadsspårning och ");
                explanation.append("ekonomisk rapportering som måste vara verifierbar.");
                break;
            case FINANCIAL_RECORDS:
                explanation.append("Uppdraget är kopplat till fakturor och ekonomiska transaktioner ");
                explanation.append("som måste bevaras för bokföring och revision.");
                break;
            case BUSINESS_RULE_VIOLATION:
                explanation.append("Företagets policyer förhindrar borttagning av denna typ av uppdrag.");
                break;
            default:
                explanation.append("Systemet har identifierat kritiska kopplingar som förhindrar borttagning.");
        }

        return explanation.toString();
    }

    /**
     * Returnerar förslag på alternativa åtgärder användaren kan vidta.
     */
    public String getSuggestedActions() {
        StringBuilder suggestions = new StringBuilder();

        switch (blockerType) {
            case ACTIVE_WORK_DAYS:
            case EMPLOYEE_TIME_RECORDS:
                suggestions.append("Alternativ:\n");
                suggestions.append("• Ändra uppdragsstatus till 'Avbrutet' istället för att ta bort\n");
                suggestions.append("• Arkivera uppdraget för att dölja det från aktiva listor\n");
                suggestions.append("• Kontakta administratör för specialhantering av datamigreringar");
                break;
            case EQUIPMENT_USAGE:
                suggestions.append("Alternativ:\n");
                suggestions.append("• Exportera utrustningsdata till ekonomisystemet först\n");
                suggestions.append("• Arkivera uppdraget efter att kostnader är fakturerade\n");
                suggestions.append("• Kontakta ekonomiavdelningen för rådgivning");
                break;
            case FINANCIAL_RECORDS:
                suggestions.append("Alternativ:\n");
                suggestions.append("• Vänta tills alla fakturor är slutförda och betalda\n");
                suggestions.append("• Kontakta ekonomiavdelningen för att klara finansiella kopplingar\n");
                suggestions.append("• Använd arkiveringsfunktion istället för borttagning");
                break;
            case BUSINESS_RULE_VIOLATION:
                suggestions.append("Kontakta din chef eller administratör för att diskutera undantag från ");
                suggestions.append("företagspolicyn, eller använd tillåtna alternativ som arkivering.");
                break;
            default:
                suggestions.append("Kontakta systemadministratör för att förstå vilka specifika ");
                suggestions.append("systemkopplingar som förhindrar borttagning.");
        }

        return suggestions.toString();
    }

    /**
     * Kontrollerar om detta fel kan lösas genom användaråtgärder.
     */
    public boolean isUserResolvable() {
        return blockerType == DeletionBlockerType.BUSINESS_RULE_VIOLATION;
    }

    /**
     * Kontrollerar om borttagning kan bli möjlig i framtiden.
     */
    public boolean mayBecomeResolvable() {
        return blockerType == DeletionBlockerType.FINANCIAL_RECORDS ||
                blockerType == DeletionBlockerType.BUSINESS_RULE_VIOLATION;
    }

    /**
     * Returnerar förslag på tidpunkt när borttagning kan bli möjlig.
     */
    public String getResolutionTimeframe() {
        return switch (blockerType) {
            case FINANCIAL_RECORDS -> "Efter att alla ekonomiska transaktioner är slutförda (vanligtvis 30-90 dagar)";
            case BUSINESS_RULE_VIOLATION -> "Efter godkännande från administratör eller policyändring";
            case ACTIVE_WORK_DAYS, EMPLOYEE_TIME_RECORDS -> "Aldrig - data måste bevaras enligt lag";
            case EQUIPMENT_USAGE -> "Efter att kostnadsspårning är slutförd (vanligtvis vid projektavslut)";
            default -> "Kontakta administratör för specifik tidplan";
        };
    }

    /**
     * Returnerar en strukturerad representation för API-responses.
     */
    public DeletionErrorDetails getErrorDetails() {
        return new DeletionErrorDetails(
                taskId,
                taskNumber,
                customerName,
                blockerType.name(),
                blockerType.getDisplayName(),
                relatedEntitiesCount,
                blockingReason,
                relatedEntitiesDetails,
                getUserFriendlyExplanation(),
                getSuggestedActions(),
                isUserResolvable(),
                mayBecomeResolvable(),
                getResolutionTimeframe()
        );
    }

    @Override
    public String toString() {
        return "TaskDeletionException{" +
                "taskId=" + taskId +
                ", taskNumber='" + taskNumber + '\'' +
                ", customerName='" + customerName + '\'' +
                ", blockerType=" + blockerType +
                ", relatedEntitiesCount=" + relatedEntitiesCount +
                ", blockingReason='" + blockingReason + '\'' +
                '}';
    }

    /**
     * Data class för strukturerade feldetaljer i API-responses.
     * Ger frontend omfattande information för att hantera borttagningsfel elegant.
     */
    public static class DeletionErrorDetails {
        private final Long taskId;
        private final String taskNumber;
        private final String customerName;
        private final String blockerType;
        private final String blockerTypeDisplay;
        private final int relatedEntitiesCount;
        private final String blockingReason;
        private final Map<String, Object> relatedEntitiesDetails;
        private final String explanation;
        private final String suggestedActions;
        private final boolean userResolvable;
        private final boolean mayBecomeResolvable;
        private final String resolutionTimeframe;

        public DeletionErrorDetails(Long taskId, String taskNumber, String customerName,
                                    String blockerType, String blockerTypeDisplay, int relatedEntitiesCount,
                                    String blockingReason, Map<String, Object> relatedEntitiesDetails,
                                    String explanation, String suggestedActions, boolean userResolvable,
                                    boolean mayBecomeResolvable, String resolutionTimeframe) {
            this.taskId = taskId;
            this.taskNumber = taskNumber;
            this.customerName = customerName;
            this.blockerType = blockerType;
            this.blockerTypeDisplay = blockerTypeDisplay;
            this.relatedEntitiesCount = relatedEntitiesCount;
            this.blockingReason = blockingReason;
            this.relatedEntitiesDetails = relatedEntitiesDetails;
            this.explanation = explanation;
            this.suggestedActions = suggestedActions;
            this.userResolvable = userResolvable;
            this.mayBecomeResolvable = mayBecomeResolvable;
            this.resolutionTimeframe = resolutionTimeframe;
        }

        // Getters
        public Long getTaskId() { return taskId; }
        public String getTaskNumber() { return taskNumber; }
        public String getCustomerName() { return customerName; }
        public String getBlockerType() { return blockerType; }
        public String getBlockerTypeDisplay() { return blockerTypeDisplay; }
        public int getRelatedEntitiesCount() { return relatedEntitiesCount; }
        public String getBlockingReason() { return blockingReason; }
        public Map<String, Object> getRelatedEntitiesDetails() { return relatedEntitiesDetails; }
        public String getExplanation() { return explanation; }
        public String getSuggestedActions() { return suggestedActions; }
        public boolean isUserResolvable() { return userResolvable; }
        public boolean isMayBecomeResolvable() { return mayBecomeResolvable; }
        public String getResolutionTimeframe() { return resolutionTimeframe; }
    }
}