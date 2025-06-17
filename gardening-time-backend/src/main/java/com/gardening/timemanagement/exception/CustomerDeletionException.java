package com.gardening.timemanagement.exception;



public class CustomerDeletionException extends RuntimeException {


    public CustomerDeletionException(String message) {
        super(message);
    }


    public CustomerDeletionException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Denna konstruktor är speciellt utformad för det vanligaste scenariot:
     * en kund kan inte tas bort eftersom den har aktiva kopplingar till
     * andra delar av systemet. Den skapar ett standardiserat meddelande
     * som förklarar problemet och antalet aktiva kopplingar.
     *
     * Exempel på användning:
     * throw new CustomerDeletionException("Växjö Kommun", 3, "aktiva uppdrag");
     * // Resulterar i: "Kunden 'Växjö Kommun' kan inte tas bort eftersom
     * //                den har 3 aktiva uppdrag"
     */
    public CustomerDeletionException(String customerName, int activeConnections, String connectionType) {
        super("Kunden '" + customerName + "' kan inte tas bort eftersom den har " +
                activeConnections + " " + connectionType);
    }

    /**
     * Denna konstruktor är användbar när borttagningen förhindras av
     * flera olika typer av kopplingar samtidigt, eller när du vill
     * ge detaljerade instruktioner om vad användaren behöver göra
     * för att möjliggöra borttagningen.
     */

    public CustomerDeletionException(String customerName, int activeConnections,
                                     String connectionType, String resolutionInstructions) {
        super("Kunden '" + customerName + "' kan inte tas bort eftersom den har " +
                activeConnections + " " + connectionType + ". " + resolutionInstructions);
    }

    /**
     * Konstruktor för regelbaserade borttagningsförbud.
     *
     * Denna konstruktor används när borttagningen förhindras av affärsregler
     * snarare än tekniska kopplingar. Till exempel kan vissa kunder vara
     * skyddade från borttagning av juridiska skäl, kontraktuella åtaganden,
     * eller företagspolicy.
     *
     * Exempel på användning:
     * throw new CustomerDeletionException(
     *     customerId,
     *     "LEGAL_HOLD",
     *     "Kunden är under juridisk granskning och kan inte tas bort förrän granskningen är avslutad"
     * );
     */
    public CustomerDeletionException(Long customerId, String ruleCode, String ruleDescription) {
        super("Kund med ID " + customerId + " kan inte tas bort [" + ruleCode + "]: " + ruleDescription);
    }

    /**
     * Konstruktor för cascade-relaterade problem.
     *
     * Denna konstruktor är särskilt användbar när borttagning av en kund
     * skulle kräva borttagning av så mycket relaterad data att det blir
     * farligt eller opraktiskt. Den varnar för omfattningen av den
     * kaskaderade borttagningen.
     *
     * Exempel på användning:
     * throw new CustomerDeletionException(
     *     "Växjö Kommun",
     *     Arrays.asList("15 historiska uppdrag", "47 arbetsdagar", "12 fakturor"),
     *     "Denna operation skulle radera kritisk historisk data som krävs för revision"
     * );
     */
    public CustomerDeletionException(String customerName, java.util.List<String> affectedEntities,
                                     String businessImpact) {
        super("Kunden '" + customerName + "' kan inte tas bort eftersom det skulle påverka: " +
                String.join(", ", affectedEntities) + ". " + businessImpact);
    }
}
