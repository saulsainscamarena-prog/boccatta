# Plan Tecnico: Audit Android backup data extraction rules

Spec ID: `backup-data`

## Lectura Inicial Obligatoria

- `AGENTS.md`
- `openspec/constitution.md`
- Archivos propietarios del flujo real antes de editar codigo.

## Punto de Entrada Real

Describe donde vive hoy el comportamiento y por que ese es el owner correcto.

- UI: N/A
- ViewModel: N/A
- Dominio/use case: N/A
- Repositorio/data: N/A
- DI: N/A
- Configuración de App: `AndroidManifest.xml` y recursos XML asociados. Son el estándar de Android para reglas de backup y data extraction.

## Archivos Probables

- `app/src/main/AndroidManifest.xml`
- `app/src/main/res/xml/backup_rules.xml`
- `app/src/main/res/xml/data_extraction_rules.xml`

## Estrategia

1. En `AndroidManifest.xml`, dentro de `<application>`, añadir `android:dataExtractionRules="@xml/data_extraction_rules"` y `android:fullBackupContent="@xml/backup_rules"`.
2. En `backup_rules.xml`, dentro del tag `<full-backup-content>`, añadir las exclusiones: `<exclude domain="file" path="logs"/>` y `<exclude domain="external" path="logs"/>`.
3. En `data_extraction_rules.xml`, dentro de `<cloud-backup>` y `<device-transfer>`, añadir las mismas exclusiones para logs.

## Contratos a Revisar

- Modelos: N/A
- Interfaces: N/A
- Koin: N/A
- Firestore/Room: Ya están excluidas las BD offline (`bocatta_offline.db`, `offline_queue.db`) en las reglas XML.
- WorkManager/sync: N/A

## Impacto Offline

- Venta online: N/A
- Venta offline: Los datos offline quedan protegidos localmente y no viajan con copias de seguridad del sistema o D2D.
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
- Gradle: Correr `.\gradlew.bat compileDebugKotlin` o `.\gradlew.bat assembleDebug` para asegurar que el Manifest y los XML no introducen errores de sintaxis o empaquetado.

## Criterio para No Continuar

Enumera condiciones que deben detener implementacion hasta aclarar alcance o datos.

- La validación del `<application>` arroja error porque algún atributo requiere un nivel específico de target API y no se configura correctamente, aunque targetSdk está en 36. No deberia pasar.
