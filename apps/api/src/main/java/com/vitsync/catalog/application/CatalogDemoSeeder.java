package com.vitsync.catalog.application;

import com.vitsync.catalog.domain.Facility;
import com.vitsync.catalog.domain.Specialty;
import com.vitsync.catalog.domain.SpecialtyKind;
import com.vitsync.catalog.infrastructure.FacilityRepository;
import com.vitsync.catalog.infrastructure.SpecialtyRepository;
import com.vitsync.iam.application.DoctorDirectory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Catálogo de demostración, <b>solo en el perfil {@code demo}</b> (VIT-81): especialidades,
 * centros, y la especialidad y los centros de cada médico de demostración.
 *
 * <p>Mismo criterio que {@code DemoUsersSeeder}: nada de esto va en {@code db/migration}, que
 * se aplicaría también en producción. En producción el catálogo lo da de alta el panel de
 * administración (Sprint 6).
 *
 * <p>Corre después de los usuarios demo ({@code @Order(2)}), porque necesita que los médicos
 * existan. Los encuentra por número de colegiado a través de {@link DoctorDirectory}
 * (ADR-0009). Los textos coinciden con el frontend simulado, para que pasar de los mocks a la
 * API real no cambie lo que se ve. Idempotente: lo que ya existe no se toca.
 */
