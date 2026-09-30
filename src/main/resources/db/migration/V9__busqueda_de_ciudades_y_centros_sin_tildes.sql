-- El buscador de ciudades y centros comparaba literal: escribir "bogota" sin tilde
-- no encontraba "BOGOTÁ", porque upper() no normaliza acentos. Como casi nadie
-- escribe tildes en un buscador, cada ciudad con acento era, en la práctica,
-- imposible de encontrar sin saber exactamente cómo está escrita en el catálogo.
--
-- unaccent es una extensión estándar de PostgreSQL (viene en contrib, no hay que
-- instalar nada externo): quita diacríticos antes de comparar.
create extension if not exists unaccent;
