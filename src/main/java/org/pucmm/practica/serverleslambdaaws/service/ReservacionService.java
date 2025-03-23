package org.pucmm.practica.serverleslambdaaws.service;

import org.pucmm.practica.serverleslambdaaws.db.DatabaseConnection;
import org.pucmm.practica.serverleslambdaaws.model.Reservacion;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class ReservacionService {

    public Reservacion createReservation(Reservacion reservation) throws SQLException {
        // Validate reservation time (must be on the hour)
        validateReservationTime(reservation.getReservationTime());

        // Check if there are already 7 reservations for this time and lab
        if (countReservationsForTimeAndLab(reservation.getReservationTime(), reservation.getLaboratory()) >= 7) {
            throw new IllegalStateException("Laboratory is already at full capacity for this time slot");
        }

        String sql = "INSERT INTO reservacion (id, email, name, student_id, laboratory, reservation_time, active) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, reservation.getId());
            stmt.setString(2, reservation.getEmail());
            stmt.setString(3, reservation.getName());
            stmt.setString(4, reservation.getStudentId());
            stmt.setString(5, reservation.getLaboratory());
            stmt.setTimestamp(6, Timestamp.valueOf(reservation.getReservationTime()));
            stmt.setBoolean(7, reservation.isActive());

            stmt.executeUpdate();
            return reservation;
        }
    }

    public List<Reservacion> getActiveReservations() throws SQLException {
        String sql = "SELECT * FROM reservacion WHERE active = true AND reservation_time > CURRENT_TIMESTAMP ORDER BY reservation_time";

        List<Reservacion> reservations = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                reservations.add(mapResultSetToReservation(rs));
            }
        }

        return reservations;
    }

    public List<Reservacion> getPastReservationsByDateRange(LocalDate startDate, LocalDate endDate) throws SQLException {
        String sql = "SELECT * FROM reservacion WHERE reservation_time >= ? AND reservation_time <= ? ORDER BY reservation_time";

        List<Reservacion> reservations = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, Timestamp.valueOf(startDate.atStartOfDay()));
            stmt.setTimestamp(2, Timestamp.valueOf(endDate.atTime(LocalTime.MAX)));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapResultSetToReservation(rs));
                }
            }
        }

        return reservations;
    }

    private int countReservationsForTimeAndLab(LocalDateTime time, String laboratory) throws SQLException {
        String sql = "SELECT COUNT(*) FROM reservacion WHERE reservation_time = ? AND laboratory = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, Timestamp.valueOf(time));
            stmt.setString(2, laboratory);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }

        return 0;
    }

    private void validateReservationTime(LocalDateTime reservationTime) {
        // Must be on the hour (minutes and seconds are 0)
        if (reservationTime.getMinute() != 0 || reservationTime.getSecond() != 0) {
            throw new IllegalArgumentException("Reservation time must be on the hour");
        }

        // Must be between 8 AM and 10 PM
        int hour = reservationTime.getHour();
        if (hour < 8 || hour > 22) {
            throw new IllegalArgumentException("Reservation time must be between 8 AM and 10 PM");
        }

        // Cannot be in the past
        if (reservationTime.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Cannot make reservations for past dates");
        }
    }

    private Reservacion mapResultSetToReservation(ResultSet rs) throws SQLException {
        Reservacion reservation = new Reservacion();
        reservation.setId(rs.getString("id"));
        reservation.setEmail(rs.getString("email"));
        reservation.setName(rs.getString("name"));
        reservation.setStudentId(rs.getString("student_id"));
        reservation.setLaboratory(rs.getString("laboratory"));
        reservation.setReservationTime(rs.getTimestamp("reservation_time").toLocalDateTime());
        reservation.setActive(rs.getBoolean("active"));
        return reservation;
    }
}