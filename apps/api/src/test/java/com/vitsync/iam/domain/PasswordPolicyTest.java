package com.vitsync.iam.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PasswordPolicyTest {

    @Test
    void requires_at_least_12_characters() {
        assertThat(PasswordPolicy.isAcceptable("elevenchars")).isFalse();
        assertThat(PasswordPolicy.isAcceptable("twelve-chars")).isTrue();
    }

    @Test
    void rejects_passwords_longer_than_72_bytes_counting_utf8() {
        assertThat(PasswordPolicy.isAcceptable("a".repeat(72))).isTrue();
        assertThat(PasswordPolicy.isAcceptable("a".repeat(73))).isFalse();
        // 37 "ñ" son 37 caracteres pero 74 bytes: BCrypt truncaría.
        assertThat(PasswordPolicy.isAcceptable("ñ".repeat(37))).isFalse();
    }

    @Test
    void counts_code_points_not_java_chars() {
        // 11 emojis = 22 "char" de Java, pero 11 caracteres: no llega al mínimo.
        assertThat(PasswordPolicy.isAcceptable("😀".repeat(11))).isFalse();
    }
}
