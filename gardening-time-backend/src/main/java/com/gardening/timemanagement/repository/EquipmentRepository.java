package com.gardening.timemanagement.repository;

import com.gardening.timemanagement.entity.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository för Equipment-entiteten med fokus på kostnadsspårning
 * och utrustningshantering. Innehåller ekonomiska beräkningar för
 * projektkalkylering och utrustningsanalys.
 */
@Repository
public interface EquipmentRepository extends JpaRepository<Equipment, Long> {

    // ===================================================================
    // GRUNDLÄGGANDE UTRUSTNINGSSÖKNING
    // ===================================================================

    /**
     * Hittar utrustning baserat på exakt namn.
     */
    Optional<Equipment> findByName(String name);

    /**
     * Söker utrustning vars namn innehåller given sträng (case-insensitive).
     */
    List<Equipment> findByNameContainingIgnoreCase(String namePattern);

    /**
     * Hittar all aktiv utrustning (tillgänglig för bokning).
     */
    List<Equipment> findByIsActiveTrue();

    /**
     * Hittar all inaktiv utrustning.
     * Användbart för administrativa rapporter och historisk data.
     */
    List<Equipment> findByIsActiveFalse();

    /**
     * Kontrollerar om utrustning med specifikt namn redan existerar.
     */
    boolean existsByName(String name);

    // ===================================================================
    // PRISBASERADE QUERIES
    // ===================================================================

    /**
     * Hittar aktiv utrustning sorterad efter dagspris (billigast först).
     * Hjälper vid val av kostnadseffektiv utrustning för projekt.
     */
    @Query("SELECT e FROM Equipment e " +
            "WHERE e.isActive = true " +
            "ORDER BY e.dailyPrice ASC")
    List<Equipment> findActiveEquipmentOrderedByPrice();

    /**
     * Hittar utrustning inom ett specifikt prisintervall.
     * Användbart för budgetplanering och kostnadsuppskattningar.
     */
    @Query("SELECT e FROM Equipment e " +
            "WHERE e.isActive = true " +
            "AND e.dailyPrice BETWEEN :minPrice AND :maxPrice " +
            "ORDER BY e.dailyPrice ASC")
    List<Equipment> findEquipmentInPriceRange(@Param("minPrice") BigDecimal minPrice,
                                              @Param("maxPrice") BigDecimal maxPrice);

    /**
     * Hittar dyraste aktiva utrustningen.
     * Kan användas för att identifiera premium-utrustning.
     */
    @Query("SELECT e FROM Equipment e " +
            "WHERE e.isActive = true " +
            "ORDER BY e.dailyPrice DESC")
    List<Equipment> findMostExpensiveEquipment();

    /**
     * Beräknar genomsnittligt dagspris för all aktiv utrustning.
     */
    @Query("SELECT AVG(e.dailyPrice) FROM Equipment e WHERE e.isActive = true")
    BigDecimal getAverageDailyPrice();

    // ===================================================================
    // ANVÄNDNINGS- OCH POPULARITETSSTATISTIK
    // ===================================================================

    /**
     * Hittar mest använda utrustning baserat på antal arbetsdagar.
     * Visar vilken utrustning som är mest populär/nödvändig.
     */
    @Query("SELECT wde.equipment, COUNT(wde) as usageCount " +
            "FROM WorkDayEquipment wde " +
            "WHERE wde.equipment.isActive = true " +
            "GROUP BY wde.equipment " +
            "ORDER BY usageCount DESC")
    List<Object[]> findMostUsedEquipment();

    /**
     * Hittar utrustning som inte har använts inom en specifik period.
     * Identifierar potentiellt överflödig utrustning.
     */
    @Query("SELECT e FROM Equipment e " +
            "WHERE e.isActive = true " +
            "AND NOT EXISTS (" +
            "    SELECT wde FROM WorkDayEquipment wde " +
            "    JOIN wde.workDay wd " +
            "    WHERE wde.equipment = e " +
            "    AND wd.date >= :cutoffDate" +
            ")")
    List<Equipment> findUnusedEquipmentSince(@Param("cutoffDate") LocalDate cutoffDate);

    /**
     * Hittar utrustning som användes på ett specifikt uppdrag.
     * Hjälper till att förstå utrustningsbehov för liknande projekt.
     */
    @Query("SELECT DISTINCT wde.equipment " +
            "FROM WorkDayEquipment wde " +
            "JOIN wde.workDay wd " +
            "WHERE wd.task.id = :taskId " +
            "ORDER BY wde.equipment.name")
    List<Equipment> findEquipmentUsedOnTask(@Param("taskId") Long taskId);

    // ===================================================================
    // KOSTNADSSPÅRNING OCH EKONOMISKA BERÄKNINGAR
    // ===================================================================

    /**
     * Beräknar total kostnad för en specifik utrustning inom en period.
     * Multiplicerar dagspris med antal dagar använd.
     */
    @Query("SELECT COALESCE(SUM(e.dailyPrice * wde.quantity), 0) " +
            "FROM WorkDayEquipment wde " +
            "JOIN wde.equipment e " +
            "JOIN wde.workDay wd " +
            "WHERE e.id = :equipmentId " +
            "AND wd.date BETWEEN :startDate AND :endDate")
    BigDecimal getTotalCostForEquipmentInPeriod(@Param("equipmentId") Long equipmentId,
                                                @Param("startDate") LocalDate startDate,
                                                @Param("endDate") LocalDate endDate);

