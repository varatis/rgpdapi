package com.minds.rgpd.business.services.impl;

import com.minds.rgpd.business.dtos.ViolationDTO;
import com.minds.rgpd.business.dtos.ViolationFilterCriteria;
import com.minds.rgpd.business.dtos.ViolationPartielDTO;
import com.minds.rgpd.business.enums.ViolationStatut;
import com.minds.rgpd.business.exceptions.DuplicateResourceException;
import com.minds.rgpd.business.exceptions.ResourceNotFoundException;
import com.minds.rgpd.business.services.ViolationService;
import com.minds.rgpd.business.utilities.mappers.ViolationMapper;
import com.minds.rgpd.persistence.entities.Client;
import com.minds.rgpd.persistence.entities.Violation;
import com.minds.rgpd.persistence.repositories.ClientRepository;
import com.minds.rgpd.persistence.repositories.ViolationRepository;
import com.minds.rgpd.persistence.specifications.ViolationSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ViolationServiceImpl implements ViolationService {

    private final ViolationRepository violationRepository;
    private final ClientRepository clientRepository;
    private final ViolationMapper violationMapper;

    @Override
    public Page<ViolationPartielDTO> getViolations(Pageable pageable, String clientNom, ViolationFilterCriteria criteria) {
        if (clientNom == null || clientNom.isBlank()) {
            return Page.empty(pageable);
        }

        Specification<Violation> spec = ViolationSpecifications.search(clientNom, criteria);

        Page<Violation> violations = violationRepository.findAll(spec, pageable);
        return violationMapper.toPartielDTOPage(violations);
    }

    @Override
    public ViolationDTO getOneViolation(UUID id) {
        return violationMapper.mapToDTO(findViolation(id));
    }

    @Override
    @Transactional
    public ViolationDTO createViolation(ViolationDTO violationDTO) {
        Client client = resolveClient(violationDTO);

        List<Violation> existantes = violationRepository.findByAllBusinessFields(
                client,
                violationDTO.dateViolation(),
                violationDTO.natureViolation(),
                violationDTO.donneesConcernees()
        );

        if (!existantes.isEmpty()) {
            throw new DuplicateResourceException("Violation", "natureViolation", violationDTO.natureViolation());
        }

        Violation violation = violationMapper.mapToViolation(violationDTO);
        // Un identifiant transmis dans le corps ferait basculer save() sur un merge :
        // le POST écraserait alors une violation existante au lieu d'en créer une.
        violation.setIdentifiant(null);
        // Le client issu du DTO est une instance détachée : on lui substitue
        // celle chargée en base pour que la violation référence bien un client existant.
        violation.setClient(client);
        // Une violation naît à instruire, comme celles issues de l'import Excel.
        if (Objects.isNull(violation.getStatut())) {
            violation.setStatut(ViolationStatut.EN_COURS);
        }

        return violationMapper.mapToDTO(violationRepository.save(violation));
    }

    @Override
    @Transactional
    public ViolationDTO updateViolation(UUID id, ViolationDTO violationDTO) {
        Violation violation = findViolation(id);
        Client client = resolveClient(violationDTO);
        ViolationStatut statutActuel = violation.getStatut();

        violationMapper.updateViolationFromDto(violationDTO, violation);
        violation.setClient(client);
        // Le statut est obligatoire en base : une modification qui ne le transmet pas
        // laisse la violation dans l'état où elle était plutôt que de l'effacer.
        if (Objects.isNull(violation.getStatut())) {
            violation.setStatut(statutActuel != null ? statutActuel : ViolationStatut.EN_COURS);
        }

        return violationMapper.mapToDTO(violationRepository.save(violation));
    }

    @Override
    @Transactional
    public void deleteViolationById(UUID id) {
        violationRepository.delete(findViolation(id));
    }

    private Violation findViolation(UUID id) {
        return violationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Violation", "identifiant", id));
    }

    private Client resolveClient(ViolationDTO violationDTO) {
        if (Objects.isNull(violationDTO.client()) || Objects.isNull(violationDTO.client().id())) {
            throw new ResourceNotFoundException("Client", "id", null);
        }
        return clientRepository.findById(violationDTO.client().id())
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", violationDTO.client().id()));
    }
}
