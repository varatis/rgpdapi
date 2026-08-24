package com.minds.rgpd.persistence.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder
@MappedSuperclass
@NoArgsConstructor
@AllArgsConstructor
public class HistorisationGenerique {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    @NotNull
    @Column(name = "date")
    private LocalDateTime date;

    @NotNull
    @Column(name = "motif")
    private String motif;
}
