package com.gardening.timemanagement.exception;

import java.time.LocalDate;

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

    // =================================================================
    // GRUNDLÄGGANDE KONSTRUKTORER - Dessa täcker de vanligaste scenariona
    // =================================================================

    /**
     * Standard konstruktor med anpassat meddelande.
     * Används när vi vill ge ett specifikt felmeddelande utan extra kontext.
     */
    public DuplicateWorkDayException(String message) {
        super(message);
    }

    /**
     * Konstruktor med orsak för chaining av exceptions.
     * Används när denna exception orsakas av en annan exception längre ner i stacken.
     */
    public DuplicateWorkDayException(String message, Throwable cause) {
        super(message, cause);
    }

    // =================================================================
    // BUSINESS-SPECIFIKA KONSTRUKTORER - Dessa ger användarvänlig kontext
    // =================================================================

    /**
     * Konstruktor för den vanligaste situationen - Task ID + Date konflikt.
     * Denna är den som din WorkDayService troligtvis kommer använda mest.
     *
     * @param taskId ID för uppdraget som redan har en arbetsdag
     * @param date Datumet som konflikten gäller (använder LocalDate för typsäkerhet)
     */
    public DuplicateWorkDayException(Long taskId, LocalDate date) {
        super("En arbetsdag för uppdrag " + taskId + " på datum " + date +
                " existerar redan. Varje uppdrag kan endast ha en arbetsdag per datum. " +
                "Använd 'update' istället för att lägga till medarbetare eller utrustning.");
    }

    /**
     * Konstruktor med användarvänlig information inklusive uppdragsnummer och kundnamn.
     * Detta ger mer kontext för frontend att visa meningsfulla felmeddelanden.
     *
     * Tänk på detta som att ge användaren en fullständig berättelse om vad som gick fel,
     * istället för bara tekniska ID-nummer.
     *
     * @param taskNumber Uppdragsnummer för bättre användarförståelse
     * @param customerName Kundnamn för kontext
     * @param date Datumet som konflikten gäller
     * @param existingWorkDayId ID för den befintliga arbetsdagen
     */
    public DuplicateWorkDayException(String taskNumber, String customerName,
                                     LocalDate date, Long existingWorkDayId) {
        super("Uppdrag " + taskNumber + " (" + customerName + ") har redan en " +
                "arbetsdag registrerad för " + date + " (ID: " + existingWorkDayId + "). " +
                "För att lägga till medarbetare eller utrustning, redigera den befintliga " +
                "arbetsdagen istället för att skapa en ny.");
    }

    // =================================================================
    // AVANCERADE SCENARIOS - För mer komplexa situationer
    // =================================================================

    /**
     * Konstruktor för batch import scenarios där flera dubbletter upptäcks.
     * Denna används när systemet importerar många arbetsdagar på en gång.
     *
     * @param conflictCount Antal konflikter som upptäckts
     * @param firstConflictTask Första konflikterande uppdraget för exemplifiering
     * @param firstConflictDate Första konflikterande datumet för exemplifiering
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
     * Ger systemet möjlighet att föreslå automatiska lösningar till användaren.
     *
     * OBSERVERA: Denna konstruktor använder en annan parameterordning för att undvika
     * signatur-konflikter med andra konstruktorer.
     *
     * @param taskNumber Uppdragsnummer för kontext
     * @param existingWorkDayId ID för befintlig arbetsdag (flyttad för unik signatur)
     * @param date Konflikterande datum
     * @param resolutionAction Föreslaget sätt att lösa konflikten
     */
    public DuplicateWorkDayException(String taskNumber, Long existingWorkDayId,
                                     String date, String resolutionAction) {
        super("Arbetsdag för uppdrag " + taskNumber + " på " + date +
                " existerar redan (ID: " + existingWorkDayId + "). " +
                "Systemet föreslår: " + resolutionAction + ". " +
                "Acceptera förslaget eller välj manuell hantering.");
    }

    // =================================================================
    // TEMPORAL OCH COMPLEX SCENARIOS
    // =================================================================

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

/*
 * LÄRDOMAR FRÅN DENNA EXCEPTION-KLASS:
 *
 * 1. KONSTRUKTOR-ÖVERLAGRAD: Java tillåter flera konstruktorer med olika parametrar.
 *    Detta kallas "constructor overloading" och låter oss skapa samma typ av objekt
 *    med olika mängder av information beroende på situationen.
 *
 * 2. SUPER()-ANROP: Varje konstruktor anropar super() för att initiera förälderklassen
 *    RuntimeException. Detta är obligatoriskt i Java-arv.
 *
 3. BUSINESS CONTEXT: Olika konstruktorer ger olika nivåer av kontext. Enkla
 *    String-konstruktorer för snabb användning, medan mer komplexa konstruktorer
 *    ger rik information för användargränssnitt och loggning.
 *
 * 4. TYPSÄKERHET: Vi använder Long för ID:n och LocalDate för datum istället för
 *    String där det är möjligt. Detta förhindrar fel och ger bättre IDE-stöd.
 *
 * 5. DOKUMENTATION: Javadoc-kommentarer förklarar när och hur varje konstruktor
 *    ska användas, vilket hjälper andra utvecklare (och framtida du) att förstå koden.
 */