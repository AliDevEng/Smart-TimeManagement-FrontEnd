package com.gardening.timemanagement.exception;

/**
 * Exception som kastas när en arbetsdag med samma Task + Date kombination redan existerar.
 *
 * Denna exception används när:
 * - Vi försöker skapa en arbetsdag för uppdrag/datum som redan finns
 * - Vid import av data där dubbletter upptäcks
 * - När användare försöker registrera samma arbetsdag flera gånger
 * - Update-operationer som skulle skapa konflikterande kombinationer
 *
 * Kommer att mappas till HTTP 409 Conflict i Controller-lagret.
 *
 * AFFÄRSREGEL: Task + Date kombinationen måste vara unik i systemet.
 * Detta säkerställer dataintegritet och förhindrar förvirring i rapporter.
 */
public class DuplicateWorkDayException extends RuntimeException {

    /**
     * Standard konstruktor med anpassat meddelande.
     */
    public DuplicateWorkDayException(String message) {
        super(message);
    }

    /**
     * Konstruktor med orsak för chaining av exceptions.
     */
    public DuplicateWorkDayException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Konstruktor för den vanligaste situationen - Task ID + Date konflikt.
     *
     * @param taskId ID för uppdraget som redan har en arbetsdag
     * @param date Datumet som konflikten gäller
     */
    public DuplicateWorkDayException(Long taskId, String date) {
        super("En arbetsdag för uppdrag " + taskId + " på datum " + date +
                " existerar redan. Varje uppdrag kan endast ha en arbetsdag per datum. " +
                "Använd 'update' istället för att lägga till medarbetare eller utrustning.");
    }

    /**
     * Konstruktor med användarvänlig information inklusive uppdragsnummer och kundnamn.
     * Detta ger mer kontext för frontend att visa meningsfulla felmeddelanden.
     *
     * @param taskNumber Uppdragsnummer för bättre användarförståelse
     * @param customerName Kundnamn för kontext
     * @param date Datumet som konflikten gäller
     * @param existingWorkDayId ID för den befintliga arbetsdagen
     */
    public DuplicateWorkDayException(String taskNumber, String customerName,
                                     String date, Long existingWorkDayId) {
        super("Uppdrag " + taskNumber + " (" + customerName + ") har redan en " +
                "arbetsdag registrerad för " + date + " (ID: " + existingWorkDayId + "). " +
                "För att lägga till medarbetare eller utrustning, redigera den befintliga " +
                "arbetsdagen istället för att skapa en ny.");
    }

    /**
     * Konstruktor för batch import scenarios där flera dubbletter upptäcks.
     *
     * @param conflictCount Antal konflikter som upptäckts
     * @param firstConflictTask Första konflikterande uppdraget
     * @param firstConflictDate Första konflikterande datumet
     */
    public DuplicateWorkDayException(int conflictCount, String firstConflictTask, String firstConflictDate) {
        super("Batch-import misslyckades: " + conflictCount + " dubletter upptäcktes. " +
                "Första konflikten: uppdrag " + firstConflictTask + " på " + firstConflictDate + ". " +
                "Kontrollera importdata för dubletter eller använd 'merge' funktionen " +
                "för att kombinera med befintliga arbetsdagar.");
    }

    /**
     * Konstruktor för update-operationer som skulle skapa dubbletter.
     * Detta händer när användare försöker ändra datum eller uppdrag på en
     * befintlig arbetsdag till en kombination som redan existerar.
     *
     * @param workDayId ID för arbetsdagen som försöks uppdateras
     * @param newTaskId Nytt uppdrag-ID som skulle skapa konflikt
     * @param newDate Nytt datum som skulle skapa konflikt
     * @param conflictingWorkDayId ID för den befintliga arbetsdagen som skulle konfliktera
     */
    public DuplicateWorkDayException(Long workDayId, Long newTaskId, String newDate,
                                     Long conflictingWorkDayId) {
        super("Uppdatering av arbetsdag " + workDayId + " till uppdrag " + newTaskId +
                " på datum " + newDate + " misslyckades eftersom denna kombination redan " +
                "existerar (ID: " + conflictingWorkDayId + "). Välj ett annat datum eller " +
                "uppdrag, eller slå samman med den befintliga arbetsdagen.");
    }

    /**
     * Konstruktor för automated system scenarios med actionable resolution.
     * Ger systemet möjlighet att föreslå automatiska lösningar.
     *
     * @param taskNumber Uppdragsnummer för kontext
     * @param date Konflikterande datum
     * @param resolutionAction Föreslaget sätt att lösa konflikten
     * @param existingWorkDayId ID för befintlig arbetsdag
     */
    public DuplicateWorkDayException(String taskNumber, String date,
                                     String resolutionAction, Long existingWorkDayId) {
        super("Arbetsdag för uppdrag " + taskNumber + " på " + date +
                " existerar redan (ID: " + existingWorkDayId + "). " +
                "Systemet föreslår: " + resolutionAction + ". " +
                "Acceptera förslaget eller välj manuell hantering.");
    }

    /**
     * Konstruktor för cross-validation scenarios.
     * När dubblett upptäcks under komplex validation som involverar
     * flera business rules samtidigt.
     *
     * @param taskId ID för uppdraget
     * @param date Konflikterande datum
     * @param additionalConstraints Andra business constraints som också bryts
     * @param suggestedAlternatives Lista med föreslagna alternativ
     */
    public DuplicateWorkDayException(Long taskId, String date,
                                     String additionalConstraints, String suggestedAlternatives) {
        super("Dubblett-konflikt för uppdrag " + taskId + " på " + date + ". " +
                "Ytterligare begränsningar: " + additionalConstraints + ". " +
                "Föreslagna alternativ: " + suggestedAlternatives + ". " +
                "Kontakta projektledare om inga alternativ fungerar.");
    }

    /**
     * Konstruktor för temporal conflict scenarios.
     * När dubbletter skapas genom tidsrelaterade operationer som
     * datum-shifts eller bulk updates.
     *
     * @param originalDate Ursprungligt datum
     * @param newDate Nytt datum som skapar konflikt
     * @param taskNumber Uppdragsnummer för kontext
     * @param conflictReason Orsak till konflikten
     */
    public DuplicateWorkDayException(String originalDate, String newDate,
                                     String taskNumber, String conflictReason) {
        super("Datum-ändring från " + originalDate + " till " + newDate +
                " för uppdrag " + taskNumber + " misslyckades: " + conflictReason + ". " +
                "Den nya datum-kombinationen existerar redan. " +
                "Använd 'shift conflict resolution' för att hantera automatiskt.");
    }
}