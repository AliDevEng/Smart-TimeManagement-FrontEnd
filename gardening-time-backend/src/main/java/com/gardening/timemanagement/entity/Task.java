package com.gardening.timemanagement.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity-klass som representerar ett arbetsuppdrag i vårt Time-Management-System.
 * Ett uppdrag är en specifik arbetsinsats som utförs för en kund under en viss tidsperiod.
 *
 * Denna entitet är central i systemet eftersom den fungerar som en länk mellan
 * kunder och arbetsdagar. Alla arbetstider registreras mot specifika uppdrag.
 *
 * Uppdrag kan ha olika statusar (aktiv, avslutad, avbruten) vilket gör det möjligt
 * att spåra projektframsteg och kontrollera vilka uppdrag som är tillgängliga
 * för ny arbetstidsregistrering.
 */
@Entity
@Table(name = "tasks")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unikt uppdragsnummer som används för identifiering och kommunikation
     * med kunden. Detta nummer används ofta i fakturering och rapportering.
     *
     * Fältet är unikt i databasen för att förhindra dubbletter.
     */
    @Column(name = "number", nullable = false, unique = true)
    @NotBlank(message = "Uppdragsnummer får inte vara tomt")
    @Size(max = 100, message = "Uppdragsnummer får inte vara längre än 100 tecken")
    private String number;

    /**
     * Relation till kunden som detta uppdrag utförs för.
     * Detta är en Many-to-One relation eftersom en kund kan ha många uppdrag,
     * men varje uppdrag tillhör endast en kund.
     *
     * @JoinColumn specificerar vilken kolumn i 'tasks' tabellen som innehåller
     * främmande nyckeln till 'customers' tabellen.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    // FetchType.LAZY säger till att denna ska laddas bara när den behövs och inte i alla Query
    @JoinColumn(name = "customer_id", nullable = false)
    // Här finns Foreign-Key
    @NotNull(message = "Kund måste anges för uppdraget")
    private Customer customer;


    @Column(name = "description", columnDefinition = "TEXT")
    private String description;


    @Column(name = "start_date")
    private LocalDate startDate;


    @Column(name = "end_date")
    private LocalDate endDate;

    // Status för uppdraget: ACTIVE, COMPLETED, CANCELLED
    // (EnumType.STRING) >>> Sparar ACTIVE, COMPLETED, CANCELLED i databasen istället av 0-1-2
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TaskStatus status = TaskStatus.ACTIVE;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Enum som definierar möjliga statusar för ett uppdrag.
     * Denna är definierad som en inre klass för att hålla relaterad kod tillsammans.
     */
    public enum TaskStatus {
        ACTIVE("active"),      // Pågående uppdrag som kan ta emot ny arbetstid
        COMPLETED("completed"), // Avslutat uppdrag, ingen mer arbetstid registreras
        CANCELLED("cancelled"); // Avbrutet uppdrag, oftast av kunden eller externa skäl

        private final String value;

        TaskStatus(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    // Default konstruktor för JPA
    public Task() {}

    /**
     * Konstruktor för att skapa ett nytt uppdrag med grundläggande information.
     * Nya uppdrag är alltid aktiva från början.
     */
    public Task(String number, Customer customer, String description) {
        this.number = number;
        this.customer = customer;
        this.description = description;
        this.status = TaskStatus.ACTIVE;
    }

    // Getter och setter metoder
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Kontrollerar om detta uppdrag kan ta emot ny arbetstidsregistrering.
     * Endast aktiva uppdrag bör visas i arbetstidsformulär.
     */
    public boolean canAcceptWorkTime() {
        return TaskStatus.ACTIVE.equals(status);
    }

    /**
     * Kontrollerar om uppdraget är pågående (har startdatum men inget slutdatum).
     */
    public boolean isInProgress() {
        return startDate != null && endDate == null && TaskStatus.ACTIVE.equals(status);
    }

    /**
     * Startar uppdraget genom att sätta startdatum till idag och status till aktiv.
     * Kan endast anropas på uppdrag som inte redan har startats.
     */
    public void startTask() {
        if (this.startDate != null) {
            throw new IllegalStateException("Uppdraget har redan startats");
        }
        this.startDate = LocalDate.now();
        this.status = TaskStatus.ACTIVE;
    }

    /**
     * Avslutar uppdraget genom att sätta slutdatum och ändra status.
     *
     * @param endDate Datum när uppdraget avslutades
     */
    public void completeTask(LocalDate endDate) {
        if (this.startDate == null) {
            throw new IllegalStateException("Uppdraget måste startas innan det kan avslutas");
        }
        if (endDate.isBefore(this.startDate)) {
            throw new IllegalArgumentException("Slutdatum kan inte vara före startdatum");
        }
        this.endDate = endDate;
        this.status = TaskStatus.COMPLETED;
    }

    /**
     * Avbryter uppdraget. Till skillnad från att avsluta kan detta göras
     * oavsett om uppdraget har startat eller inte.
     */
    public void cancelTask() {
        this.status = TaskStatus.CANCELLED;
        if (this.endDate == null) {
            this.endDate = LocalDate.now();
        }
    }

    /**
     * Beräknar hur många dagar uppdraget har pågått eller totalt varade.
     * Returnerar 0 om uppdraget inte har startat.
     */
    public long getDurationInDays() {
        if (startDate == null) {
            return 0;
        }
        LocalDate end = endDate != null ? endDate : LocalDate.now();
        return startDate.until(end).getDays() + 1; // +1 för att inkludera startdagen
    }

    /**
     * Returnerar en användarvänlig representation av uppdragsstatus.
     */
    public String getStatusDisplayName() {
        return switch (status) {
            case ACTIVE -> "Pågående";
            case COMPLETED -> "Avslutad";
            case CANCELLED -> "Avbruten";
        };
    }

    @Override
    public String toString() {
        return "Task{" +
                "id=" + id +
                ", number='" + number + '\'' +
                ", customer=" + (customer != null ? customer.getName() : "null") +
                ", description='" + description + '\'' +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", status=" + status +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}