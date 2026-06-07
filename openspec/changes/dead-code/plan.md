# Plan Tecnico: Dead Code & Redundant Configurations Audit

Spec ID: `dead-code-001`

## Lectura Inicial Obligatoria

- `AGENTS.md`
- `openspec/constitution.md`
- Archivos propietarios del flujo real antes de editar codigo (en este caso, `app/proguard-rules.pro` y el reporte de Lint).

## Punto de Entrada Real

El "comportamiento" reside en el proceso de build (Lint y R8).
- UI: N/A
- ViewModel: N/A
- Dominio/use case: N/A
- Repositorio/data: N/A
- DI: N/A
- Tests: N/A

## Archivos Probables

- `app/proguard-rules.pro`
- `app/build/reports/lint-results-debug.xml` o equivalente.
- Clases fuentes y XMLs reportados.

## Estrategia

1. Ejecutar `./gradlew lintDebug` para forzar la recolección de warnings sobre `UnusedResources` y `UnusedDeclaration`.
2. Analizar `app/proguard-rules.pro` manualmente:
   - Identificar si `com.bocatta.pos.domain.model.**` requiere keep absoluto de campos, o si `@Keep` o `@Serializable` es suficiente.
   - Revisar si Koin requiere reglas adicionales.
3. Extraer y filtrar los reportes de Lint.
4. Generar un listado de recomendaciones y reportar al usuario. No aplicar cambios de código.

## Contratos a Revisar

- Modelos: Validar por qué los modelos de dominio están excluidos completamente de la ofuscación en Firestore (`toObject()`).
- Interfaces: N/A
- Koin: N/A
- Firestore/Room: N/A
- WorkManager/sync: N/A

## Impacto Offline

- Venta online: N/A
- Venta offline: N/A
- Reconexion: N/A
- Deduccion de inventario: N/A
- Idempotencia/reintentos: N/A

## Impacto UI

- Movil: N/A
- Tablet: N/A
- Estados de carga/error: N/A
- Eventos consumibles: N/A
- Accesibilidad: N/A

## Plan de Pruebas

- Unit: N/A
- Instrumented: N/A
- Compose UI: N/A
- Maestro/manual: N/A
- Gradle: Verificar que tras aplicar la limpieza futura, el app compile y ofusque correctamente con `assembleRelease`.

## Criterio para No Continuar

- Fallo persistente en el daemon de Gradle al intentar ejecutar Lint (ya mitigado si lo corremos offline).
- Si el usuario solicita modificar el código en este issue, deberemos crear un PR separado.
