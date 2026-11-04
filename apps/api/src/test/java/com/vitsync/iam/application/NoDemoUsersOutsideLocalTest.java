package com.vitsync.iam.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.vitsync.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

/** Fuera del perfil {@code local} el seeder ni siquiera existe como bean (VIT-67). */
class NoDemoUsersOutsideLocalTest extends IntegrationTest {

    @Autowired ApplicationContext context;

    @Test
    void demo_seeder_is_not_registered() {
        assertThat(context.getBeansOfType(DemoUsersSeeder.class)).isEmpty();
    }
}
