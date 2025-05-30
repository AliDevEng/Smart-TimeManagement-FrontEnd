package com.gardening.timemanagement.dto.response;

/**
 * Förenklad DTO för medarbetare-listor.
 * Innehåller endast den viktigaste informationen för prestanda.
 */
public class EmployeeSummaryDto {

    private Long id;
    private String name;
    private String phone;
    private Boolean isActive;

    public EmployeeSummaryDto() {}

    public EmployeeSummaryDto(Long id, String name, String phone, Boolean isActive) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.isActive = isActive;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    @Override
    public String toString() {
        return "EmployeeSummaryDto{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", phone='" + phone + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}