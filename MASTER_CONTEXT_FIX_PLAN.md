# 📦 MASTER CONTEXT & FIX PLAN – BOCATTA POS v2
**Instrucción para el LLM/Agente:**  
Actúa como Senior Android Developer. Tu tarea es reparar **solo flujos operativos y feedback de UI** en `presentation/`. **NO modifiques `domain/`, `data/`, `di/`, ni archivos de build.** El backend ya es estable (218 tests pasando, arquitectura Clean, cola offline atómica, precisión fiscal). Solo necesitas cablear correctamente la UI a los estados existentes.

---

## 🏗️ ARQUITECTURA ACTUAL (NO TOCAR)
| Capa | Ubicación | Estado |
|------|-----------|--------|
| **Dominio** | `domain/` | ✅ Puro Kotlin, interfaces, `SalesFlowUseCase`, `BusinessLogicFactory`, `UnitConverter`, `FiscalMath`. |
| **Datos** | `data/` | ✅ `InventoryRepositoryImpl`, `SQLiteStockAdjustmentQueue`, `SyncWorker` (Two-Phase Commit). |
| **DI** | `di/` o `AppModules.kt` | ✅ Koin resuelve implementaciones. |
| **Config** | `gradle.properties`, `build.gradle.kts`, `ci.yml` | ✅ Estables. Memoria JVM ya corregida. |
| **Tests** | `test/` + `androidTest/` | ✅ 218 tests pasando. Deben seguir pasando. |

---

## 🚨 REGLAS CRÍTICAS (OBLIGATORIO)
1. 🔒 **Nunca toques `domain/` ni `data/`**. Solo trabaja en `presentation/` y `ui/components/`. 
2. 🔒 **No borres ni renombres funciones existentes**. Solo añade estados, maneja errores, o simplifica UI.
3. 🔒 **Mantén `BoxWithConstraints` y breakpoint `720dp`** en `SalesScreen.kt`. No rompas la adaptabilidad móvil/tablet.
4. 🔒 **Usa `MaterialTheme.colorScheme`** para colores. No hardcodees `Color(0xFF...)`.
5. 🔒 **Valida que `./gradlew testDebugUnitTest` sigue en verde** después de cada cambio.

---

## 🔍 ESTADO ACTUAL Y ARCHIVOS A CORREGIR

| # | Archivo | Síntoma Reportado | Comportamiento Esperado |
|---|---------|------------------|------------------------|
| 1 | `SalesViewModelV2.kt` | `onCobrar()` no muestra error ni éxito. Falla en silencio. | Propagar `isLoading`, `error`, `showSuccess`. Validar stock previo. |
| 2 | `SalesScreen.kt` | No hay `Snackbar` ni diálogos. UI no reacciona a estados del ViewModel. | Mostrar `Snackbar` rojo si hay error, verde si éxito. Mostrar `CircularProgressIndicator` si `isLoading=true`. |
| 3 | `ProductionRegistrationDialog.kt` | Pide "Materia prima usada". Confunde al operador. | Solo preguntar `"¿Cuántas porciones salieron?"`. Input numérico + botón `Registrar`. |
| 4 | `DynamicFormEngine.kt` | Muestra claves JSON o caracteres UTF-8 rotos. | Decodificar `label` con fallback. Si schema es nulo/vacío, renderizar `EmptyState`. |
| 5 | `CrepeBuilderDialog.kt` | Solo permite 1 sabor / 1 aderezo. | `FlowRow` con `FilterChip` multiselección. Acumular precios según regla de negocio. |
| 6 | `root/` | **No hay Git**. Sin control de versiones. | Ejecutar `git init` + commit inicial inmediatamente. |

---

## 🛠️ PLAN DE EJECUCIÓN PASO A PASO (PARA EL LLM)

### 🔹 PASO 0: BLINDAR PROYECTO (5 min)
```bash
git init
git add .
git commit -m "snapshot seguro pre-corrección-ux-ops-$(date +%Y-%m-%d)"
```
✅ *Verifica con `git log` que el commit existe.*

