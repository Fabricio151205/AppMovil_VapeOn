# Especificación Técnica — Datos de Entrega y Boleta Electrónica (`DatosEntregaScreen.kt`)

| Atributo | Detalle |
| :--- | :--- |
| **Identificador** | `SPEC-VAPEON-008` |
| **Modulo** | Checkout / Proceso de Compra |
| **Versión** | `1.0.0` |
| **Estado** | `Aprobado` |
| **Componentes Asociados** | `DatosEntregaScreen.kt`, `VapeONApp.kt`, `CarritoViewModel.kt`, `PedidoViewModel.kt` |

---

## 1. Propósito y Alcance

Definir la estructura, comportamiento visual y renderizado de datos para la pantalla de **Datos de Entrega**, la cual presenta al cliente la **Boleta Electrónica** troquelada previa a la confirmación de su pedido.

---

## 2. Requerimientos Funcionales y Visuales

### 2.1 Número Correlativo Incremental Global
- Muestra el encabezado central: **`BOLETA ELECTRONICA N° %04d`** (ejemplo: `BOLETA ELECTRONICA N° 0001`, `0002`, `0003`).
- Se calcula dinámicamente consultando la caja global de compras registradas en la aplicación (`pedidoViewModel.pedidosCaja.size + 1`).

### 2.2 Troquelado y Diseño Estilizado
- Utiliza las imágenes de borde troquelado **`R.drawable.arreglo_boleta`** (superior) y **`R.drawable.arreglo_pedido`** (inferior) con escala `ContentScale.FillWidth` / `ContentScale.FillBounds` de borde a borde.
- Se elimina cualquier recuadro o tarjeta gris rígida con borde cuadrado. El cuerpo de la boleta flote de forma limpia con fondo transparente sobre la interfaz.
- El contenedor utiliza **`Modifier.matchParentSize()`** o flujo de columna para expandirse automáticamente en sentido vertical independientemente de la cantidad de productos agregados al carrito (1, 3, 5 o más ítems).

### 2.3 Exclusión de Datos de Prueba (Sin Dummys)
- Se eliminaron por completo todos los textos fijos de prueba o productos dummy ("Vasito Snowwolf", "Kit Completo", números fijos).
- Muestra exclusivamente:
  - **Fecha y Hora**: En tiempo real (`dd-MM-yyyy` y `HH:mm`).
  - **Cliente**: Nombre y apellido del usuario autenticado (`usuario.nombre`, `usuario.apellido`).
  - **Teléfono**: Teléfono registrado del usuario (`usuario.telefono`).
  - **Productos de la Compra**: Renderiza dinámicamente cada ítem real del carrito (`carritoViewModel.items`) detallando nombre, sabor seleccionado, cantidad e importe.
  - **Desglose Financiero**: Sub Total, IGV (18%), Delivery fijo (S/. 5.00) y Total a pagar derivados del resumen del carrito (`carritoViewModel.resumen`).

---

## 3. Tipografía y Estilos

- **Título Boleta**: `VapeOnGold`, `16.sp`, `FontWeight.Bold`, `letterSpacing = 1.sp`.
- **Datos de Cabecera**: `VapeOnTextPrimary`, `14.sp`, `FontWeight.Medium`.
- **Sección Productos**: `VapeOnGold`, `15.sp`, `FontWeight.Bold`.
- **Filas de Ítems**: `VapeOnTextPrimary`, `14.sp`, `FontWeight.SemiBold`.
- **Total a Pagar**: `VapeOnGold`, `16.sp`, `FontWeight.Bold`.
- **Botón de Acción**: Botón rojo circular `VapeOnRed` ("CONTINUAR").
