-- Versión v10: campos de combinación contable por contrato para tabla de actas
-- Ejecutar solo si Hibernate no crea las columnas automáticamente.

ALTER TABLE contratos ADD COLUMN cuenta_contable_renta VARCHAR(255) DEFAULT '4513.01.10.01';
ALTER TABLE contratos ADD COLUMN centro_costo_renta VARCHAR(255) DEFAULT '5500';
ALTER TABLE contratos ADD COLUMN area_presupuesto_renta VARCHAR(255) DEFAULT '5500';
ALTER TABLE contratos ADD COLUMN tipo_gasto_renta VARCHAR(255) DEFAULT '000000000000';

ALTER TABLE contratos ADD COLUMN cuenta_contable_gasto_comun VARCHAR(255) DEFAULT '4513.01.20.04';
ALTER TABLE contratos ADD COLUMN centro_costo_gasto_comun VARCHAR(255) DEFAULT '5500';
ALTER TABLE contratos ADD COLUMN area_presupuesto_gasto_comun VARCHAR(255) DEFAULT '5500';
ALTER TABLE contratos ADD COLUMN tipo_gasto_gasto_comun VARCHAR(255) DEFAULT 'SE000000251';

UPDATE contratos SET cuenta_contable_renta = '4513.01.10.01' WHERE cuenta_contable_renta IS NULL OR cuenta_contable_renta = '';
UPDATE contratos SET centro_costo_renta = '5500' WHERE centro_costo_renta IS NULL OR centro_costo_renta = '';
UPDATE contratos SET area_presupuesto_renta = '5500' WHERE area_presupuesto_renta IS NULL OR area_presupuesto_renta = '';
UPDATE contratos SET tipo_gasto_renta = '000000000000' WHERE tipo_gasto_renta IS NULL OR tipo_gasto_renta = '';

UPDATE contratos SET cuenta_contable_gasto_comun = '4513.01.20.04' WHERE cuenta_contable_gasto_comun IS NULL OR cuenta_contable_gasto_comun = '';
UPDATE contratos SET centro_costo_gasto_comun = '5500' WHERE centro_costo_gasto_comun IS NULL OR centro_costo_gasto_comun = '';
UPDATE contratos SET area_presupuesto_gasto_comun = '5500' WHERE area_presupuesto_gasto_comun IS NULL OR area_presupuesto_gasto_comun = '';
UPDATE contratos SET tipo_gasto_gasto_comun = 'SE000000251' WHERE tipo_gasto_gasto_comun IS NULL OR tipo_gasto_gasto_comun = '';
