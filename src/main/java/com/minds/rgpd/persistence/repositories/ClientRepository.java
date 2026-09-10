package com.minds.rgpd.persistence.repositories;

import com.minds.rgpd.persistence.entities.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClientRepository extends JpaRepository<Client, UUID> {

    Optional<Client>  findByNom(String nom);

    @Modifying(clearAutomatically = true)
    @Query("delete from Client c where c.id = :id")
    int supprimerParId(@Param("id") UUID id);
}