    /**
     * Räknar antal dagar en specifik utrustning har använts inom en period.
     */
    @Query("SELECT COUNT(wde) " +
            "FROM WorkDayEquipment wde " +
            "JOIN wde.workDay wd " +
            "WHERE wde.equipment.id = :equipmentId " +
            "AND wd.date BETWEEN :startDate AND :endDate")
    Long getUsageDaysForEquipmentInPeriod(@Param("equipmentId") Long equipmentId,
                                          @Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate);

    /**
     * Beräknar total utrustningskostnad för alla uppdrag inom en period.
     * Viktigt för företagsekonomisk analys och budgetuppföljning.
     */
    @Query("SELECT COALESCE(SUM(e.dailyPrice * wde.quantity), 0) " +
            "FROM WorkDayEquipment wde " +
            "JOIN wde.equipment e " +
            "JOIN wde.workDay wd " +
            "WHERE wd.date BETWEEN :startDate AND :endDate")
    BigDecimal getTotalEquipmentCostInPeriod(@Param("startDate") LocalDate startDate,
                                             @Param("endDate") LocalDate endDate);

    /**
     * Genererar kostnadrapport per utrustning för en specifik period.
     * Visar vilken utrustning som genererar mest kostnad.
     */
    @Query("SELECT wde.equipment, " +
            "COUNT(wde) as usageDays, " +
            "SUM(wde.quantity) as totalQuantity, " +
            "SUM(e.dailyPrice * wde.quantity) as totalCost " +
            "FROM WorkDayEquipment wde " +
            "JOIN wde.equipment e " +
            "JOIN wde.workDay wd " +
            "WHERE wd.date BETWEEN :startDate AND :endDate " +
            "GROUP BY wde.equipment " +
            "ORDER BY totalCost DESC")
    List<Object[]> generateEquipmentCostReport(@Param("startDate") LocalDate startDate,
                                               @Param("endDate") LocalDate endDate);

    // ===================================================================
    // UTRUSTNINGSEFFEKTIVITET OCH ROI-ANALYS
    // ===================================================================

    /**
     * Beräknar kostnad per användningsdag för varje utrustning.
     * Hjälper till att identifiera kostnadseffektiv utrustning.
     */
    @Query("SELECT wde.equipment, " +
            "e.dailyPrice as dailyPrice, " +
            "COUNT(wde) as usageDays, " +
            "(SUM(e.dailyPrice * wde.quantity) / COUNT(wde)) as avgCostPerDay " +
            "FROM WorkDayEquipment wde " +
            "JOIN wde.equipment e " +
            "GROUP BY wde.equipment, e.dailyPrice " +
            "HAVING COUNT(wde) >= :minUsageDays " +
            "ORDER BY avgCostPerDay ASC")
    List<Object[]> findMostCostEffectiveEquipment(@Param("minUsageDays") Long minUsageDays);

    /**
     * Hittar utrustning med högest användningsfrekvens vs kostnad.
     * Identifierar "värdefull" utrustning som används ofta trots hög kostnad.
     */
    @Query("SELECT wde.equipment, " +
            "COUNT(wde) as usageDays, " +
            "e.dailyPrice, " +
            "(COUNT(wde) * 1.0 / e.dailyPrice) as efficiencyRatio " +
            "FROM WorkDayEquipment wde " +
            "JOIN wde.equipment e " +
            "WHERE e.isActive = true " +
            "GROUP BY wde.equipment, e.dailyPrice " +
            "ORDER BY efficiencyRatio DESC")
    List<Object[]> findHighestEfficiencyEquipment();

    // ===================================================================
    // VALIDERING OCH KVALITETSKONTROLL
    // ===================================================================

    /**
     * Kontrollerar om specifik utrustning redan används på en arbetsdag.
     * Förhindrar dubbelbokning av unik utrustning.
     */
    @Query("SELECT COUNT(wde) > 0 FROM WorkDayEquipment wde " +
            "JOIN wde.workDay wd " +
            "WHERE wde.equipment.id = :equipmentId " +
            "AND wd.date = :date")
    boolean isEquipmentUsedOnDate(@Param("equipmentId") Long equipmentId,
                                  @Param("date") LocalDate date);

    /**
     * Hittar utrustning med suspekt låg användning vs hög kostnad.
     * Kan indikera överflödig eller felaktigt prissatt utrustning.
     */
    @Query("SELECT e FROM Equipment e " +
            "WHERE e.isActive = true " +
            "AND e.dailyPrice > :minPrice " +
            "AND (" +
            "    SELECT COUNT(wde) FROM WorkDayEquipment wde " +
            "    JOIN wde.workDay wd " +
            "    WHERE wde.equipment = e " +
            "    AND wd.date >= :cutoffDate" +
            ") < :maxUsages")
    List<Equipment> findExpensiveUnderutilizedEquipment(@Param("minPrice") BigDecimal minPrice,
                                                        @Param("cutoffDate") LocalDate cutoffDate,
                                                        @Param("maxUsages") Long maxUsages);

    /**
     * Söker utrustning baserat på namn.
     * Kraftfull sökfunktion för användargrässnitt.
     */
    @Query("SELECT e FROM Equipment e " +
            "WHERE LOWER(e.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
            "ORDER BY e.isActive DESC, e.dailyPrice ASC")
    List<Equipment> searchEquipment(@Param("searchTerm") String searchTerm);
}