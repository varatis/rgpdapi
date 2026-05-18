package com.minds.rgpd.business.services;

import com.minds.rgpd.business.dtos.ProfilDTO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface ProfilService {

    List<ProfilDTO> getProfils();
}
