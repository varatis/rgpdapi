package com.minds.rgpd.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Builder
@Data
@Entity
@Table(name = "PROFIL")
@NoArgsConstructor
@AllArgsConstructor
public class Profil {

    @Id
    UUID id;

    @Column(name = "code")
    String code;

    @Column(name = "description")
    String description;
}