@Component
@Profile("demo")
@Order(2)
class CatalogDemoSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CatalogDemoSeeder.class);

    record DemoSpecialty(String code, String slug, String name, SpecialtyKind kind, String description) {
    }

    record DemoFacility(String name, String address, String city, String postalCode, String phone) {
    }

    /** Especialidad y centros de un médico demo, por su número de colegiado. */
    record Assignment(String licenseNumber, String specialtyCode, List<String> facilityNames) {
    }

    private static final String TURIA = "Centro Jardín del Turia";
    private static final String ALBUFERA = "Clínica Albufera";
    private static final String RETIRO = "Consultas Retiro";
    private static final String MEDITERRANI = "Espai Mediterrani";

    static final List<DemoSpecialty> SPECIALTIES = List.of(
            new DemoSpecialty("FAMILY_MEDICINE", "medicina-familiar", "Medicina familiar", SpecialtyKind.GENERAL,
                    "Tu primer punto de contacto: revisiones, seguimiento y derivaciones."),
            new DemoSpecialty("CARDIOLOGY", "cardiologia", "Cardiología", SpecialtyKind.MEDICAL,
                    "Corazón y sistema circulatorio, de la prevención al seguimiento."),
            new DemoSpecialty("DERMATOLOGY", "dermatologia", "Dermatología", SpecialtyKind.MEDICAL,
                    "Piel, cabello y uñas. Revisión de lunares y tratamientos."),
            new DemoSpecialty("PEDIATRICS", "pediatria", "Pediatría", SpecialtyKind.MEDICAL,
                    "Salud y desarrollo desde el nacimiento hasta la adolescencia."),
            new DemoSpecialty("GYNECOLOGY", "ginecologia", "Ginecología y obstetricia", SpecialtyKind.SURGICAL,
                    "Salud de la mujer, embarazo y revisiones periódicas."),
            new DemoSpecialty("TRAUMATOLOGY", "traumatologia", "Traumatología", SpecialtyKind.SURGICAL,
                    "Huesos, articulaciones y lesiones deportivas."),
            new DemoSpecialty("OPHTHALMOLOGY", "oftalmologia", "Oftalmología", SpecialtyKind.SURGICAL,
                    "Revisión de la vista y patologías oculares."),
            new DemoSpecialty("NEUROLOGY", "neurologia", "Neurología", SpecialtyKind.MEDICAL,
                    "Migrañas, trastornos del sueño y sistema nervioso."),
            new DemoSpecialty("PSYCHIATRY", "psiquiatria", "Psiquiatría", SpecialtyKind.MEDICAL,
                    "Salud mental con seguimiento continuado."),
            new DemoSpecialty("RADIOLOGY", "radiologia", "Radiología", SpecialtyKind.DIAGNOSTIC,
                    "Pruebas de imagen: radiografía, ecografía y resonancia."),
            new DemoSpecialty("CLINICAL_ANALYSIS", "analisis-clinicos", "Análisis clínicos", SpecialtyKind.DIAGNOSTIC,
                    "Analíticas de sangre y orina con resultados en tu perfil."),
            new DemoSpecialty("ENDOCRINOLOGY", "endocrinologia", "Endocrinología", SpecialtyKind.MEDICAL,
                    "Tiroides, diabetes y metabolismo."));

    // Direcciones y teléfonos inventados (rango 0000 de cada provincia).
    static final List<DemoFacility> FACILITIES = List.of(
            new DemoFacility(TURIA, "Paseo de la Ribera 12", "València", "46010", "+34 960 000 010"),
            new DemoFacility(ALBUFERA, "Avenida del Puerto 88", "València", "46023", "+34 960 000 020"),
            new DemoFacility(RETIRO, "Calle de los Tilos 5", "Madrid", "28009", "+34 910 000 030"),
            new DemoFacility(MEDITERRANI, "Passeig de la Mar 21", "Barcelona", "08003", "+34 930 000 040"));

    static final List<Assignment> ASSIGNMENTS = List.of(
            new Assignment("DEMO-00001", "FAMILY_MEDICINE", List.of(TURIA)),
            new Assignment("46/10000", "FAMILY_MEDICINE", List.of(TURIA, ALBUFERA)),
            new Assignment("46/10137", "FAMILY_MEDICINE", List.of(RETIRO)),
            new Assignment("46/10274", "CARDIOLOGY", List.of(TURIA)),
            new Assignment("46/10411", "CARDIOLOGY", List.of(RETIRO, MEDITERRANI)),
            new Assignment("46/10548", "DERMATOLOGY", List.of(ALBUFERA)),
            new Assignment("46/10685", "PEDIATRICS", List.of(MEDITERRANI)),
            new Assignment("46/10822", "PEDIATRICS", List.of(TURIA)),
            new Assignment("46/10959", "GYNECOLOGY", List.of(TURIA, RETIRO)),
            new Assignment("46/11096", "TRAUMATOLOGY", List.of(ALBUFERA, RETIRO)),
            new Assignment("46/11233", "OPHTHALMOLOGY", List.of(MEDITERRANI)),
            new Assignment("46/11370", "NEUROLOGY", List.of(RETIRO)),
            new Assignment("46/11507", "PSYCHIATRY", List.of(TURIA, MEDITERRANI)),
            new Assignment("46/11644", "RADIOLOGY", List.of(ALBUFERA)),
            new Assignment("46/11781", "CLINICAL_ANALYSIS", List.of(TURIA)),
            new Assignment("46/11918", "ENDOCRINOLOGY", List.of(RETIRO)));

    private final SpecialtyRepository specialties;
    private final FacilityRepository facilities;
    private final DoctorDirectory doctors;
    private final TransactionTemplate tx;

    CatalogDemoSeeder(SpecialtyRepository specialties, FacilityRepository facilities, DoctorDirectory doctors,
                      TransactionTemplate tx) {
        this.specialties = specialties;
        this.facilities = facilities;
        this.doctors = doctors;
        this.tx = tx;
    }

    @Override
    public void run(ApplicationArguments args) {
        tx.executeWithoutResult(s -> {
            Map<String, Long> specialtyIds = new HashMap<>();
            for (DemoSpecialty d : SPECIALTIES) {
                Specialty specialty = specialties.findByCode(d.code()).orElseGet(() ->
                        specialties.save(Specialty.create(d.code(), d.slug(), d.name(), d.description(), d.kind())));
                specialtyIds.put(d.code(), specialty.getId());
            }

            Map<String, Long> facilityIds = new HashMap<>();
            for (DemoFacility d : FACILITIES) {
                Facility facility = facilities.findByName(d.name()).orElseGet(() ->
                        facilities.save(Facility.create(d.name(), d.address(), d.city(), d.postalCode(), d.phone())));
                facilityIds.put(d.name(), facility.getId());
            }

            // Un médico que no exista (p. ej. alguien lo borró a mano en local) se salta.
            Map<String, Long> doctorIds = doctors.idsByLicenseNumber(
                    ASSIGNMENTS.stream().map(Assignment::licenseNumber).collect(Collectors.toSet()));
            for (Assignment a : ASSIGNMENTS) {
                Long doctorId = doctorIds.get(a.licenseNumber());
                if (doctorId == null) {
                    log.warn("Demo doctor {} not found; skipping its catalog data", a.licenseNumber());
                    continue;
                }
                doctors.assignSpecialty(doctorId, specialtyIds.get(a.specialtyCode()));
                a.facilityNames().forEach(name -> facilities.assignDoctor(doctorId, facilityIds.get(name)));
            }
        });
        log.info("Demo catalog ready: {} specialties, {} facilities", SPECIALTIES.size(), FACILITIES.size());
    }
}
