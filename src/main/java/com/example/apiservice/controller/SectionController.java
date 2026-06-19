package com.example.apiservice.controller;

import com.example.apiservice.pojo.SectionResponse;
import com.example.apiservice.dbentity.Section;
import com.example.apiservice.mapper.SectionMapper;
import com.example.apiservice.service.SectionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sections")
public class SectionController {

    private final SectionService sectionService;

    public SectionController(SectionService sectionService) {
        this.sectionService = sectionService;
    }

    @GetMapping
    public List<SectionResponse> getAllSections() {
        return sectionService.getAllSections().stream()
                .map(SectionMapper::toResponse)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SectionResponse> getSectionById(@PathVariable Long id) {
        Optional<Section> section = sectionService.getSectionById(id);
        return section.map(s -> ResponseEntity.ok(SectionMapper.toResponse(s)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public SectionResponse createSection(@RequestBody Section section) {
        Section created = sectionService.saveSection(section);
        return SectionMapper.toResponse(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SectionResponse> updateSection(@PathVariable Long id, @RequestBody Section sectionDetails) {
        Section updatedSection = sectionService.updateSection(id, sectionDetails);
        if (updatedSection != null) {
            return ResponseEntity.ok(SectionMapper.toResponse(updatedSection));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSection(@PathVariable Long id) {
        sectionService.deleteSection(id);
        return ResponseEntity.noContent().build();
    }
}

