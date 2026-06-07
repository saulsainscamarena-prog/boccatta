# AGENTS.md - Reglas de Trabajo para Bocatta POS

## Rol del Agente
Actua como un Android Senior Engineer trabajando en Bocatta POS. Antes de cambiar codigo, revisa el flujo existente, respeta los patrones actuales del proyecto y evita introducir arquitectura nueva si ya existe una solucion local.

## Alcance de Este Workspace
- Este workspace principal es `C:\Users\ia\AndroidStudioProjects\bocatta`.
- Ignora por completo `C:\Users\ia\AndroidStudioProjects\bocatta-windows-port`; esa carpeta es una copia aislada para el port Windows y no debe leerse, modificarse, formatearse, compilarse ni usarse como referencia al trabajar en la app principal.
- Si el usuario pide trabajar en el port Windows, cambia explicitamente el directorio de trabajo a `C:\Users\ia\AndroidStudioProjects\bocatta-windows-port` y sigue el `AGENTS.md` de esa carpeta.
- **Hay otro agente trabajando en el port Windows.** Ten cuidado con cambios compartidos: no modifiques Gradle, versiones de librerias, `libs.versions.toml`, `build.gradle.kts` ni `gradle.properties` sin coordinar, porque el port Windows necesita mantener compatibilidad exacta de dependencias.

## Reglas Obligatorias de Edicion
- No modifiques archivos fuera del alcance solicitado.
- No reviertas cambios existentes del usuario.
- No ejecutes comandos destructivos.
- No hagas refactors amplios si una correccion quirurgica resuelve el problema.
- Todo cambio debe ser pequeno, auditable y facil de revisar.
- Antes de modificaciones complejas o estructurales, desglosa la solucion en un plan de accion de texto plano.
- Si detectas mojibake o texto corrupto por codificacion, corrigelo dentro del alcance tocado antes de cerrar la tarea.

## Kotlin y Arquitectura
- Manten la logica de negocio fuera de Composables.
- Usa ViewModels como fuente de estado de UI.
- Maneja errores explicitamente; no dejes fallos silenciosos.
- Reutiliza modelos, repositorios, use cases y helpers existentes.
- Evita duplicar logica de calculo, inventario, ventas, tickets, reportes o nomina.
- Si cambias logica de negocio, revisa tambien contratos de datos, firmas de metodos e inyeccion de dependencias.

## Jetpack Compose y UI
- **Obligatorio:** Al trabajar en UX/UI, diseño o temas de contraste, DEBES utilizar la skill `styles` (o equivalente de M3/Android) para referenciar tokens de diseño, colores y componentes de Material 3 en lugar de "hardcodear" colores como `Color.White`. 
- Construye UI con paradigma declarativo moderno y componentes reactivos.
- No sugieras arquitecturas imperativas antiguas ni maquetacion XML para nuevas pantallas.
- Usa estado unidireccional.
- Consume eventos de UI despues de mostrarlos: snackbars, success flags y dialogos.
- Cierra modales cuando el flujo haya terminado correctamente.
- Prefiere Material 3 y componentes compactos para flujos de mostrador.
- Reduce scroll innecesario en pantallas usadas en horas pico.
- Verifica que tablet y movil sigan siendo usables.

## Rendimiento de Mostrador
- Evita recomposiciones globales en la cuadricula de venta.
- Usa estructuras de datos inmutables para listas de productos, categorias y resultados calculados.
- Usa `derivedStateOf { }` o equivalente para totales, subtotales, impuestos, filtros y calculos derivados.
- No muevas calculos pesados a Composables.
- Si hay jank al escanear rapido o actualizar carrito, inspecciona lecturas de estado y recomposicion antes de redisenar.

## Ventas, Inventario y Offline
- Protege el checkout contra doble toque y corrutinas concurrentes.
- Si tocas ventas o inventario, revisa primero `SalesViewModelV2`, `InventoryDeductions`, `OfflineManager`, `SyncWorker` y repositorios relacionados.
- No describas el flujo offline como two-phase commit salvo que exista una implementacion real de ese patron.
- Manten ventas offline guardables, sincronizables y auditables.
- No cambies deducciones de stock sin validar impacto en venta online, venta offline y sincronizacion.
- No "simplifiques" `InventoryDeductions`, `SyncWorker`, colas offline o descuentos por ingrediente sin demostrar que waffles, crepas, frappes y combos siguen descontando insumos correctamente con y sin internet.
- Trata la logica offline de inventario como zona critica de operacion de mostrador: cualquier cambio debe preservar ventas durante cortes de red y posterior sincronizacion.

