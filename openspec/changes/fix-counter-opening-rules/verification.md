# Verification: Fix Counter Opening Rules

Spec ID: `fix-counter-opening-rules`

## Gradle

- `.\gradlew.bat compileDebugKotlin --no-daemon --no-parallel --max-workers=1`
  - Resultado: `BUILD SUCCESSFUL`
- `.\gradlew.bat assembleDebug --no-daemon --no-parallel --max-workers=1`
  - Resultado: `BUILD SUCCESSFUL`
  - Warnings no bloqueantes: deprecations Gradle y tareas Crashlytics debug presentes.

## Firebase

- `firebase deploy --only firestore:rules --project boccata-dce3d --non-interactive`
  - Resultado: reglas desplegadas.
- Venta real encontrada:
  - `v2_ventas/BC27S0V2pGCSdOJYKue0`
  - `codigoTicket=MT0606-001`
  - `total=40`
  - `sucursal=metepec`
  - `metodoPago=Efectivo`
  - `producto=p_carlota`

## AVD

- AVD: `bocatta_tablet_api36`, device `emulator-5554`.
- Apertura: llega a `BOCATTA POS` con `SISTEMA ONLINE - METEPEC`.
- Checkout inicial: confirmo el bug de datos/codigo donde `v2_inventory_branch_portions/metepec_carlota_unidad` tenia `cantidadEnBase=49` pero `currentQty=-1`; la validacion leia `currentQty` y bloqueaba venta con stock real disponible.
- Post-fix: el APK compila y se instala. El error de `carlota_unidad` ya no reaparece; la siguiente barrera detectada fue falta de stock operativo en `charola`.
- Se crearon docs minimos de stock para Metepec en Firestore para consumibles y vendibles base.

## Pendiente De Verificacion

- Repetir un flujo completo post-fix sin interrupciones de `adb`.
- Bloqueo local actual: hay dos clientes `adb` instalados:
  - `C:\Windows\adb.exe` = 1.0.40
  - SDK/`D:\Android\Sdk\platform-tools\adb.exe` = 1.0.41
  - Otro proceso reinicia el server con una version distinta durante la automatizacion, dejando temporalmente `device offline`.

## Startup AVD

- Tras reinstalar `app-debug.apk`, el AVD reporto `ANR in com.bocatta.pos` con razon `failed to complete startup`.
- Se aplicaron dos mitigaciones acotadas:
  - `BocattaApp` mueve limpieza/programacion de logs a `Dispatchers.Default`.
  - `Analytics` y `Crashlytics` pasan a `releaseImplementation`; `LogHelper` usa Crashlytics por reflexion solo si la clase existe.
- `compileDebugKotlin` y `assembleDebug` siguen pasando.
- Resultado AVD posterior: la app sigue visible solo como splash y el sistema mantiene la actividad sin completar startup en el tiempo esperado. Requiere spec separada de startup/performance antes de continuar QA visual.
