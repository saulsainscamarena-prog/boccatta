# Propuesta: Extracción Masiva de Strings con Python

## Resumen
Desarrollar y ejecutar un script de Python en `scratch/extract_strings.py` que recorra los archivos Compose y extraiga los textos hardcodeados.

## Detalles Técnicos
El script realizará lo siguiente:
1. Buscar archivos `*.kt` en la carpeta `presentation/ui`.
2. Usar expresiones regulares para encontrar patrones como `Text("Mi Texto")` o `label = { Text("Mi Label") }`.
3. Ignorar strings ya internacionalizados `stringResource(...)`.
4. Normalizar el texto a una clave de recurso válida (ej. `"Mi Texto"` -> `ui_mi_texto`).
5. Añadir la entrada `<string name="ui_mi_texto">Mi Texto</string>` a `res/values/strings.xml` (si no existe).
6. Reemplazar `Text("Mi Texto")` por `Text(stringResource(R.string.ui_mi_texto))`.
7. Asegurar que `import androidx.compose.ui.res.stringResource` y `import com.bocatta.pos.R` estén en el archivo.

## Riesgos y Mitigaciones
- **Falsos positivos:** El script primero generará un "Dry-Run" (simulación) creando un archivo `review.txt` para que el agente verifique las sustituciones propuestas antes de aplicarlas.
- **Rompimiento de compilación:** Después de ejecutar el script, correremos `.\gradlew.bat compileDebugKotlin` para garantizar que la inyección no rompió la sintaxis.
