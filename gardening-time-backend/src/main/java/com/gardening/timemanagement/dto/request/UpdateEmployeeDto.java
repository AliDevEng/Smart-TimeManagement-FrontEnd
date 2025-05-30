package com.gardening.timemanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO för att uppdatera en befintlig medarbetare.
 * Innehåller endast fält som får uppdateras (inte status eller ID).
 */
public class UpdateEmployeeDto {

    @NotBlank(message = "Namn måste anges")
    @Size(min = 2, max = 255, message = "Namn måste vara mellan 2 och 255 tecken")
    private String name;

    @Pattern(regexp = "^(\\+46|0)[1-9]\\d{7,9}$|^$",
            message = "Ogiltigt telefonnummer format (använd svenskt format)")
    private String phone;

    public UpdateEmployeeDto() {}

    public UpdateEmployeeDto(String name, String phone) {
        this.name = name;
        this.phone = phone;
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

    @Override
    public String toString() {
        return "UpdateEmployeeDto{" +
                "name='" + name + '\'' +
                ", phone='" + phone + '\'' +
                '}';
    }
}