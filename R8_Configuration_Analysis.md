# R8 Configuration Analysis — Bocatta POS

## Configuración actual

| Parámetro | Valor |
|-----------|-------|
| `android.enableR8.fullMode` | `true` (en `gradle.properties`) |
| `isMinifyEnabled` (release) | `true` |
| AGP version | `9.2.1` (≥9, incluye optimizaciones R8 avanzadas) |

## Keep rules — evaluación

### 1. Atributos (líneas 10–14)

```
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses
-keepattributes EnclosingMethod
```

**Acción:** ✅ CONSERVAR. Necesarios para stack traces legibles y tipos genéricos.

---

### 2. `-keep class com.bocatta.pos.domain.model.** { *; }` (línea 17)

**Impacto:** ALTO — impide renombrar/eliminar cualquier clase o miembro en `domain.model`.

**Razonamiento:** Requerido porque Firestore `toObject()` usa reflection para mapear documentos a objetos Kotlin/data class.

**Acción:** ✅ CONSERVAR pero acotar. Se puede reemplazar por:

```
-keepclassmembers class com.bocatta.pos.domain.model.** {
    <fields>;
}
```

Esto permite renombrar las clases (reduciendo tamaño) pero mantiene los nombres de campos que Firestore necesita.

---

### 3. `-keep class com.google.firebase.** { *; }` y `-keep class com.google.android.gms.** { *; }` (líneas 20–21)

**Impacto:** ALTO — mantiene TODAS las clases de Firebase y Google Play Services sin ofuscar/shrink.

**Razonamiento:** ❌ **REDUNDANTE**. Las librerías de Google ya incluyen sus propias reglas consumer ProGuard en los AAR. R8 las aplica automáticamente.

**Acción:** ❌ ELIMINAR ambas líneas.

---

### 4. `-dontwarn com.google.firebase.**` y `-dontwarn com.google.android.gms.**` (líneas 22–23)

**Impacto:** BAJO — solo suprime warnings.

**Razonamiento:** Con Firebase SDK actual (BOM 34.12.0) ya no deberían aparecer warnings. Se pueden eliminar junto con las reglas anteriores.

**Acción:** ❌ ELIMINAR. Si aparecen warnings después de la limpieza, se pueden restaurar selectivamente.

---

### 5. `-keepclassmembers class com.bocatta.pos.** { @kotlinx.serialization.Serializable <fields>; }` (líneas 26–28)

**Impacto:** BAJO — solo mantiene campos anotados con `@Serializable`.

**Razonamiento:** El plugin de kotlinx.serialization ya genera reglas de ofuscación automáticas para cada clase anotada. Esta regla es un salvaguarda adicional.

**Acción:** ✅ CONSERVAR (costo mínimo, aporta seguridad extra).

---

### 6. `-dontwarn kotlin.**` (línea 29)

**Impacto:** NULO — solo suprime warnings.

**Razonamiento:** ❌ **REDUNDANTE**. Las librerías Kotlin estándar no producen warnings que necesiten supresión.

**Acción:** ❌ ELIMINAR.

---

### 7. `-keep class * { org.koin.core.annotation.Single *; org.koin.core.annotation.Factory *; }` (líneas 32–35)

**Impacto:** MEDIO — mantiene cualquier clase que tenga una anotación `@Single` o `@Factory` de Koin.

**Razonamiento:** El proyecto no usa declaración de módulos mediante anotaciones de Koin. Se usa el DSL tradicional `module { single { ... } }`, que no requiere reflection sobre las clases. Esta regla es innecesaria.

**Acción:** ❌ ELIMINAR.

---

## Plan de acción (ordenado por impacto)

| Orden | Regla | Acción | Impacto en tamaño |
|-------|-------|--------|-------------------|
| 1 | `-keep class com.google.firebase.**` | ❌ Eliminar | Alto |
| 2 | `-keep class com.google.android.gms.**` | ❌ Eliminar | Alto |
| 3 | `-dontwarn com.google.firebase.**` / `-dontwarn com.google.android.gms.**` | ❌ Eliminar | Nulo |
| 4 | `-dontwarn kotlin.**` | ❌ Eliminar | Nulo |
| 5 | `-keep class * { org.koin.core.annotation... }` | ❌ Eliminar | Medio |
| 6 | `-keep class com.bocatta.pos.domain.model.** { *; }` | ⚠️ Acotar a `-keepclassmembers` + `<fields>` | Bajo |

## Recomendación

> **ATENCIÓN:** No se debe confiar en el tamaño del APK solo tras la limpieza de reglas. Se debe ejecutar `./gradlew :app:assembleRelease` y comparar el tamaño del APK antes/después.
