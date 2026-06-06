# Web Research Policy

Esta politica evita dos fallos opuestos: agentes que programan con conocimiento viejo y agentes que queman tokens navegando sin una pregunta concreta.

## Principio

El orden de contexto es:

1. Codigo local y `AGENTS.md`.
2. `openspec/constitution.md`, `openspecfig.yaml` y `openspec/skill-registry.yaml`.
3. Specs activas en `openspec/changes/<id>/`.
4. Fuentes externas de `openspec/source-catalog.yaml`, solo si el dato es externo, cambiante o incierto.

## Cuando Si Buscar Web

Busca web cuando:

- La informacion puede haber cambiado: Firebase, Android SDK, Compose, Gradle, Kotlin, Koin, Play Store, permisos, politicas, release notes o APIs externas.
- Hay un error de build/runtime cuyo mensaje apunta a una version, issue conocido o cambio de comportamiento.
- La spec requiere una decision tecnica con tradeoffs que no se resuelve leyendo el repo.
- La tarea toca seguridad, privacidad, pagos, permisos, autenticacion, backup o publicacion.
- Se necesita confirmar documentacion oficial antes de instalar, actualizar o configurar una dependencia.

## Cuando No Buscar Web

No busques web cuando:

- El comportamiento esta definido por codigo local, reglas de negocio de Bocatta o specs existentes.
- El cambio es cosmetico, texto interno o ajuste de layout sin dependencia externa.
- Ya hay un patron local claro.
- La pregunta es sobre archivos concretos del repo y no sobre APIs externas.

## Presupuesto de Research

Antes de buscar, escribe la pregunta exacta:

```text
Research question:
Source category:
Expected decision:
Stop condition:
```

Limites por defecto:

- Normal: 2 a 4 fuentes oficiales, maximo 10 minutos.
- Critico: 4 a 6 fuentes oficiales o primarias, maximo 20 minutos.
- No usar mas de 1 fuente secundaria salvo para contexto, nunca como autoridad principal.

## Jerarquia de Fuentes

1. Documentacion oficial y release notes.
2. Repositorios oficiales, issues del proveedor y changelogs.
3. Articulos tecnicos reconocidos solo para contexto.
4. Stack Overflow, Reddit y blogs personales solo como pistas, no como fuente final.

## Evidencia Minima

En `research.md` registra:

- URLs consultadas.
- Fecha de consulta.
- Version o rango aplicable.
- Decision tomada.
- Dato que sigue incierto.

En `proposal.md` registra cualquier suposicion externa que pueda romperse con una version futura.

## Regla de Cierre

Cuando dos fuentes primarias actuales respondan la pregunta, deja de buscar y vuelve al repo. Si las fuentes se contradicen, documenta la contradiccion y prioriza la fuente oficial mas cercana al producto afectado.
