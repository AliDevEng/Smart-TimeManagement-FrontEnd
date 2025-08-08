package com.gardening.timemanagement.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Entity
@Table(name = "work_days")
public class WorkDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Ändrat till Long för konsistens

    @Column(name = "date", nullable = false)
    @NotNull(message = "Datum måste anges")
    private LocalDate date;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    @NotNull(message = "Uppdrag måste anges")
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supervisor_id")
    private Employee supervisor;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "workDay", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkDayEquipment> equipmentUsed = new ArrayList<>();

    @OneToMany(mappedBy = "workDay", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EmployeeTime> employeeTimes = new ArrayList<>();

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Konstruktorer
    public WorkDay() {}

    public WorkDay(LocalDate date, Task task, Employee supervisor) {
        this.date = date;
        this.task = task;
        this.supervisor = supervisor;
    }

    public WorkDay(LocalDate date, Task task) {
        this.date = date;
        this.task = task;
    }

    // Grundläggande getters och setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public Task getTask() { return task; }
    public void setTask(Task task) { this.task = task; }

    public Employee getSupervisor() { return supervisor; }
    public void setSupervisor(Employee supervisor) { this.supervisor = supervisor; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public List<WorkDayEquipment> getEquipmentUsed() { return equipmentUsed; }
    public void setEquipmentUsed(List<WorkDayEquipment> equipmentUsed) { this.equipmentUsed = equipmentUsed; }

    public List<EmployeeTime> getEmployeeTimes() { return employeeTimes; }
    public void setEmployeeTimes(List<EmployeeTime> employeeTimes) { this.employeeTimes = employeeTimes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // Kompatibilitetsmetoder för WorkDayService
    public String getDescription() {
        return this.notes != null ? this.notes : "";
    }

    public void setDescription(String description) {
        this.notes = description;
    }

    public String getWeatherConditions() {
        return ""; // Returnerar tom sträng tills vi lägger till detta fält
    }

    public void setWeatherConditions(String weatherConditions) {
        // Ingen implementation än - kan läggas till senare
    }

    // Affärslogik-metoder
    public double getTotalWorkHours() {
        if (employeeTimes == null || employeeTimes.isEmpty()) {
            return 0.0;
        }
        return employeeTimes.stream()
                .filter(Objects::nonNull)
                .mapToDouble(et -> et.getTotalHours() != null ? et.getTotalHours().doubleValue() : 0.0)
                .sum();
    }

    public double getTotalDriveHours() {
        return employeeTimes.stream()
                .mapToDouble(et -> et.getIsDriver() ? et.getDriveTimeHours().doubleValue() : 0.0)
                .sum();
    }

    public int getEmployeeCount() {
        return this.employeeTimes != null ? this.employeeTimes.size() : 0;
    }

    public int getEquipmentCount() {
        return this.equipmentUsed != null ? this.equipmentUsed.size() : 0;
    }

    public void addEmployeeTime(EmployeeTime employeeTime) {
        if (employeeTime == null) {
            throw new IllegalArgumentException("EmployeeTime cannot be null");
        }
        if (this.employeeTimes == null) {
            this.employeeTimes = new ArrayList<>();
        }

        // Förhindra dubbletter
        boolean alreadyExists = this.employeeTimes.stream()
                .anyMatch(et -> et.getEmployee() != null &&
                        employeeTime.getEmployee() != null &&
                        et.getEmployee().getId().equals(employeeTime.getEmployee().getId()));

        if (alreadyExists) {
            throw new IllegalStateException("Employee already has time entry for this WorkDay");
        }

        this.employeeTimes.add(employeeTime);
        employeeTime.setWorkDay(this);
    }

    public void removeEmployeeTime(EmployeeTime employeeTime) {
        this.employeeTimes.remove(employeeTime);
        employeeTime.setWorkDay(null);
    }

    public void addEquipment(Equipment equipment, int quantity) {
        if (equipment == null) {
            throw new IllegalArgumentException("Equipment cannot be null");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Equipment quantity must be positive");
        }
        if (this.equipmentUsed == null) {
            this.equipmentUsed = new ArrayList<>();
        }

        // Kontrollera om utrustningen redan används
        Optional<WorkDayEquipment> existing = this.equipmentUsed.stream()
                .filter(we -> we.getEquipment() != null &&
                        we.getEquipment().getId().equals(equipment.getId()))
                .findFirst();

        if (existing.isPresent()) {
            existing.get().setQuantity(existing.get().getQuantity() + quantity);
        } else {
            WorkDayEquipment workDayEquipment = new WorkDayEquipment(this, equipment, quantity);
            this.equipmentUsed.add(workDayEquipment);
        }
    }

    public void removeEquipment(Equipment equipment) {
        this.equipmentUsed.removeIf(wde -> wde.getEquipment().equals(equipment));
    }

    public boolean hasEmployee(Employee employee) {
        if (employee == null || this.employeeTimes == null) {
            return false;
        }
        return this.employeeTimes.stream()
                .anyMatch(et -> et.getEmployee() != null &&
                        et.getEmployee().getId().equals(employee.getId()));
    }

    public boolean canAcceptNewEmployeeTime() {
        return task != null && task.canAcceptWorkTime() && !date.isAfter(LocalDate.now());
    }

    public boolean isValid() {
        if (this.date == null || this.task == null) {
            return false;
        }
        if (this.employeeTimes == null || this.employeeTimes.isEmpty()) {
            return false;
        }
        return this.employeeTimes.stream()
                .allMatch(et -> et != null && et.getEmployee() != null);
    }

    @Override
    public String toString() {
        return "WorkDay{" +
                "id=" + id +
                ", date=" + date +
                ", task=" + (task != null ? task.getNumber() : "null") +
                ", supervisor=" + (supervisor != null ? supervisor.getName() : "none") +
                ", employeeCount=" + getEmployeeCount() +
                ", totalHours=" + getTotalWorkHours() +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}