package com.gardening.timemanagement.service;

import com.gardening.timemanagement.entity.Customer;
import com.gardening.timemanagement.exception.CustomerDeletionException;
import com.gardening.timemanagement.exception.CustomerNotFoundException;
import com.gardening.timemanagement.exception.DuplicateCustomerException;
import com.gardening.timemanagement.repository.CustomerRepository;
import com.gardening.timemanagement.repository.TaskRepository;
// import com.gardening.timemanagement.exception.CustomerNotFoundException;
// import com.gardening.timemanagement.exception.CustomerDeletionException;
// import com.gardening.timemanagement.exception.DuplicateCustomerException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

/**
 * Service-klass för affärslogik kring kundhantering.
 * Hanterar CRUD-operationer, validering, och affärsregler för kunder.
 *
 * Denna service implementerar transaktionshantering och säkerställer
 * att alla kundoperationer följer affärsreglerna för systemet.
 */
@Service
@Transactional(readOnly = true) // Default för alla metoder - optimerar läsoperationer
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final TaskRepository taskRepository;

    /**
     * Konstruktor-baserad dependency injection.
     * Spring injicerar automatiskt repository-beroendena.
     */
    public CustomerService(CustomerRepository customerRepository,
                           TaskRepository taskRepository) {
        this.customerRepository = customerRepository;
        this.taskRepository = taskRepository;
    }

    // ===================================================================
    // GRUNDLÄGGANDE CRUD-OPERATIONER
    // ===================================================================

    /**
     * Hämtar alla kunder sorterade efter namn.
     *
     * @return Lista med alla kunder
     */
    public List<Customer> getAllCustomers() {
        return customerRepository.findAll()
                .stream()
                .sorted((c1, c2) -> c1.getName().compareToIgnoreCase(c2.getName()))
                .toList();
    }

    /**
     * Hämtar en kund baserat på ID.
     *
     * @param id Kundernas ID
     * @return Customer-objektet
     * @throws CustomerNotFoundException om kunden inte finns
     */
    public Customer getCustomerById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException("Kund med ID " + id + " finns inte"));
    }

    /**
     * Skapar en ny kund med validering av affärsregler.
     *
     * @param customer Den nya kunden att skapa
     * @return Den sparade kunden med genererat ID
     * @throws DuplicateCustomerException om kund med samma namn redan finns
     */
    @Transactional // Override default readOnly för denna metod
    public Customer createCustomer(Customer customer) {
        // Validera affärsregler innan sparning
        validateNewCustomer(customer);

        // Kontrollera dubbletter
        if (customerRepository.existsByName(customer.getName())) {
            throw new DuplicateCustomerException(
                    "En kund med namnet '" + customer.getName() + "' finns redan"
            );
        }

        // Validera telefonnummer om det finns
        if (customer.getPhone() != null && !customer.getPhone().trim().isEmpty()) {
            if (customerRepository.existsByPhone(customer.getPhone())) {
                throw new DuplicateCustomerException(
                        "En kund med telefonnummer '" + customer.getPhone() + "' finns redan"
                );
            }
        }

        // Spara och returnera kunden
        Customer savedCustomer = customerRepository.save(customer);

        // Logga affärsaktivitet (i verkliga system skulle detta gå till en audit-log)
        logCustomerActivity("CREATED", savedCustomer.getId(), "Ny kund skapad: " + savedCustomer.getName());

        return savedCustomer;
    }

    /**
     * Uppdaterar en befintlig kund.
     *
     * @param id ID för kunden att uppdatera
     * @param updatedCustomer Kund med uppdaterad information
     * @return Den uppdaterade kunden
     * @throws CustomerNotFoundException om kunden inte finns
     */
    @Transactional
    public Customer updateCustomer(Long id, Customer updatedCustomer) {
        Customer existingCustomer = getCustomerById(id);

        // Kontrollera namn-konflikter (om namnet ändras)
        if (!existingCustomer.getName().equals(updatedCustomer.getName())) {
            if (customerRepository.existsByName(updatedCustomer.getName())) {
                throw new DuplicateCustomerException(
                        "En annan kund med namnet '" + updatedCustomer.getName() + "' finns redan"
                );
            }
        }

        // Kontrollera telefon-konflikter (om telefonen ändras)
        if (updatedCustomer.getPhone() != null &&
                !updatedCustomer.getPhone().equals(existingCustomer.getPhone())) {
            if (customerRepository.existsByPhone(updatedCustomer.getPhone())) {
                throw new DuplicateCustomerException(
                        "En annan kund med telefonnummer '" + updatedCustomer.getPhone() + "' finns redan"
                );
            }
        }

        // Uppdatera fält som får ändras
        existingCustomer.setName(updatedCustomer.getName());
        existingCustomer.setPhone(updatedCustomer.getPhone());
        existingCustomer.setAddress(updatedCustomer.getAddress());

        Customer savedCustomer = customerRepository.save(existingCustomer);

        logCustomerActivity("UPDATED", savedCustomer.getId(), "Kund uppdaterad: " + savedCustomer.getName());

        return savedCustomer;
    }

    /**
     * Tar bort en kund efter validering av affärsregler.
     * Kunder med aktiva uppdrag kan inte tas bort.
     *
     * @param id ID för kunden att ta bort
     * @throws CustomerNotFoundException om kunden inte finns
     * @throws CustomerDeletionException om kunden har aktiva uppdrag
     */
    @Transactional
    public void deleteCustomer(Long id) {
        Customer customer = getCustomerById(id);

        // Kontrollera att kunden inte har aktiva uppdrag
        long activeTaskCount = taskRepository.countActiveTasksForCustomer(id);
        if (activeTaskCount > 0) {
            throw new CustomerDeletionException(
                    "Kunden '" + customer.getName() + "' kan inte tas bort eftersom den har " +
                            activeTaskCount + " aktiva uppdrag. Avsluta alla uppdrag först."
            );
        }

        // Kontrollera att kunden inte har några uppdrag alls (historiska data)
        List<com.gardening.timemanagement.entity.Task> allTasks = taskRepository.findByCustomerId(id);
        if (!allTasks.isEmpty()) {
            throw new CustomerDeletionException(
                    "Kunden '" + customer.getName() + "' kan inte tas bort eftersom den har " +
                            allTasks.size() + " historiska uppdrag. Historisk data måste bevaras."
            );
        }

        logCustomerActivity("DELETED", customer.getId(), "Kund borttagen: " + customer.getName());

        customerRepository.deleteById(id);
    }

    // ===================================================================
    // SÖKFUNKTIONER OCH SPECIALISERADE QUERIES
    // ===================================================================

    /**
     * Söker kunder baserat på namn eller telefonnummer.
     *
     * @param searchTerm Sökterm för namn eller telefon
     * @return Lista med matchande kunder
     */
    public List<Customer> searchCustomers(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return getAllCustomers();
        }

        return customerRepository.searchByNameOrPhone(searchTerm.trim());
    }

    /**
     * Hämtar alla kunder som har aktiva uppdrag.
     * Användbart för rapporter och dashboard.
     *
     * @return Lista med aktiva kunder
     */
    public List<Customer> getActiveCustomers() {
        return customerRepository.findCustomersWithActiveTasks();
    }

    /**
     * Hämtar kund baserat på exakt namn.
     *
     * @param name Kundens namn
     * @return Optional med kunden om den finns
     */
    public Optional<Customer> getCustomerByName(String name) {
        return customerRepository.findByName(name);
    }

    /**
     * Hämtar kund baserat på telefonnummer.
     *
     * @param phone Telefonnummer
     * @return Optional med kunden om den finns
     */
    public Optional<Customer> getCustomerByPhone(String phone) {
        return customerRepository.findByPhone(phone);
    }

    // ===================================================================
    // AFFÄRSLOGIK OCH STATISTIK
    // ===================================================================

    /**
     * Räknar antal aktiva uppdrag för en kund.
     *
     * @param customerId Kundens ID
     * @return Antal aktiva uppdrag
     */
    public long getActiveTaskCount(Long customerId) {
        // Validera att kunden finns
        getCustomerById(customerId);

        return taskRepository.countActiveTasksForCustomer(customerId);
    }

    /**
     * Kontrollerar om en kund är aktiv (har pågående eller nyliga uppdrag).
     *
     * @param customerId Kundens ID
     * @return true om kunden är aktiv
     */
    public boolean isActiveCustomer(Long customerId) {
        return getActiveTaskCount(customerId) > 0;
    }

    /**
     * Validerar att en kund kan tilldelas nya uppdrag.
     *
     * @param customerId Kundens ID
     * @return true om kunden kan få nya uppdrag
     * @throws CustomerNotFoundException om kunden inte finns
     */
    public boolean canAssignNewTask(Long customerId) {
        Customer customer = getCustomerById(customerId);

        // I framtiden kan vi lägga till fler affärsregler här:
        // - Kreditkontroll
        // - Kontraktsstatus
        // - Betalningshistorik

        return true; // Alla befintliga kunder kan få nya uppdrag för tillfället
    }

    // ===================================================================
    // PRIVATA HJÄLPMETODER
    // ===================================================================

    /**
     * Validerar en ny kund enligt affärsregler.
     *
     * @param customer Kunden att validera
     * @throws IllegalArgumentException om valideringen misslyckas
     */
    private void validateNewCustomer(Customer customer) {
        if (customer == null) {
            throw new IllegalArgumentException("Kund kan inte vara null");
        }

        if (customer.getName() == null || customer.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Kundnamn måste anges");
        }

        if (customer.getName().trim().length() < 2) {
            throw new IllegalArgumentException("Kundnamn måste vara minst 2 tecken långt");
        }

        if (customer.getName().trim().length() > 255) {
            throw new IllegalArgumentException("Kundnamn får inte vara längre än 255 tecken");
        }

        // Validera telefonnummer format om det finns
        if (customer.getPhone() != null && !customer.getPhone().trim().isEmpty()) {
            if (!isValidPhoneNumber(customer.getPhone())) {
                throw new IllegalArgumentException("Ogiltigt telefonnummer format");
            }
        }
    }

    /**
     * Enkel validering av telefonnummer.
     * I ett riktigt system skulle detta vara mer sofistikerat.
     *
     * @param phone Telefonnummer att validera
     * @return true om formatet är giltigt
     */
    private boolean isValidPhoneNumber(String phone) {
        if (phone == null) return true; // Null är OK (valfritt fält)

        String cleanPhone = phone.trim().replaceAll("[\\s\\-()]", "");

        // Acceptera svenska mobilnummer och fasta nummer
        return cleanPhone.matches("^(\\+46|0)[1-9]\\d{7,9}$");
    }

    /**
     * Loggar kundaktiviteter för audit trail.
     * I ett riktigt system skulle detta integrera med ett loggningsramverk.
     *
     * @param action Typ av aktivitet
     * @param customerId ID för kunden
     * @param details Detaljer om aktiviteten
     */
    private void logCustomerActivity(String action, Long customerId, String details) {
        // I produktion: skicka till audit log, databas, eller loggningsystem
        System.out.println(String.format("[CUSTOMER_AUDIT] %s - Customer ID: %d - %s",
                action, customerId, details));
    }
}