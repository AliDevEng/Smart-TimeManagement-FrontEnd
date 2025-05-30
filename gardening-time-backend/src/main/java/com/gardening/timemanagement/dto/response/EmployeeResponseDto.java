package com.gardening.timemanagement.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

/**
 * Response DTO för Employee-entitet.
 * Används när vi skickar Employee-data tillbaka till klienten.
 * Innehåller all information inklusive databas-metadata.
 */
public class EmployeeResponseDto {

    private Long id;
    private String name;
    private String phone;
    private Boolean isActive;
    private Boolean canBeAssigned;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    // Default konstruktor för JSON serialisering
    public EmployeeResponseDto() {}

    // Getters och setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public Boolean getCanBeAssigned() { return canBeAssigned; }
    public void setCanBeAssigned(Boolean canBeAssigned) { this.canBeAssigned = canBeAssigned; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}