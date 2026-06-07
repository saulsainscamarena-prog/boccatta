# Plan de Implementación: Extracción de Strings

## Fases del Trabajo

1. **Desarrollo del Script (Dry-Run)**
   - Escribir `extract_strings.py` en la carpeta de scratch del proyecto.
   - Ejecutar el script en modo "Sólo lectura" para escanear `SalesScreen.kt` y `CarritoPanelV2.kt` primero como prueba de concepto.
   
2. **Revisión de Salida**
   - Inspeccionar los strings identificados.
   
3. **Ejecución (Aplicación de Cambios)**
   - Correr el script en modo de escritura (`--apply`).
   - El script modificará los archivos `.kt` e inyectará los recursos en `strings.xml`.
   
4. **Verificación Local**
   - Ejecutar `.\gradlew.bat compileDebugKotlin` o `lintDebug` para asegurar integridad.
   
5. **Commit**
   - `git add .` y `git commit -m "Refactor: Extract hardcoded strings to strings.xml via automation"`
