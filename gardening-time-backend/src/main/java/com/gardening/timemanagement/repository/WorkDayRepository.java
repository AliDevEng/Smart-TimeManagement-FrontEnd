package com.gardening.timemanagement.repository;

import com.gardening.timemanagement.entity.WorkDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository för WorkDay-entiteten - systemets nav som förbinder
 * alla andra entiteter. Innehåller de mest komplexa queries som
 * sträcker sig över hela datamodellen för rapporter och analys.
 */
@Repository
public interface WorkDayRepository extends JpaRepository<WorkDay, Long> {

    // ===================================================================
    // GRUNDLÄGGANDE ARBETSDAGSSÖKNING
    // ===================================================================

    /**
     * Hittar alla arbetsdagar för ett specifikt uppdrag.
     */
    List<WorkDay> findByTaskId(Long taskId);

    /**
     * Hittar arbetsdagar för ett specifikt datum.
     */
    List<WorkDay> findByDate(LocalDate date);

    /**
     * Hittar arbetsdag för specifikt uppdrag och datum.
     * Denna kombination bör vara unik enligt vår affärslogik.
     */
    Optional<WorkDay> findByTaskIdAndDate(Long taskId, LocalDate date);

    /**
     * Hittar arbetsdagar inom ett datumintervall.
     */
    List<WorkDay> findByDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Hittar arbetsdagar för en specifik kund (via uppdrag).
     */
    @Query("SELECT wd FROM WorkDay wd " +
            "JOIN wd.task t " +
            "WHERE t.customer.id = :customerId " +
            "ORDER BY wd.date DESC")
    List<WorkDay> findByCustomerId(@Param("customerId") Long customerId);

    /**
     * Kontrollerar om det redan finns en arbetsdag för uppdrag och datum.
     * Förhindrar dubbletter av arbetsdagar.
     */
    boolean existsByTaskIdAndDate(Long taskId, LocalDate date);

    // ===================================================================
    // ARBETSLEDARE OCH PERSONALHANTERING
    // ===================================================================

    /**
     * Hittar arbetsdagar där en specifik medarbetare är arbetsledare.
     */
    List<WorkDay> findBySupervisorId(Long supervisorId);

    /**
     * Hittar arbetsdagar där en specifik medarbetare deltog (oavsett roll).
     */
    @Query("SELECT DISTINCT wd FROM WorkDay wd " +
            "JOIN wd.employeeTimes et " +
            "WHERE et.employee.id = :employeeId " +
            "ORDER BY wd.date DESC")
    List<WorkDay> findWorkDaysForEmployee(@Param("employeeId") Long employeeId);

    /**
     * Hittar arbetsdagar där en medarbetare var förare.
     */
    @Query("SELECT DISTINCT wd FROM WorkDay wd " +
            "JOIN wd.employeeTimes et " +
            "WHERE et.employee.id = :employeeId " +
            "AND et.isDriver = true " +
            "ORDER BY wd.date DESC")
    List<WorkDay> findDriverWorkDaysForEmployee(@Param("employeeId") Long employeeId);

    // ===================================================================
    // OMFATTANDE RAPPORTER MED ALLA ENTITETER
    // ===================================================================

    /**
     * Hittar arbetsdagar med all relaterad information för rapporter.
     * Använder JOIN FETCH för att undvika N+1 query problem.
     */
    @Query("SELECT wd FROM WorkDay wd " +
            "JOIN FETCH wd.task t " +
            "JOIN FETCH t.customer c " +
            "LEFT JOIN FETCH wd.supervisor s " +
            "WHERE wd.date BETWEEN :startDate AND :endDate " +
            "ORDER BY wd.date DESC, c.name, t.number")
    List<WorkDay> findDetailedWorkDaysInPeriod(@Param("startDate") LocalDate startDate,
                                               @Param("endDate") LocalDate endDate);

