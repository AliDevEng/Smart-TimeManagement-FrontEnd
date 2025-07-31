package com.gardening.timemanagement.exception;

/**
 * Exception som kastas när en arbetsdag inte kan tas bort eller ändras.
 *
 * Denna exception används när:
 * - Arbetsdagen har EmployeeTime-registreringar som blockerar borttagning
 * - Utrustningsanvändning är registrerad som inte kan tas bort
 * - Datum ligger i det förflutna och företagspolicy förbjuder ändringar
 * - Arbetsdagen är del av en avslutad rapport som är låst
 * - Affärsregler förhindrar modifiering av historisk data
 *
 * Kommer att mappas till HTTP 409 Conflict i Controller-lagret.
 */
public class WorkDayDeletionException extends RuntimeException {

    /**
     * Standard konstruktor med anpassat meddelande.
     */
    public WorkDayDeletionException(String message) {
        super(message);
    }

    /**
     * Konstruktor med orsak för chaining av exceptions.
     */
    public WorkDayDeletionException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Konstruktor för arbetsdagar med aktiva EmployeeTime-registreringar.
     * Detta är det vanligaste scenariot där deletion blockeras.
     *
     * @param workDayId ID för arbetsdagen
     * @param employeeTimeCount Antal medarbetartider som blockerar
     * @param date Datumet för arbetsdagen
     */
    public WorkDayDeletionException(Long workDayId, int employeeTimeCount, String date) {
        super("Arbetsdagen " + workDayId + " (" + date + ") kan inte tas bort eftersom " +
                "den har " + employeeTimeCount + " registrerade arbetstider. " +
                "Ta bort alla arbetstidsregistreringar först eller använd 'force delete' om tillgängligt.");
    }

    /**
     * Konstruktor för historical data protection.
     * Skyddar äldre data från oavsiktlig modifiering.
     *
     * @param date Datumet för arbetsdagen
     * @param daysOld Antal dagar sedan arbetsdagen
     * @param policyLimit Företagets policy-gräns för ändringar
     */
    public WorkDayDeletionException(String date, long daysOld, int policyLimit) {
        super("Arbetsdagen " + date + " kan inte tas bort eftersom den är " +
                daysOld + " dagar gammal. Företagspolicy tillåter endast ändringar av " +
                "arbetsdagar som är högst " + policyLimit + " dagar gamla. " +
                "Kontakta administratör för historiska ändringar.");
    }

    /**
     * Konstruktor för utrustnings-relaterade deletion blockers.
     * När utrustning är registrerad på arbetsdagen som inte kan tas bort.
     *
     * @param workDayId ID för arbetsdagen
     * @param equipmentCount Antal utrustningar som blockerar
     * @param equipmentNames Lista med utrustningsnamn för kontext
     */
    public WorkDayDeletionException(Long workDayId, int equipmentCount, String equipmentNames) {
        super("Arbetsdagen " + workDayId + " kan inte tas bort eftersom " +
                "den har " + equipmentCount + " registrerade utrustningar: " + equipmentNames + ". " +
                "Ta bort utrustningsregistreringarna först eller använd batch-deletion.");
    }

    /**
     * Konstruktor för rapport-relaterade blockers.
     * När arbetsdagen ingår i en genererad rapport som inte får ändras.
     *
     * @param workDayId ID för arbetsdagen
     * @param reportType Typ av rapport som blockerar (t.ex. "månadsrapport", "fakturaunderlag")
     * @param reportDate Datum när rapporten genererades
     */
    public WorkDayDeletionException(Long workDayId, String reportType, String reportDate) {
        super("Arbetsdagen " + workDayId + " kan inte tas bort eftersom den ingår i " +
                "en låst " + reportType + " genererad " + reportDate + ". " +
                "Lås upp rapporten eller kontakta ekonomiavdelningen för att tillåta ändringar.");
    }

    /**
     * Konstruktor för multiple constraint violations.
     * När flera olika faktorer blockerar deletion samtidigt.
     *
     * @param workDayId ID för arbetsdagen
     * @param constraintTypes Lista med olika typer av blockers
     * @param totalBlockers Totalt antal blockers
     */
    public WorkDayDeletionException(Long workDayId, String constraintTypes, int totalBlockers) {
        super("Arbetsdagen " + workDayId + " kan inte tas bort på grund av " +
                totalBlockers + " olika blockeringar: " + constraintTypes + ". " +
                "Alla blockeringar måste åtgärdas innan deletion kan genomföras. " +
                "Använd 'analyze constraints' endpoint för detaljerad information.");
    }

    /**
     * Konstruktor för cascade deletion prevention.
     * När deletion skulle orsaka oönskade cascade-effekter.
     *
     * @param workDayId ID för arbetsdagen
     * @param cascadeType Typ av cascade som skulle uppstå
     * @param affectedEntities Antal entiteter som skulle påverkas
     */
    public WorkDayDeletionException(Long workDayId, String cascadeType, int affectedEntities) {
        super("Arbetsdagen " + workDayId + " kan inte tas bort eftersom det skulle " +
                "trigga " + cascadeType + " som påverkar " + affectedEntities + " relaterade poster. " +
                "Använd selective deletion eller archive funktionen istället för permanent deletion.");
    }

    /**
     * Konstruktor för payroll integration protection.
     * När arbetsdagen är integrerad med lönesystem och låst för ändringar.
     *
     * @param workDayId ID för arbetsdagen
     * @param payrollPeriod Löneperiod som arbetsdagen tillhör
     * @param lockDate Datum när låsningen skedde
     */
    public WorkDayDeletionException(Long workDayId, String payrollPeriod, String lockDate) {
        super("Arbetsdagen " + workDayId + " kan inte tas bort eftersom den är låst " +
                "för löneperiod " + payrollPeriod + " sedan " + lockDate + ". " +
                "Kontakta HR eller ekonomiavdelningen för att öppna löneperioden " +
                "om korrigeringar måste göras.");
    }
}