package com.vitsync.iam.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class NifTest {

    // Ejemplos sintéticos calculados con el algoritmo oficial.
    @ParameterizedTest
    @ValueSource(strings = {"12345678Z", "00000000T", "99999999R", " 12345678z ", "12345678-Z",
            "X1234567L", "Y1234567X", "Z1234567R"})
    void accepts_dni_and_nie_with_correct_control_letter(String nif) {
        assertThat(Nif.isValid(nif)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345678A", "X1234567A", "1234567Z", "123456789Z", "A1234567L",
            "12345678", "ZZZZZZZZZ", ""})
    void rejects_wrong_letter_or_format(String nif) {
        assertThat(Nif.isValid(nif)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345678z", " 12345678Z", "12345678 Z", "12345678-z"})
    void normalizes_to_the_canonical_form(String raw) {
        assertThat(Nif.normalize(raw)).isEqualTo("12345678Z");
    }
}
