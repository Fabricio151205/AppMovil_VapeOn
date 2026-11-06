# Documento de Especificación Técnica y Ética - VapeON Móvil (`spec.md`)

| Atributo | Detalle |
| :--- | :--- |
| **Identificador** | `SPEC-VAPEON-001` |
| **Proyecto** | VapeON - Aplicación Móvil Android |
| **Versión** | `1.6.0` |
| **Estado** | `Aprobado` |
| **Fecha de Publicación** | `2026-11-06` |
| **Plataforma Objetivo** | Android (minSdk 31, targetSdk 37, compileSdk 37) |
| **Ubicación del Documento** | `doc/spec.md` (Raíz del Repositorio) |

---

## 1. Resumen Ejecutivo y Alcance

### 1.1 Declaración del Problema
El mercado de dispositivos y accesorios de vapeo requiere plataformas digitales ágiles que permitan a los usuarios mayores de edad consultar catálogos actualizados por marca y stock en tiempo real, garantizando un control estricto de acceso (+18), veracidad en especificaciones de producto y herramientas administrativas para la gestión del inventario.

### 1.2 Alcance del Sistema (Version 1.5.0)
- **Flujo Guiado de Recuperación de Contraseña (3 Pasos)**:
  - Paso 1: Solicitud de correo con validación de sintaxis y existencia en Firestore REST. Notificación flotante simulada sobre el tope de pantalla con código de 6 dígitos.
  - Paso 2: Verificación de código de 6 casilleros dorados/oscuros (`BasicTextField` con captura directa).
  - Paso 3: Registro de nueva contraseña con toggle de visibilidad e ingreso directo a base de datos.
- **Catálogo de Productos Inteligente (RF12 / RF13 / RNE01)**:
  - Búsqueda reactiva por nombre, marca o descripción (RF12).
  - Categorías superiores: **"Todos"**, **"Nuevos"** (agregados hace máximo 1 semana / 7 días) y **"Promociones"** (`precio <= 60.0`).
  - Botón e ícono flotante de filtros (`Icons.Default.Tune`) ubicado a la izquierda del botón *"Todos"*, con modal para marcas y orden de precio (RF13).
  - Control de visibilidad de stock por rol (**RNE01**): Clientes solo ven productos con `stockTotal > 0`. Administradores ven todos los productos y los agotados muestran la etiqueta roja **"Sin stock"** en la tarjeta.
- **Ficha Técnica, Sabores e Imágenes Dinámicas**:
  - `DetalleProductoScreen` renderiza únicamente los sabores pertenecientes a dicho producto. Si fue creado sin sabor (`tieneSabor == false`), la sección de sabores se oculta.
  - `AgregarSaborScreen` y `EditarSaborScreen` permiten seleccionar una foto de sabor desde la galería o cámara, la cual se comprime y se almacena como data URI base64 en Firestore.
  - Al pulsar cualquier chip de sabor en `DetalleProductoScreen`, la imagen del producto cambia instantáneamente para mostrar la foto correspondiente a dicho sabor.
  - Si un sabor no tiene foto Base64 en Firestore, el resolvedor de imágenes `resolverImagenProducto` identifica el tipo de producto (**Kit Completo** `kitcompleto` vs **Recargas/Cartuchos** `recarga`) y muestra el drawable `muestra_...` nativo correspondiente.
- **Flujo de Checkout y Tarjetas en 4 Pasos**:
  - Registro de tarjetas simulado en 4 pasos (`AgregarTarjetaNumeroScreen`, `AgregarTarjetaExpiracionScreen`, `AgregarTarjetaCvvScreen`, `AgregarTarjetaTitularScreen`).
  - Uso de `VisualTransformation`: `CardNumberVisualTransformation` para agrupar los 16 dígitos en bloques de 4 con espacios sin saltos de cursor, y `ExpirationDateVisualTransformation` para el formato **Año / Mes** (`AA / MM`).
  - Boleta electrónica troquelada (`DatosEntregaScreen`) con número correlativo incremental (`BOLETA ELECTRONICA N° %04d`), bordes troquelados (`arreglo_boleta` / `arreglo_pedido`) y renderizado 100% dinámico de ítems reales del carrito sin datos de prueba.
  - Selección de método de pago (`MetodoPagoScreen`), dirección (`SeleccionDireccionScreen`) y tarjetas guardadas (`SeleccionTarjetaScreen`).
- **Barra de Notificaciones y Menú de Administración**:
  - Configuración global en `Theme.kt` (`window.statusBarColor = VapeOnBackground` con `isAppearanceLightStatusBars = false`) para garantizar la barra de estado negra con íconos blancos y espaciado consistente.
  - Habilitación permanente de la opción **"Marcas"** en el menú de navegación lateral para el rol Administrador hacia `MarcasAdminScreen`.
