package com.inklusport.accessibility.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AssistiveCommandCatalogTest {

    private final AssistiveCommandCatalog catalog = new AssistiveCommandCatalog();

    @Test
    void reconoceComandosDeFuenteYContrasteEnEspanolEIngles() {
        assertThat(catalog.matchVoice("alto contraste")).isPresent()
                .get().extracting(match -> match.command().getAction())
                .isEqualTo("a11y.high_contrast");
        assertThat(catalog.matchVoice("high contrast")).isPresent()
                .get().extracting(match -> match.command().getAction())
                .isEqualTo("a11y.high_contrast");
        assertThat(catalog.matchVoice("increase font")).isPresent()
                .get().extracting(match -> match.command().getAction())
                .isEqualTo("a11y.increase_font");
        assertThat(catalog.matchVoice("letra más pequeña")).isPresent()
                .get().extracting(match -> match.command().getAction())
                .isEqualTo("a11y.decrease_font");
    }
}
