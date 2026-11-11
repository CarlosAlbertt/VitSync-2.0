# ADR-0009: Referencias entre módulos por id, no por entidad JPA

## Estado

**Aceptado** — 2026-11-11

## Contexto

El backend es un monolito modular con seis módulos (`iam`, `catalog`, `scheduling`,
`clinical`, `gdpr`, `notification`), cada uno en cuatro capas (`api`, `application`,
`domain`, `infrastructure`). `ArchitectureTest` impone que un módulo solo use de otro su
capa `application`: nunca sus controladores, entidades ni repositorios.

Las slices que vienen cruzan módulos en cuanto empiezan:

- Una **cita** (`scheduling`) tiene paciente y médico (`iam`) y centro (`catalog`).
- Un **informe** (`clinical`) tiene paciente y médico (`iam`).
- Un **perfil de médico** (`iam`) tiene especialidad (`catalog`).

El ejemplo de la guía (Fase 4.3) modela la cita con `@ManyToOne` hacia `PatientProfile` y
`DoctorProfile`. Esa clase no compila contra las reglas: `Appointment` viviría en
`scheduling.domain` y dependería de `iam.domain`, que es justo lo que el test prohíbe. Hay
que elegir entre relajar la regla o cambiar la forma de referenciar.

Lo que ya hay en el código apunta en una dirección: `DoctorProfile` guarda `specialtyId`
(una especialidad es de `catalog`) y `OneTimeCode` guarda `userId` aunque `User` está en su
mismo módulo. Dentro de `iam` sí hay relaciones JPA (`RefreshToken → User`, los perfiles con
`@MapsId`), y están bien: son el mismo módulo.

En la base de datos todo sigue unido por claves foráneas (`appointments.doctor_id REFERENCES
doctor_profiles(user_id)`, etc.). La duda no es de integridad, es de acoplamiento en Java.

## Opciones consideradas

1. **`@ManyToOne` entre módulos (lo de la guía)**, relajando la regla para el paquete
   `domain`.
   - `+` Navegación cómoda (`appointment.getDoctor().getUser().getFirstName()`) y joins
     que escribe Hibernate.
   - `−` `scheduling` conoce la forma interna de las entidades de `iam`: renombrar un campo
     de `User` rompe citas e informes. Los módulos pasan a ser carpetas, no fronteras.
   - `−` Relaciones perezosas que cruzan módulos: `LazyInitializationException` o N+1 fuera
     del módulo que entiende esa entidad. En v1 fue un problema recurrente.
   - `−` Cualquier módulo podría modificar una entidad ajena a través de la relación
     (`appointment.getDoctor().setBio(...)`) y Hibernate lo guardaría sin pasar por las
     reglas de `iam`.
   - `−` Partir un módulo en un servicio aparte, si algún día hiciera falta, obliga a
     rehacer todas esas relaciones.

2. **Referencia por id y consulta al `application` del otro módulo** (elegida).
   - `+` Es lo que el test ya exige: no hay que tocar las reglas.
   - `+` Cada módulo es el único que carga y modifica sus entidades; el resto recibe
     *records* de solo lectura.
   - `+` La FK de la base de datos se mantiene: la integridad no depende del código.
   - `−` Para pintar un listado hay que pedir los datos al otro módulo. Mal hecho, es un
     N+1; bien hecho (una consulta por lote), son dos o tres consultas por página.
   - `−` Más código: una interfaz pública por módulo y sus *records*.

3. **Eventos y copias locales** (cada módulo guarda una réplica de lo que necesita de otros,
   actualizada por eventos).
   - `+` Lecturas sin llamadas entre módulos y máxima independencia.
   - `−` Consistencia eventual, réplicas que sincronizar y eventos que versionar. Es la
     complejidad de los microservicios sin tenerlos. Desproporcionado para un monolito con
     una sola base de datos.

## Decisión

**Entre módulos se referencia por id; dentro de un módulo, las relaciones JPA son libres.**

1. **En la entidad**, la referencia a otro módulo es una columna con el id interno
   (`Long`), nombrada como la columna: `patientId`, `doctorId`, `facilityId`,
   `specialtyId`. Nunca `@ManyToOne`, `@OneToOne` ni `@OneToMany` hacia una entidad de otro
   módulo.
2. **En la base de datos**, la FK se declara igual que si hubiera relación JPA. Las
   migraciones son las que garantizan que el id existe.
3. **Para leer datos de otro módulo**, se llama a un servicio de su capa `application` que
   devuelve *records* inmutables, nunca entidades. Si un método público devolviera una
   entidad, el módulo que lo llama dependería de su `domain` y `ArchitectureTest` fallaría:
   la regla se vigila sola.
4. **En listados, siempre por lote**: los servicios públicos aceptan un conjunto de ids
   (`findSummaries(Set<Long> ids)` → `Map<Long, DoctorSummary>`). Pedir un registro por fila
   está prohibido en revisión de código.
5. **Las reglas de negocio de otro módulo se preguntan, no se replican**: para reservar,
   `scheduling` pregunta a `iam` si el médico existe y está activo; no lee su estado por SQL.
6. **Fuera de Java**, en la API se exponen los `public_id` (UUID), nunca los ids internos.
   El id interno solo viaja entre módulos dentro del proceso.

Ejemplo de la forma esperada:

```java
// scheduling.domain
public class Appointment {
    @Column(name = "patient_id", nullable = false, updatable = false)
    private Long patientId;

    @Column(name = "doctor_id", nullable = false, updatable = false)
    private Long doctorId;

    public static Appointment schedule(Long patientId, Long doctorId, Instant startsAt,
                                       Modality modality) { ... }
}

// iam.application: la puerta pública de iam para el resto de módulos
public record DoctorSummary(Long id, UUID publicId, String fullName, Long specialtyId) {}

public interface DoctorDirectory {
    DoctorSummary getBookable(UUID publicId);                  // 404 o 409 si no se puede
    Map<Long, DoctorSummary> findSummaries(Set<Long> ids);     // una consulta por página
}
```

**Escape para lecturas pesadas.** Si un listado concreto resulta lento con la composición
por lote (medido, no supuesto), se permite una consulta de solo lectura con joins entre
tablas de varios módulos, en la `infrastructure` del módulo que la necesita y con un
comentario que enlace este ADR. Nunca para escribir.

## Consecuencias

- `+` Las fronteras entre módulos las comprueba la CI, sin excepciones que mantener.
- `+` Cada módulo puede cambiar sus entidades sin romper a los demás mientras respete sus
  *records* públicos.
- `+` No hay relaciones perezosas que crucen módulos: lo que se lee está cargado.
- `+` Si un módulo tuviera que separarse algún día, su interfaz pública ya existe.
- `−` Un listado de citas hace tres consultas (citas, médicos, centros) en vez de un join.
  Con paginación de 20 filas es irrelevante; se vigila con los tests de integración.
- `−` Un `Long` no dice de qué es: un `doctorId` y un `patientId` se pueden confundir. Se
  mitiga con nombres explícitos y con fábricas en el dominio que los reciben por separado.
  Si llegara a dar problemas, se valorarán ids tipados (`record DoctorId(Long value)`).
- `−` Los `ON DELETE CASCADE` de la base de datos sí cruzan módulos (borrar un perfil de
  médico borra su agenda). Hoy no se borran usuarios: se desactivan, y el derecho al olvido
  anonimiza (módulo `gdpr`). **A vigilar** si algún día se permite borrado físico.
- `−` El ejemplo de la guía (Fase 4.3 y 5) se actualiza para seguir esta decisión.