    /**
     * Genererar fullständig månadsrapport med all arbetstid, utrustningskostnader
     * och projektinformation. Detta är en av de mest komplexa queries i systemet.
     */
    @Query("SELECT wd.date, " +
            "t.number as taskNumber, " +
            "c.name as customerName, " +
            "s.name as supervisorName, " +
            "COUNT(DISTINCT et.employee) as employeeCount, " +
            "SUM(((HOUR(et.endTime) * 60 + MINUTE(et.endTime)) - " +
            "     (HOUR(et.startTime) * 60 + MINUTE(et.startTime)) - " +
            "     et.lunchMinutes) / 60.0 + " +
            "    CASE WHEN et.isDriver = true THEN et.driveTimeHours ELSE 0 END) as totalHours, " +
            "SUM(CASE WHEN et.isDriver = true THEN et.driveTimeHours ELSE 0 END) as totalDriveHours, " +
            "COALESCE(SUM(eq.dailyPrice * wde.quantity), 0) as equipmentCost " +
            "FROM WorkDay wd " +
            "JOIN wd.task t " +
            "JOIN t.customer c " +
            "LEFT JOIN wd.supervisor s " +
            "JOIN wd.employeeTimes et " +
            "LEFT JOIN wd.equipmentUsed wde " +
            "LEFT JOIN wde.equipment eq " +
            "WHERE wd.date BETWEEN :startDate AND :endDate " +
            "GROUP BY wd.id, wd.date, t.number, c.name, s.name " +
            "ORDER BY wd.date DESC")
    List<Object[]> generateComprehensiveReport(@Param("startDate") LocalDate startDate,
                                               @Param("endDate") LocalDate endDate);

    // ===================================================================
    // EKONOMISK ANALYS OCH PROJEKTUPPFÖLJNING
    // ===================================================================

    /**
     * Beräknar total arbetstid för alla arbetsdagar inom en period.
     */
    @Query("SELECT COALESCE(SUM(" +
            "    ((HOUR(et.endTime) * 60 + MINUTE(et.endTime)) - " +
            "     (HOUR(et.startTime) * 60 + MINUTE(et.startTime)) - " +
            "     et.lunchMinutes) / 60.0 + " +
            "    CASE WHEN et.isDriver = true THEN et.driveTimeHours ELSE 0 END" +
            "), 0) " +
            "FROM WorkDay wd " +
            "JOIN wd.employeeTimes et " +
            "WHERE wd.date BETWEEN :startDate AND :endDate")
    Double getTotalHoursInPeriod(@Param("startDate") LocalDate startDate,
                                 @Param("endDate") LocalDate endDate);

    /**
     * Beräknar total utrustningskostnad för alla arbetsdagar inom en period.
     */
    @Query("SELECT COALESCE(SUM(eq.dailyPrice * wde.quantity), 0) " +
            "FROM WorkDay wd " +
            "JOIN wd.equipmentUsed wde " +
            "JOIN wde.equipment eq " +
            "WHERE wd.date BETWEEN :startDate AND :endDate")
    Double getTotalEquipmentCostInPeriod(@Param("startDate") LocalDate startDate,
                                         @Param("endDate") LocalDate endDate);

    /**
     * Hittar mest aktiva arbetsdagar baserat på antal medarbetare.
     */
    @Query("SELECT wd, COUNT(et) as employeeCount " +
            "FROM WorkDay wd " +
            "JOIN wd.employeeTimes et " +
            "WHERE wd.date BETWEEN :startDate AND :endDate " +
            "GROUP BY wd " +
            "ORDER BY employeeCount DESC")
    List<Object[]> findMostActiveWorkDays(@Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate);

