# Investigación: Extracción de Strings a strings.xml (i18n)

## Contexto
El usuario ha identificado más de 500 strings "hardcodeados" en archivos Jetpack Compose (e.g. `Text("...")`). Esto viola las buenas prácticas de internacionalización (i18n) y dificulta el mantenimiento.

## Problema
Modificar manualmente >500 líneas en múltiples archivos `.kt` es un proceso propenso a errores y extremadamente lento.

## Investigación de Soluciones
1. **Manual / Search & Replace:** Muy lento, riesgo de romper código si se reemplazan strings dentro de anotaciones o variables no relacionadas a UI.
2. **Script de Python (AST / Regex):** Crear un script automatizado que escanee archivos `.kt` en `app/src/main/java/com/bocatta/pos/presentation/ui/`, busque el patrón `Text("...")` o etiquetas similares, extraiga el valor, genere un key (e.g. `sales_text_...`), lo inyecte en `strings.xml`, y reemplace la llamada en Kotlin por `stringResource(R.string.key)`.
3. **Refactorización con Android Studio CLI:** Lento e interactivo.

## Conclusión
La vía más eficiente, alineada con el rol de Agente, es desarrollar un script en Python/Kotlin local que automatice la extracción. Este script será verificado antes de ejecutar un reemplazo masivo.
