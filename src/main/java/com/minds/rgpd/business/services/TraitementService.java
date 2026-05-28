package com.minds.rgpd.business.services;

import com.minds.rgpd.business.dtos.TraitementDTO;
import com.minds.rgpd.business.dtos.TraitementPartielDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TraitementService {

    Page<TraitementPartielDTO> getTraitements(Pageable pageable);

    TraitementDTO getOneTraitement(Integer id);

}
