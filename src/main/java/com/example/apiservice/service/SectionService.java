package com.example.apiservice.service;

import com.example.apiservice.dbentity.Section;
import com.example.apiservice.repository.SectionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SectionService {

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

    public Section saveSection(Section section) {
        return sectionRepository.save(section);
    }

    public void deleteSection(Long id) {
        sectionRepository.deleteById(id);
    }

    public Section updateSection(Long id, Section sectionDetails) {
        Optional<Section> optionalSection = sectionRepository.findById(id);
        if (optionalSection.isPresent()) {
            Section section = optionalSection.get();
            if (sectionDetails.getName() != null) {
                section.setName(sectionDetails.getName());
            }
            var floor = sectionDetails.getFloor();
            if (floor != null) {
                section.setFloor(floor);
            }
            return sectionRepository.save(section);
        }
        return null;
    }
}

