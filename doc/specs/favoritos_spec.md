# Especificación — Favoritos del Cliente y Ranking Administrativo (`favoritos_spec.md`)

| Atributo | Detalle |
| :--- | :--- |
| **Identificador** | `SPEC-VAPEON-004` |
| **Proyecto** | VapeON - Aplicación Móvil Android |
| **Versión** | `1.1.0` |
| **Estado** | `Aprobado` |
| **Fecha** | `2026-11-16` |
| **Módulo** | Favoritos del cliente y popularidad ADMIN |
| **Ubicación** | `doc/specs/favoritos_spec.md` |

---

## 1. Objetivo

Permitir que cada cliente guarde productos de su interés y los consulte después en una pantalla propia, sin duplicar precios, stock ni imágenes en la base de datos. Los datos visibles siempre se resuelven contra el catálogo vigente.

Además, el administrador consulta un ranking de **productos más populares** calculado con la cantidad de favoritos por producto, sin acceder a los datos personales de los clientes.

## 2. Alcance funcional

- El corazón de `DetalleProductoScreen` alterna el favorito del producto para el cliente autenticado:
  - Corazón vacío: el producto no está guardado.
  - Corazón rojo: el producto ya está en favoritos.
  - Para el rol **ADMIN** el control permanece deshabilitado.
- El menú lateral del rol **CLIENTE** incluye la opción **"Mis favoritos"**.
- `FavoritosScreen` muestra una grilla de tarjetas con datos reales de cada producto:
  - Imagen del producto o del primer sabor disponible.
  - `Stock` total vigente.
  - Marca, nombre y precio.
  - Corazón para eliminar el favorito.
  - Botón **"Añadir al Carrito"**, que usa el primer sabor con stock disponible.
- La tarjeta abre el detalle del producto al tocar la imagen o el nombre.
- Estados de la pantalla: carga, error con reintento, lista vacía y catálogo sin coincidencias.
- Las rutas de favoritos son exclusivas de CLIENTE y se revalidan al restaurar la navegación.

### 2.1 Ranking administrativo de productos más populares

- El botón **"PRODUCTOS MÁS POPULARES"** de `AdminHomeScreen` abre `FavoritosAdminScreen`.
- La ruta `favoritosAdmin` es exclusiva del rol **ADMIN** y se incluye en `RUTAS_ADMIN`; si la sesión no es ADMIN, la navegación regresa al inicio correspondiente.
- El ranking se calcula contando documentos de `favoritos` agrupados por `productoId`, de mayor a menor cantidad.
- La pantalla es de **solo lectura**: no crea, modifica ni elimina favoritos.
- Cada fila muestra posición, imagen, nombre, marca, stock vigente y cantidad de favoritos.
- No se muestra `usuarioId`, nombre, correo ni ningún otro dato del cliente.
- Si un producto del ranking ya no existe en el catálogo, se muestra **"Producto no disponible"** sin inventar precio, stock, marca ni imagen.
- Los documentos sin `productoId` válido se descartan y el total se informa como advertencia.
- Estados de la pantalla: carga, error con reintento, ranking vacío y sesión sin permisos.

## 3. Modelo de datos

Colección Firestore: `favoritos`.

| Campo | Tipo | Obligatorio | Descripción |
| :--- | :--- | :---: | :--- |
| `id` | string | Sí | UUID del documento; coincide con el ID de Firestore |
| `usuarioId` | string | Sí | Cliente propietario del favorito |
| `productoId` | string | Sí | Producto del catálogo guardado |
| `fechaCreacion` | timestamp | Sí | Fecha ISO-8601 del alta |

No se guardan nombre, marca, precio, stock ni imagen: esos valores se leen del catálogo vigente. En **"Mis favoritos"** un producto desaparecido deja de mostrarse como tarjeta; en el ranking ADMIN se muestra un aviso neutro de producto no disponible.

## 4. API REST (Firestore v1)

| Operación | Método y ruta | Uso |
| :--- | :--- | :--- |
| Crear | `POST favoritos?documentId={id}` | Alta del favorito |
| Consultar | `POST ../documents:runQuery` | Filtro `usuarioId == sesión` (CLIENTE) |
| Consultar ranking | `POST ../documents:runQuery` | Consulta global sin filtro, solo ADMIN |
| Mostrar | `GET favoritos/{id}` | Verificación de propiedad antes de eliminar |
| Eliminar | `DELETE favoritos/{id}` | Baja del favorito |

La consulta del cliente usa `structuredQuery` con `collectionId = favoritos` y `fieldFilter` sobre `usuarioId` con operador `EQUAL`. La consulta administrativa omite `where` para recuperar todos los documentos y agregarlos por producto.

## 5. Reglas de seguridad y consistencia

- Cada documento se interpreta solo si `fields.usuarioId` coincide con la sesión.
- El `id` del documento debe coincidir con el sufijo de `name` y no contener `/`.
- Antes de eliminar se consulta el documento remoto y se valida su propietario.
- El alta y la baja se ejecutan con un indicador por producto para evitar dobles pulsaciones.
- La lista se deduplica por `productoId` y se ordena por `fechaCreacion` descendente.
- Al cambiar de sesión se cancelan las operaciones pendientes y se limpia el estado.
- El ranking administrativo solo se calcula para una sesión con rol ADMIN y se reinicia al cambiar de usuario.
- La agregación conserva únicamente `productoId` y el conteo; `usuarioId` se descarta y nunca se publica en la vista.
- Los documentos con `productoId` vacío o con `/` se cuentan como inválidos y no entran al ranking.
- La autorización remota definitiva depende de las reglas de Firestore; las guardias del cliente y del ViewModel no sustituyen controles de servidor.

## 6. Criterios de aceptación

- **Dado** un cliente sin favoritos, **cuando** pulsa el corazón de un producto, **entonces** se crea un documento en `favoritos` y el corazón se muestra lleno.
- **Dado** un cliente con el producto ya guardado, **cuando** vuelve a pulsar el corazón, **entonces** el documento se elimina y el corazón vuelve a estar vacío.
- **Dado** un administrador, **cuando** abre el detalle de un producto, **entonces** el corazón está deshabilitado y no se crea ningún favorito.
- **Dado** un producto con precio o stock modificados, **cuando** el cliente abre "Mis favoritos", **entonces** la tarjeta muestra los valores vigentes del catálogo.
- **Dado** un favorito cuyo producto ya no existe, **cuando** se carga la pantalla, **entonces** no se muestra una tarjeta con datos inventados.
- **Dado** un favorito ajeno, **cuando** se intenta eliminar, **entonces** la operación se rechaza sin modificar la lista local.
- **Dado** un administrador, **cuando** pulsa "PRODUCTOS MÁS POPULARES", **entonces** se abre `FavoritosAdminScreen` con el ranking ordenado de mayor a menor por cantidad de favoritos.
- **Dado** un cliente, **cuando** intenta restaurar la ruta `favoritosAdmin`, **entonces** la navegación se rechaza y vuelve al inicio de su rol.
- **Dado** un producto con favoritos que ya no existe en el catálogo, **cuando** se abre el ranking, **entonces** se muestra un aviso neutro sin precio, stock ni imagen inventados.
- **Dado** un documento de favorito sin `productoId` válido, **cuando** se agrega el ranking, **entonces** se descarta y la pantalla informa la cantidad de registros omitidos.
- **Dado** el ranking administrativo, **cuando** se inspecciona la pantalla, **entonces** no aparece ningún `usuarioId` ni dato personal de los clientes.
