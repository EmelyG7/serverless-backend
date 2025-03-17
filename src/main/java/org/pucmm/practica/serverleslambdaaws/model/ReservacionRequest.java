package org.pucmm.practica.serverleslambdaaws.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservacionRequest {
    private String email;
    private String name;
    private String studentId;
    private String laboratory;
    private String reservationTime; // ISO format: "2025-03-10T14:00:00"
}