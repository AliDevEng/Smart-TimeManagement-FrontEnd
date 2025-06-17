# REST API Development Pipeline
> Professional Spring Boot Architecture Guide

## 🎯 Steg 1: Planering & Design
**Vad bygger vi och varför?**

### Identifiera Entiteten
- Vilken affärsentitet arbetar vi med? (Customer, Employee, Task, etc.)
- Vilka fält behöver entiteten ha?
- Vilka affärsregler gäller för denna entitet?

### Bestäm API-Endpoints
- `POST /api/customers` (Skapa ny)
- `GET /api/customers/{id}` (Hämta en)
- `GET /api/customers` (Hämta alla)
- `PUT /api/customers/{id}` (Uppdatera)
- `DELETE /api/customers/{id}` (Ta bort)

### Definiera Affärsregler
- Vad får vara duplicerat, vad får inte?
- Vilka valideringsregler behövs?
- När får entiteter tas bort/inaktiveras?

---

## 🏗️ Steg 2: Bygg från Insidan och Ut
**Database → Service → Controller**

### 2.1 ENTITET (Entity)
📁 `src/main/java/.../entity/Customer.java`

```java
@Entity
@Table(name = "customers")
public class Customer {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank 
    @Size(max = 255)
    private String name;
    
    // Relationer till andra entiteter
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Task> tasks = new ArrayList<>();
    
    // Konstruktorer, getters, setters, toString()
}
```

**Kom ihåg:**
- `@Entity` och `@Table` annotationer
- `@Id` och `@GeneratedValue` för primärnyckel
- Valideringsannotationer (`@NotBlank`, `@Size`)
- Relationer till andra entiteter (`@OneToMany`, `@ManyToOne`)
- Konstruktorer (default + med parametrar)
- Getters/Setters
- `toString()` för debugging

---

### 2.2 DTOs (Data Transfer Objects)
📁 `src/main/java/.../dto/request/` & `dto/response/`

#### CreateDto (Inkommande data)
```java
public class CreateCustomerDto {
    @NotBlank(message = "Namn måste anges")
    @Size(min = 2, max = 255, message = "Namn måste vara mellan 2 och 255 tecken")
    private String name;
    
    @Pattern(regexp = "^(\\+46|0)[1-9]\\d{7,9}$|^$", 
             message = "Ogiltigt telefonnummer format (använd svenskt format)")
    private String phone;
    
    @Size(max = 500, message = "Adress får inte vara längre än 500 tecken")
    private String address;
    
    // Endast fält som användaren fyller i
    // INTE id, createdAt, updatedAt
}
```

#### ResponseDto (Utgående data)
```java
public class CustomerResponseDto {
    private Long id;
    private String name;
    private String phone;
    private String address;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
    
    // ALLA fält inklusive systemgenererade
}
```

#### UpdateDto (För framtida PUT-endpoints)
```java
public class UpdateCustomerDto {
    // Samma valideringar som CreateDto
    // Används för partiella uppdateringar
}
```

---

### 2.3 EXCEPTIONS (Felhantering)
📁 `src/main/java/.../exception/`

#### Specifika Exceptions
```java
// När entitet inte hittas
public class CustomerNotFoundException extends RuntimeException {
    public CustomerNotFoundException(Long id) {
        super("Customer med ID " + id + " finns inte");
    }
    
    public CustomerNotFoundException(String message) {
        super(message);
    }
}

// När dubbletter upptäcks
public class DuplicateCustomerException extends RuntimeException {
    public DuplicateCustomerException(String field, String value) {
        super("En customer med " + field + " '" + value + "' finns redan");
    }
    
    public DuplicateCustomerException(String message) {
        super(message);
    }
}

// När borttagning inte tillåts
public class CustomerDeletionException extends RuntimeException {
    public CustomerDeletionException(String customerName, int activeConnections, String connectionType) {
        super("Kunden '" + customerName + "' kan inte tas bort eftersom den har " +
              activeConnections + " " + connectionType);
    }
    
    public CustomerDeletionException(String message) {
        super(message);
    }
}
```

