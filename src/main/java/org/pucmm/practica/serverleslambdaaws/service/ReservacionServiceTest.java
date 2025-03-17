package org.pucmm.practica.serverleslambdaaws.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.pucmm.practica.serverleslambdaaws.db.DatabaseConnection;
import org.pucmm.practica.serverleslambdaaws.model.Reservacion;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReservacionServiceTest {

    @Mock
    private Connection mockConnection;

    @Mock
    private PreparedStatement mockPreparedStatement;

    @Mock
    private ResultSet mockResultSet;

    @InjectMocks
    private ReservacionService reservacionService;

    private Reservacion validReservation;

    @BeforeEach
    public void setUp() throws Exception {
        // Configurar una reservación válida para pruebas
        LocalDateTime futureTime = LocalDateTime.now().plusDays(1)
                .withHour(14).withMinute(0).withSecond(0).withNano(0); // 2PM al día siguiente

        validReservation = new Reservacion(
                "test@example.com",
                "Test User",
                "20200123",
                "Lab A",
                futureTime
        );

        // Mock para DatabaseConnection usando PowerMockito
        try (var mockedStatic = mockStatic(DatabaseConnection.class)) {
            mockedStatic.when(DatabaseConnection::getConnection).thenReturn(mockConnection);

            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        }
    }

    @Test
    public void testCreateReservation_Success() throws SQLException {
        // Configurar el mock para contar las reservaciones (debe devolver menos de 7)
        when(mockResultSet.next()).thenReturn(true).thenReturn(false);
        when(mockResultSet.getInt(1)).thenReturn(5); // 5 reservaciones existentes (menos que el límite de 7)

        // Ejecutar
        Reservacion result = reservacionService.createReservation(validReservation);

        // Verificar
        assertNotNull(result);
        assertEquals(validReservation.getEmail(), result.getEmail());
        assertEquals(validReservation.getName(), result.getName());
        verify(mockPreparedStatement, times(1)).executeUpdate();
    }

    @Test
    public void testCreateReservation_LabAtCapacity() throws SQLException {
        // Configurar el mock para contar las reservaciones (debe devolver 7 o más)
        when(mockResultSet.next()).thenReturn(true).thenReturn(false);
        when(mockResultSet.getInt(1)).thenReturn(7); // 7 reservaciones existentes (igual al límite)

        // Ejecutar y verificar que lanza la excepción apropiada
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> reservacionService.createReservation(validReservation)
        );

        assertTrue(exception.getMessage().contains("capacity"));
    }

    @Test
    public void testCreateReservation_InvalidTime_NotOnTheHour() {
        // Crear una reservación con tiempo no en hora exacta
        LocalDateTime invalidTime = LocalDateTime.now().plusDays(1)
                .withHour(14).withMinute(30).withSecond(0); // 2:30 PM
        Reservacion invalidReservation = new Reservacion(
                "test@example.com",
                "Test User",
                "20200123",
                "Lab A",
                invalidTime
        );

        // Ejecutar y verificar que lanza la excepción apropiada
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> reservacionService.createReservation(invalidReservation)
        );

        assertTrue(exception.getMessage().contains("on the hour"));
    }

    @Test
    public void testCreateReservation_InvalidTime_OutsideHours() {
        // Crear una reservación con tiempo fuera del horario permitido
        LocalDateTime invalidTime = LocalDateTime.now().plusDays(1)
                .withHour(23).withMinute(0).withSecond(0); // 11 PM
        Reservacion invalidReservation = new Reservacion(
                "test@example.com",
                "Test User",
                "20200123",
                "Lab A",
                invalidTime
        );

        // Ejecutar y verificar que lanza la excepción apropiada
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> reservacionService.createReservation(invalidReservation)
        );

        assertTrue(exception.getMessage().contains("between 8 AM and 10 PM"));
    }

    @Test
    public void testGetActiveReservations_Success() throws SQLException {
        // Configurar el mock para devolver resultados
        when(mockResultSet.next()).thenReturn(true, true, false); // Dos resultados
        when(mockResultSet.getString("id")).thenReturn("uuid1", "uuid2");
        when(mockResultSet.getString("email")).thenReturn("user1@example.com", "user2@example.com");
        when(mockResultSet.getString("name")).thenReturn("User 1", "User 2");
        when(mockResultSet.getString("student_id")).thenReturn("20200001", "20200002");
        when(mockResultSet.getString("laboratory")).thenReturn("Lab A", "Lab B");
        when(mockResultSet.getTimestamp("reservation_time")).thenReturn(
                Timestamp.valueOf(LocalDateTime.now().plusHours(2)),
                Timestamp.valueOf(LocalDateTime.now().plusHours(3))
        );
        when(mockResultSet.getBoolean("active")).thenReturn(true, true);

        // Ejecutar
        List<Reservacion> result = reservacionService.getActiveReservations();

        // Verificar
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("User 1", result.get(0).getName());
        assertEquals("User 2", result.get(1).getName());
    }

    @Test
    public void testGetPastReservationsByDateRange_Success() throws SQLException {
        // Configurar el mock para devolver resultados
        when(mockResultSet.next()).thenReturn(true, false); // Un resultado
        when(mockResultSet.getString("id")).thenReturn("uuid1");
        when(mockResultSet.getString("email")).thenReturn("past@example.com");
        when(mockResultSet.getString("name")).thenReturn("Past User");
        when(mockResultSet.getString("student_id")).thenReturn("20190001");
        when(mockResultSet.getString("laboratory")).thenReturn("Lab C");
        when(mockResultSet.getTimestamp("reservation_time")).thenReturn(
                Timestamp.valueOf(LocalDateTime.now().minusDays(5))
        );
        when(mockResultSet.getBoolean("active")).thenReturn(false);

        // Ejecutar
        LocalDate startDate = LocalDate.now().minusDays(10);
        LocalDate endDate = LocalDate.now();
        List<Reservacion> result = reservacionService.getPastReservationsByDateRange(startDate, endDate);

        // Verificar
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Past User", result.get(0).getName());

        // Verificar que se pasaron los parámetros correctos
        verify(mockPreparedStatement).setTimestamp(eq(1), any(Timestamp.class));
        verify(mockPreparedStatement).setTimestamp(eq(2), any(Timestamp.class));
    }
}