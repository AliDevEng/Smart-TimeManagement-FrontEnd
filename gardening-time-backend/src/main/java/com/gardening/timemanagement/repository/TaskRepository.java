package com.gardening.timemanagement.repository;

import com.gardening.timemanagement.entity.Task;
import com.gardening.timemanagement.entity.Task.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;


@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    // GRUNDLÄGGANDE SÖKNINGAR



    // Hittar uppdrag baserat på uppdragsnummer
    Optional<Task> findByNumber(String number);


    // Hittar alla uppdrag för en specifik kund
    List<Task> findByCustomerId(Long customerId);


    // Hittar uppdrag baserat på status
    List<Task> findByStatus(TaskStatus status);


    // Hittar alla aktiva uppdrag för en specifik kund
    List<Task> findByCustomerIdAndStatus(Long customerId, TaskStatus status);


    // Kontrollerar om ett uppdragsnummer redan existerar
    boolean existsByNumber(String number);


    // DATUM-BASERADE QUERIES



    // Hittar uppdrag som startade inom ett specifikt datumintervall
    List<Task> findByStartDateBetween(LocalDate startDate, LocalDate endDate);


    // Hittar uppdrag som startade innan ett specifikt datum
    List<Task> findByStartDateBefore(LocalDate date);


    // Hittar alla uppdrag som är aktiva och har startdatum satt
    List<Task> findByStatusAndStartDateIsNotNull(TaskStatus status);


    // KOMPLEXA RAPPORT-QUERIES


    /**
     * Hittar alla uppdrag för en kund med eager loading av kunddata.
     * JOIN FETCH förhindrar N+1 query problem när vi behöver kundinfo.
     */
    @Query("SELECT t FROM Task t JOIN FETCH t.customer " +
            "WHERE t.customer.id = :customerId " +
            "ORDER BY t.startDate DESC")
    List<Task> findTasksWithCustomerByCustomerId(@Param("customerId") Long customerId);

    /**
     * Hittar alla aktiva uppdrag med kundinfo för dropdown-menyer.
     */
    @Query("SELECT t FROM Task t JOIN FETCH t.customer " +
            "WHERE t.status = 'ACTIVE' " +
            "ORDER BY t.customer.name, t.number")
    List<Task> findActiveTasksWithCustomer();

    /**
     * Räknar total antal arbetsdagar för ett specifikt uppdrag.
     */
    @Query("SELECT COUNT(wd) FROM WorkDay wd WHERE wd.task.id = :taskId")
    long countWorkDaysForTask(@Param("taskId") Long taskId);

    /**
     * Beräknar total arbetstid för ett uppdrag i timmar.
     * Summerar all registrerad arbetstid från alla medarbetare och dagar.
     */
    @Query("SELECT COALESCE(SUM(" +
            "    (HOUR(et.endTime) * 60 + MINUTE(et.endTime)) - " +
            "    (HOUR(et.startTime) * 60 + MINUTE(et.startTime)) - " +
            "    et.lunchMinutes + " +
            "    (et.driveTimeHours * 60)" +
            ") / 60.0, 0) " +
            "FROM EmployeeTime et " +
            "JOIN et.workDay wd " +
            "WHERE wd.task.id = :taskId")
    Double getTotalHoursForTask(@Param("taskId") Long taskId);

    /**
     * Hittar uppdrag som har arbetstidsregistreringar inom ett datumintervall.
     * Användbart för månads- och årsrapporter.
     */
    @Query("SELECT DISTINCT t FROM Task t " +
            "JOIN t.workDays wd " +
            "WHERE wd.date BETWEEN :startDate AND :endDate " +
            "ORDER BY t.customer.name, t.number")
    List<Task> findTasksWithWorkInPeriod(@Param("startDate") LocalDate startDate,
                                         @Param("endDate") LocalDate endDate);

    /**
     * Hittar uppdrag som behöver uppmärksamhet (aktiva men ingen nylig aktivitet).
     * Hjälper projektledare att identifiera stagnerade projekt.
     */
    @Query("SELECT t FROM Task t " +
            "WHERE t.status = 'ACTIVE' " +
            "AND t.startDate IS NOT NULL " +
            "AND NOT EXISTS (" +
            "    SELECT wd FROM WorkDay wd " +
            "    WHERE wd.task = t " +
            "    AND wd.date > :cutoffDate" +
            ")")
    List<Task> findStagnantTasks(@Param("cutoffDate") LocalDate cutoffDate);

    // STATISTIK OCH AGGREGERING


    /**
     * Räknar uppdrag per status för dashboard-statistik.
     */
    @Query("SELECT t.status, COUNT(t) FROM Task t GROUP BY t.status")
    List<Object[]> countTasksByStatus();

    /**
     * Hittar de mest aktiva kunderna baserat på antal uppdrag.
     */
    @Query("SELECT t.customer, COUNT(t) as taskCount FROM Task t " +
            "GROUP BY t.customer " +
            "ORDER BY taskCount DESC")
    List<Object[]> findMostActiveCustomers();

    /**
     * Söker uppdrag baserat på uppdragsnummer eller kundens namn.
     */
    @Query("SELECT t FROM Task t JOIN t.customer c " +
            "WHERE LOWER(t.number) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
            "OR LOWER(c.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
            "ORDER BY t.createdAt DESC")
    List<Task> searchTasks(@Param("searchTerm") String searchTerm);


    /**
     * Räknar antal aktiva uppdrag för en specifik kund.
     * Används för att kontrollera affärsregler om max antal uppdrag per kund.
     *
     * @param customerId Kundens ID
     * @return Antal aktiva uppdrag (status = ACTIVE)
     */
    @Query("SELECT COUNT(t) FROM Task t " +
            "WHERE t.customer.id = :customerId AND t.status = 'ACTIVE'")
    long countActiveTasksForCustomer(@Param("customerId") Long customerId);
    
}