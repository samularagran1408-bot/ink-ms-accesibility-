package com.inklusport.accessibility.controller;

import com.inklusport.accessibility.dto.CommandDefinition;
import com.inklusport.accessibility.dto.CommandLogRequest;
import com.inklusport.accessibility.dto.InterpretRequest;
import com.inklusport.accessibility.dto.InterpretResponse;
import com.inklusport.accessibility.model.CommandLog;
import com.inklusport.accessibility.service.AssistiveCommandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/voice")
@RequiredArgsConstructor
public class VoiceController {

    private final AssistiveCommandService assistiveCommandService;

    @GetMapping("/commands")
    public ResponseEntity<Map<String, Object>> listCommands() {
        List<CommandDefinition> commands = assistiveCommandService.voiceCommands();
        return ResponseEntity.ok(Map.of(
                "count", commands.size(),
                "commands", commands
        ));
    }

    @PostMapping("/interpret")
    public ResponseEntity<InterpretResponse> interpret(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody InterpretRequest request) {
        return ResponseEntity.ok(assistiveCommandService.interpretVoice(userId, request));
    }

    @PostMapping("/log")
    public ResponseEntity<Map<String, String>> log(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody CommandLogRequest request) {
        request.setModality("voice");
        assistiveCommandService.logCommand(userId, request);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @GetMapping("/history")
    public ResponseEntity<List<CommandLog>> history(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(assistiveCommandService.recent(userId));
    }
}
