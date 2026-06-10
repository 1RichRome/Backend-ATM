# Backend ATM Contratos BN

Backend Spring Boot para gestionar cajeros ATM, proveedores/arrendadores, contratos, vencimientos, actas mensuales de conformidad y documentos firmados.

## Stack
- Java 17
- Spring Boot 3.3.5
- Maven
- Spring Data JPA
- Spring Security + JWT
- MySQL
- Apache POI para generación DOCX
- OpenPDF / Lowagie para generación PDF
- Swagger/OpenAPI

## Base de datos
La aplicación crea la base si no existe:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/atm_contratos_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=America/Lima&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=richar
```

## Carpeta local de documentos
Por defecto, los archivos se guardan en tu computadora en:

```text
C:/BN_ATM_CONTRATOS
```

En `src/main/resources/application.properties` puedes cambiar:

```properties
app.storage-dir=C:/BN_ATM_CONTRATOS
app.upload-dir=${app.storage-dir}/uploads
app.actas-dir=${app.storage-dir}/actas
```

Estructura generada para actas:

```text
C:/BN_ATM_CONTRATOS/actas/{PROVEEDOR}/{CONTRATO}/{AÑO}/{MES}/ACTA_CONFORMIDAD_....docx
C:/BN_ATM_CONTRATOS/actas/{PROVEEDOR}/{CONTRATO}/{AÑO}/{MES}/ACTA_CONFORMIDAD_....pdf
C:/BN_ATM_CONTRATOS/actas/{PROVEEDOR}/{CONTRATO}/{AÑO}/{MES}/FIRMADAS/{archivo_firmado.pdf|docx}
```

## Ejecutar
```bash
mvn clean install
mvn spring-boot:run
```

Swagger:
```text
http://localhost:8080/swagger-ui/index.html
```

Usuario inicial:
```text
admin / admin123
```

## Funcionalidades principales
- Login JWT.
- CRUD de ATM.
- CRUD de proveedores.
- Enlace ATM -> proveedor.
- CRUD de contratos.
- Al registrar/editar contrato se selecciona proveedor y solo se asocian cajeros del proveedor seleccionado.
- Cálculo automático de vencimiento de contrato.
- Importación CSV/XLSX de ATM y contratos.
- Generación de actas DOCX y PDF por contrato, mes y año.
- Calendario anual de actas por contrato: Enero a Diciembre con estado PENDIENTE, GENERADA, FIRMADA, OBSERVADA o ANULADA.
- Descarga separada de DOCX y PDF.
- Subida de actas firmadas PDF/DOCX.
- Repositorio documental.
- Dashboard.
- Historial de eventos.

## Endpoints importantes

### ATM
```text
GET /api/atms?q=&proveedorId=
POST /api/atms
PUT /api/atms/{id}
POST /api/atms/importar
```

### Proveedores
```text
GET /api/proveedores
GET /api/proveedores/{id}/atms
GET /api/proveedores/{id}/contratos
```

### Contratos
```text
GET /api/contratos
POST /api/contratos
PUT /api/contratos/{id}
GET /api/contratos/{id}/actas/calendario?anio=2026
GET /api/contratos/{id}/actas
GET /api/contratos/{id}/documentos
```

### Actas
```text
POST /api/actas/generar
GET /api/actas/{id}/descargar
GET /api/actas/{id}/descargar-docx
GET /api/actas/{id}/descargar-pdf
POST /api/actas/{id}/subir-firmada
```

## Generación de actas
Al generar un acta mensual, el backend:

1. Valida contrato, mes y año.
2. Registra el acta en MySQL.
3. Crea la carpeta local por proveedor, contrato, año y mes.
4. Genera un DOCX editable.
5. Genera un PDF con formato de acta de conformidad.
6. Registra ambos documentos en el repositorio documental.
7. Permite anexar posteriormente el acta firmada.


## Corrección v4: apertura directa de PDFs

Los endpoints de descarga directa de actas y documentos PDF están permitidos para `GET` sin JWT, porque cuando se abre una URL directamente en Chrome/Edge el navegador no envía el header `Authorization: Bearer ...`.

Rutas principales:

- `GET /api/actas/{id}/ver-pdf` abre el PDF en el navegador.
- `GET /api/actas/{id}/descargar-pdf` también entrega el PDF con `Content-Disposition: inline`.
- `GET /api/actas/{id}/descargar-docx` descarga el DOCX.
- `GET /api/documentos/{id}/download` abre PDFs en navegador y descarga otros formatos.

El resto de endpoints CRUD sigue protegido con JWT.

## Cambios v5 - Actas mejoradas

- Se retiró del PDF/DOCX la línea de firma y sello del responsable.
- Se agregó logo institucional en la parte superior izquierda del acta. El archivo editable está en `src/main/resources/static/img/ic_banco.png`; puedes reemplazarlo por el logo oficial manteniendo el mismo nombre.
- El checklist de penalidades ya no calcula mora automáticamente por atraso en la generación. Ahora se pregunta antes de generar:
  - Adjunta informe de penalidades: NO por defecto.
  - Adjunta otro informe: NO por defecto.
  - Tiene penalidades por mora: NO por defecto.
  - Tiene otras penalidades: NO por defecto.
- Nuevo endpoint para generación por lote:
  - `POST /api/actas/generar-lote`
- El lote permite seleccionar un proveedor y varios ATM de ese proveedor. El sistema ubica los contratos relacionados y genera las actas respetando las fechas propias de cada contrato.

## Cambios v6

- Se agregó eliminación de actas desde `DELETE /api/actas/{id}`.
- Al eliminar un acta se eliminan sus archivos DOCX, PDF y firmados de la carpeta local.
- Al eliminar un contrato se eliminan sus actas, documentos asociados y relaciones con ATM.
- Al eliminar un documento firmado asociado a un acta, el acta vuelve a estado `GENERADA` y se limpia la referencia del archivo firmado.

## Versión v9 - Actas complejas por varios cajeros

Cambios incluidos:

- El contrato ahora tiene `incluyeIgv` (`incluye_igv` en MySQL) para que el acta muestre **con IGV** o **sin IGV**.
- El contrato ahora tiene `fechaSuscripcion` para mostrar la fecha de firma/suscripción de la adenda o contrato.
- La generación de actas ya no detalla direcciones en el primer párrafo cuando hay varios cajeros; ahora resume la cantidad de espacios/cajeros asociados.
- El monto mensual por espacio se calcula con `montoTotalMensual`; si no existe, usa `rentaMensual + otrosGastos`.
- El monto a pagar del mes, cuando no se envía manualmente, se calcula como `monto mensual por espacio * cantidad de cajeros incluidos`.
- El acta DOCX/PDF agrega una segunda hoja de anexo con una tabla de cajeros asociados: item, N° ATM, ubicación, distrito, provincia, total y dirección/SISCAL.

SQL recomendado si ya tenías una base de datos cargada:

```sql
ALTER TABLE contratos ADD COLUMN incluye_igv BIT DEFAULT 0;
ALTER TABLE contratos ADD COLUMN fecha_suscripcion DATE NULL;
UPDATE contratos SET incluye_igv = 0 WHERE incluye_igv IS NULL;
UPDATE contratos
SET incluye_igv = 1
WHERE LOWER(impuesto) LIKE '%incluido igv%'
   OR LOWER(impuesto) LIKE '%incluye igv%'
   OR LOWER(impuesto) LIKE '%con igv%'
   OR LOWER(impuesto) LIKE '%incluido impuesto%';
```

Si Hibernate está en `spring.jpa.hibernate.ddl-auto=update`, las columnas se pueden crear automáticamente al levantar el backend. El SQL anterior sirve para limpiar/actualizar datos existentes.

## Actualización v10 - Tabla contable del acta

Se agregaron campos de combinación contable al contrato para separar Renta y Gastos Comunes:

- cuentaContableRenta / cuentaContableGastoComun
- centroCostoRenta / centroCostoGastoComun
- areaPresupuestoRenta / areaPresupuestoGastoComun
- tipoGastoRenta / tipoGastoGastoComun

El acta ahora genera una tabla de 3 columnas: concepto, Renta y Gastos Comunes. La fila Total se calcula automáticamente con:

- Total Renta = rentaMensual × cantidad de cajeros incluidos en el acta.
- Total Gastos Comunes = otrosGastos × cantidad de cajeros incluidos en el acta.

Para bases existentes, revisar `sql/v10_combinacion_contable_contratos.sql`.