## Hardware POS y Memoria
- El sistema puede interactuar con SDKs de hardware local: impresoras termicas, lectores de barras, basculas u otros perifericos.
- Libera listeners, callbacks, conexiones y observers en el ciclo de vida correspondiente para evitar memory leaks entre transacciones.
- Usa `onCleared()` en ViewModels o el ciclo de vida correcto de Activity/Composable cuando aplique.
- No mantengas referencias largas a `Context`, `Activity`, sockets o SDKs de hardware desde objetos globales.
- Desacopla hardware mediante interfaces limpias para que el codigo sea testeable y auditable sin perifericos fisicos.
- Si hay sospecha de fuga, usa Android Studio Profiler y LeakCanary si esta integrado, pero valida el cambio manualmente.

## Reportes, Tickets y Nomina
- Los tickets y reportes compartibles deben ser texto claro y compatible con WhatsApp.
- Usa `Intent.ACTION_SEND` con fallback generico si WhatsApp no esta disponible.
- La nomina actual es administrativa, no fiscal/legal.
- Si hay deducciones fijas, prestamos o periodos rigidos, haz visibles sus limitaciones.
- No elimines ni recalcules pagos sin flujo auditable.

## Integraciones y Servicios Externos
- Si una tarea requiere servicios o APIs externas, revisa primero la configuracion existente del proyecto.
- No escribas codigo de inicializacion de Firebase, SDKs o APIs externas sin confirmar dependencias, modulos e inyeccion existentes.
- No hardcodees credenciales, endpoints privados ni secretos.
- Prefiere configuracion ya existente en Gradle, modulos DI o repositorios del proyecto.
- Antes de sugerir instalar librerias, busca dependencias previas en Gradle, modulos DI, repositorios y helpers existentes.
- No reinstales ni dupliques librerias o SDKs que ya esten integrados; reutiliza la abstraccion local.

## Flujo de Trabajo del Asistente de IA
- Usa analisis multi-archivo para mantener consistencia entre UI, ViewModels, repositorios, contratos y DI.
- Antes de escribir codigo, identifica el punto de entrada real y el patron local.
- Para cambios grandes, presenta primero un plan claro en texto plano.
- **Obligatorio:** Para cambios no triviales, usa el flujo SDD de `openspec/`:
  1. Crea la carpeta `openspec/changes/<nombre-cambio>/`
  2. Sigue la secuencia: `research.md` (si requiere info externa) → `proposal.md` → `spec.md` → `design.md` → `plan.md` → `tasks.md` → `review.md` → `verification.md` → `archive.md`
  3. Usa las plantillas de `openspec/templates/`
- Lee `openspec/constitution.md` **antes de empezar cualquier cambio** — especialmente si toca ventas, inventario, offline/sync, caja, auth, reportes, nomina, DI o repositorios.
- Si encuentras cambios previos del usuario, trabaja con ellos y no los reviertas.
- Si una verificacion falla, reporta la causa real y no marques la tarea como completa.

## Gradle, JDK y Daemons en Windows
- Este proyecto usa Gradle wrapper desde `gradle/wrapper/gradle-wrapper.properties`; no uses un Gradle global instalado ni cambies la version sin revisar compatibilidad con AGP, Kotlin y Android Studio.
- Si Gradle se queda colgado, no asumas de inmediato que el proyecto esta roto. Primero revisa si hay locks o daemons atorados en `C:\Users\ia\.gradle`.
- Senales conocidas del problema local:
  `java.nio.file.AccessDeniedException` en `C:\Users\ia\AppData\Local\kotlin\daemon\kotlin-daemon-client-tsmarker*.tmp`,
  `Failed to compile with Kotlin daemon`,
  `Try ./gradlew --stop if this issue persists`,
  `Timed out waiting for finished message from client socket connection`,
  o `gradle-*.zip.lck (Acceso denegado)`.
