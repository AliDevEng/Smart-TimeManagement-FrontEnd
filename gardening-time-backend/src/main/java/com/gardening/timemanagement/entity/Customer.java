package com.gardening.timemanagement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public class Customer {


    // AUTO_INCREMENT i MySQL motsvaras av GenerationType.IDENTITY i JPA.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Kundens namn. Detta fält är obligatoriskt och får inte vara tomt.
     * Validering sker både på Java-nivå och databasnivå.
     */
    @Column(name = "name", nullable = false)
    @NotBlank(message = "Kundnamn får inte vara tomt")
    @Size(max = 255, message = "Kundnamn får inte vara längre än 255 tecken")
    private String name;


    // Kundens telefonnummer. Detta är valfritt så vi tillåter null-värden.

    @Column(name = "phone")
    @Size(max = 20, message = "Telefonnummer får inte vara längre än 20 tecken")
    private String phone;


    // Kundens adress. Också valfritt och kan vara längre text
    @Column(name = "address", columnDefinition = "TEXT")
    private String address;


    // Tidpunkt när kunden skapades. Sätts automatiskt av databasen.
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;


    // Tidpunkt när kunden senast uppdaterades. Uppdateras automatiskt av databasen.
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Default konstruktor krävs av JPA
    public Customer() {}

    // Konstruktor för att skapa en ny kund med grundläggande information.
    // ID, createdAt och updatedAt hanteras automatiskt av databasen.
    public Customer(String name, String phone, String address) {
        this.name = name;
        this.phone = phone;
        this.address = address;
    }

    // Getter och setter metoder
    // Dessa behövs för att Spring ska kunna komma åt och modifiera objektets data

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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
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

    // toString metod för att få en läsbar representation av kunden
    @Override
    public String toString() {
        return "Customer{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", phone='" + phone + '\'' +
                ", address='" + address + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }


}
