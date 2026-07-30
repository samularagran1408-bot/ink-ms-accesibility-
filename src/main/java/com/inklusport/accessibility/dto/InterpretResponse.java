package com.inklusport.accessibility.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InterpretResponse {
    private boolean matched;
    private String action;
    private String label;
    private String route;
    private Double confidence;
    private String feedback;
    private String modality;
}
