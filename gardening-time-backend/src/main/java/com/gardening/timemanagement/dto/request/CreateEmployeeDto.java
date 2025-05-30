
package com.gardening.timemanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO för att skapa en ny medarbetare.
 * Innehåller alla obligatoriska och valfria fält för medarbetarskapande.
 */
public class CreateEmployeeDto {

    @NotBlank(message = "Namn måste anges")
    @Size(min = 2, max = 255, message = "Namn måste vara mellan 2 och 255 tecken")
    private String name;

    @Pattern(regexp = "^(\\+46|0)[1-9]\\d{7,9}$|^$",
            message = "Ogiltigt telefonnummer format (använd svenskt format)")
    private String phone;

    // Default konstruktor för JSON deserialization
    public CreateEmployeeDto() {}

    public CreateEmployeeDto(String name, String phone) {
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
        return "CreateEmployeeDto{" +
                "name='" + name + '\'' +
                ", phone='" + phone + '\'' +
                '}';
    }
}