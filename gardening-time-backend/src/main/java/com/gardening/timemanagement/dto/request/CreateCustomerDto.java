package com.gardening.timemanagement.dto.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO för att skapa en ny kund.
 * Innehåller endast de fält som användaren ska ange när de skapar en kund.
 *
 * Notera vad som INTE finns här:
 * - ID (genereras av databasen)
 * - createdAt/updatedAt (hanteras av databasen)
 */


public class CreateCustomerDto {


    @NotBlank(message = "Kundnamn måste anges")
    @Size(min = 2, max = 255, message = "Kundnamn måste vara mellan 2 och 255 tecken")
    private String name;

    @Pattern(regexp = "^(\\+46|0)[1-9]\\d{7,9}$|^$",
            message = "Ogiltigt telefonnummer format (använd svenskt format)")
    private String phone;

    @Size(max = 500, message = "Adress får inte vara längre än 500 tecken")
    private String address;

    // Default konstruktor för JSON deserialization
    public CreateCustomerDto() {}

    public CreateCustomerDto(String name, String phone, String address) {
        this.name = name;
        this.phone = phone;
        this.address = address;
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



}
