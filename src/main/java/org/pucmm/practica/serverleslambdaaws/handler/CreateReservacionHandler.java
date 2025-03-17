package org.pucmm.practica.serverleslambdaaws.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.pucmm.practica.serverleslambdaaws.model.Reservacion;
import org.pucmm.practica.serverleslambdaaws.model.ReservacionRequest;
import org.pucmm.practica.serverleslambdaaws.service.ReservacionService;
import org.pucmm.practica.serverleslambdaaws.util.JsonAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class CreateReservacionHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final Logger logger = LoggerFactory.getLogger(CreateReservacionHandler.class);
    private final ReservacionService reservationService = new ReservacionService();
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new JsonAdapter.LocalDateTimeAdapter())
            .create();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        logger.info("Received request: {}", event.getBody());

        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setHeaders(getCorsHeaders());

        try {
            ReservacionRequest request = gson.fromJson(event.getBody(), ReservacionRequest.class);

            // Validate request
            if (request.getEmail() == null || request.getName() == null ||
                    request.getStudentId() == null || request.getLaboratory() == null ||
                    request.getReservationTime() == null) {

                return response
                        .withStatusCode(400)
                        .withBody(gson.toJson(Map.of("message", "All fields are required")));
            }

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

            return response
                    .withStatusCode(201)
                    .withBody(gson.toJson(createdReservation));

        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.error("Validation error", e);
            return response
                    .withStatusCode(400)
                    .withBody(gson.toJson(Map.of("message", e.getMessage())));

        } catch (Exception e) {
            logger.error("Error creating reservation", e);
            return response
                    .withStatusCode(500)
                    .withBody(gson.toJson(Map.of("message", "Internal server error")));
        }
    }

    private Map<String, String> getCorsHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Access-Control-Allow-Origin", "*");
        headers.put("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        headers.put("Access-Control-Allow-Headers", "Content-Type,X-Amz-Date,Authorization,X-Api-Key");
        headers.put("Content-Type", "application/json");
        return headers;
    }
}