package com.gardening.timemanagement.entity;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;

@Entity
@Table (name= "work_days")
public class WorkDay {

    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private long Id;

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

    // Anteckningar för arbetsdagen (valfritt)
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

    // En tom konstruktor som krävs av Spring
    public WorkDay() {
    }

    // En konstruktor för att skapa en arbetsdag med grundläggande information
    public WorkDay(LocalDate date, Task task, Employee supervisor) {
        this.date = date;
        this.task = task;
        this.supervisor = supervisor;
    }

    // Konstruktor utan arbetsledare
    public WorkDay(LocalDate date, Task task) {
        this.date = date;
        this.task = task;
    }

    public long getId() {
        return Id;
    }

    public void setId(long id) {
        Id = id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public Employee getSupervisor() {
        return supervisor;
    }

    public void setSupervisor(Employee supervisor) {
        this.supervisor = supervisor;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<WorkDayEquipment> getEquipmentUsed() {
        return equipmentUsed;
    }

    public void setEquipmentUsed(List<WorkDayEquipment> equipmentUsed) {
        this.equipmentUsed = equipmentUsed;
    }

    public List<EmployeeTime> getEmployeeTimes() {
        return employeeTimes;
    }

    public void setEmployeeTimes(List<EmployeeTime> employeeTimes) {
        this.employeeTimes = employeeTimes;
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

    // Lägga till utrustning till arbetsdag
    public void addEquipment (Equipment equipment) {
        WorkDayEquipment workDayEquipment = new WorkDayEquipment (this, equipment, quantity);
        this.equipmentUsed.add(workDayEquipment);
    }

    // Tar bort utrustning från arbetsdag
    public void removeEquipment (Equipment equipment) {
        this.equipmentUsed.removeIf(wde -> wde.getEquipment().equals(equipment));
        // wde >>> WorkDayEquipment
    }



    // Lägger till en medarbetares arbetstid för denna dag.
    public void addEmployeeTime(EmployeeTime employeeTime) {
        employeeTime.setWorkDay(this);
        this.employeeTimes.add(employeeTime);
    }


    // Tar bort en medarbetares arbetstid från denna dag.
    public void removeEmployeeTime(EmployeeTime employeeTime) {
        this.employeeTimes.remove(employeeTime);
        employeeTime.setWorkDay(null);
    }


    // Beräknar totalt antal arbetstimmar för alla medarbetare denna dag
    // Exkluderar lunchtid men inkluderar körtid
    public double getTotalWorkHours() {
        return employeeTimes.stream()
                .mapToDouble(EmployeeTime::getTotalHours)
                .sum();
    }


    // Beräknar total körtid för denna arbetsdag
    public double getTotalDriveHours() {
        return employeeTimes.stream()
                .mapToDouble(et -> et.getIsDriver() ? et.getDriveTimeHours().doubleValue() : 0.0)
                .sum();
    }


    // Räknar antal unika medarbetare som arbetade denna dag
    public int getEmployeeCount() {
        return employeeTimes.size();
    }


     // Kontrollerar om en specifik medarbetare redan är registrerad för denna dag.
     // Förhindrar dubbelregistrering av samma medarbetare.
    public boolean hasEmployee(Employee employee) {
        return employeeTimes.stream()
                .anyMatch(et -> et.getEmployee().equals(employee));
    }


    // Kontrollerar om denna arbetsdag kan ta emot ny arbetstidsregistrering
    // Baserat på uppdragets status och dagens datum
    public boolean canAcceptNewEmployeeTime() {
        return task != null && task.canAcceptWorkTime() && !date.isAfter(LocalDate.now());
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

