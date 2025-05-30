package com.gardening.timemanagement.repository;


import com.gardening.timemanagement.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;


//  Spring Data JPA genererar automatiskt implementationer av alla metoder
//  baserat på metodnamn och @Query-annotationer

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long>  {

    // QUERY BY METHOD NAME - Spring genererar SQL automatiskt


    // Spring konverterar automatiskt till: SELECT * FROM customers WHERE name = ?
    Optional<Customer> findByName(String name);


    // Hittar alla kunder vars namn innehåller given sträng (case-insensitive)
    // Genererar: SELECT * FROM customers WHERE LOWER(name) LIKE LOWER(?)
    List<Customer> findByNameContainingIgnoreCase(String namePattern);


    // Hitta kunder baserat på telefonnummer
    Optional<Customer> findByPhone(String phone);


    // Hittar alla kunder vars namn börjar med given bokstav/bokstäver
    List<Customer> findByNameStartingWithIgnoreCase(String namePrefix);

    boolean existsByName(String name);

    boolean existsByPhone(String phone);


    // CUSTOM QUERIES - Mer komplexa sökningar med @Query

    // Hittar alla kunder som har aktiva uppdrag
    @Query("SELECT DISTINCT c FROM Customer c " +
            "JOIN c.tasks t " +
            "WHERE t.status = 'ACTIVE'")
    List<Customer> findCustomersWithActiveTasks();



    // Hittar kunder baserat på partiell matchning av namn eller telefon
    @Query("SELECT c FROM Customer c " +
            "WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
            "OR c.phone LIKE CONCAT('%', :searchTerm, '%')")
    List<Customer> searchByNameOrPhone(@Param("searchTerm") String searchTerm);


    // Räknar antal aktiva uppdrag en kund har
    @Query("SELECT COUNT(t) FROM Task t " +
            "WHERE t.customer.id = :customerId AND t.status = 'ACTIVE'")
    long countActiveTasksForCustomer(@Param("customerId") Long customerId);


    // Hittar kunder sorterade efter senaste aktivitet
    @Query("SELECT DISTINCT c FROM Customer c " +
            "LEFT JOIN c.tasks t " +
            "ORDER BY t.createdAt DESC")
    List<Customer> findCustomersOrderedByRecentActivity();
}
