# Research: User Identity and Auth Audit

Spec ID: `identity-auth`
Capability: `User Identity, Firebase Auth, Roles, PIN Flows, Access Control`
Fecha: `2026-06-06`

## Brief Humano

Resume el pedido original, contexto del negocio y restricciones conocidas.

- Audit user identity, Firebase Auth, roles, employee authorization, PIN/admin flows, session state, and auditable access control in Bocatta POS.

## Web Research Gate

Completar antes de buscar en internet.

- Research question: N/A
- Source category from `openspec/source-catalog.yaml`: N/A
- Why local context is insufficient: Local context is sufficient for an audit.
- Expected decision: N/A
- Stop condition: N/A
- Budget: none

Si `Budget` es `none`, explicar por que el repo local basta.
- El repositorio local contiene todas las clases necesarias (AuthViewModelV2, SessionViewModel, AuthorizationManager) para auditar el estado actual del sistema de identidad y autenticacion.

## Contexto Local Leido

- Constitution: `openspec/constitution.md`
- AGENTS: `AGENTS.md`
- Skill registry: `android-user-identity-auth`
- Source catalog: N/A
- Archivos inspeccionados:
  - `app/src/main/java/com/bocatta/pos/domain/usecase/AuthorizationManager.kt`
  - `app/src/main/java/com/bocatta/pos/presentation/viewmodel/AuthViewModelV2.kt`
  - `app/src/main/java/com/bocatta/pos/presentation/viewmodel/SessionViewModel.kt`

## Fuentes Externas Consultadas

| Fuente | URL | Fecha | Version/alcance | Decision soportada |
|--------|-----|-------|-----------------|--------------------|
| N/A | N/A | N/A | N/A | N/A |

## Alternativas

### Opcion A

- Descripcion: N/A
- Ventajas: N/A
- Costos: N/A
- Riesgos: N/A

## Decision Recomendada

- Proceder con la elaboracion de propuestas y planes basados en el estado actual de la logica de autorizacion y sesiones.

## Suposiciones Externas

- Ninguna

## Tradeoffs

- Ninguno

## Preguntas Que Deben Aclararse Antes de Proponer

- Ninguna. El estado actual es claro y esta contenido en las clases inspeccionadas.
