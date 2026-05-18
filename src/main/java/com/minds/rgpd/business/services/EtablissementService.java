package com.minds.rgpd.business.services;

import com.minds.rgpd.business.dtos.EtablissementDTO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface EtablissementService {

    List<EtablissementDTO> getEtablissements();
}
