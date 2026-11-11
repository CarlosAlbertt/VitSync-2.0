package com.vitsync.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.vitsync.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

/** Sin el perfil {@code demo} (como en producción) el seeder del catálogo ni siquiera existe (VIT-81). */
class NoCatalogDemoOutsideDemoTest extends IntegrationTest {

    @Autowired ApplicationContext context;

    @Test
    void demo_catalog_seeder_is_not_registered() {
        assertThat(context.getBeansOfType(CatalogDemoSeeder.class)).isEmpty();
    }
}