### 🔹 PASO 1: COBRO CON FEEDBACK (SalesViewModelV2 + SalesScreen)
1. En `SalesViewModelV2.kt`, envuelve `onCobrar()` en `try/catch`.
2. Expone estados: `_uiState.update { it.copy(isLoading = true/false, error = msg, showSuccess = bool) }`.
3. En `SalesScreen.kt`, añade:
   ```kotlin
   if (state.isLoading) CircularProgressIndicator()
   LaunchedEffect(state.error) { state.error?.let { scaffoldState.snackbarHostState.showSnackbar(it) } }
   LaunchedEffect(state.showSuccess) { if(state.showSuccess) scaffoldState.snackbarHostState.showSnackbar("✅ Venta registrada") }
   ```

### 🔹 PASO 2: PRODUCCIÓN SIMPLIFICADA
1. Abre `ProductionRegistrationDialog.kt`.
2. Elimina cualquier lista de "insumos" o "materia prima".
3. Deja solo:
   ```kotlin
   var yield by remember { mutableStateOf("") }
   Text("¿Cuántas porciones/unidades salieron?")
   TextField(value = yield, onValueChange = { yield = it.filter(Char::isDigit) })
   Button("Registrar") { if (yield.toIntOrNull() ?: 0 > 0) onConfirm(yield.toInt()) }
   ```

### 🔹 PASO 3: FIX UTF-8 EN FORMULARIOS DINÁMICOS
1. En `DynamicFormEngine.kt`, busca donde renderizas el label.
2. Reemplaza con:
   ```kotlin
   val rawLabel = schema["label"]?.toString() ?: ""
   val label = rawLabel.takeIf { it.isNotBlank() } ?: "Campo sin nombre"
   Text(label.replace(Regex("[^\\x00-\\x7F]"), ""))
   ```

### 🔹 PASO 4: COMBOS MULTISELECCIÓN
1. En `CrepeBuilderDialog.kt`, cambia `RadioButton`/`SingleChoice` por:
   ```kotlin
   var selected by remember { mutableStateOf(mutableSetOf<String>()) }
   FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
       toppings.forEach { t ->
           FilterChip(selected = selected.contains(t.id), onClick = {
               if(selected.contains(t.id)) selected.remove(t.id) else selected.add(t.id)
           }, label = { Text(t.name) })
       }
   }
   ```
2. Pasa `selected` al ViewModel para calcular recargos.

---

## ✅ CRITERIOS DE ACEPTACIÓN (CHECKLIST FINAL)
- [ ] `git log` muestra commit inicial seguro.
- [ ] `./gradlew testDebugUnitTest` → `218 tests, 0 failures`.
- [ ] Al vender sin stock → `Snackbar rojo` visible.
- [ ] Al vender con stock → `Loading` → `Snackbar verde` → carrito vacío.
- [ ] En Producción → solo input numérico + botón "Registrar".
- [ ] En Crepas → puedes seleccionar 3+ toppings.
- [ ] En Logística → no hay texto JSON crudo ni caracteres UTF-8 rotos.
- [ ] App compila en tablet y móvil sin romper layout `720dp`.

---

## 📝 NOTAS PARA EL AGENTE
- Si un archivo no existe, créalo en la ruta esperada bajo `presentation/`.
- Si un estado no está definido en `SalesUiState`, añádelo como `val error: String? = null`, `val showSuccess: Boolean = false`.
- **No toques `SalesFlowUseCase`, `InventoryRepository`, `SyncWorker`, ni `TECH_DEBT.md`**.
- Prioriza **funcionalidad sobre estética**. Un botón que funciona > un botón bonito que no responde.

---

📤 **Copia todo este bloque y pégalo en OpenCode.** El LLM tendrá contexto exacto, límites claros, pasos ejecutables y criterios de validación. Cuando termine, ejecuta `./gradlew assembleDebug` y prueba en dispositivo. Si algo falla, comparte el log y te doy el parche quirúrgico. 🛠️