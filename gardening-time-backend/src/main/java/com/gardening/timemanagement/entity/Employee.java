package com.gardening.timemanagement.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;


@Entity
@Table(name = "employees")
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(name = "name", nullable = false)
    @NotBlank(message = "Medarbetarens namn får inte vara tomt")
    @Size(max = 255, message = "Medarbetarens namn får inte vara längre än 255 tecken")
    private String name;


    @Column(name = "phone")
    @Size(max = 20, message = "Telefonnummer får inte vara längre än 20 tecken")
    private String phone;


    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Default konstruktor för JPA
    public Employee() {}

    /**
     * Konstruktor för att skapa en ny aktiv medarbetare.
     * Nya medarbetare är alltid aktiva från början.
     */
    public Employee(String name, String phone) {
        this.name = name;
        this.phone = phone;
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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
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


    // Hjälpmetod för att avgöra om medarbetaren kan tilldelas nya uppdrag.
    public boolean canBeAssignedToWork() {
        return isActive != null && isActive;
    }


    // Hjälpmetod för att inaktivera en medarbetare istället för att ta bort den.
    public void deactivate() {
        this.isActive = false;
    }


    // Metod för att reaktivera en medarbetare som tidigare inaktiverats.
    public void reactivate() {
        this.isActive = true;
    }

    // toString metod för att få en läsbar representation av medarbetaren
    @Override
    public String toString() {
        return "Employee{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", phone='" + phone + '\'' +
                ", isActive=" + isActive +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}