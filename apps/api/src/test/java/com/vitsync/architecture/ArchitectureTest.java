package com.vitsync.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Reglas de arquitectura del monolito modular (guía, Fase 4).
 *
 * <p>ArchUnit lee las clases compiladas y comprueba quién depende de quién. Si una regla se
 * incumple, el test falla y con él el build y la CI: las fronteras entre módulos y capas las
 * vigila la máquina, no la memoria de quien revisa el PR.
 *
 * <p>Cada módulo ({@code iam}, {@code catalog}...) se organiza en cuatro capas:
 * <pre>
 *   api            controladores, DTOs, mappers      (nadie lo importa)
 *   application    casos de uso, @Transactional      (la puerta pública del módulo)
 *   domain         entidades y reglas de negocio
 *   infrastructure repositorios y adaptadores externos
 * </pre>
 *
 * <p>Los módulos aún están vacíos, por eso las reglas llevan {@code allowEmptyShould(true)} y
 * {@code withOptionalLayers(true)}: sin ellos, ArchUnit falla cuando una regla no encuentra
 * ninguna clase que comprobar. En cuanto aparezcan clases, se aplican sin cambiar nada.
 *
 * <p>Usa ArchUnit como librería con tests JUnit normales (no su motor propio de JUnit): ver
 * el comentario de la dependencia en el pom.xml.
 */
class ArchitectureTest {

    // Clases compiladas de producción, importadas una sola vez para todas las reglas.
    // DO_NOT_INCLUDE_TESTS: los tests pueden saltarse las fronteras (p. ej. un controlador
    // de prueba), así que no se analizan.
    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.vitsync");

    // Dentro de cada módulo, las capas solo se usan en una dirección:
    // api -> application -> domain, y application -> infrastructure.
    // El * de "com.vitsync.*.api.." significa "la capa api de cualquier módulo".
    // consideringOnlyDependenciesInLayers: solo mira dependencias entre nuestras capas, no
    // con librerías (Spring, Jakarta...), que todas pueden usar.
    @Test
    void layers_inside_each_module_respect_their_direction() {
         layeredArchitecture()
                 .consideringOnlyDependenciesInLayers()
                .withOptionalLayers(true)
                .layer("Api").definedBy("com.vitsync.*.api..")
                .layer("Application").definedBy("com.vitsync.*.application..")
                .layer("Domain").definedBy("com.vitsync.*.domain..")
                .layer("Infrastructure").definedBy("com.vitsync.*.infrastructure..")
                // Nadie llama a un controlador: es el punto de entrada HTTP.
                .whereLayer("Api").mayNotBeAccessedByAnyLayer()
                // Los casos de uso solo se invocan desde los controladores (y desde la
                // application de otro módulo, que al ser la misma capa está permitido).
                .whereLayer("Application").mayOnlyBeAccessedByLayers("Api")
                // Los repositorios solo los usa application: un controlador nunca va
                // directo a la base de datos.
                .whereLayer("Infrastructure").mayOnlyBeAccessedByLayers("Application")
                .check(CLASSES);
    }

    // Un módulo solo puede usar de otro su capa application: nunca sus controladores,
    // entidades ni repositorios. @ParameterizedTest ejecuta el método una vez por cada
    // valor de @ValueSource, así cada módulo sale como un caso separado en el informe.
    // Consecuencia: entre módulos se referencia por id, no por entidad JPA (Q13).
    @ParameterizedTest
    @ValueSource(strings = {"iam", "catalog", "scheduling", "clinical", "gdpr", "notification"})
    void other_modules_only_use_the_application_layer(String module){
        String base = "com.vitsync." + module;
        noClasses()
                // "que estén fuera del módulo" (el propio módulo sí usa sus capas)
                .that().resideOutsideOfPackage(base + "..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        base + ".api..", base + ".domain..", base + ".infrastructure.."
                )
                .allowEmptyShould(true)
                .check(CLASSES);
    }

    // common es la base compartida (errores, seguridad, traza): todos los módulos dependen
    // de él y él no depende de ninguno. Si lo hiciera, dejaría de ser común y crearía ciclos.
    // Al añadir un módulo nuevo, hay que añadirlo también a esta lista.
    @Test
    void common_does_not_depend_on_any_module() {
        noClasses()
                .that().resideInAPackage("com.vitsync.common..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.vitsync.iam..", "com.vitsync.catalog..",
                        "com.vitsync.scheduling..", "com.vitsync.clinical..",
                        "com.vitsync.gdpr..", "com.vitsync.notification..")
                .allowEmptyShould(true)
                .check(CLASSES);
    }

    // El dominio es lógica de negocio pura: no sabe de HTTP (spring web) ni de cómo se
    // guardan los datos (Spring Data). Así se puede probar sin levantar nada.
    // Las anotaciones JPA (@Entity) sí se permiten: son jakarta.persistence, no Spring Data.
    @Test
    void domain_does_not_depend_on_web_or_persistence_adapters() {
        noClasses()
                .that().resideInAPackage("com.vitsync.*.domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework.web..", "org.springframework.data..")
                .allowEmptyShould(true)
                .check(CLASSES);
    }

    // Cada "slice" es un paquete de primer nivel bajo com.vitsync (el (*) lo captura):
    // iam, catalog, common... Si A depende de B y B de A, hay un ciclo: en realidad son
    // un solo módulo mal partido, y ninguno se puede cambiar sin tocar el otro.
    @Test
    void modules_are_free_of_cycles() {
        slices().matching("com.vitsync.(*)..")
                .should().beFreeOfCycles()
                .allowEmptyShould(true)
                .check(CLASSES);
    }
}
