package com.gardening.timemanagement.dto.response;


import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

/**
 * Response DTO för customer-entitet
 * Skickar kunddata tillbaka till klient efter Create, Update, Get
 * Innehåller alla fält som FRONTEND kan behöva
 */


public class CustomerResponseDto {

    private Long id;

    private String name;

    private String phone;

    private String address;

    @JsonFormat (pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    // Tidpunkt när kund senast uppdaterades
    @JsonFormat (pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    // Tom konstruktor krävs för JSON serialisering av Spring
    public CustomerResponseDto() {
    }

    public CustomerResponseDto
            (Long id, String name, String phone, String address,
             LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.address = address;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }


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


    @Override
    public String toString() {
        return "CustomerResponseDto{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", phone='" + phone + '\'' +
                ", address='" + address + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }


}
