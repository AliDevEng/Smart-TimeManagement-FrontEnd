package com.gardening.timemanagement.exception;

/**
 * Exception som kastas när en arbetsdag inte kan hittas.
 *
 * Denna exception används när:
 * - Vi söker efter en arbetsdag med ett ID som inte existerar
 * - Vi försöker hämta en arbetsdag som har tagits bort
 * - Vi söker kombinationer av datum/uppdrag som inte finns
 *
 * Kommer att mappas till HTTP 404 Not Found i Controller-lagret.
 */
public class WorkDayNotFoundException extends RuntimeException {

    /**
     * Standard konstruktor med anpassat meddelande.
     */
    public WorkDayNotFoundException(String message) {
        super(message);
    }

    /**
     * Konstruktor med orsak för chaining av exceptions.
     */
    public WorkDayNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Bekvämlighets-konstruktor för ID-baserade sökningar.
     *
     * @param workDayId ID för arbetsdagen som inte hittades
     */
    public WorkDayNotFoundException(Long workDayId) {
        super("Arbetsdag med ID " + workDayId + " finns inte");
    }

    /**
     * Konstruktor för datum/uppdrag-kombinationer som inte finns.
     * Detta är vanligt när frontend försöker hämta en specifik
     * kombination som användaren tror existerar.
     *
     * @param taskId ID för uppdraget
     * @param date Datumet som söks
     */
    public WorkDayNotFoundException(Long taskId, String date) {
        super("Ingen arbetsdag hittades för uppdrag " + taskId + " på datum " + date);
    }

    /**
     * Konstruktor för mer detaljerade sökningar med både uppdragsnummer
     * och kundnamn för användarvänliga felmeddelanden.
     *
     * @param taskNumber Uppdragsnummer för bättre användarförståelse
     * @param customerName Kundnamn för kontext
     * @param date Datumet som söks
     */
    public WorkDayNotFoundException(String taskNumber, String customerName, String date) {
        super("Ingen arbetsdag hittades för uppdrag " + taskNumber +
                " (" + customerName + ") på datum " + date +
                ". Kontrollera att arbetsdagen är korrekt registrerad.");
    }

    /**
     * Konstruktor för sökningar inom datumintervall som inte ger resultat.
     * Användbart för rapporter och filtered queries.
     *
     * @param startDate Startdatum för sökningen
     * @param endDate Slutdatum för sökningen
     * @param additionalContext Extra kontext som kan hjälpa användaren
     */
    public WorkDayNotFoundException(String startDate, String endDate, String additionalContext) {
        super("Inga arbetsdagar hittades mellan " + startDate + " och " + endDate +
                (additionalContext != null && !additionalContext.isEmpty() ?
                        ". " + additionalContext : ""));
    }
}