#### Global Exception Handler
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(CustomerNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCustomerNotFound(CustomerNotFoundException ex) {
        ErrorResponse errorResponse = new ErrorResponse(
            "CUSTOMER_NOT_FOUND",
            ex.getMessage(),
            HttpStatus.NOT_FOUND.value(),
            LocalDateTime.now()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }
    
    @ExceptionHandler(DuplicateCustomerException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateCustomer(DuplicateCustomerException ex) {
        ErrorResponse errorResponse = new ErrorResponse(
            "DUPLICATE_CUSTOMER",
            ex.getMessage(),
            HttpStatus.CONFLICT.value(),
            LocalDateTime.now()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }
    
    @ExceptionHandler(CustomerDeletionException.class)
    public ResponseEntity<ErrorResponse> handleCustomerDeletion(CustomerDeletionException ex) {
        ErrorResponse errorResponse = new ErrorResponse(
            "CUSTOMER_DELETION_FORBIDDEN",
            ex.getMessage(),
            HttpStatus.CONFLICT.value(),
            LocalDateTime.now()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> validationErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            validationErrors.put(fieldName, errorMessage);
        });
        
        ValidationErrorResponse errorResponse = new ValidationErrorResponse(
            "VALIDATION_FAILED",
            "Validering misslyckades för " + validationErrors.size() + " fält",
            HttpStatus.BAD_REQUEST.value(),
            LocalDateTime.now(),
            validationErrors
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        System.err.println("Oväntat fel: " + ex.getClass().getSimpleName());
        System.err.println("Meddelande: " + ex.getMessage());
        ex.printStackTrace();
        
        ErrorResponse errorResponse = new ErrorResponse(
            "INTERNAL_SERVER_ERROR",
            "Ett oväntat fel uppstod. Kontakta support om problemet kvarstår.",
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            LocalDateTime.now()
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    // ErrorResponse och ValidationErrorResponse inner classes
    public static class ErrorResponse {
        private String errorCode;
        private String message;
        private int statusCode;
        private LocalDateTime timestamp;
        
        // Konstruktor och getters
    }
    
    public static class ValidationErrorResponse extends ErrorResponse {
        private Map<String, String> validationErrors;
        
        // Konstruktor och getters
    }
}
```

---

### 2.4 REPOSITORY (Databasåtkomst)
📁 `src/main/java/.../repository/CustomerRepository.java`

```java
@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    
    // Automatiska metoder (ärver från JpaRepository):
    // save(), findById(), findAll(), deleteById(), existsById()
    
    // Query by Method Name:
    Optional<Customer> findByName(String name);
    Optional<Customer> findByPhone(String phone);
    boolean existsByName(String name);
    boolean existsByPhone(String phone);
    List<Customer> findByNameContainingIgnoreCase(String namePattern);
    List<Customer> findByNameStartingWithIgnoreCase(String namePrefix);
    
    // Anpassade queries:
    @Query("SELECT c FROM Customer c " +
           "WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR c.phone LIKE CONCAT('%', :searchTerm, '%')")
    List<Customer> searchByNameOrPhone(@Param("searchTerm") String searchTerm);
    
    @Query("SELECT DISTINCT c FROM Customer c " +
           "JOIN c.tasks t " +
           "WHERE t.status = 'ACTIVE'")
    List<Customer> findCustomersWithActiveTasks();
    
    @Query("SELECT COUNT(t) FROM Task t " +
           "WHERE t.customer.id = :customerId AND t.status = 'ACTIVE'")
    long countActiveTasksForCustomer(@Param("customerId") Long customerId);
    
    @Query("SELECT DISTINCT c FROM Customer c " +
           "LEFT JOIN c.tasks t " +
           "ORDER BY t.createdAt DESC")
    List<Customer> findCustomersOrderedByRecentActivity();
}
```

**Repository-metodmönster:**
- `findBy{Field}` - Hitta baserat på fält
- `existsBy{Field}` - Kontrollera existens (prestanda)
- `findBy{Field}Containing` - Partiell matchning
- `findBy{Field}IgnoreCase` - Case-insensitive
- `@Query` för komplexa sökningar

---

### 2.5 MAPPER (Konvertering)
📁 `src/main/java/.../mapper/CustomerMapper.java`

```java
@Component
public class CustomerMapper {
    
    // CreateDto → Entity (för POST)
    public Customer toEntity(CreateCustomerDto dto) {
        if (dto == null) return null;
        
        Customer customer = new Customer();
        customer.setName(dto.getName());
        customer.setPhone(dto.getPhone());
        customer.setAddress(dto.getAddress());
        return customer;
    }
    
    // Entity → ResponseDto (för alla svar)
    public CustomerResponseDto toResponseDto(Customer customer) {
        if (customer == null) return null;
        
        CustomerResponseDto dto = new CustomerResponseDto();
        dto.setId(customer.getId());
        dto.setName(customer.getName());
        dto.setPhone(customer.getPhone());
        dto.setAddress(customer.getAddress());
        dto.setCreatedAt(customer.getCreatedAt());
        dto.setUpdatedAt(customer.getUpdatedAt());
        return dto;
    }
    
    // UpdateDto → Entity (för PUT)
    public void updateEntityFromDto(UpdateCustomerDto dto, Customer customer) {
        if (dto == null || customer == null) return;
        
        if (dto.getName() != null) customer.setName(dto.getName());
        if (dto.getPhone() != null) customer.setPhone(dto.getPhone());
        if (dto.getAddress() != null) customer.setAddress(dto.getAddress());
    }
}
```

---

### 2.6 SERVICE (Affärslogik)
📁 `src/main/java/.../service/CustomerService.java`

```java
@Service
@Transactional(readOnly = true)  // Default för läsoperationer
public class CustomerService {
    
    private final CustomerRepository customerRepository;
    private final TaskRepository taskRepository;
    
    public CustomerService(CustomerRepository customerRepository, TaskRepository taskRepository) {
        this.customerRepository = customerRepository;
        this.taskRepository = taskRepository;
    }
    
    // CREATE
    @Transactional  // Override för skrivoperationer
    public Customer createCustomer(Customer customer) {
        // 1. Validera affärsregler
        validateNewCustomer(customer);
        
        // 2. Kontrollera dubbletter
        if (customerRepository.existsByName(customer.getName())) {
            throw new DuplicateCustomerException("namn", customer.getName());
        }
        
        if (customer.getPhone() != null && !customer.getPhone().trim().isEmpty()) {
            if (customerRepository.existsByPhone(customer.getPhone())) {
                throw new DuplicateCustomerException("telefonnummer", customer.getPhone());
            }
        }
        
        // 3. Spara och returnera
        Customer savedCustomer = customerRepository.save(customer);
        logCustomerActivity("CREATED", savedCustomer.getId(), "Ny kund skapad: " + savedCustomer.getName());
        return savedCustomer;
    }
    
    // READ
    public Customer getCustomerById(Long id) {
        return customerRepository.findById(id)
            .orElseThrow(() -> new CustomerNotFoundException(id));
    }
    
    public List<Customer> getAllCustomers() {
        return customerRepository.findAll()
                .stream()
                .sorted((c1, c2) -> c1.getName().compareToIgnoreCase(c2.getName()))
                .toList();
    }
    
    // UPDATE
    @Transactional
    public Customer updateCustomer(Long id, Customer updatedCustomer) {
        Customer existingCustomer = getCustomerById(id);
        
        // Kontrollera namn-konflikter (om namnet ändras)
        if (!existingCustomer.getName().equals(updatedCustomer.getName())) {
            if (customerRepository.existsByName(updatedCustomer.getName())) {
                throw new DuplicateCustomerException("namn", updatedCustomer.getName());
            }
        }
        
        // Kontrollera telefon-konflikter (om telefonen ändras)
        if (updatedCustomer.getPhone() != null &&
                !updatedCustomer.getPhone().equals(existingCustomer.getPhone())) {
            if (customerRepository.existsByPhone(updatedCustomer.getPhone())) {
                throw new DuplicateCustomerException("telefonnummer", updatedCustomer.getPhone());
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
    
    // DELETE
    @Transactional
    public void deleteCustomer(Long id) {
        Customer customer = getCustomerById(id);
        
        // Kontrollera att kunden inte har aktiva uppdrag
        long activeTaskCount = taskRepository.countActiveTasksForCustomer(id);
        if (activeTaskCount > 0) {
            throw new CustomerDeletionException(
                customer.getName(), (int) activeTaskCount, "aktiva uppdrag"
            );
        }
        
        // Kontrollera att kunden inte har några uppdrag alls (historiska data)
        List<Task> allTasks = taskRepository.findByCustomerId(id);
        if (!allTasks.isEmpty()) {
            throw new CustomerDeletionException(
                "Kunden '" + customer.getName() + "' kan inte tas bort eftersom den har " +
                allTasks.size() + " historiska uppdrag. Historisk data måste bevaras."
            );
        }
        
        logCustomerActivity("DELETED", customer.getId(), "Kund borttagen: " + customer.getName());
        customerRepository.deleteById(id);
    }
    
    // SÖKFUNKTIONER
    public List<Customer> searchCustomers(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return getAllCustomers();
        }
        return customerRepository.searchByNameOrPhone(searchTerm.trim());
    }
    
    public List<Customer> getActiveCustomers() {
        return customerRepository.findCustomersWithActiveTasks();
    }
    
    // PRIVATA HJÄLPMETODER
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
    
    private boolean isValidPhoneNumber(String phone) {
        if (phone == null) return true;
        String cleanPhone = phone.trim().replaceAll("[\\s\\-()]", "");
        return cleanPhone.matches("^(\\+46|0)[1-9]\\d{7,9}$");
    }
    
    private void logCustomerActivity(String action, Long customerId, String details) {
        System.out.println(String.format("[CUSTOMER_AUDIT] %s - Customer ID: %d - %s",
                action, customerId, details));
    }
}
```

**Service-lagrets ansvar:**
- Affärsregelvalidering
- Transaktionshantering
- Dublettkontroll
- Säkerhetsregler
- Loggning/audit trail

---

### 2.7 CONTROLLER (HTTP-hantering)
📁 `src/main/java/.../controller/CustomerController.java`

```java
@RestController
@RequestMapping("/api/customers")
public class CustomerController {
    
    private final CustomerService customerService;
    private final CustomerMapper customerMapper;
    
    public CustomerController(CustomerService customerService, CustomerMapper customerMapper) {
        this.customerService = customerService;
        this.customerMapper = customerMapper;
    }
    
    // CREATE
    @PostMapping
    public ResponseEntity<CustomerResponseDto> createCustomer(
            @Valid @RequestBody CreateCustomerDto createDto) {
        
        // 1. Konvertera DTO → Entity
        Customer customer = customerMapper.toEntity(createDto);
        
        // 2. Delegera till Service
        Customer savedCustomer = customerService.createCustomer(customer);
        
        // 3. Konvertera Entity → ResponseDto
        CustomerResponseDto responseDto = customerMapper.toResponseDto(savedCustomer);
        
        // 4. Returnera med korrekt HTTP-status
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }
    
    // READ ONE
    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponseDto> getCustomerById(@PathVariable Long id) {
        Customer customer = customerService.getCustomerById(id);
        CustomerResponseDto responseDto = customerMapper.toResponseDto(customer);
        return ResponseEntity.ok(responseDto);
    }
    
    // READ ALL
    @GetMapping
    public ResponseEntity<List<CustomerResponseDto>> getAllCustomers() {
        List<Customer> customers = customerService.getAllCustomers();
        List<CustomerResponseDto> responseDtos = customers.stream()
            .map(customerMapper::toResponseDto)
            .toList();
        return ResponseEntity.ok(responseDtos);
    }
    
    // UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<CustomerResponseDto> updateCustomer(
            @PathVariable Long id, 
            @Valid @RequestBody UpdateCustomerDto updateDto) {
        // Implementeras när UpdateCustomerDto är klar
        return ResponseEntity.ok().build();
    }
    
    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) {
        customerService.deleteCustomer(id);
        return ResponseEntity.noContent().build();
    }
    
    // SEARCH
    @GetMapping("/search")
    public ResponseEntity<List<CustomerResponseDto>> searchCustomers(
            @RequestParam(required = false) String term) {
        List<Customer> customers = customerService.searchCustomers(term);
        List<CustomerResponseDto> responseDtos = customers.stream()
            .map(customerMapper::toResponseDto)
            .toList();
        return ResponseEntity.ok(responseDtos);
    }
}
```

**Controller-mönster:**
- En metod per HTTP-verb (POST, GET, PUT, DELETE)
- `@Valid` för automatisk validering
- Använd `ResponseEntity` för full kontroll över HTTP-status
- Delegera ALL affärslogik till Service
- Använd Mapper för ALL konvertering

---

## 🔄 Dataflöde: Följ Pipelinen

### HTTP Request → Response Flow:

1. **HTTP POST /api/customers** (med JSON body)
2. **Spring deserialiser JSON → CreateCustomerDto**
3. **Spring validerar @Valid annotationer**
4. **CustomerController.createCustomer()** tar emot
5. **CustomerMapper.toEntity()** konverterar DTO → Entity
6. **CustomerService.createCustomer()** utför affärslogik
7. **CustomerRepository.save()** sparar till databas
8. **CustomerMapper.toResponseDto()** konverterar Entity → ResponseDto
9. **ResponseEntity** med HTTP 201 Created
10. **Spring serialiser ResponseDto → JSON**

### Vid fel:
- **Exception kastas** (vilken nivå som helst)
- **GlobalExceptionHandler** fångar automatiskt
- **HTTP-felrespons** med rätt statuskod och meddelande

---

## 📋 Checklista för Varje Endpoint

### ✅ Skapa ny entitet (POST):
- [ ] CreateDto med valideringsannotationer
- [ ] ResponseDto med alla fält
- [ ] Mapper-metoder för konvertering
- [ ] Service-metod med affärslogikvalidering
- [ ] Repository-metoder för dublettkontroll
- [ ] Controller-metod som returnerar HTTP 201
- [ ] Exception-hantering för dubbletter

### ✅ Hämta entitet (GET):
- [ ] Service-metod som kastar NotFoundException om inte hittas
- [ ] Controller-metod som returnerar HTTP 200
- [ ] Repository findById() eller anpassade sökmetoder

### ✅ Uppdatera entitet (PUT):
- [ ] UpdateDto (ofta samma som CreateDto)
- [ ] Service-metod som kontrollerar existens och konflikter
- [ ] Mapper-metod för att uppdatera befintlig entitet
- [ ] Controller-metod som returnerar HTTP 200

### ✅ Ta bort entitet (DELETE):
- [ ] Service-metod som kontrollerar affärsregler för borttagning
- [ ] DeletionException för när borttagning inte tillåts
- [ ] Controller-metod som returnerar HTTP 204 No Content

---

## 🎓 Designprinciper att Komma Ihåg

### Separation of Concerns:
- **Controller**: Endast HTTP-hantering
- **Service**: Endast affärslogik
- **Repository**: Endast databasåtkomst
- **Mapper**: Endast konvertering
- **DTO**: Endast dataöverföring

### Felhantering:
- Specifika Exceptions för olika fel
- GlobalExceptionHandler för centraliserad hantering
- Meningsfulla HTTP-statuskoder
- Användarvänliga felmeddelanden

### Prestanda:
- `@Transactional(readOnly = true)` som default
- `exists()` metoder istället för `find()` för kontroller
- Lazy loading för relationer
- Optimerade queries i Repository

### Säkerhet:
- Validering på alla ingångar (`@Valid`)
- Aldrig exponera känslig information i fel
- Sanitering av användarinput

---

## 🚀 Framtida Utbyggnad

### När API:et växer, lägg till:
- Paginering för stora listor
- Sortering och filtrering
- Cachning för prestanda
- API-versioning
- Säkerhetsautentisering
- API-dokumentation (Swagger)
- Testning (Unit + Integration)

---

## 💡 Viktiga Påminnelser

**MINNS:** Bygg alltid från Database → Service → Controller

**FOKUS:** En endpoint i taget, komplett pipeline för varje

**KVALITET:** Robust felhantering är lika viktigt som happy path

**KONSISTENS:** Följ samma mönster för alla entiteter

**DOKUMENTATION:** Kommentera varför, inte bara vad