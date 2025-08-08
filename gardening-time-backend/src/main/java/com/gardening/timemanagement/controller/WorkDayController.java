package com.gardening.timemanagement.controller;

import com.gardening.timemanagement.dto.request.CreateWorkDayDto;
import com.gardening.timemanagement.dto.request.UpdateWorkDayDto;
import com.gardening.timemanagement.dto.response.WorkDayResponseDto;
import com.gardening.timemanagement.service.WorkDayService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller för WorkDay API - Tydlig och funktionell design
 *
 * Denna controller representerar slutstenen i vår arkitektoniska resa
 * och binder samman alla komponenter vi byggt för att leverera
 * komplett REST API-funktionalitet för vårt trädgårdssystem.
 *
 * Designprinciper för studentlärande:
 * - Tydliga REST endpoints med standardiserade HTTP-metoder
 * - Konsistent error handling med begripliga felmeddelanden
 * - Robust input validation med användarvänlig feedback
 * - Enkel men effektiv request/response hantering
 *
 * Som student är detta ett utmärkt exempel på hur man strukturerar
 * REST controllers som balanserar funktionalitet med läsbarhet.
 */
@RestController
@RequestMapping("/api/workdays")
@CrossOrigin(origins = "*") // För utveckling - konfigurera säkert för produktion
public class WorkDayController {

    private final WorkDayService workDayService;

    /**
     * Constructor injection för clean dependency management
     * Spring hanterar automatiskt injektion av WorkDayService
     */
    @Autowired
    public WorkDayController(WorkDayService workDayService) {
        this.workDayService = workDayService;
    }

    // =================================================================
    // GRUNDLÄGGANDE CRUD OPERATIONER - Core API Functionality
    // =================================================================

    /**
     * Skapar en ny arbetsdag med full business logic validation
     *
     * POST /api/workdays
     * Content-Type: application/json
     *
     * Denna endpoint hanterar skapandet av nya arbetsdagar inklusive
     * all nödvändig validering och koordination med relaterade entities.
     *
     * @param createDto Validerad DTO med all information för arbetsdagen
     * @return 201 Created med WorkDayResponseDto eller error response
     */
    @PostMapping
    public ResponseEntity<?> createWorkDay(@Valid @RequestBody CreateWorkDayDto createDto) {
        try {
            // Service layer hanterar all komplex business logic
            WorkDayResponseDto createdWorkDay = workDayService.createWorkDay(createDto);

            // Returnera 201 Created med den skapade arbetsdagen
            return ResponseEntity.status(HttpStatus.CREATED).body(createdWorkDay);

        } catch (RuntimeException e) {
            // Hantera alla runtime exceptions med användarvänliga meddelanden
            return buildErrorResponse(HttpStatus.BAD_REQUEST,
                    "Kunde inte skapa arbetsdag", e.getMessage());

        } catch (Exception e) {
            // Hantera oväntade fel med generisk felhantering
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Systemfel", "Ett oväntat fel uppstod vid skapande av arbetsdag");
        }
    }

