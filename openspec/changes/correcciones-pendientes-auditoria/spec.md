# Spec: Correcciones Pendientes de Auditoría

## Objetivos
- Eliminar tests frágiles que leen código fuente como texto plano
- Eliminar anti-patrón CountDownLatch en tests instrumentados
- Extraer strings hardcodeados a recursos de string.xml por pantalla

## No Objetivos
- Refactorizar mutable state en ViewModels (solo documentar)
- Cambiar valores .dp fijos (son intencionales y correctos)

## Criterios de Aceptación
- Tests frágiles reemplazados por tests de comportamiento real
- Compilación exitosa con `compileDebugKotlin`
- Tests unitarios pasando con `testDebugUnitTest`
