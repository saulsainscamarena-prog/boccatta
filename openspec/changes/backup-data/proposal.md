# Proposal: Audit Android backup data extraction rules

Spec ID: `backup-data`
Capability: `Data Security and Local Storage`
Strict TDD: `false`

## Problema

- `AndroidManifest.xml` declara `android:allowBackup="false"`, pero no especifica las reglas explícitas de `android:dataExtractionRules` (requerido para Android 12+) ni `android:fullBackupContent`. Esto puede resultar en que transferencias dispositivo-a-dispositivo migren accidentalmente datos que no deseamos en Android 12+, como los archivos generados en el almacenamiento interno de la app.
- `LogHelper.kt` genera diagnósticos operativos en la carpeta `logs/` dentro de `filesDir` o `externalFilesDir`. Estos logs contienen JSON serializers, stacktraces, y operaciones de POS que se consideran datos sensibles o auditables. Actualmente, la carpeta de logs no está excluida en `backup_rules.xml` ni `data_extraction_rules.xml`.
- Las bases de datos offline de Room (ventas pendientes, etc.) y Session SharedPreferences sí están presentes en los archivos de exclusión, pero sin el vínculo en el Manifest, podrían ignorarse en algunos OEMs.

## Alcance

- Añadir `android:fullBackupContent="@xml/backup_rules"` y `android:dataExtractionRules="@xml/data_extraction_rules"` al elemento `<application>` en `AndroidManifest.xml`.
- Añadir la exclusión de la carpeta de logs a `backup_rules.xml` y `data_extraction_rules.xml` (`<exclude domain="file" path="logs"/>` y `<exclude domain="external" path="logs"/>` por si acaso).

## Fuera de Alcance

- Modificar la lógica de serialización JSON en `OfflineDatabase` o la escritura de archivos en `LogHelper.kt` y `LogCleanupWorker.kt`.

## Archivos Afectados Esperados

- `app/src/main/AndroidManifest.xml`
- `app/src/main/res/xml/backup_rules.xml`
- `app/src/main/res/xml/data_extraction_rules.xml`

## Riesgos

- Ventas: N/A
- Inventario: N/A
- Offline/sync: N/A
- Caja/pagos: N/A
- UI: N/A
- Seguridad: Riesgo disminuido al asegurar que los logs operativos no se extraen mediante D2D transfers ni ADB backups si se intentan habilitar.

## Research Externo

- Web requerida: no
- Pregunta investigada: Comportamiento de dataExtractionRules en API 31+.
- Fuentes primarias: Android Developer Docs (conocimiento base).
- Decision tomada: Declarar explícitamente en el Manifest e incluir la exclusión de la carpeta de logs.
- Suposicion sensible a version: Android 12 (API 31) cambia el comportamiento del atributo allowBackup respecto a transferencias de dispositivo (D2D).

## Estrategia de Rollback

- Revertir los tres archivos XML modificados usando git checkout.

## Criterios de Exito

- [ ] `AndroidManifest.xml` incluye atributos de backup.
- [ ] `backup_rules.xml` excluye las rutas de `logs`.
- [ ] `data_extraction_rules.xml` excluye las rutas de `logs`.

## Checkpoint Humano

No pasar a `spec.md` y `design.md` hasta que esta propuesta este aceptada o ajustada.
