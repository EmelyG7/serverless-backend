package org.pucmm.practica.serverleslambdaaws.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Reservacion {
    private String id;
    private String email;
    private String name;
    private String studentId;
    private String laboratory;
    private LocalDateTime reservationTime;
    private boolean active;

    public Reservacion(String email, String name, String studentId, String laboratory, LocalDateTime reservationTime) {
        this.id = UUID.randomUUID().toString();
        this.email = email;
        this.name = name;
        this.studentId = studentId;
        this.laboratory = laboratory;
        this.reservationTime = reservationTime;
        this.active = true;
    }
}