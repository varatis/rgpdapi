package com.minds.rgpd.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@Entity
@Table(name = "PROFIL")
@NoArgsConstructor
@AllArgsConstructor
public class Profil {

    @Id
    Integer id;

    @Column(name = "code")
    String code;

    @Column(name = "description")
    String description;
}