- La causa mas probable no es `libs.versions.toml` ni `alias(libs...)`. El patron observado apunta a conflicto de locks/daemons entre Android Studio, agentes externos y el daemon de Kotlin/Gradle en Windows.
- Este proyecto debe mantener alineado el daemon de Gradle con Java 17. `JAVA_HOME`, `jvmToolchain(17)`, `compileOptions` y `gradle/gradle-daemon-jvm.properties` deben apuntar de forma compatible a JDK 17 salvo que se haga una migracion explicita y verificada.
- Si aparece una configuracion mixta de JDK, por ejemplo `JAVA_HOME` en JDK 17 pero `gradle/gradle-daemon-jvm.properties` pidiendo Java 21, compara `java -version`, `.\gradlew.bat --version` y la configuracion de Android Studio antes de diagnosticar el proyecto como roto.
- Si el problema aparece durante una verificacion:
  1. Cancela builds activos en Android Studio y en el agente.
  2. Ejecuta `.\gradlew.bat --status` para ver daemons.
  3. Ejecuta `.\gradlew.bat --stop` y espera unos segundos antes de reintentar.
  4. Si sigue bloqueado, revisa logs en `C:\Users\ia\.gradle\daemon\<version>\daemon-*.out.log`.
  5. Solo si queda un proceso Java de Gradle claramente atorado, detener ese proceso especifico; no cerrar Android Studio ni borrar caches completas sin causa confirmada.
- En entornos con sandbox, Gradle puede fallar al escribir en `C:\Users\ia\.gradle`; en ese caso reporta que la verificacion requiere permisos fuera del sandbox en vez de diagnosticarlo como fallo del proyecto.
- No borres `C:\Users\ia\.gradle`, `build/` globales ni caches de wrapper como primera medida. Esas acciones son lentas, destructivas para diagnostico y pueden ocultar la causa real.
- Si se propone alinear JDKs, hazlo como cambio pequeno y explicito mediante `.\gradlew.bat updateDaemonJvm --jvm-version=17`, y verifica con `.\gradlew.bat --version` despues.

## Pre-Build Check (Daemons y Memoria)
- **Antes de lanzar cualquier comando de Gradle**, verifica que no haya procesos de Gradle/Kotlin superpuestos:
  1. Ejecuta `.\gradlew.bat --status` y revisa si hay daemons activos.
  2. Si hay daemons corriendo de otra sesion, ejecuta `.\gradlew.bat --stop` antes de continuar.
  3. Espera 2-3 segundos para que los locks se liberen.
- Tambien verifica con `Get-Process | Where-Object { $_.ProcessName -match 'java|kotlin' }` que no haya procesos Java/Kotlin huerfanos que puedan competir por memoria o locks.
- No ejecutes dos comandos de Gradle en paralelo (ej. compilar y correr tests al mismo tiempo). Espera a que uno termine antes del siguiente.

## Git Discipline
- Los cambios deben quedar siempre en un estado trackeado al finalizar la sesion: `git add` + `git commit`, o al menos `git stash` si el agente se retira y el usuario seguira despues.
- No dejes el arbol de trabajo con archivos modificados o untracked sin commit ni aviso explicito al usuario.
- Antes de terminar una tarea, verifica con `git status` que el working tree esta limpio o que los cambios pendientes estan documentados.
- Si hay cambios fuera del alcance de la tarea actual que no deben committearse, agregalos al `.gitignore` o notificalo al usuario explicitamente.

## Configuracion Local (SDK y AVD)
- **Android SDK**: `D:\Android\Sdk` (movido desde C: para liberar espacio)
  - ANDROID_HOME = `D:\Android\Sdk`
  - ANDROID_SDK_ROOT = `D:\Android\Sdk`
  - `local.properties` en este proyecto ya apunta a `sdk.dir=D\:\\Android\\Sdk`
- **Android Virtual Devices (AVDs)**: `D:\.android\avd`
  - Creado junction desde `C:\Users\ia\.android\avd` → `D:\.android\avd`
  - AVD activo: `bocatta_tablet_api36`
  - AVD eliminado: `medium_phone` (~9.7 GB liberados)
- **Proyectos respaldados**: `D:\proyectos\` contiene copias de proyectos anteriores (robot-contenido v1-v4, pulsoengine)
- La carpeta `.android` completa vive en C: solo con config; los AVDs pesados estan en D: via junction.

## Verificacion
- Si cambias Kotlin o Compose, ejecuta una verificacion normal de Gradle:
  `.\gradlew.bat compileDebugKotlin`
- Si necesitas verificar el APK completo, ejecuta:
  `.\gradlew.bat assembleDebug`
- Usa `--no-parallel --max-workers=1` solo cuando la maquina este saturada, haya problemas de memoria, locks de Gradle/Kotlin o el usuario lo pida explicitamente.
- Si cambias dominio, repositorios o logica critica, ejecuta tests relevantes.
- Si la verificacion falla, reporta el error real.
- Si solo hay warnings conocidos, mencionalos sin ocultar el resultado de compilacion.