    /**
     * Hämtar en specifik arbetsdag med all relaterad information
     *
     * GET /api/workdays/{id}
     *
     * @param id WorkDay ID att hämta
     * @return 200 OK med WorkDayResponseDto eller 404 Not Found
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getWorkDay(@PathVariable Long id) {
        try {
            WorkDayResponseDto workDay = workDayService.getWorkDayById(id);
            return ResponseEntity.ok(workDay);

        } catch (RuntimeException e) {
            // Om service kastar RuntimeException, tolka som "not found"
            return buildErrorResponse(HttpStatus.NOT_FOUND,
                    "Arbetsdag inte hittad", "Arbetsdag med ID " + id + " finns inte");

        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Systemfel", "Ett fel uppstod vid hämtning av arbetsdag");
        }
    }

    /**
     * Uppdaterar en befintlig arbetsdag med delta-semantik
     *
     * PUT /api/workdays/{id}
     * Content-Type: application/json
     *
     * Endast fält som inte är null i UpdateWorkDayDto kommer att uppdateras.
     * Detta ger flexibel partial update funktionalitet.
     *
     * @param id WorkDay ID att uppdatera
     * @param updateDto DTO med fält att uppdatera
     * @return 200 OK med uppdaterad WorkDayResponseDto
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateWorkDay(@PathVariable Long id,
                                           @Valid @RequestBody UpdateWorkDayDto updateDto) {
        try {
            WorkDayResponseDto updatedWorkDay = workDayService.updateWorkDay(id, updateDto);
            return ResponseEntity.ok(updatedWorkDay);

        } catch (RuntimeException e) {
            // Olika typer av runtime exceptions kan indikera olika problem
            String message = e.getMessage();
            if (message.contains("finns inte") || message.contains("not found")) {
                return buildErrorResponse(HttpStatus.NOT_FOUND,
                        "Arbetsdag inte hittad", message);
            } else {
                return buildErrorResponse(HttpStatus.BAD_REQUEST,
                        "Kunde inte uppdatera arbetsdag", message);
            }

        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Systemfel", "Ett fel uppstod vid uppdatering av arbetsdag");
        }
    }

    /**
     * Tar bort en arbetsdag med business rule kontroll
     *
     * DELETE /api/workdays/{id}
     *
     * @param id WorkDay ID att ta bort
     * @return 204 No Content vid framgång eller error response
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteWorkDay(@PathVariable Long id) {
        try {
            workDayService.deleteWorkDay(id);
            // 204 No Content indikerar framgångsrik deletion utan response body
            return ResponseEntity.noContent().build();

        } catch (RuntimeException e) {
            String message = e.getMessage();
            if (message.contains("finns inte") || message.contains("not found")) {
                return buildErrorResponse(HttpStatus.NOT_FOUND,
                        "Arbetsdag inte hittad", message);
            } else {
                return buildErrorResponse(HttpStatus.CONFLICT,
                        "Kan inte ta bort arbetsdag", message);
            }

        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Systemfel", "Ett fel uppstod vid borttagning av arbetsdag");
        }
    }

    // =================================================================
    // QUERY OPERATIONER - Avancerad datahämtning
    // =================================================================

    /**
     * Hämtar alla arbetsdagar med optional filtering
     *
     * GET /api/workdays
     *
     * Query parameters:
     * - startDate: Filtrera från datum (YYYY-MM-DD)
     * - endDate: Filtrera till datum (YYYY-MM-DD)
     * - taskId: Filtrera på specifikt uppdrag
     * - employeeId: Filtrera på specifik medarbetare
     *
     * @param startDate Optional startdatum för filtrering
     * @param endDate Optional slutdatum för filtrering
     * @param taskId Optional task ID för filtrering
     * @param employeeId Optional employee ID för filtrering
     * @return Lista med WorkDayResponseDto objekt
     */
    @GetMapping
    public ResponseEntity<?> getWorkDays(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,

            @RequestParam(required = false) Long taskId,
            @RequestParam(required = false) Long employeeId) {

        try {
            List<WorkDayResponseDto> workDays;

            // Smart routing baserat på vilka query parameters som finns
            if (startDate != null && endDate != null) {
                workDays = workDayService.getWorkDaysInDateRange(startDate, endDate);
            } else if (taskId != null) {
                workDays = workDayService.getWorkDaysByTask(taskId);
            } else if (employeeId != null) {
                workDays = workDayService.getWorkDaysByEmployee(employeeId);
            } else {
                // Default: hämta senaste 30 dagarnas arbetsdagar
                workDays = workDayService.getRecentWorkDays(30);
            }

            // Skapa response med metadata för frontend användning
            Map<String, Object> response = new HashMap<>();
            response.put("workDays", workDays);
            response.put("count", workDays.size());

            // Lägg till query information för frontend context
            if (startDate != null && endDate != null) {
                response.put("dateRange", Map.of("start", startDate, "end", endDate));
            }
            if (taskId != null) {
                response.put("taskId", taskId);
            }
            if (employeeId != null) {
                response.put("employeeId", employeeId);
            }

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            // Hantera ogiltiga query parameters
            return buildErrorResponse(HttpStatus.BAD_REQUEST,
                    "Ogiltiga sökparametrar", e.getMessage());

        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Systemfel", "Ett fel uppstod vid hämtning av arbetsdagar");
        }
    }

