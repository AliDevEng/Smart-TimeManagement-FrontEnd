package com.gardening.timemanagement.repository;

import com.gardening.timemanagement.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;


@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    // ===================================================================
    // GRUNDLÄGGANDE MEDARBETARSÖKNING
    // ===================================================================


    // Hittar medarbetare baserat på exakt namn
    Optional<Employee> findByName(String name);


    //Söker medarbetare vars namn innehåller given sträng
    List<Employee> findByNameContainingIgnoreCase(String namePattern);


    // Hittar medarbetare baserat på telefonnummer
    Optional<Employee> findByPhone(String phone);


    //Hittar alla aktiva medarbetare som kan tilldelas nya uppdrag
    List<Employee> findByIsActiveTrue();


    // Hitta alla inaktiva medarbetare
    List<Employee> findByIsActiveFalse();


    // Kontrollerar om en medarbetare med specifikt namn redan existerar
    boolean existsByName(String name);


    // Kontrollerar om en medarbetare med specifikt telefonnummer existerar
    boolean existsByPhone(String phone);

    // ===================================================================
    // ARBETSTIDSBASERADE QUERIES
    // ===================================================================


    // Hittar alla medarbetare som har arbetat inom ett specifikt datumintervall
    @Query("SELECT DISTINCT e FROM Employee e " +
            "JOIN e.employeeTimes et " +
            "JOIN et.workDay wd " +
            "WHERE wd.date BETWEEN :startDate AND :endDate " +
            "ORDER BY e.name")
    List<Employee> findEmployeesWhoWorkedInPeriod(@Param("startDate") LocalDate startDate,
                                                  @Param("endDate") LocalDate endDate);


    // Hittar medarbetare som arbetade på ett specifikt uppdrag
    @Query("SELECT DISTINCT e FROM Employee e " +
            "JOIN e.employeeTimes et " +
            "JOIN et.workDay wd " +
            "WHERE wd.task.id = :taskId " +
            "ORDER BY e.name")
    List<Employee> findEmployeesWhoWorkedOnTask(@Param("taskId") Long taskId);


    // Hittar aktiva medarbetare som INTE har arbetat inom ett specifikt datumintervall
    @Query("SELECT e FROM Employee e " +
            "WHERE e.isActive = true " +
            "AND NOT EXISTS (" +
            "    SELECT et FROM EmployeeTime et " +
            "    JOIN et.workDay wd " +
            "    WHERE et.employee = e " +
            "    AND wd.date BETWEEN :startDate AND :endDate" +
            ")")
    List<Employee> findInactiveEmployeesInPeriod(@Param("startDate") LocalDate startDate,
                                                 @Param("endDate") LocalDate endDate);

    // ===================================================================
    // ARBETSTIDSSTATISTIK OCH BERÄKNINGAR
    // ===================================================================

    /**
     * Beräknar total arbetstid för en medarbetare inom ett datumintervall.
     * Returnerar timmar som decimal (t.ex. 37.5 för 37,5 timmar).
     *
     * Denna query gör komplexa tidsberäkningar direkt i databasen:
     * - Konverterar start/sluttider till minuter
     * - Drar av lunchtid
     * - Lägger till körtid för förare
     * - Konverterar tillbaka till timmar
     */
    @Query("SELECT COALESCE(SUM(" +
            "    ((HOUR(et.endTime) * 60 + MINUTE(et.endTime)) - " +
            "     (HOUR(et.startTime) * 60 + MINUTE(et.startTime)) - " +
            "     et.lunchMinutes) / 60.0 + " +
            "    CASE WHEN et.isDriver = true THEN et.driveTimeHours ELSE 0 END" +
            "), 0) " +
            "FROM EmployeeTime et " +
            "JOIN et.workDay wd " +
            "WHERE et.employee.id = :employeeId " +
            "AND wd.date BETWEEN :startDate AND :endDate")
    Double getTotalHoursForEmployeeInPeriod(@Param("employeeId") Long employeeId,
                                            @Param("startDate") LocalDate startDate,
                                            @Param("endDate") LocalDate endDate);


    // Räknar antal arbetsdagar för en medarbetare inom ett datumintervall
    @Query("SELECT COUNT(DISTINCT wd.date) " +
            "FROM EmployeeTime et " +
            "JOIN et.workDay wd " +
            "WHERE et.employee.id = :employeeId " +
            "AND wd.date BETWEEN :startDate AND :endDate")
    Long getWorkDaysCountForEmployeeInPeriod(@Param("employeeId") Long employeeId,
                                             @Param("startDate") LocalDate startDate,
                                             @Param("endDate") LocalDate endDate);


    // Beräknar total körtid för förare inom ett datumintervall
    @Query("SELECT COALESCE(SUM(et.driveTimeHours), 0) " +
            "FROM EmployeeTime et " +
            "JOIN et.workDay wd " +
            "WHERE et.employee.id = :employeeId " +
            "AND et.isDriver = true " +
            "AND wd.date BETWEEN :startDate AND :endDate")
    Double getTotalDriveHoursForEmployeeInPeriod(@Param("employeeId") Long employeeId,
                                                 @Param("startDate") LocalDate startDate,
                                                 @Param("endDate") LocalDate endDate);

    // ===================================================================
    // MEDARBETARRANKNING OCH PRESTANDA
    // ===================================================================

    /**
     * Hittar mest aktiva medarbetare baserat på antal arbetade timmar inom en period.
     * Returnerar medarbetare sorterade efter total arbetstid (mest aktiva först).
     */

    @Query("SELECT et.employee, " +
            "SUM(((HOUR(et.endTime) * 60 + MINUTE(et.endTime)) - " +
            "     (HOUR(et.startTime) * 60 + MINUTE(et.startTime)) - " +
            "     et.lunchMinutes) / 60.0 + " +
            "    CASE WHEN et.isDriver = true THEN et.driveTimeHours ELSE 0 END) as totalHours " +
            "FROM EmployeeTime et " +
            "JOIN et.workDay wd " +
            "WHERE wd.date BETWEEN :startDate AND :endDate " +
            "AND et.employee.isActive = true " +
            "GROUP BY et.employee " +
            "ORDER BY totalHours DESC")
    List<Object[]> findMostActiveEmployeesInPeriod(@Param("startDate") LocalDate startDate,
                                                   @Param("endDate") LocalDate endDate);


    // Hittar medarbetare som ofta fungerar som förare, användbart för planering av transport
    @Query("SELECT et.employee, COUNT(et) as driverDays, SUM(et.driveTimeHours) as totalDriveTime " +
            "FROM EmployeeTime et " +
            "WHERE et.isDriver = true " +
            "AND et.employee.isActive = true " +
            "GROUP BY et.employee " +
            "HAVING COUNT(et) >= :minDriverDays " +
            "ORDER BY totalDriveTime DESC")
    List<Object[]> findFrequentDrivers(@Param("minDriverDays") Long minDriverDays);


    // Beräknar genomsnittlig arbetstid per dag för en medarbetare
    @Query("SELECT AVG(" +
            "    ((HOUR(et.endTime) * 60 + MINUTE(et.endTime)) - " +
            "     (HOUR(et.startTime) * 60 + MINUTE(et.startTime)) - " +
            "     et.lunchMinutes) / 60.0 + " +
            "    CASE WHEN et.isDriver = true THEN et.driveTimeHours ELSE 0 END" +
            ") " +
            "FROM EmployeeTime et " +
            "JOIN et.workDay wd " +
            "WHERE et.employee.id = :employeeId " +
            "AND wd.date BETWEEN :startDate AND :endDate")
    Double getAverageHoursPerDayForEmployee(@Param("employeeId") Long employeeId,
                                            @Param("startDate") LocalDate startDate,
                                            @Param("endDate") LocalDate endDate);

    // ===================================================================
    // KVALITETS- OCH VALIDERING QUERIES
    // ===================================================================

    /**
     * Hittar medarbetare med potentiellt problematiska tidsregistreringar.
     * Identifierar registreringar som kan behöva granskas (för långa eller korta dagar).
     */

    @Query("SELECT DISTINCT et.employee " +
            "FROM EmployeeTime et " +
            "JOIN et.workDay wd " +
            "WHERE wd.date BETWEEN :startDate AND :endDate " +
            "AND (((HOUR(et.endTime) * 60 + MINUTE(et.endTime)) - " +
            "      (HOUR(et.startTime) * 60 + MINUTE(et.startTime)) - " +
            "      et.lunchMinutes) / 60.0 + " +
            "     CASE WHEN et.isDriver = true THEN et.driveTimeHours ELSE 0 END) " +
            "    NOT BETWEEN :minHours AND :maxHours")
    List<Employee> findEmployeesWithUnusualHours(@Param("startDate") LocalDate startDate,
                                                 @Param("endDate") LocalDate endDate,
                                                 @Param("minHours") Double minHours,
                                                 @Param("maxHours") Double maxHours);

    /**
     * Kontrollerar om en medarbetare redan är registrerad för en specifik arbetsdag.
     * Förhindrar dubbelregistreringar.
     */

    @Query("SELECT COUNT(et) > 0 FROM EmployeeTime et " +
            "JOIN et.workDay wd " +
            "WHERE et.employee.id = :employeeId " +
            "AND wd.date = :date " +
            "AND wd.task.id = :taskId")
    boolean isEmployeeRegisteredForWorkDay(@Param("employeeId") Long employeeId,
                                           @Param("date") LocalDate date,
                                           @Param("taskId") Long taskId);

    // ===================================================================
    // RAPPORTERING OCH EXPORT
    // ===================================================================

    /**
     * Genererar månadsrapport för alla aktiva medarbetare.
     * Returnerar medarbetare med total arbetstid för månaden.
     */
    @Query("SELECT et.employee, " +
            "COUNT(DISTINCT wd.date) as workDays, " +
            "SUM(((HOUR(et.endTime) * 60 + MINUTE(et.endTime)) - " +
            "     (HOUR(et.startTime) * 60 + MINUTE(et.startTime)) - " +
            "     et.lunchMinutes) / 60.0 + " +
            "    CASE WHEN et.isDriver = true THEN et.driveTimeHours ELSE 0 END) as totalHours, " +
            "SUM(CASE WHEN et.isDriver = true THEN et.driveTimeHours ELSE 0 END) as totalDriveHours " +
            "FROM EmployeeTime et " +
            "JOIN et.workDay wd " +
            "WHERE et.employee.isActive = true " +
            "AND wd.date BETWEEN :startDate AND :endDate " +
            "GROUP BY et.employee " +
            "ORDER BY et.employee.name")
    List<Object[]> generateMonthlyReportForAllEmployees(@Param("startDate") LocalDate startDate,
                                                        @Param("endDate") LocalDate endDate);


    // Söker medarbetare baserat på namn eller telefonnummer
    @Query("SELECT e FROM Employee e " +
            "WHERE LOWER(e.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
            "OR e.phone LIKE CONCAT('%', :searchTerm, '%') " +
            "ORDER BY e.isActive DESC, e.name")
    List<Employee> searchEmployees(@Param("searchTerm") String searchTerm);
}