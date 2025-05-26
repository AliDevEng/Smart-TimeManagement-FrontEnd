package com.gardening.timemanagement.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;


@Entity
@Table(name = "equipment")
public class Equipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Namnet på utrustningen. Detta bör vara beskrivande nog för att
     * medarbetare ska kunna identifiera rätt utrustning när de registrerar
     * vad som användes på en arbetsdag.
     */
    @Column(name = "name", nullable = false)
    @NotBlank(message = "Utrustningsnamn får inte vara tomt")
    @Size(max = 255, message = "Utrustningsnamn får inte vara längre än 255 tecken")
    private String name;

    /**
     * Dagspriset för denna utrustning i svenska kronor.
     * Vi använder BigDecimal istället för double för att undvika
     * avrundningsfel som kan uppstå vid ekonomiska beräkningar.
     * BigDecimal säkerställer exakt precision för penningbelopp,
     * vilket är kritiskt för korrekt fakturering och kostnadskalkylering.
     */
    @Column(name = "daily_price", nullable = false, precision = 10, scale = 2)
    @NotNull(message = "Dagspris måste anges")
    @DecimalMin(value = "0.0", inclusive = false, message = "Dagspris måste vara större än 0")
    private BigDecimal dailyPrice;

    /**
     * Indikerar om utrustningen fortfarande är tillgänglig för användning.
     * Precis som med medarbetare tar vi sällan bort utrustning helt
     * eftersom vi behöver bevara historisk data för kostnadsspårning.
     *
     * Utrustning kan bli inaktiv om den är skadad, såld, eller av andra
     * skäl inte längre tillgänglig för projekt.
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Default konstruktor för JPA
    public Equipment() {}

    /**
     * Konstruktor för att skapa ny utrustning med namn och dagspris.
     * Ny utrustning är alltid aktiv från början.
     */
    public Equipment(String name, BigDecimal dailyPrice) {
        this.name = name;
        this.dailyPrice = dailyPrice;
        this.isActive = true;
    }

    /**
     * Bekvämlighets-konstruktor som accepterar dagspris som double
     * och konverterar det till BigDecimal automatiskt.
     * Detta gör det enklare att skapa utrustning i testkod.
     */
    public Equipment(String name, double dailyPrice) {
        this.name = name;
        this.dailyPrice = BigDecimal.valueOf(dailyPrice);
        this.isActive = true;
    }

    // Getter och setter metoder
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getDailyPrice() {
        return dailyPrice;
    }

    public void setDailyPrice(BigDecimal dailyPrice) {
        this.dailyPrice = dailyPrice;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
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
     * Kontrollerar om denna utrustning kan bokas för nya uppdrag.
     * Endast aktiv utrustning bör visas i bokningsformulär.
     */
    public boolean isAvailableForBooking() {
        return isActive != null && isActive;
    }

    /**
     * Beräknar kostnaden för att använda denna utrustning under
     * ett specificerat antal dagar.
     * @param days Antal dagar som utrustningen används
     * @return Total kostnad som BigDecimal
     */
    public BigDecimal calculateCostForDays(int days) {
        if (days <= 0) {
            return BigDecimal.ZERO;
        }
        return dailyPrice.multiply(BigDecimal.valueOf(days));
    }

    /**
     * Uppdaterar dagspriset med automatisk validering.
     * Denna metod säkerställer att priset alltid är positivt.
     */
    public void updateDailyPrice(BigDecimal newPrice) {
        if (newPrice == null || newPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Dagspris måste vara större än 0");
        }
        this.dailyPrice = newPrice;
    }

    /**
     * Inaktiverar utrustningen och förhindrar att den bokas för nya uppdrag.
     * Historisk användning påverkas inte.
     */
    public void deactivate() {
        this.isActive = false;
    }


    // Reaktiverar tidigare inaktiverad utrustning.
    public void reactivate() {
        this.isActive = true;
    }


    // Formaterar dagspriset som en läsbar sträng med valuta
    public String getFormattedDailyPrice() {
        return dailyPrice.toString() + " SEK";
    }

    // toString metod för att få en läsbar representation av verktyget
    @Override
    public String toString() {
        return "Equipment{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", dailyPrice=" + dailyPrice +
                ", isActive=" + isActive +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}