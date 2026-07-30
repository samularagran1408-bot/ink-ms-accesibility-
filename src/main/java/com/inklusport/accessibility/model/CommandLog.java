package com.inklusport.accessibility.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "command_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommandLog {

    @Id
    private String id;

    @Indexed
    private String userId;

    /** voice | sign */
    private String modality;

    /** Texto reconocido o id de gesto. */
    private String input;

    private String action;
    private String route;
    private Double confidence;
    private Boolean executed;

    @CreatedDate
    private LocalDateTime createdAt;
}
