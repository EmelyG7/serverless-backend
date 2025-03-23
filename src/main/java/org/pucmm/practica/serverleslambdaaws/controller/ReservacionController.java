package org.pucmm.practica.serverleslambdaaws.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.pucmm.practica.serverleslambdaaws.model.Reservacion;
import org.pucmm.practica.serverleslambdaaws.model.ReservacionRequest;
import org.pucmm.practica.serverleslambdaaws.service.ReservacionService;
import org.pucmm.practica.serverleslambdaaws.util.JsonAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ReservacionController {
    private static final Logger logger = LoggerFactory.getLogger(ReservacionController.class);
    private final ReservacionService reservationService = new ReservacionService();
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new JsonAdapter.LocalDateTimeAdapter())
            .create();

    @PostMapping("/reservations")
    public ResponseEntity<String> createReservation(@RequestBody ReservacionRequest request) {
        logger.info("Received request: {}", gson.toJson(request));

        // Validate request
        if (request.getEmail() == null || request.getName() == null ||
                request.getStudentId() == null || request.getLaboratory() == null ||
                request.getReservationTime() == null) {
            return ResponseEntity.badRequest()
                    .body(gson.toJson(Map.of("message", "All fields are required")));
        }

        try {
            // Parse and validate the reservation time
            LocalDateTime reservationTime = LocalDateTime.parse(request.getReservationTime());

            // Create reservation
            Reservacion reservation = new Reservacion(
                    request.getEmail(),
                    request.getName(),
                    request.getStudentId(),
                    request.getLaboratory(),
                    reservationTime
            );

            Reservacion createdReservation = reservationService.createReservation(reservation);

            return ResponseEntity.status(201)
                    .body(gson.toJson(createdReservation));

        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.error("Validation error", e);
            return ResponseEntity.badRequest()
                    .body(gson.toJson(Map.of("message", e.getMessage())));
        } catch (Exception e) {
            logger.error("Error creating reservation", e);
            return ResponseEntity.status(500)
                    .body(gson.toJson(Map.of("message", "Internal server error: " + e.getMessage())));
        }
    }

    @GetMapping("/reservations/active")
    public ResponseEntity<String> getActiveReservations() {
        try {
            List<Reservacion> activeReservations = reservationService.getActiveReservations();
            return ResponseEntity.ok(gson.toJson(activeReservations));
        } catch (Exception e) {
            logger.error("Error retrieving active reservations", e);
            return ResponseEntity.status(500)
                    .body(gson.toJson(Map.of("message", "Internal server error: " + e.getMessage())));
        }
    }

    @GetMapping("/reservations/past")
    public ResponseEntity<String> getPastReservations(
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate) {
        try {
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);
            List<Reservacion> pastReservations = reservationService.getPastReservationsByDateRange(start, end);
            return ResponseEntity.ok(gson.toJson(pastReservations));
        } catch (Exception e) {
            logger.error("Error retrieving past reservations", e);
            return ResponseEntity.status(500)
                    .body(gson.toJson(Map.of("message", "Internal server error: " + e.getMessage())));
        }
    }
}