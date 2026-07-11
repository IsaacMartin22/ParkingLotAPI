package apiservice.controller;

import apiservice.dbentity.Section;
import apiservice.mapper.SectionMapper;
import apiservice.service.SectionService;
import parkinglot.common.response.SectionResponse;
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
}

