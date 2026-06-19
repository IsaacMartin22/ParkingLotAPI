package com.example.apiservice.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SectionResponse {
    private Long id;
    private String name;
    private Long floorId;
    private List<Long> parkingSpaceIds;
}

