package com.minds.rgpd.persistence.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CurrentUser {

    private String identifiant;
    private String nom;
    private String email;
    private List<String> roles;
}
