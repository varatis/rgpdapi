package com.minds.rgpd.persistence.repositories;

import com.minds.rgpd.persistence.entities.Client;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientRepository extends JpaRepository<Client, Integer> {

    Client findByNom(String nom);
}
