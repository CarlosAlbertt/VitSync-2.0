package com.vitsync.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SearchPatternsTest {

    @Test
    void blank_text_matches_everything() {
        assertThat(SearchPatterns.contains(null)).isEqualTo("%");
        assertThat(SearchPatterns.containsAsSlug("  ")).isEqualTo("%");
    }

    @Test
    void text_is_trimmed_and_lowercased() {
        assertThat(SearchPatterns.contains("  Cardio ")).isEqualTo("%cardio%");
    }

    @Test
    void slug_form_drops_accents_and_joins_words_with_hyphens() {
        assertThat(SearchPatterns.containsAsSlug("Análisis  Clínicos")).isEqualTo("%analisis-clinicos%");
    }

    @Test
    void like_wildcards_are_escaped() {
        assertThat(SearchPatterns.contains("50%_a\\b")).isEqualTo("%50\\%\\_a\\\\b%");
    }
}
