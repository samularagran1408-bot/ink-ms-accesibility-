package com.inklusport.accessibility.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CommandDefinition {
    private String action;
    private String label;
    private String description;
    private String route;
    private List<String> phrases;
}
