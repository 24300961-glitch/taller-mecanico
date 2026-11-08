-- Retiro total del módulo de citas (decisión 21): la tabla estaba vacía y el permiso
-- CITAS_GESTIONAR desapareció del enum Permiso, por lo que sus filas deben borrarse
-- antes de que Hibernate lea los roles (EnumType.STRING fallaría con valores huérfanos).

DELETE FROM rol_permisos WHERE permiso = 'CITAS_GESTIONAR';

DROP TABLE IF EXISTS citas;
