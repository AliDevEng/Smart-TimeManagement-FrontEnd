package com.gardening.timemanagement.entity;


import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Entity
@Table(name = "work_day_equipment")
public class WorkDayEquipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    /**
     * Relation till arbetsdagen som denna utrustningsanvändning tillhör.
     * Many-to-One eftersom många utrustningsanvändningar kan tillhöra samma arbetsdag.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_day_id", nullable = false)
    @NotNull(message = "Arbetsdag måste anges")
    private WorkDay workDay;


    /**
     * Relation till utrustningen som användes.
     * Many-to-One eftersom samma utrustning kan användas på många arbetsdagar.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_id", nullable = false)
    @NotNull(message = "Utrustning måste anges")
    private Equipment equipment;


    @Column(name = "quantity", nullable = false)
    @Min(value = 1, message = "Antal måste vara minst 1")
    private Integer quantity = 1;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    public WorkDayEquipment() {
    }

    // Konstruktor för att skapa en ny utrustningsanvändning
    public WorkDayEquipment(WorkDay workDay, Equipment equipment, Integer quantity) {
        this.workDay = workDay;
        this.equipment = equipment;
        this.quantity = quantity;
    }

    public WorkDayEquipment(WorkDay workDay, Equipment equipment) {
        this(workDay, equipment, 1);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public WorkDay getWorkDay() {
        return workDay;
    }

    public void setWorkDay(WorkDay workDay) {
        this.workDay = workDay;
    }

    public Equipment getEquipment() {
        return equipment;
    }

    public void setEquipment(Equipment equipment) {
        this.equipment = equipment;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }


    public BigDecimal calculateTotalCost () {
        if (equipment == null || equipment.getDailyPrice() == null || quantity == null) {
            return BigDecimal.ZERO;
            // om någon av dessa värden saknas, är kostnaden noll
        }


        return equipment.getDailyPrice().multiply(BigDecimal.valueOf(quantity));
    }


    // Kontrollerar om denna utrustningsanvändning är giltig
    // Validerar att utrustningen är aktiv och tillgänglig för bokning
    public boolean isValidUsage () {
        return equipment != null &&
               equipment.isAvailableForBooking() &&
                quantity != null &&
                quantity >0;
    }

    /**
     * Uppdaterar antalet enheter med validering.
     * Säkerställer att antalet alltid är positivt.
     */
    public void updateQuantity(Integer newQuantity) {
        if (newQuantity == null || newQuantity < 1) {
            throw new IllegalArgumentException("Antal måste vara minst 1");
        }
        this.quantity = newQuantity;
    }

    /**
     * Returnerar en beskrivande text för denna utrustningsanvändning.
     * Användbart för rapporter och användargrässnitt.
     */
    public String getUsageDescription() {
        if (equipment == null) {
            return "Okänd utrustning";
        }

        String description = equipment.getName();
        if (quantity > 1) {
            description += " (x" + quantity + ")";
        }

        return description;
    }


    public String getFormattedCost() {
        BigDecimal cost = calculateTotalCost();
        return cost.toString() + " SEK";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WorkDayEquipment)) return false;

        WorkDayEquipment that = (WorkDayEquipment) o;

        // Två WorkDayEquipment är lika om de har samma workDay och equipment
        // Detta förhindrar dubbletter av samma utrustning på samma arbetsdag
        return workDay != null && workDay.equals(that.workDay) &&
                equipment != null && equipment.equals(that.equipment);
    }

    @Override
    public int hashCode() {
        int result = workDay != null ? workDay.hashCode() : 0;
        result = 31 * result + (equipment != null ? equipment.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "WorkDayEquipment{" +
                "id=" + id +
                ", workDay=" + (workDay != null ? workDay.getId() : "null") +
                ", equipment=" + (equipment != null ? equipment.getName() : "null") +
                ", quantity=" + quantity +
                ", totalCost=" + calculateTotalCost() +
                ", notes='" + notes + '\'' +
                '}';
    }
}
