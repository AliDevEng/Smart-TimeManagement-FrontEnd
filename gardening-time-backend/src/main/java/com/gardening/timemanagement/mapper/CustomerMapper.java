package com.gardening.timemanagement.mapper;


import com.gardening.timemanagement.dto.request.CreateCustomerDto;
import com.gardening.timemanagement.dto.response.CustomerResponseDto;
import com.gardening.timemanagement.entity.Customer;
import org.springframework.stereotype.Component;

/**
 * Mapper-klass för Customer-entiteten.
 * Denna klass fungerar som en "översättare" mellan olika representationer
 * av kunddata i vårt system. Den konverterar mellan:
 * - CreateCustomerDto (data från frontend) → Customer (entitet för databas)
 * - Customer (entitet från databas) → CustomerResponseDto (data till frontend)
 */


@Component
public class CustomerMapper {


    public Customer toEntity(CreateCustomerDto dto) {

        // Null-check för säkerhet - vi vill inte krascha om vi får null
        if (dto == null) {
            return null;
        }

        // Skapa en ny Customer-entitet och fyll i fälten från DTO:n
        Customer customer = new Customer();

        // Kopiera alla fält från CreateCustomerDto till Customer-entiteten
        customer.setName(dto.getName());
        customer.setPhone(dto.getPhone());
        customer.setAddress(dto.getAddress());

        // Notera: Vi sätter INTE id, createdAt eller updatedAt
        // Dessa hanteras automatiskt av databasen

        return customer;
    }

    /**
     * Konverterar från Customer-entitet till CustomerResponseDto.
     * Denna metod används när vi ska skicka kunddata tillbaka till frontend.
     */
    public CustomerResponseDto toResponseDto(Customer customer) {

        // Null-check för säkerhet
        if (customer == null) {
            return null;
        }

        // Skapa en ny CustomerResponseDto och fyll i ALLA fält från entiteten
        CustomerResponseDto dto = new CustomerResponseDto();

        // Kopiera alla fält från Customer-entiteten till ResponseDto
        dto.setId(customer.getId());                    // Systemgenererat ID
        dto.setName(customer.getName());                // Användardata
        dto.setPhone(customer.getPhone());              // Användardata (kan vara null)
        dto.setAddress(customer.getAddress());          // Användardata (kan vara null)
        dto.setCreatedAt(customer.getCreatedAt());      // Systemgenererad tidsstämpel
        dto.setUpdatedAt(customer.getUpdatedAt());      // Systemgenererad tidsstämpel

        return dto;
    }


    public CustomerResponseDto toResponseDtoWithConstructor(Customer customer) {
        if (customer == null) {
            return null;
        }

        // Använd konstruktorn som tar alla fält som parametrar
        return new CustomerResponseDto(
                customer.getId(),
                customer.getName(),
                customer.getPhone(),
                customer.getAddress(),
                customer.getCreatedAt(),
                customer.getUpdatedAt()
        );
    }



    // public void updateEntityFromDto(UpdateCustomerDto dto, Customer customer) {
    //
    // }
}
