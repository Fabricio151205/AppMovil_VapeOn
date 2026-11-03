# Especificación Técnica, Ética y UI/UX - Detalle de Producto (`DetalleProductoScreen`)

| Atributo | Detalle |
| :--- | :--- |
| **Identificador** | `SPEC-VAPEON-DETALLE-001` |
| **Modulo / Pantalla** | `DetalleProductoScreen` |
| **Versión** | `1.1.0` |
| **Estado** | `Aprobado` |
| **Fecha de Publicación** | `2026-10-30` |
| **Plataforma Objetivo** | Android Nativo (Jetpack Compose, Material 3) |
| **Ubicación del Documento** | `doc/specs/detalle_producto_spec.md` |
| **Archivos Fuente** | [DetalleProductoScreen.kt](file:///d:/AppMovil_VapeOn/app/src/main/java/com/example/vapeon_movil/ui/DetalleProductoScreen.kt) · [Producto.kt](file:///d:/AppMovil_VapeOn/app/src/main/java/com/example/vapeon_movil/entities/Producto.kt) |

---

## 1. Resumen Ejecutivo y Alcance

### 1.1 Propósito
La pantalla `DetalleProductoScreen` proporciona la ficha técnica completa de un producto. Garantiza la veracidad de la información del catálogo y filtra dinámicamente **únicamente los sabores que corresponden a dicho producto**.

### 1.2 Alcance (Scope)
- **In-Scope**:
  - **Filtrado de Sabores Exclusivos**: Muestra únicamente los sabores registrados en `producto.stockPorSabor` o `producto.sabor`. Se eliminó el *fallback* que cargaba todos los sabores de la marca.
  - **Ocultamiento Condicional de Sabores**: Si el producto fue agregado sin sabor (`sabor == ""` y `stockPorSabor` vacío), la sección **"Seleccionar Sabor"** no se muestra en la pantalla.
  - **Etiquetas de Botón Inteligentes**: Para productos sin sabor con stock 0, el botón indica *"Sin stock"* en lugar de *"Sabor sin stock"*.
  - **Acción por Rol**: Botón *"Agregar al carrito"* para `CLIENTE` y *"Editar Producto"* para `ADMIN`.

---

## 2. Criterios de Aceptación (BDD Spec)

### Escenario 1: Producto sin sabor no despliega sección de sabores
- **Dado que** el cliente selecciona un producto registrado sin sabores en el catálogo.
- **Cuando** se compone `DetalleProductoScreen`.
- **Entonces** la sección *"Seleccionar Sabor"* no se muestra en la pantalla y se presenta la información general con su precio unitario.
