package org.pucmm.practica.serverleslambdaaws.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.pucmm.practica.serverleslambdaaws.model.Reservacion;
import org.pucmm.practica.serverleslambdaaws.model.ReservacionRequest;
import org.pucmm.practica.serverleslambdaaws.service.ReservacionService;
import org.pucmm.practica.serverleslambdaaws.util.JsonAdapter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class LambdaHandlersTest {

    @Mock
    private ReservacionService mockReservacionService;

    @Mock
    private Context mockContext;

    @InjectMocks
    private CreateReservacionHandler createHandler;

    @InjectMocks
    private GetActiveReservacionesHandler activeHandler;

    @InjectMocks
    private GetPastReservacionesHandler pastHandler;

    private Gson gson;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new JsonAdapter.LocalDateTimeAdapter())
                .create();
    }

    @Test
    public void testCreateReservacionHandler_Success() throws Exception {
        // Preparar request
        ReservacionRequest request = new ReservacionRequest(
                "test@example.com",
                "Test User",
                "20200123",
                "Lab A",
                LocalDateTime.now().plusDays(1).withHour(14).withMinute(0).toString()
        );

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setBody(gson.toJson(request));

        // Mock servicio
        Reservacion mockReservacion = new Reservacion(
                request.getEmail(),
                request.getName(),
                request.getStudentId(),
                request.getLaboratory(),
                LocalDateTime.parse(request.getReservationTime())
        );
        when(mockReservacionService.createReservation(any(Reservacion.class))).thenReturn(mockReservacion);

        // Ejecutar
        APIGatewayProxyResponseEvent response = createHandler.handleRequest(event, mockContext);

        // Verificar
        assertNotNull(response);
        assertEquals(201, response.getStatusCode());
        Reservacion result = gson.fromJson(response.getBody(), Reservacion.class);
        assertEquals(request.getEmail(), result.getEmail());
    }

    @Test
    public void testCreateReservacionHandler_InvalidRequest() {
        // Preparar request con campos faltantes
        ReservacionRequest request = new ReservacionRequest(
                "test@example.com",
                null, // Nombre faltante
                "20200123",
                "Lab A",
                LocalDateTime.now().plusDays(1).withHour(14).toString()
        );

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setBody(gson.toJson(request));

        // Ejecutar
        APIGatewayProxyResponseEvent response = createHandler.handleRequest(event, mockContext);

        // Verificar
        assertNotNull(response);
        assertEquals(400, response.getStatusCode());
    }

    @Test
    public void testGetActiveReservacionesHandler_Success() throws Exception {
        // Preparar mock
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();

        // Mock servicio
        Reservacion res1 = new Reservacion();
        res1.setId("uuid1");
        res1.setName("User 1");

        Reservacion res2 = new Reservacion();
        res2.setId("uuid2");
        res2.setName("User 2");

        when(mockReservacionService.getActiveReservations()).thenReturn(Arrays.asList(res1, res2));

        // Ejecutar
        APIGatewayProxyResponseEvent response = activeHandler.handleRequest(event, mockContext);

        // Verificar
        assertNotNull(response);
        assertEquals(200, response.getStatusCode());
        // Verificar que la respuesta contiene un array con 2 elementos
        assertTrue(response.getBody().contains("User 1"));
        assertTrue(response.getBody().contains("User 2"));
    }

    @Test
    public void testGetPastReservacionesHandler_Success() throws Exception {
        // Preparar mock
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("startDate", LocalDate.now().minusDays(10).toString());
        queryParams.put("endDate", LocalDate.now().toString());
        event.setQueryStringParameters(queryParams);

        // Mock servicio
        Reservacion pastRes = new Reservacion();
        pastRes.setId("uuid-past");
        pastRes.setName("Past User");

        when(mockReservacionService.getPastReservationsByDateRange(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Arrays.asList(pastRes));

        // Ejecutar
        APIGatewayProxyResponseEvent response = pastHandler.handleRequest(event, mockContext);

        // Verificar
        assertNotNull(response);
        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("Past User"));
    }

    @Test
    public void testGetPastReservacionesHandler_MissingParams() {
        // Preparar mock sin parámetros
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        // No establecemos queryStringParameters

        // Ejecutar
        APIGatewayProxyResponseEvent response = pastHandler.handleRequest(event, mockContext);

        // Verificar
        assertNotNull(response);
        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody().contains("startDate and endDate"));
    }

    private void assertTrue(boolean condition) {
        org.junit.jupiter.api.Assertions.assertTrue(condition);
    }
}