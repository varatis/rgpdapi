package com.minds.rgpd.persistence.repositories;

import com.minds.rgpd.persistence.entities.Client;
import com.minds.rgpd.persistence.entities.HistorisationRegistre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistorisationRegistreRepository extends JpaRepository<HistorisationRegistre, Integer> {

    List<HistorisationRegistre> findByClientOrderByDateDesc(Client client);

    List<HistorisationRegistre> findByClient_NomOrderByDateDesc(String nom);
}