- **HU-05 — Pronóstico de Demanda**:
  - Acceso exclusivo ADMIN desde `AdminHomeScreen`.
  - Lectura de snapshots reales de la colección `pedidos` mediante Firestore REST, sin crear una colección de pronósticos.
  - Solo los pedidos con `estado = ENTREGADO` forman parte del entrenamiento.
  - Las ventas se agrupan por `productoId` y día local de `America/Lima`; los días intermedios sin ventas se representan con demanda cero.
  - Modelo Holt lineal con `alpha` y `beta` ajustados mediante búsqueda acotada y selección por menor error cuadrático de entrenamiento.
  - Validación cronológica final con MAE y RMSE; nunca se mezcla aleatoriamente el orden temporal.
  - Mínimos centralizados: 14 observaciones diarias, 3 días con demanda y separación viable entre entrenamiento y validación.
  - Cuando no se cumplen los mínimos, la UI muestra el histórico real y `Datos insuficientes`, sin fabricar pronóstico.
  - Gráfica responsive con Canvas que diferencia histórico, validación y horizonte futuro.

---

## 2. Historial de Cambios del Proyecto

| Versión | Fecha | Autor / Equipo | Cambios Realizados |
| :--- | :--- | :--- | :--- |
| `1.0.0` | `2026-09-26` | Equipo VapeON | Creación inicial de especificación técnica y marco ético. |
| `1.1.0` | `2026-10-02` | Equipo VapeON | Integración de `DetalleProductoScreen`, `ForgotPasswordScreen` e indicadores de carga. |
| `1.2.0` | `2026-10-11` | Equipo VapeON | Persistencia local del carrito (`SharedPreferences` + `Gson`) y RBAC de carrito. |
| `1.3.0` | `2026-10-30` | Equipo VapeON | Flujo de recuperación de contraseña de 3 pasos con notificación flotante, filtros flotantes por marca/precio (RF12/RF13), regla de stock por rol (RNE01), conmutador `tieneSabor` e indicadores de carga en `VapeOnGold`. |
| `1.4.0` | `2026-11-04` | Equipo VapeON | CRUD de marcas con siembra inicial y guardas de relaciones; imagen opcional de sabor desde galería/cámara almacenada como data URI base64 en Firestore. |
| `1.5.0` | `2026-11-04` | Equipo VapeON | Flujo de tarjetas en 4 pasos con `VisualTransformation`, boleta troquelada incremental dinámica, resolvedor de imágenes por sabor/tipo (Kit Completo vs Recarga), barra de notificaciones negra global y menú de marcas. |
| `1.6.0` | `2026-11-06` | Equipo VapeON | HU-05 con series reales de pedidos entregados, ajuste Holt, validación temporal MAE/RMSE, gráfica administrativa y estado explícito de datos insuficientes. |

---

## 3. Especificación ética y técnica de HU-05

### 3.1 Transparencia y límites

- El módulo se denomina **Pronóstico de demanda — Modelo Holt** y no se presenta como inteligencia artificial generativa.
- No utiliza datos de clientes, direcciones, teléfonos ni información financiera para entrenar.
- No genera observaciones, ventas ni pedidos artificiales.
- La ausencia de historial suficiente se informa explícitamente; no se sustituye por promedios fijos o resultados aleatorios.
- El pronóstico es informativo y no modifica automáticamente inventario, precios ni decisiones de compra.

### 3.2 Flujo técnico

```mermaid
flowchart TD
    A[Firestore: pedidos reales] --> B[PedidoService REST]
    B --> C[PronosticoDemandaViewModel]
    C --> D[Pedidos ENTREGADO deduplicados]
    D --> E[Serie diaria por productoId]
    E --> F{¿Datos suficientes?}
    F -- No --> G[Histórico + estado Datos insuficientes]
    F -- Sí --> H[Entrenamiento Holt]
    H --> I[Validación temporal MAE/RMSE]
    I --> J[Pronóstico futuro]
    J --> K[PronosticoDemandaScreen + Canvas]
```

### 3.3 Criterios de aceptación

- **Dado** un pedido con estado distinto de `ENTREGADO`, **cuando** se construye el dataset, **entonces** sus cantidades no se contabilizan.
- **Dado** un producto con menos de 14 observaciones o menos de 3 días con demanda, **cuando** el administrador lo selecciona, **entonces** no se genera una serie futura.
- **Dado** un historial suficiente, **cuando** se entrena el modelo, **entonces** `alpha` y `beta` proceden del ajuste sobre los datos reales y el tramo final se reserva para MAE/RMSE.
- **Dado** un producto eliminado del catálogo, **cuando** conserva ventas históricas, **entonces** continúa disponible mediante el snapshot del pedido.
- **Dado** un usuario CLIENTE, **cuando** intenta restaurar la ruta de pronóstico, **entonces** la guardia de navegación lo devuelve a su pantalla autorizada.