    /**
     * Hämtar arbetsdagar för ett specifikt datum
     *
     * GET /api/workdays/by-date/{date}
     *
     * @param date Datum att filtrera på (YYYY-MM-DD format)
     * @return Lista med arbetsdagar för det angivna datumet
     */
    @GetMapping("/by-date/{date}")
    public ResponseEntity<?> getWorkDaysByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        try {
            List<WorkDayResponseDto> workDays = workDayService.getWorkDaysByDate(date);

            Map<String, Object> response = new HashMap<>();
            response.put("date", date);
            response.put("workDays", workDays);
            response.put("count", workDays.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Systemfel", "Ett fel uppstod vid hämtning av arbetsdagar för datum " + date);
        }
    }

    // =================================================================
    // REPORTING ENDPOINTS - Frontend Integration Support
    // =================================================================

    /**
     * Hämtar arbetstimmar för en specifik medarbetare
     *
     * GET /api/workdays/employee/{employeeId}/hours
     * GET /api/workdays/employee/{employeeId}/hours?date=2024-01-15
     *
     * Denna endpoint stöder frontend medarbetarsökning genom att returnera
     * detaljerade arbetstidsdata för en specifik medarbetare.
     *
     * @param employeeId Medarbetare att hämta arbetstimmar för
     * @param date Optional specifikt datum för filtrering
     * @return Lista med arbetstidsdata eller error response
     */
    @GetMapping("/employee/{employeeId}/hours")
    public ResponseEntity<?> getEmployeeWorkHours(
            @PathVariable Long employeeId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        try {
            List<Map<String, Object>> workHours = workDayService.calculateEmployeeWorkHours(employeeId, date);

            // Beräkna sammanfattande statistik för response
            double totalHours = workHours.stream()
                    .mapToDouble(wh -> (Double) wh.get("totalHours"))
                    .sum();
            double totalDriveTime = workHours.stream()
                    .mapToDouble(wh -> (Double) wh.get("driveTime"))
                    .sum();

            Map<String, Object> response = new HashMap<>();
            response.put("employeeId", employeeId);
            response.put("filterDate", date != null ? date.toString() : "Alla datum");
            response.put("workHours", workHours);
            response.put("summary", Map.of(
                    "totalHours", totalHours,
                    "totalDriveTime", totalDriveTime,
                    "workDayCount", workHours.size()
            ));

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST,
                    "Kunde inte hämta arbetstimmar", e.getMessage());

        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Systemfel", "Ett fel uppstod vid hämtning av arbetstimmar för medarbetare");
        }
    }

    /**
     * Hämtar arbetstimmar för en specifik kunds projekt
     *
     * GET /api/workdays/customer/{customerId}/hours
     * GET /api/workdays/customer/{customerId}/hours?date=2024-01-15
     *
     * Denna endpoint stöder frontend kundsökning genom att aggregera
     * all arbetstid som lagts på en kunds uppdrag.
     *
     * @param customerId Kund att hämta arbetstimmar för
     * @param date Optional specifikt datum för filtrering
     * @return Aggregerad arbetstidsdata för kundens projekt
     */
    @GetMapping("/customer/{customerId}/hours")
    public ResponseEntity<?> getCustomerWorkHours(
            @PathVariable Long customerId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        try {
            List<Map<String, Object>> customerWork = workDayService.calculateCustomerWorkHours(customerId, date);

            // Beräkna totaler för kunden
            double totalHours = customerWork.stream()
                    .mapToDouble(cw -> (Double) cw.get("totalHours"))
                    .sum();
            int totalEmployees = customerWork.stream()
                    .mapToInt(cw -> (Integer) cw.get("employeeCount"))
                    .sum();

            Map<String, Object> response = new HashMap<>();
            response.put("customerId", customerId);
            response.put("filterDate", date != null ? date.toString() : "Alla datum");
            response.put("customerWork", customerWork);
            response.put("summary", Map.of(
                    "totalHours", totalHours,
                    "totalEmployeeInvolvements", totalEmployees,
                    "workDayCount", customerWork.size()
            ));

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST,
                    "Kunde inte hämta kundarbetstimmar", e.getMessage());

        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Systemfel", "Ett fel uppstod vid hämtning av arbetstimmar för kund");
        }
    }

    /**
     * Genererar månadsrapport för alla medarbetare
     *
     * GET /api/workdays/reports/monthly?month=2024-01
     *
     * Denna endpoint stöder frontend månadsrapporter genom att returnera
     * sammanfattad arbetstidsdata för alla medarbetare under en månad.
     *
     * @param month Månad att generera rapport för (YYYY-MM format)
     * @return Månadsrapport med alla medarbetares arbetstid
     */
    @GetMapping("/reports/monthly")
    public ResponseEntity<?> getMonthlyReport(@RequestParam String month) {
        try {
            // Grundläggande validering av månadsformat
            if (!month.matches("\\d{4}-\\d{2}")) {
                return buildErrorResponse(HttpStatus.BAD_REQUEST,
                        "Ogiltigt månadsformat", "Använd format YYYY-MM (exempel: 2024-01)");
            }

            Map<String, Object> monthlyReport = workDayService.generateMonthlyReport(month);
            return ResponseEntity.ok(monthlyReport);

        } catch (RuntimeException e) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST,
                    "Kunde inte generera månadsrapport", e.getMessage());

        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Systemfel", "Ett fel uppstod vid generering av månadsrapport");
        }
    }

    /**
     * Genererar statistik för arbetsdagar inom en period
     *
     * GET /api/workdays/statistics
     *
     * Query parameters:
     * - startDate: Rapportperiod start (obligatorisk)
     * - endDate: Rapportperiod slut (obligatorisk)
     *
     * @param startDate Rapportperiod start
     * @param endDate Rapportperiod slut
     * @return Statistik rapport med multiple dimensioner
     */
    @GetMapping("/statistics")
    public ResponseEntity<?> getWorkDayStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        try {
            // Grundläggande validering av datumintervall
            if (startDate.isAfter(endDate)) {
                return buildErrorResponse(HttpStatus.BAD_REQUEST,
                        "Ogiltigt datumintervall", "Startdatum kan inte vara efter slutdatum");
            }

            Map<String, Object> statistics = workDayService.generateWorkDayStatistics(startDate, endDate);
            return ResponseEntity.ok(statistics);

        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Systemfel", "Ett fel uppstod vid generering av statistik");
        }
    }

    // =================================================================
    // HELPER METHODS - Återanvändbara hjälpmetoder
    // =================================================================

    /**
     * Bygger konsistent error response struktur
     *
     * Denna metod säkerställer att alla error responses följer samma format,
     * vilket gör det lättare för frontend att hantera fel på ett enhetligt sätt.
     *
     * @param status HTTP status code
     * @param title Kort beskrivande titel för felet
     * @param message Detaljerat felmeddelande
     * @return ResponseEntity med strukturerat error response
     */
    private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status,
                                                                   String title, String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", true);
        errorResponse.put("status", status.value());
        errorResponse.put("title", title);
        errorResponse.put("message", message);
        errorResponse.put("timestamp", java.time.Instant.now().toString());

        // Lägg till hjälpsamma förslag baserat på feltyp
        switch (status) {
            case BAD_REQUEST:
                errorResponse.put("suggestion", "Kontrollera att all indata är korrekt och försök igen");
                break;
            case NOT_FOUND:
                errorResponse.put("suggestion", "Verifiera att den begärda resursen existerar");
                break;
            case CONFLICT:
                errorResponse.put("suggestion", "Lös konflikten genom att uppdatera befintlig data");
                break;
            case INTERNAL_SERVER_ERROR:
                errorResponse.put("suggestion", "Kontakta support om problemet kvarstår");
                break;
        }

        return ResponseEntity.status(status).body(errorResponse);
    }
}