package com.inklusport.accessibility.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CommandLogRequest {

    @NotBlank
    private String modality;

    @NotBlank
    private String input;

    private String action;
    private String route;
    private Double confidence;
    private Boolean executed;
}
