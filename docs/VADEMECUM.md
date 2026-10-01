# Vademécum técnico

> Vocabulario de ingeniería de software y de **AI engineering**, con las siglas
> desarrolladas y la definición en castellano. Donde el término aparece en VitSync,
> se indica dónde y para qué.

193 términos en 13 secciones.

01. [Arquitectura y diseño](#01-arquitectura-y-diseño) — 15 términos
02. [Backend, Java y Spring](#02-backend-java-y-spring) — 16 términos
03. [Bases de datos](#03-bases-de-datos) — 15 términos
04. [API y contratos](#04-api-y-contratos) — 13 términos
05. [Frontend](#05-frontend) — 15 términos
06. [Seguridad y criptografía](#06-seguridad-y-criptografía) — 20 términos
07. [Protección de datos](#07-protección-de-datos) — 13 términos
08. [Pruebas](#08-pruebas) — 13 términos
09. [Entrega y operación](#09-entrega-y-operación) — 14 términos
10. [Proceso y equipo](#10-proceso-y-equipo) — 13 términos
11. [Inteligencia artificial: Fundamentos](#11-inteligencia-artificial-fundamentos) — 18 términos
12. [Inteligencia artificial: Construcción de aplicaciones](#12-inteligencia-artificial-construcción-de-aplicaciones) — 16 términos
13. [Inteligencia artificial: Evaluación y operación](#13-inteligencia-artificial-evaluación-y-operación) — 12 términos

---

## 01. Arquitectura y diseño

**ADR**  
*Architecture Decision Record — registro de decisión de arquitectura*  
Documento breve que fija una decisión técnica junto con el contexto que la motivó, las alternativas descartadas y el coste aceptado. Nunca se borra: si la decisión cambia, se escribe otro que lo sustituye.
> **En VitSync** — Los ocho ADR de la Fase 0.2 en `docs/adr/`.

**Monolito modular**  
*modular monolith*  
Aplicación que se despliega como una sola unidad pero está dividida internamente en módulos con fronteras explícitas. Da el orden de los microservicios sin su coste operativo.
> **En VitSync** — La arquitectura elegida para v2, verificada con ArchUnit.

**Microservicios**  
*microservices*  
Estilo en el que cada capacidad de negocio se despliega como un servicio independiente con su propia base de datos. Aporta escalado y despliegue por separado a cambio de latencia de red, consistencia eventual y mucha infraestructura.

**Package-by-feature**  
*paquete por funcionalidad, frente a package-by-layer*  
Organizar el código por lo que hace (`appointments`, `reports`) en lugar de por su papel técnico (`controller`, `service`). Un cambio funcional queda contenido en una carpeta en vez de repartido por seis.

**Arquitectura hexagonal**  
*puertos y adaptadores · ports and adapters*  
El dominio define interfaces (puertos) y la infraestructura las implementa (adaptadores). Permite cambiar base de datos, proveedor de correo o almacenamiento sin tocar la lógica de negocio.

**DDD**  
*Domain-Driven Design — diseño guiado por el dominio*  
Enfoque que parte del lenguaje del negocio para modelar el software, de modo que el código use las mismas palabras que usan las personas que trabajan en ese dominio.

**Bounded context**  
*contexto delimitado*  
Frontera dentro de la cual un término del negocio tiene un único significado. «Paciente» puede significar cosas distintas en agenda y en facturación, y cada contexto tiene su propio modelo.

**Acoplamiento y cohesión**  
*coupling / cohesion*  
Acoplamiento es cuánto depende un módulo de otros; cohesión es cuánto tienen que ver entre sí las cosas que hay dentro de un módulo. La meta permanente es bajo acoplamiento y alta cohesión.

**SOLID**  
*Single responsibility · Open-closed · Liskov substitution · Interface segregation · Dependency inversion*  
Cinco principios de diseño orientado a objetos. El más usado en el día a día es el primero (una clase, un motivo para cambiar) y el último (depender de abstracciones, no de implementaciones).

**Inyección de dependencias**  
*DI — dependency injection*  
Una clase recibe sus colaboradores desde fuera en vez de construirlos ella misma. Es lo que permite sustituirlos por dobles en los tests.
> **En VitSync** — La hace el contenedor de Spring por constructor.

**CQRS**  
*Command Query Responsibility Segregation — separación de responsabilidad entre comandos y consultas*  
Separar el modelo que escribe del modelo que lee, para poder optimizar cada uno por su cuenta. En su forma ligera basta con usar proyecciones de solo lectura en las consultas.

**Outbox transaccional**  
*transactional outbox*  
Patrón para no perder efectos externos: la acción y el aviso pendiente (correo, evento) se guardan en la misma transacción, y un proceso aparte lo envía después con reintentos.
> **En VitSync** — Sustituye al envío de correo síncrono de v1, que tragaba los fallos.

**Idempotencia**  
*idempotency*  
Propiedad de una operación que produce el mismo resultado se ejecute una o varias veces. Es lo que hace seguro reintentar tras un error de red.

**Deuda técnica**  
*technical debt*  
Coste futuro que se asume al elegir una solución rápida en lugar de la correcta. Como la financiera, se puede aceptar conscientemente; el problema es no anotarla ni devolverla.

**Vertical slice**  
*rebanada vertical*  
Trozo de trabajo que atraviesa todas las capas —base de datos, servicio, endpoint, interfaz y tests— para entregar una funcionalidad completa en vez de una capa entera a medias.


---

## 02. Backend, Java y Spring

**JVM · JDK · JRE**  
*Java Virtual Machine · Java Development Kit · Java Runtime Environment*  
La JVM ejecuta el bytecode; el JDK es el kit de desarrollo (compilador y herramientas) e incluye el entorno de ejecución, que antes se distribuía aparte como JRE.
> **En VitSync** — JDK 21, versión con soporte a largo plazo.

**LTS**  
*Long-Term Support — soporte a largo plazo*  
Versión que recibe correcciones de seguridad durante años, frente a las intermedias que caducan en meses. Es el criterio para elegir versión en algo que va a producción.

**Spring Boot**  
*sobre Spring Framework*  
Capa sobre Spring que configura automáticamente lo habitual (servidor web, acceso a datos, seguridad) a partir de las dependencias declaradas, para no arrancar cada proyecto desde cero.

**Contenedor IoC · Bean**  
*IoC — Inversion of Control, inversión de control*  
El contenedor crea los objetos de la aplicación (beans), resuelve sus dependencias y gestiona su ciclo de vida. El código deja de construir sus colaboradores y solo los declara.

**ORM**  
*Object-Relational Mapping — mapeo objeto-relacional*  
Técnica que traduce entre filas de una base de datos relacional y objetos del lenguaje. Ahorra SQL repetitivo a cambio de esconder el coste real de cada consulta.

**JPA**  
*Jakarta Persistence API — antes Java Persistence API*  
Especificación estándar de Java para persistencia con ORM. Define las anotaciones (`@Entity`, `@Column`) y la semántica; no es una implementación.

**Hibernate**  
*implementación de JPA*  
El motor ORM más extendido en Java y el que usa Spring Boot por defecto. Añade su propio comportamiento por encima del estándar (caché, generación de esquema, estrategias de carga).

**Entidad**  
*entity*  
Clase que representa una fila de una tabla y está gestionada por el contexto de persistencia. Regla práctica: nunca debe salir serializada al cliente ni llevar `@Data`.

**DTO**  
*Data Transfer Object — objeto de transferencia de datos*  
Objeto plano que define exactamente qué entra y qué sale por la API, desacoplado del modelo de base de datos. Evita filtrar campos sensibles por descuido.
> **En VitSync** — v1 serializaba entidades y parcheaba con anotaciones de Jackson; v2 usa DTO de entrada y de salida.

**MapStruct**  
*generador de mapeadores*  
Biblioteca que genera en tiempo de compilación el código que convierte entidad a DTO y viceversa. Al ser código generado, un campo que no cuadra falla al compilar, no en producción.

**Lombok**  
*procesador de anotaciones*  
Genera constructores, *getters* y *setters* a partir de anotaciones. `@Data` sobre una entidad JPA es peligroso: genera `equals`, `hashCode` y `toString` que rompen con los proxies de Hibernate.

**Transacción · ACID**  
*Atomicity · Consistency · Isolation · Durability*  
Unidad de trabajo que se aplica entera o no se aplica. Las cuatro propiedades garantizan que sea atómica, deje datos coherentes, no interfiera con otras y sobreviva a una caída.
> **En VitSync** — `@Transactional` solo en servicios, nunca en controladores.

**Carga perezosa · N+1**  
*lazy loading · problema N+1*  
La relación no se carga hasta que se accede a ella. Si se recorre una lista de N elementos accediendo a la relación de cada uno, se lanzan N consultas extra más la inicial: el clásico problema de rendimiento del ORM.

**AOP**  
*Aspect-Oriented Programming — programación orientada a aspectos*  
Permite aplicar comportamiento transversal (auditoría, métricas, transacciones) alrededor de métodos sin ensuciar su código.
> **En VitSync** — `@Auditable` más un aspecto que escribe en `audit_logs`.

**Pool de conexiones**  
*connection pool · HikariCP*  
Conjunto de conexiones a base de datos reutilizadas entre peticiones, porque abrir una es caro. Su tamaño máximo es un límite real de concurrencia.
> **En VitSync** — Fijado a 3 por el límite del plan gratuito de Neon.

**Maven · artefacto · dependencia transitiva**  
*build tool*  
Maven compila, prueba y empaqueta según `pom.xml`. El artefacto es el resultado empaquetado (un JAR); las transitivas son las dependencias que arrastran tus dependencias, y son la vía habitual de las vulnerabilidades heredadas.


---

## 03. Bases de datos

**SQL · DDL · DML**  
*Structured Query Language · Data Definition Language · Data Manipulation Language*  
SQL es el lenguaje de las bases relacionales. DDL define la estructura (`CREATE`, `ALTER`); DML manipula los datos (`SELECT`, `INSERT`, `UPDATE`).

**Clave primaria y foránea**  
*primary key · foreign key*  
La primaria identifica de forma única cada fila; la foránea apunta a la primaria de otra tabla y es lo que impide referencias a filas inexistentes.

**Índice**  
*index*  
Estructura auxiliar que acelera las búsquedas por ciertas columnas a cambio de ocupar espacio y encarecer las escrituras.

**Índice único parcial**  
*partial unique index*  
Índice único que solo se aplica a las filas que cumplen una condición. Permite reglas como «no puede haber dos citas activas a la misma hora» dejando fuera las canceladas.
> **En VitSync** — `ux_citas_medico_fechahora_activa`: la garantía real contra la doble reserva, imposible de reproducir en H2.

**Normalización**  
*normal forms*  
Organizar las tablas para que cada dato viva en un solo sitio, evitando duplicados que puedan quedar desincronizados. Se desnormaliza a propósito solo cuando hay una razón de rendimiento medida.

**Migración de esquema**  
*schema migration*  
Cambio versionado de la estructura de la base de datos, guardado como fichero en el repositorio y aplicado en orden en todos los entornos. Regla de oro: una migración ya fusionada nunca se edita, se corrige con otra nueva.

**Flyway**  
*herramienta de migraciones*  
Aplica los ficheros `V1__…sql`, `V2__…sql` en orden y registra en una tabla cuáles ya se ejecutaron.
> **En VitSync** — Sustituye a los scripts que en v1 había que acordarse de ejecutar a mano en Neon.

**ddl-auto**  
*ajuste de Hibernate*  
Controla si Hibernate genera el esquema. `validate` solo comprueba que el mapeo cuadra con las tablas existentes; `create-drop` lo regenera en cada arranque y solo tiene sentido en pruebas desechables.

**Condición de carrera**  
*race condition*  
Dos operaciones simultáneas producen un resultado incorrecto según cuál llegue antes. Comprobar en el servicio y luego insertar no la evita: la garantía tiene que estar en la base de datos.

**Bloqueo optimista y pesimista**  
*optimistic / pessimistic locking*  
El optimista asume que no habrá conflicto y comprueba una columna de versión al guardar; el pesimista bloquea la fila mientras se trabaja con ella. El primero escala mejor, el segundo evita reintentos.

**Paginación por desplazamiento y por cursor**  
*offset / cursor pagination*  
Por desplazamiento se pide «la página 40», y la base de datos descarta antes todas las filas anteriores; por cursor se pide «lo siguiente a este identificador», que es estable y constante aunque el listado crezca.

**Borrado lógico**  
*soft delete*  
Marcar la fila como eliminada en lugar de borrarla. Necesario cuando hay obligaciones de conservación o auditoría, pero obliga a filtrar en todas las consultas.

**Particionado**  
*partitioning*  
Dividir físicamente una tabla muy grande en trozos (por ejemplo, por mes) para que las consultas y el mantenimiento solo toquen la parte relevante.
> **En VitSync** — Previsto para `audit_logs`, con cinco años de retención.

**Identificador público**  
*public id · UUID — Universally Unique Identifier*  
Identificador aleatorio expuesto al exterior, distinto de la clave primaria numérica interna. Evita que alguien recorra recursos ajenos sumando uno al identificador.

**Base de datos serverless**  
*serverless database*  
Servicio gestionado que escala y se suspende solo, facturando por uso. A cambio impone límites (conexiones, arranques en frío) que condicionan la configuración de la aplicación.


---

## 04. API y contratos

**API**  
*Application Programming Interface — interfaz de programación de aplicaciones*  
Conjunto de operaciones que un sistema ofrece a otros, con un contrato explícito de qué recibe y qué devuelve.

**REST**  
*Representational State Transfer*  
Estilo de API basado en recursos identificados por URL y manipulados con verbos HTTP. El servidor no guarda estado de sesión entre peticiones.

**Verbos HTTP**  
*GET · POST · PUT · PATCH · DELETE*  
GET lee; POST crea; PUT reemplaza por completo; PATCH modifica parcialmente; DELETE elimina. GET, PUT y DELETE deben ser idempotentes.

**Códigos de estado**  
*HTTP status codes*  
`200` correcto · `201` creado · `400` petición mal formada · `401` sin autenticar · `403` autenticado pero sin permiso · `404` no existe · `409` conflicto con el estado actual · `422` validación fallida · `429` demasiadas peticiones · `500` error del servidor.

**OpenAPI**  
*antes Swagger Specification*  
Formato estándar para describir una API REST en un fichero legible por máquinas, del que se pueden generar documentación, clientes y pruebas.

**Contract-first**  
*contrato primero*  
Definir o generar el contrato de la API antes de escribir el cliente, y derivar de él los tipos del frontend. Elimina por construcción la desviación entre lo que la API devuelve y lo que el cliente espera.

**springdoc-openapi**  
*biblioteca de Spring*  
Genera la especificación OpenAPI a partir de los controladores y sus anotaciones, de modo que la documentación no se escribe a mano ni se queda vieja.

**Drift de contrato**  
*contract drift*  
Desfase entre la API real y lo que el cliente cree que existe. Se detecta en producción salvo que la integración continua compare la especificación regenerada con la versionada.

**ProblemDetail**  
*RFC 9457 — Problem Details for HTTP APIs*  
Formato estándar de respuesta de error, con campos `type`, `title`, `status`, `detail` e `instance`. Evita que cada endpoint invente su propio JSON de error.

**Versionado de API**  
*API versioning*  
Convivencia de versiones (`/api/v1`, `/api/v2`) para poder introducir cambios incompatibles sin romper a los clientes existentes.

**CORS**  
*Cross-Origin Resource Sharing — intercambio de recursos entre orígenes*  
Mecanismo por el que el navegador solo permite a una web llamar a otro dominio si ese dominio lo autoriza por cabeceras. Es una protección del navegador, no del servidor.

**Limitación de peticiones**  
*rate limiting*  
Tope de peticiones por ventana de tiempo para una IP o una cuenta. Es la defensa básica contra la fuerza bruta y el abuso; se responde con `429`.

**Webhook**  
*devolución de llamada HTTP*  
Petición que un sistema externo envía a una URL tuya cuando ocurre un evento, en lugar de que tú preguntes periódicamente. Requiere verificar la firma del emisor.


---

## 05. Frontend

**SPA**  
*Single-Page Application — aplicación de página única*  
La navegación ocurre en el navegador sin recargar la página; el servidor entrega datos, no HTML completo. Rápida al navegar, más pesada en la primera carga.

**CSR · SSR · SSG**  
*Client-Side / Server-Side Rendering · Static Site Generation*  
Dónde se construye el HTML: en el navegador, en el servidor por petición, o al compilar. Afecta a la primera pintura, al posicionamiento y a la complejidad del despliegue.

**Hidratación**  
*hydration*  
Proceso por el que el JavaScript del cliente toma el HTML ya renderizado y le engancha los manejadores de eventos para volverlo interactivo.

**DOM virtual · reconciliación**  
*virtual DOM — Document Object Model*  
React mantiene una representación en memoria del árbol de la interfaz, la compara con la anterior y aplica al DOM real solo las diferencias.

**Componente · props · estado**  
*component / properties / state*  
El componente es la unidad reutilizable de interfaz; las *props* son los datos que recibe de su padre y no modifica; el estado es lo que gestiona por sí mismo y provoca un nuevo renderizado al cambiar.

**Hook**  
*gancho*  
Función de React que da acceso a estado y ciclo de vida dentro de un componente de función (`useState`, `useEffect`). Los propios se extraen para reutilizar lógica sin duplicar componentes.

**Estado de servidor y estado de interfaz**  
*server state / UI state*  
El de servidor es una copia local de datos que viven en la API, y por tanto puede quedar obsoleto; el de interfaz es puramente local (un menú abierto). Confundirlos es el origen de la mayoría de las gestiones de estado caseras.

**TanStack Query**  
*antes React Query*  
Biblioteca que gestiona el estado de servidor: caché, estados de carga y error, reintentos, datos obsoletos e invalidación tras una mutación.
> **En VitSync** — Sustituye a los once *stores* caseros de v1.

**Zustand**  
*gestor de estado ligero*  
Almacén mínimo para estado de interfaz compartido entre componentes, sin la ceremonia de Redux.

**TypeScript**  
*superconjunto tipado de JavaScript*  
Añade tipos comprobados al compilar, que desaparecen en ejecución. En modo estricto obliga a tratar los valores nulos y convierte en error de compilación lo que en JavaScript sería un fallo en producción.

**Vite · empaquetador**  
*bundler*  
Herramienta que sirve el proyecto en desarrollo con recarga instantánea y lo empaqueta para producción, eliminando código no usado (*tree shaking*) y dividiéndolo en trozos que se cargan bajo demanda (*code splitting*).

**Tailwind CSS**  
*utility-first CSS*  
Estilos aplicados con clases de propósito único directamente en el marcado. Evita hojas de estilo globales que nadie se atreve a borrar, a cambio de un marcado más denso.

**shadcn/ui**  
*colección de componentes*  
Componentes accesibles que se copian al propio repositorio en lugar de instalarse como dependencia, de modo que se pueden modificar sin luchar contra la biblioteca.

**Accesibilidad**  
*a11y · WCAG — Web Content Accessibility Guidelines · ARIA — Accessible Rich Internet Applications*  
Que la interfaz sea usable con teclado, lector de pantalla y contraste suficiente. WCAG son las pautas de referencia; ARIA es el conjunto de atributos para describir componentes a las tecnologías de apoyo.

**Core Web Vitals**  
*LCP — Largest Contentful Paint · CLS — Cumulative Layout Shift · INP — Interaction to Next Paint*  
Métricas de experiencia real: cuánto tarda en pintarse el contenido principal, cuánto se mueve el diseño mientras carga y cuánto tarda la interfaz en responder a una interacción.


---

## 06. Seguridad y criptografía

**Autenticación y autorización**  
*authentication / authorization*  
Autenticar es comprobar quién eres; autorizar es decidir qué puedes hacer. Se corresponden con los códigos `401` y `403`.

**JWT**  
*JSON Web Token*  
Token firmado que transporta afirmaciones (*claims*) sobre el usuario. Va firmado, no cifrado: cualquiera puede leer su contenido, así que no debe llevar datos sensibles.

**RS256 frente a HS256**  
*RSA-SHA256 · HMAC-SHA256*  
RS256 firma con clave privada y se verifica con la pública, de modo que quien valida no puede emitir tokens. HS256 usa un secreto compartido: más simple, pero cualquiera que valide puede falsificar.

**Token de acceso y de refresco**  
*access token / refresh token*  
El de acceso es de vida corta y acompaña a cada petición; el de refresco es de vida larga y solo sirve para obtener uno nuevo. Separarlos limita la ventana de daño si el primero se filtra.

**Token opaco**  
*opaque token*  
Cadena aleatoria sin contenido interpretable, cuyo estado vive en el servidor. Al contrario que un JWT, se puede revocar de verdad porque hay que consultarlo en la base de datos.

**Rotación y detección de reuso**  
*refresh token rotation / reuse detection*  
Cada canje invalida el token usado y emite otro de la misma familia. Si aparece uno ya consumido, se asume robo y se revoca la familia entera.
> **En VitSync** — El hueco de v1: había rotación, pero no detección de reuso.

**OAuth 2.0 · OIDC**  
*OpenID Connect*  
OAuth 2.0 es el marco estándar de autorización delegada («esta aplicación puede acceder a esto en tu nombre»); OIDC es la capa de identidad construida encima que añade autenticación.

**2FA · MFA · TOTP**  
*Two/Multi-Factor Authentication · Time-based One-Time Password*  
Exigir un segundo factor además de la contraseña. TOTP es el código de seis dígitos que cambia cada treinta segundos, generado a partir de un secreto compartido y la hora.

**Hash, cifrado y codificación**  
*hash / encryption / encoding*  
El hash es de una sola dirección y no se deshace; el cifrado es reversible con la clave; la codificación (Base64) no protege nada, solo cambia la representación. Confundirlos es un error de seguridad clásico.

**BCrypt · Argon2id**  
*funciones de derivación de contraseña*  
Algoritmos deliberadamente lentos y ajustables para almacenar contraseñas. El coste se sube con el hardware, de modo que probar millones de candidatas siga siendo caro.

**Sal y pimienta**  
*salt / pepper*  
La sal es un valor aleatorio distinto por registro, guardado junto al hash, que impide tablas precalculadas. La pimienta es un secreto global que vive fuera de la base de datos, de modo que robar la base no basta.

**AES-256-GCM**  
*Advanced Encryption Standard · Galois/Counter Mode*  
Cifrado simétrico autenticado: además de ocultar el contenido, detecta si alguien lo ha manipulado. El vector de inicialización debe ser distinto en cada operación con la misma clave.

**IV · nonce · tag**  
*Initialization Vector · number used once · authentication tag*  
El IV o nonce garantiza que cifrar dos veces el mismo dato dé resultados distintos; el *tag* es la marca que permite comprobar la integridad al descifrar.

**Cifrado en reposo y en tránsito**  
*at rest / in transit*  
En reposo protege los datos guardados (base de datos, copias, ficheros); en tránsito protege la comunicación mediante TLS. Son medidas complementarias, no alternativas.

**HMAC**  
*Hash-based Message Authentication Code — código de autenticación de mensajes basado en hash*  
Hash calculado con una clave secreta. Sirve para verificar integridad y origen, y es la base del *blind index*.

**Blind index**  
*índice ciego*  
Columna adicional con el HMAC del valor cifrado, que permite buscar por igualdad y garantizar unicidad sin descifrar la tabla. Contrapartida: es determinista y revela qué filas comparten valor.

**IDOR**  
*Insecure Direct Object Reference — referencia directa insegura a objetos*  
Acceder a un recurso ajeno simplemente cambiando el identificador de la URL. Se evita comprobando la propiedad del recurso en cada acceso, no solo el rol.

**XSS · CSRF · SQLi**  
*Cross-Site Scripting · Cross-Site Request Forgery · SQL Injection*  
Inyectar JavaScript en una página que verán otros; hacer que el navegador de la víctima envíe una petición autenticada sin querer; e insertar SQL en un parámetro para alterar la consulta.

**CSP**  
*Content Security Policy — política de seguridad de contenidos*  
Cabecera que declara de qué orígenes puede cargar recursos la página. Es la segunda barrera contra XSS cuando la sanitización falla.

**OWASP · CVE · CVSS**  
*Open Worldwide Application Security Project · Common Vulnerabilities and Exposures · Common Vulnerability Scoring System*  
OWASP publica el *Top 10* de riesgos web; CVE es el identificador público de una vulnerabilidad concreta; CVSS es la puntuación de 0 a 10 que mide su gravedad.
> **En VitSync** — La auditoría V01–V21 de v1 usa CVSS y mapeo normativo.


---

## 07. Protección de datos

**RGPD**  
*Reglamento General de Protección de Datos · GDPR en inglés*  
Norma europea que regula el tratamiento de datos personales. Aplica por el hecho de tratar datos de personas en la Unión Europea, con independencia del tamaño del proyecto.

**Categoría especial**  
*Artículo 9 RGPD*  
Datos con protección reforzada: salud, biometría, orientación sexual, creencias, origen étnico. Su tratamiento está prohibido salvo excepción tasada, y eleva el listón de todas las medidas técnicas.
> **En VitSync** — Es la categoría de todo el historial clínico de la plataforma.

**Responsable y encargado**  
*controller / processor*  
El responsable decide para qué se tratan los datos; el encargado los trata por cuenta del responsable (un proveedor de correo o de chat). La relación exige un contrato del artículo 28.

**Base jurídica**  
*lawful basis*  
Motivo legal que legitima cada tratamiento: consentimiento, contrato, obligación legal, interés vital, interés público o interés legítimo. Sin base jurídica no se puede tratar el dato, por útil que sea.

**Minimización**  
*Artículo 5.1.c*  
Tratar solo los datos necesarios para la finalidad declarada. En la práctica: no pedir campos «por si acaso» ni cargar registros completos cuando basta con una proyección.

**Seudonimización y anonimización**  
*pseudonymisation / anonymisation*  
Seudonimizar sustituye los identificadores pero permite revertir con información adicional, y sigue siendo dato personal. Anonimizar es irreversible y saca el dato del ámbito del RGPD.
> **En VitSync** — La supresión de cuenta anonimiza, no borra, por la obligación de conservar documentación clínica.

**EIPD · DPIA**  
*Evaluación de Impacto relativa a la Protección de Datos · Data Protection Impact Assessment — Artículo 35*  
Análisis previo de riesgos obligatorio cuando el tratamiento es de alto riesgo, como los datos de salud a escala. Documenta amenazas, medidas y riesgo residual.

**Registro de actividades de tratamiento**  
*RAT — Artículo 30*  
Inventario de qué datos se tratan, con qué finalidad, con qué base jurídica, quién accede, dónde se guardan y cuánto tiempo. Es lo primero que pide una autoridad de control.

**Derechos del interesado**  
*acceso, rectificación, supresión, portabilidad, oposición, limitación*  
Facultades que la persona puede ejercer sobre sus datos, con plazo de respuesta de un mes. La portabilidad exige entregarlos en formato estructurado y legible por máquina.

**Brecha de seguridad**  
*data breach — Artículos 33 y 34*  
Incidente que provoca destrucción, pérdida, alteración o acceso no autorizado a datos personales. Se notifica a la autoridad en 72 horas, y a los afectados si el riesgo es alto.

**Política de retención**  
*retention policy*  
Cuánto tiempo se conserva cada tipo de dato y qué se hace al vencer el plazo. Conservar «por si acaso» incumple la limitación del plazo de conservación.

**Auditoría append-only**  
*registro de solo adición*  
Traza de accesos y cambios que solo admite inserciones, nunca modificación ni borrado. Es lo que la convierte en evidencia frente a una reclamación.

**LOPDGDD · Ley 41/2002**  
*Ley Orgánica de Protección de Datos Personales y garantía de los derechos digitales · Ley básica de autonomía del paciente*  
Normativa española que complementa al RGPD. La Ley 41/2002 fija además la conservación de la documentación clínica durante un mínimo de cinco años.


---

## 08. Pruebas

**Prueba unitaria, de integración y de extremo a extremo**  
*unit / integration / end-to-end (E2E)*  
La unitaria comprueba una pieza aislada; la de integración, varias piezas reales juntas (incluida la base de datos); la E2E recorre el flujo completo por la interfaz como haría una persona.

**Pirámide de pruebas**  
*testing pyramid*  
Muchas unitarias, bastantes de integración y pocas E2E. Al invertirse produce suites lentas y frágiles que el equipo acaba ignorando.

**TDD**  
*Test-Driven Development — desarrollo guiado por pruebas*  
Escribir primero una prueba que falla, luego el código mínimo que la hace pasar, y por último refactorizar. Su efecto principal es un diseño más desacoplado.

**Doble de prueba**  
*mock · stub · fake · spy*  
Sustitutos de una dependencia real: el *stub* devuelve respuestas fijas; el *mock* además verifica que se le llamó como se esperaba; el *fake* es una implementación simplificada pero funcional; el *spy* registra las llamadas al objeto real.

**Fixture**  
*datos de partida*  
Estado conocido que se prepara antes de cada prueba para que su resultado sea reproducible y no dependa del orden de ejecución.

**Cobertura**  
*code coverage · JaCoCo*  
Porcentaje de líneas o de ramas ejecutadas por las pruebas. Mide qué se ha ejecutado, no qué se ha verificado: es un indicador de hueco, no de calidad.

**Testcontainers**  
*dependencias reales en contenedores*  
Levanta la base de datos real (u otro servicio) en un contenedor durante la prueba y la destruye al terminar. Elimina la diferencia entre el motor de pruebas y el de producción.
> **En VitSync** — Sustituye a H2, que no soportaba el índice parcial que evita la doble reserva.

**Prueba de concurrencia**  
*concurrency test*  
Lanza varias operaciones simultáneas contra el mismo recurso y comprueba que solo una tiene éxito. Es la única forma de demostrar que la protección contra condiciones de carrera funciona.

**MSW**  
*Mock Service Worker*  
Intercepta las peticiones de red del frontend en las pruebas y responde con datos simulados, sin tocar el código de la aplicación.

**Playwright · Vitest · JUnit**  
*herramientas de prueba*  
Playwright automatiza navegadores reales para las pruebas E2E; Vitest ejecuta las de JavaScript y TypeScript; JUnit 5 es el marco estándar en Java.

**ArchUnit**  
*pruebas de arquitectura*  
Comprueba reglas estructurales como código: que un módulo no importe otro, o que los controladores no accedan a repositorios. Impide que la arquitectura se degrade en silencio.

**Prueba inestable**  
*flaky test*  
Prueba que a veces pasa y a veces falla sin cambiar el código. Destruye la confianza en la suite entera, así que se arregla o se elimina, nunca se reintenta a ciegas.

**Regresión**  
*regression*  
Fallo que reaparece o aparece en algo que antes funcionaba. Cada corrección de error debería dejar tras de sí una prueba que impida su vuelta.


---

## 09. Entrega y operación

**CI · CD**  
*Continuous Integration · Continuous Delivery / Deployment*  
Integración continua: cada cambio se compila y prueba automáticamente al subirlo. Entrega continua: queda siempre listo para desplegar. Despliegue continuo: se publica solo si pasa todo.

**Pipeline · job · runner**  
*tubería · tarea · ejecutor*  
La *pipeline* es la secuencia automatizada que se dispara con cada cambio; el *job* es cada paso; el *runner* es la máquina que lo ejecuta.

**Contenedor · imagen**  
*Docker*  
La imagen es la plantilla inmutable con la aplicación y sus dependencias; el contenedor es una instancia en ejecución. Resuelve el «en mi máquina funciona».

**Docker Compose**  
*orquestación local*  
Describe en un fichero los servicios que necesita el entorno de desarrollo para levantarlos con una orden.
> **En VitSync** — PostgreSQL 16, MinIO y Mailpit.

**Almacenamiento compatible con S3**  
*S3 — Simple Storage Service · MinIO*  
Almacenamiento de objetos accesible por una API estándar. MinIO la implementa en local, lo que permite desarrollar sin cuenta en la nube y desplegar sin cambiar código.

**URL prefirmada**  
*presigned URL*  
Enlace temporal que autoriza a subir o descargar un objeto concreto sin credenciales. Permite que el navegador hable directamente con el almacenamiento sin exponer el *bucket*.

**Disco efímero**  
*ephemeral storage*  
Sistema de ficheros del contenedor, que se pierde en cada despliegue o reinicio. Guardar ahí algo que debe persistir es un error de arquitectura, no de configuración.
> **En VitSync** — El fallo de v1: los ficheros subidos desaparecían con cada despliegue en Render.

**IaC**  
*Infrastructure as Code — infraestructura como código*  
Definir servidores, redes y servicios en ficheros versionados en vez de configurarlos a mano en un panel. Hace el entorno reproducible y auditable.

**Entornos**  
*desarrollo · pruebas · preproducción · producción*  
Copias del sistema con distinta finalidad y distintos datos. Preproducción debe parecerse a producción en todo salvo en los datos reales.

**Feature flag**  
*interruptor de funcionalidad*  
Condición que activa o desactiva una funcionalidad sin desplegar. Permite fusionar código incompleto sin exponerlo y desactivar algo al instante si falla.

**Despliegue azul-verde · canario · reversión**  
*blue-green · canary · rollback*  
Azul-verde mantiene dos entornos y cambia el tráfico de golpe; canario envía primero un porcentaje pequeño; la reversión vuelve a la versión anterior. Poder revertir importa más que desplegar rápido.

**Observabilidad**  
*logs, métricas y trazas*  
Capacidad de entender qué pasa dentro del sistema desde fuera. Los registros cuentan sucesos, las métricas agregan números en el tiempo y las trazas siguen una petición por todos sus saltos.

**Identificador de traza**  
*trace id · correlation id*  
Identificador único que acompaña a una petición por todo el sistema, de modo que todos sus registros se puedan reunir. Sin él, diagnosticar en producción es adivinar.

**SLI · SLO · SLA**  
*Service Level Indicator / Objective / Agreement*  
El indicador es lo que se mide (latencia, errores); el objetivo es la meta interna; el acuerdo es el compromiso contractual con el cliente, con consecuencias si se incumple.


---

## 10. Proceso y equipo

**Scrum · sprint**  
*marco de trabajo ágil*  
Ciclos cortos y fijos (una o dos semanas) que terminan con algo demostrable, más reuniones de planificación, seguimiento diario y revisión.

**Backlog · épica · historia de usuario**  
*pila de producto*  
El *backlog* es la lista priorizada de trabajo pendiente; la épica agrupa historias relacionadas; la historia describe una necesidad desde el punto de vista de quien la tiene.

**Criterios de aceptación · Gherkin**  
*Dado / Cuando / Entonces — Given / When / Then*  
Condiciones verificables que determinan si una historia está terminada. Gherkin las escribe en un formato estructurado que se puede traducir casi literalmente a una prueba.

**Definition of Done · Definition of Ready**  
*definición de terminado / de preparado*  
Lista fija que toda tarea debe cumplir para entrar al sprint o para darse por cerrada. Es lo que impide que «terminado» signifique cosas distintas cada semana.

**Kanban · WIP**  
*Work In Progress — trabajo en curso*  
Flujo continuo visualizado en columnas, con un límite de tareas simultáneas. El límite es lo que fuerza a terminar antes de empezar algo nuevo.

**Trunk-based development**  
*desarrollo sobre tronco*  
Ramas de vida corta que se integran a la principal varias veces por semana. Evita las fusiones enormes y dolorosas de las ramas de larga duración.

**Pull request · revisión de código**  
*PR · code review*  
Propuesta de fusión que se revisa antes de integrarse. Su valor no es solo detectar errores: reparte el conocimiento del código.

**Conventional Commits**  
*convención de mensajes*  
Formato `tipo(ámbito): descripción` — por ejemplo `feat(appointments): add cancel endpoint`. Permite generar el registro de cambios y deducir la subida de versión automáticamente.

**SemVer**  
*Semantic Versioning — versionado semántico*  
`MAYOR.MENOR.PARCHE`: la mayor rompe compatibilidad, la menor añade sin romper, el parche corrige. Comunica el riesgo de actualizar sin leer el código.

**Squash merge · rebase · historial lineal**  
*estrategias de integración en Git*  
*Squash* condensa la rama en un solo commit; *rebase* reescribe los commits sobre la punta actual. Ambos producen un historial lineal, mucho más fácil de leer y de revertir.

**Refactorización**  
*refactoring*  
Cambiar la estructura interna del código sin alterar su comportamiento observable. Si cambia el comportamiento, no es refactorizar: es un cambio funcional sin pruebas.

**Spike**  
*investigación acotada*  
Tarea de duración limitada cuyo único objetivo es responder a una pregunta técnica. Su entregable es una decisión o un ADR, no código de producción.

**MVP**  
*Minimum Viable Product — producto mínimo viable*  
La versión más pequeña que ya resuelve el problema para alguien. Definir explícitamente qué queda fuera importa tanto como definir qué entra.


---

## 11. Inteligencia artificial: Fundamentos

**IA · ML · DL**  
*Inteligencia Artificial · Machine Learning (aprendizaje automático) · Deep Learning (aprendizaje profundo)*  
Círculos concéntricos: la IA es el campo general; el aprendizaje automático son los sistemas que aprenden de datos en lugar de seguir reglas escritas; el profundo es el subconjunto que usa redes neuronales de muchas capas.

**Modelo · parámetros · pesos**  
*model · parameters · weights*  
El modelo es la función aprendida; los parámetros o pesos son los números ajustados durante el entrenamiento que la definen. «7B» significa siete mil millones de parámetros.

**LLM**  
*Large Language Model — modelo grande de lenguaje*  
Modelo entrenado para predecir el siguiente fragmento de texto sobre enormes cantidades de datos. De esa tarea simple emergen capacidades de resumen, traducción, razonamiento y programación.

**Transformer · atención**  
*attention mechanism*  
Arquitectura de red neuronal en la que cada fragmento del texto pondera su relación con todos los demás. Es lo que permitió entrenar en paralelo y dio lugar a los modelos actuales.

**Token · tokenización**  
*unidad mínima de texto*  
Fragmento en que se divide el texto: aproximadamente una palabra corta o parte de una larga. El modelo no ve letras ni palabras, ve tokens, y tanto el coste como los límites se miden en ellos.

**Ventana de contexto**  
*context window*  
Cantidad máxima de tokens que el modelo puede tener presentes a la vez, sumando la entrada y la salida. Todo lo que no cabe hay que resumirlo, recuperarlo o descartarlo.

**Embedding**  
*vector de representación*  
Lista de números que representa el significado de un texto, de modo que textos parecidos queden cerca en el espacio vectorial. Es la base de la búsqueda semántica.

**Similitud coseno**  
*cosine similarity*  
Medida de parecido entre dos vectores según el ángulo que forman, no su longitud. Es la operación con la que se decide qué fragmentos son relevantes para una consulta.

**Búsqueda semántica**  
*semantic search*  
Buscar por significado en lugar de por coincidencia literal de palabras, comparando *embeddings*. Encuentra «cita médica» al buscar «consulta con el doctor».

**Preentrenamiento y ajuste fino**  
*pre-training / fine-tuning*  
El preentrenamiento es la fase larga y cara sobre datos generales; el ajuste fino adapta ese modelo a una tarea o estilo con muchos menos ejemplos.

**RLHF**  
*Reinforcement Learning from Human Feedback — aprendizaje por refuerzo con retroalimentación humana*  
Fase en la que personas comparan respuestas y esas preferencias entrenan al modelo para que sea útil, honesto e inofensivo, no solo estadísticamente probable.

**Inferencia**  
*inference*  
Ejecutar el modelo ya entrenado para obtener una respuesta. Es lo que se paga en producción, frente al coste puntual del entrenamiento.

**Temperatura · top-p**  
*parámetros de muestreo*  
Controlan la aleatoriedad al elegir el siguiente token. Temperatura baja da respuestas más deterministas y repetibles; alta, más variadas. Para extraer datos estructurados se baja; para redactar se sube.

**Alucinación**  
*hallucination*  
Respuesta fluida y segura pero falsa. No es un fallo puntual sino consecuencia de que el modelo predice texto plausible; se mitiga aportando fuentes y verificando, no pidiéndole que no invente.

**Cuantización**  
*quantization*  
Reducir la precisión numérica de los pesos (de 16 a 4 bits, por ejemplo) para que el modelo ocupe menos y corra en hardware modesto, a cambio de algo de calidad.

**Destilación**  
*distillation*  
Entrenar un modelo pequeño para imitar a uno grande. Da modelos más baratos y rápidos que conservan buena parte de la capacidad en tareas concretas.

**Multimodal**  
*multimodal model*  
Modelo capaz de procesar más de un tipo de entrada: texto, imagen, audio o vídeo dentro de la misma conversación.

**Modelo de razonamiento**  
*reasoning model · chain of thought*  
Modelo entrenado para generar pasos intermedios antes de responder, gastando más cómputo en el problema. Mejora en matemáticas, lógica y programación a costa de latencia.


---

## 12. Inteligencia artificial: Construcción de aplicaciones

**Prompt**  
*instrucción o entrada*  
El texto que se envía al modelo. En una aplicación real no lo escribe la persona usuaria entero: se compone con instrucciones fijas, contexto recuperado y la petición concreta.

**System prompt**  
*instrucción de sistema*  
Instrucciones de máxima prioridad que fijan el papel, el tono y los límites del modelo durante toda la conversación. Es donde vive la política de la aplicación, no en cada mensaje.

**Zero-shot · few-shot**  
*sin ejemplos / con pocos ejemplos*  
Pedir la tarea directamente, o incluir dos o tres ejemplos resueltos en el propio prompt. Los ejemplos son la forma más barata de fijar formato y estilo sin reentrenar nada.

**Ingeniería de contexto**  
*context engineering*  
Decidir qué información entra en la ventana de contexto, en qué orden y con qué formato. Con modelos buenos, es donde está hoy la mayor parte del trabajo de calidad.

**RAG**  
*Retrieval-Augmented Generation — generación aumentada por recuperación*  
Buscar fragmentos relevantes en una base de conocimiento propia e incluirlos en el prompt para que el modelo responda sobre ellos. Es la forma habitual de trabajar con datos que el modelo no vio al entrenarse.

**Fragmentación**  
*chunking*  
Partir los documentos en trozos indexables por separado. El tamaño y el solapamiento condicionan la calidad de la recuperación más que casi cualquier otro ajuste.

**Base de datos vectorial**  
*vector database · pgvector*  
Almacén optimizado para buscar los vectores más cercanos a uno dado. PostgreSQL puede hacerlo con la extensión `pgvector`, sin añadir otro sistema.

**Reordenación**  
*reranking*  
Segunda pasada que reordena los fragmentos recuperados con un modelo más preciso y caro, para que lo mejor quede arriba. Suele mejorar más que ampliar el número de fragmentos.

**Uso de herramientas**  
*tool use · function calling*  
El modelo devuelve, en lugar de texto, la petición de ejecutar una función que tú defines, con sus argumentos. Tu código la ejecuta y le devuelve el resultado. Así consulta datos reales y actúa.

**Agente**  
*agent*  
Sistema en el que el modelo decide en bucle qué herramienta usar, observa el resultado y continúa hasta cumplir un objetivo. Se diferencia de un flujo fijo en que el camino no está predeterminado.

**MCP**  
*Model Context Protocol — protocolo de contexto de modelo*  
Protocolo abierto para conectar modelos con herramientas y fuentes de datos externas mediante servidores reutilizables, en vez de programar cada integración a medida.

**Salida estructurada**  
*structured output · JSON mode*  
Forzar que la respuesta cumpla un esquema definido, de modo que el programa pueda consumirla sin analizar texto libre. Es lo que hace fiable integrar un modelo en un backend.

**Barreras de protección**  
*guardrails*  
Comprobaciones alrededor del modelo —validación de esquema, filtros de contenido, listas de permitidos— que impiden que una salida inesperada llegue al usuario o dispare una acción.

**Inyección de prompt**  
*prompt injection*  
Ataque en el que texto de una fuente externa (una web, un documento, un correo) contiene instrucciones que el modelo obedece como si vinieran del usuario. La defensa es tratar todo lo leído como datos, nunca como órdenes.

**Caché de prompts**  
*prompt caching*  
Reutilizar el procesamiento de la parte fija del prompt entre llamadas. Reduce latencia y coste de forma notable cuando hay instrucciones largas repetidas.

**Streaming · TTFT**  
*Time To First Token — tiempo hasta el primer token*  
Entregar la respuesta token a token mientras se genera. No acelera el total, pero cambia por completo la percepción de velocidad; el TTFT es la métrica que de verdad se nota.


---

## 13. Inteligencia artificial: Evaluación y operación

**Eval**  
*evaluación sistemática*  
Conjunto de casos con resultado esperado que se ejecuta contra el sistema para medir si un cambio mejora o empeora. Es el equivalente a la suite de pruebas cuando la salida no es determinista.

**Benchmark**  
*prueba comparativa pública*  
Conjunto estandarizado de tareas para comparar modelos entre sí. Útil para orientarse, insuficiente para decidir: lo que importa es cómo rinde en tu caso concreto.

**LLM-as-a-judge**  
*modelo como evaluador*  
Usar un modelo para puntuar las respuestas de otro según una rúbrica. Escala mucho mejor que la revisión manual, pero hereda sesgos y necesita calibrarse contra juicios humanos.

**Conjunto de referencia**  
*ground truth · golden dataset*  
Casos con la respuesta correcta verificada por personas. Es el activo más valioso de un proyecto de IA y el que más cuesta construir.

**Precisión · exhaustividad · F1**  
*precision · recall · F1 score*  
Precisión: de lo devuelto, cuánto era correcto. Exhaustividad: de lo correcto que existía, cuánto se devolvió. F1 combina ambas; casi siempre hay que sacrificar una por la otra.

**Sobreajuste**  
*overfitting*  
El modelo memoriza los casos de entrenamiento y falla con los nuevos. Su equivalente al construir aplicaciones es afinar el prompt hasta que solo funciona con los ejemplos que probaste.

**Deriva**  
*drift*  
Degradación con el tiempo porque los datos reales o el comportamiento del modelo cambian. Obliga a reevaluar periódicamente, no solo al lanzar.

**Coste por token**  
*input / output pricing*  
Se factura por tokens de entrada y de salida, normalmente con la salida bastante más cara. Es lo que hace que el diseño del contexto sea también una decisión económica.

**Trazabilidad de LLM**  
*LLM observability · tracing*  
Registrar cada llamada con su prompt, su salida, su latencia y su coste. Sin esa traza es imposible diagnosticar por qué una respuesta salió mal.

**Red teaming**  
*pruebas adversarias*  
Atacar deliberadamente el sistema para encontrar formas de saltarse sus límites antes de que lo haga alguien más.

**Humano en el bucle**  
*human-in-the-loop*  
Diseño en el que una persona revisa o aprueba antes de que la salida tenga efecto. Es obligatorio de facto en cuanto la decisión afecta a la salud, al dinero o a los derechos de alguien.

**Reglamento Europeo de IA**  
*AI Act*  
Norma europea que clasifica los sistemas de IA por riesgo e impone obligaciones crecientes. Un sistema de IA aplicado al ámbito sanitario cae en la categoría de alto riesgo.

---

Las siglas se presentan desarrolladas en su idioma original y traducidas cuando existe
un término asentado en castellano. Las notas `En VitSync` indican dónde aparece cada
concepto en el proyecto, para poder ir del término al código.