    /**
     * Analyserar produktivitet per kund baserat på arbetstid per arbetsdag.
     */
    @Query("SELECT c.name as customerName, " +
            "COUNT(wd) as totalWorkDays, " +
            "AVG((" +
            "    SELECT SUM(((HOUR(et2.endTime) * 60 + MINUTE(et2.endTime)) - " +
            "                (HOUR(et2.startTime) * 60 + MINUTE(et2.startTime)) - " +
            "                et2.lunchMinutes) / 60.0) " +
            "    FROM EmployeeTime et2 " +
            "    WHERE et2.workDay = wd" +
            ")) as avgHoursPerDay " +
            "FROM WorkDay wd " +
            "JOIN wd.task t " +
            "JOIN t.customer c " +
            "WHERE wd.date BETWEEN :startDate AND :endDate " +
            "GROUP BY c.id, c.name " +
            "ORDER BY avgHoursPerDay DESC")
    List<Object[]> analyzeCustomerProductivity(@Param("startDate") LocalDate startDate,
                                               @Param("endDate") LocalDate endDate);

    // ===================================================================
    // KVALITETSKONTROLL OCH DATAVALIDERING
    // ===================================================================

    /**
     * Hittar arbetsdagar med potentiellt problematiska registreringar.
     * Identifierar dagar med extremt många eller få arbetstimmar.
     */
    @Query("SELECT wd FROM WorkDay wd " +
            "WHERE wd.date BETWEEN :startDate AND :endDate " +
            "AND (" +
            "    (SELECT COUNT(et) FROM EmployeeTime et WHERE et.workDay = wd) = 0 " +
            "    OR " +
            "    (SELECT SUM(((HOUR(et.endTime) * 60 + MINUTE(et.endTime)) - " +
            "                 (HOUR(et.startTime) * 60 + MINUTE(et.startTime)) - " +
            "                 et.lunchMinutes) / 60.0) " +
            "     FROM EmployeeTime et WHERE et.workDay = wd) NOT BETWEEN :minHours AND :maxHours" +
            ")")
    List<WorkDay> findProblematicWorkDays(@Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate,
                                          @Param("minHours") Double minHours,
                                          @Param("maxHours") Double maxHours);

    /**
     * Hittar arbetsdagar utan registrerad arbetstid.
     * Indikerar ofullständiga registreringar.
     */
    @Query("SELECT wd FROM WorkDay wd " +
            "WHERE NOT EXISTS (" +
            "    SELECT et FROM EmployeeTime et WHERE et.workDay = wd" +
            ") " +
            "ORDER BY wd.date DESC")
    List<WorkDay> findWorkDaysWithoutEmployeeTimes();

    /**
     * Hittar arbetsdagar med bara en medarbetare registrerad.
     * Kan indikera ofullständiga registreringar för team-projekt.
     */
    @Query("SELECT wd FROM WorkDay wd " +
            "WHERE (" +
            "    SELECT COUNT(et) FROM EmployeeTime et WHERE et.workDay = wd" +
            ") = 1 " +
            "AND wd.date BETWEEN :startDate AND :endDate " +
            "ORDER BY wd.date DESC")
    List<WorkDay> findSingleEmployeeWorkDays(@Param("startDate") LocalDate startDate,
                                             @Param("endDate") LocalDate endDate);

    // ===================================================================
    // DASHBOARD OCH ÖVERSIKTSSTATISTIK
    // ===================================================================

    /**
     * Räknar antal arbetsdagar per månad för trendanalys.
     */
    @Query("SELECT YEAR(wd.date), MONTH(wd.date), COUNT(wd) " +
            "FROM WorkDay wd " +
            "WHERE wd.date >= :startDate " +
            "GROUP BY YEAR(wd.date), MONTH(wd.date) " +
            "ORDER BY YEAR(wd.date) DESC, MONTH(wd.date) DESC")
    List<Object[]> getWorkDayCountByMonth(@Param("startDate") LocalDate startDate);

    /**
     * Hittar senaste arbetsdagar för dashboard-översikt.
     */
    @Query("SELECT wd FROM WorkDay wd " +
            "JOIN FETCH wd.task t " +
            "JOIN FETCH t.customer c " +
            "ORDER BY wd.date DESC, wd.createdAt DESC")
    List<WorkDay> findRecentWorkDays();
}