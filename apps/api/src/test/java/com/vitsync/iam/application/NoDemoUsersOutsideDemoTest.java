package com.vitsync.iam.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.vitsync.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

/** Sin el perfil {@code demo} (como en producción) el seeder ni siquiera existe como bean (VIT-67). */
class NoDemoUsersOutsideDemoTest extends IntegrationTest {

    @Autowired ApplicationContext context;

    @Test
    void demo_seeder_is_not_registered() {
        assertThat(context.getBeansOfType(DemoUsersSeeder.class)).isEmpty();
    }
}
