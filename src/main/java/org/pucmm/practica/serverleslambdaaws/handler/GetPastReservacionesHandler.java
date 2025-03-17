package org.pucmm.practica.serverleslambdaaws.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.pucmm.practica.serverleslambdaaws.model.Reservacion;
import org.pucmm.practica.serverleslambdaaws.service.ReservacionService;
import org.pucmm.practica.serverleslambdaaws.util.JsonAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GetPastReservacionesHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final Logger logger = LoggerFactory.getLogger(GetPastReservacionesHandler.class);
    private final ReservacionService reservationService = new ReservacionService();
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new JsonAdapter.LocalDateTimeAdapter())
            .create();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        logger.info("Processing past reservations request");

        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setHeaders(getCorsHeaders());

        try {
            // Get query parameters
            Map<String, String> queryParams = event.getQueryStringParameters();
            if (queryParams == null || !queryParams.containsKey("startDate") || !queryParams.containsKey("endDate")) {
                return response
                        .withStatusCode(400)
                        .withBody(gson.toJson(Map.of("message", "startDate and endDate parameters are required")));
            }

            LocalDate startDate = LocalDate.parse(queryParams.get("startDate"));
            LocalDate endDate = LocalDate.parse(queryParams.get("endDate"));

            List<Reservacion> pastReservations = reservationService.getPastReservationsByDateRange(startDate, endDate);

            return response
                    .withStatusCode(200)
                    .withBody(gson.toJson(pastReservations));

        } catch (Exception e) {
            logger.error("Error retrieving past reservations", e);
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