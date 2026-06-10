-- Ejecutar solo si tu base actual no tiene estas columnas.
-- Si Spring Boot con ddl-auto=update ya las creó, puedes omitir los ALTER TABLE.

ALTER TABLE contratos ADD COLUMN incluye_igv BIT DEFAULT 0;
ALTER TABLE contratos ADD COLUMN fecha_suscripcion DATE NULL;

UPDATE contratos
SET incluye_igv = 0
WHERE incluye_igv IS NULL;

UPDATE contratos
SET incluye_igv = 1
WHERE LOWER(impuesto) LIKE '%incluido igv%'
   OR LOWER(impuesto) LIKE '%incluye igv%'
   OR LOWER(impuesto) LIKE '%con igv%'
   OR LOWER(impuesto) LIKE '%incluido impuesto%';
