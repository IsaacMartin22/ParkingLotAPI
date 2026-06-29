package com.example.apiservice.service;

import com.example.apiservice.dbentity.Section;
import com.example.apiservice.repository.SectionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
public class SectionService {

    private static final int MAX_SECTIONS_PER_FLOOR = 10;

    private final SectionRepository sectionRepository;

    public SectionService(SectionRepository sectionRepository) {
        this.sectionRepository = sectionRepository;
    }

    public List<Section> getAllSections() {
        return sectionRepository.findAll();
    }

    public Optional<Section> getSectionById(Long id) {
        return sectionRepository.findById(id);
    }

    public Section save(Section section) {
        validateSectionCapacity(section);
        return sectionRepository.save(section);
    }

    private void validateSectionCapacity(Section section) {
        if (section.getFloor() == null || section.getFloor().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Section must reference a floor ID");
        }

        Long targetFloorId = section.getFloor().getId();

        if (section.getId() != null) {
            Optional<Section> existingSection = sectionRepository.findById(section.getId());
            if (existingSection.isPresent()) {
                Long currentFloorId = existingSection.get().getFloor() == null
                        ? null
                        : existingSection.get().getFloor().getId();
                if (targetFloorId.equals(currentFloorId)) {
                    return;
                }
            }
        }

        long sectionCount = sectionRepository.countByFloor_Id(targetFloorId);
        if (sectionCount >= MAX_SECTIONS_PER_FLOOR) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Floor cannot have more than " + MAX_SECTIONS_PER_FLOOR + " sections"
            );
        }
    }
}
