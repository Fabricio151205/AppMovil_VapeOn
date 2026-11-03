# Especificación Técnica - Gestión de Productos (`AgregarProductoScreen` y `EditarProductoScreen`)

| Atributo | Detalle |
| :--- | :--- |
| **Identificador** | `SPEC-VAPEON-GESTION-001` |
| **Módulo** | Creación y Edición de Productos con Control Concurrente |
| **Versión** | `1.2.0` |
| **Estado** | `Aprobado` |
| **Fecha de Publicación** | `2026-10-30` |
| **Plataforma Objetivo** | Android Nativo (Jetpack Compose, Material 3) |
| **Ubicación del Documento** | `doc/specs/gestion_productos_spec.md` |
| **Archivos Fuente** | [AgregarProductoScreen.kt](file:///d:/AppMovil_VapeOn/app/src/main/java/com/example/vapeon_movil/ui/AgregarProductoScreen.kt) · [EditarProductoScreen.kt](file:///d:/AppMovil_VapeOn/app/src/main/java/com/example/vapeon_movil/ui/EditarProductoScreen.kt) |

---

## 1. Resumen Ejecutivo y Funcionalidades

### 1.1 Selector Conmutador `tieneSabor`
Tanto en `AgregarProductoScreen.kt` como en `EditarProductoScreen.kt` se integra el conmutador **"¿El producto incluye selección de sabores?"**:
- **`tieneSabor == true`**: Despliega la cuadrícula de sabores de la marca y permite definir stock individual por sabor.
- **`tieneSabor == false`**: Oculta la selección de sabores y guarda el producto con `sabor = ""` y `stockPorSabor = emptyMap()`.

### 1.2 Vista de Carga y Recarga Estandarizada
En `EditarProductoScreen.kt`, el estado `documento == null` presenta una vista de carga estandarizada con `CircularProgressIndicator(color = VapeOnGold, modifier = Modifier.size(36.dp))` y el botón en dorado **"RECARGAR DATOS"** (`VapeOnGold`), integrando la paleta oficial de la aplicación.
