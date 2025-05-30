package com.gardening.timemanagement.dto.request;

import jakarta.validation.constraints.Size;


public class EmployeeStatusChangeDto {

    @Size(max = 500, message = "Orsak får inte vara längre än 500 tecken")
    private String reason;

    public EmployeeStatusChangeDto() {}

    public EmployeeStatusChangeDto(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    @Override
    public String toString() {
        return "EmployeeStatusChangeDto{" +
                "reason='" + reason + '\'' +
                '}';
    }
}