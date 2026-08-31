package com.minds.rgpd.business.services;

import com.minds.rgpd.business.dtos.ViolationDTO;
import com.minds.rgpd.business.dtos.ViolationFilterCriteria;
import com.minds.rgpd.business.dtos.ViolationPartielDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ViolationService {

    Page<ViolationPartielDTO> getViolations(Pageable pageable, String clientNom, ViolationFilterCriteria criteria);

    ViolationDTO getOneViolation(UUID id);

    ViolationDTO createViolation(ViolationDTO violation);

    ViolationDTO updateViolation(UUID id, ViolationDTO violation);

    void deleteViolationById(UUID id);
}
