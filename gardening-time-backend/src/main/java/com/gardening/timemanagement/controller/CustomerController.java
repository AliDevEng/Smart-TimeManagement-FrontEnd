package com.gardening.timemanagement.controller;

import com.gardening.timemanagement.dto.request.CreateCustomerDto;
import com.gardening.timemanagement.dto.response.CustomerResponseDto;
import com.gardening.timemanagement.entity.Customer;
import com.gardening.timemanagement.mapper.CustomerMapper;
import com.gardening.timemanagement.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Denna controller exponerar endpoints för:
 *  - Skapa nya kunder
 *  - Hämta kundinformation
 *  - Uppdatera kunder
 *  - Hantera kunddata
 */


@RestController
@RequestMapping ("/api/customers")
public class CustomerController {

    // Dependency injection av våra service-komponenter
    // Spring injicerar automatiskt dessa när controllern skapas
    private final CustomerService customerService;
    private final CustomerMapper customerMapper;

    public CustomerController (CustomerService customerService, CustomerMapper customerMapper) {
        this.customerService = customerService;
        this.customerMapper = customerMapper;
    }


    @PostMapping
    public ResponseEntity<CustomerResponseDto> createCustomer(@Valid @RequestBody CreateCustomerDto createDto) {

        // STEG 1: Konvertera från DTO till Entity
        // Vi använder vår CustomerMapper för att översätta från frontend-format
        // till database-format.
        Customer customer = customerMapper.toEntity(createDto);

        // STEG 2: Delegera affärslogiken till CustomerService
        Customer savedCustomer = customerService.createCustomer(customer);

        // STEG 3: Konvertera från Entity till Response DTO
        // Nu när kunden är sparad (och har fått ID och tidsstämplar från databasen),
        // konverterar vi tillbaka till frontend-format för att skicka som svar.
        CustomerResponseDto responseDto = customerMapper.toResponseDto(savedCustomer);

        // STEG 4: Returnera med korrekt HTTP-status
        // HTTP 201 Created betyder "resursen har skapats framgångsrikt".
        // ResponseEntity låter oss specificera både data och statuskod.
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }



}
