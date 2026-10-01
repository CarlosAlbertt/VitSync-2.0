# ADR-0006: Almacenamiento de ficheros en S3-compatible con URLs prefirmadas

## Estado

**Aceptado** — 2026-09-12

## Contexto

v1 guardaba avatares e imágenes en el disco local del servidor. En Render (y en cualquier
plataforma de contenedores) **el disco es efímero**: cada despliegue, reinicio o escalado
borra lo subido. Era una pérdida de datos silenciosa, no un fallo visible.

v2 añade además informes médicos con adjuntos: PDF de analíticas, imágenes de diagnóstico.
Eso son datos de salud, y su almacenamiento tiene los mismos requisitos del Art. 32 que las
columnas cifradas del [ADR-0005](0005-cifrado-en-reposo-y-blind-index.md).

## Opciones consideradas

1. **Disco del servidor** (lo de v1).
   - `−` Efímero. Descartado sin más discusión.

2. **`bytea` en PostgreSQL.**
   - `+` Transaccional: el fichero y su fila viven o mueren juntos, y la copia de seguridad
     es una sola.
   - `−` Infla la base de datos y cada copia de seguridad; los `SELECT *` se vuelven caros;
     servir el fichero obliga a cargarlo entero en memoria de la aplicación.

3. **Objeto en S3 (o compatible: R2, MinIO) con metadatos en PostgreSQL** (elegida).
   - `+` Es la solución estándar y la que espera ver un revisor.
   - `+` La descarga no pasa por la API: sin consumo de memoria ni timeouts.
   - `−` Deja de ser transaccional: puede quedar un objeto sin fila, o una fila sin objeto.
   - `−` Requiere un servicio más en el entorno local.

## Decisión

Los ficheros viven en un **bucket S3-compatible privado**. En la base de datos se guardan
**solo metadatos**: `storage_key`, `filename`, `content_type`, `size_bytes` y
`checksum_sha256` (tabla `report_files`; para fotos de perfil, la columna `photo_key`).
**Nunca una URL**: la URL se construye al vuelo y caduca.

**En local, MinIO** vía `docker-compose`, con la misma API S3 que producción: no hay una
implementación distinta para desarrollo y otra para producción.

El acceso se abstrae tras una interfaz `StorageService` (`put` / `presignedGet` / `delete`),
de modo que cambiar de proveedor sea cambiar una implementación.

**Flujo de subida** (el fichero no atraviesa la API):

1. `POST /api/v1/reports/{id}/files:presign` — el backend valida permisos y devuelve una URL
   prefirmada de subida más el `storage_key`.
2. El cliente sube **directamente** al bucket.
3. `POST /api/v1/reports/{id}/files` — el backend verifica tamaño, **tipo MIME real detectado
   con Tika sobre los primeros bytes** (nunca el `Content-Type` que declara el cliente) y
   checksum, y entonces crea la fila.

**Flujo de descarga**: `GET /api/v1/files/{publicId}` comprueba la autorización, audita el
acceso y responde `302` a una URL prefirmada de **60 segundos**.

El nombre del objeto lo genera el servidor; el nombre original del fichero se guarda como
metadato y nunca se usa como clave.

## Consecuencias

- `+` Los ficheros sobreviven a los despliegues.
- `+` La API no gasta memoria ni tiempo sirviendo binarios.
- `+` El bucket privado más URLs de vida corta impide compartir un enlace permanente a un
  informe médico.
- `+` La detección de MIME por contenido cierra la subida de un ejecutable disfrazado de PDF.
- `−` **Pérdida de transaccionalidad**: si el paso 3 nunca llega, queda un objeto huérfano en
  el bucket. Hace falta una tarea periódica que borre los objetos sin fila con más de 24 h.
  Al revés no puede ocurrir: la fila solo se crea tras confirmar el objeto.
- `−` Un servicio más que levantar en local y que configurar en cada entorno
  (`S3_ENDPOINT`, `S3_BUCKET`, `S3_ACCESS_KEY`, `S3_SECRET_KEY`).
- `−` **A vigilar**: el borrado RGPD tiene que alcanzar también al bucket, no solo a la base
  de datos. Se trata en el flujo de supresión (Fase 6.5).
- `−` **A vigilar**: el objeto se guarda tal cual, sin cifrar por la aplicación. Se delega en
  el cifrado del lado del servidor del proveedor. Si se quisiera cifrado extremo a extremo
  como el de las columnas, habría que cifrar antes de subir y perder la subida directa.
