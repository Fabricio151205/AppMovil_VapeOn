# Especificación — CRUD de Direcciones del Cliente (`direcciones_spec.md`)

| Atributo | Detalle |
| :--- | :--- |
| **Identificador** | `SPEC-VAPEON-002` |
| **Proyecto** | VapeON - Aplicación Móvil Android |
| **Versión** | `1.3.0` |
| **Estado** | `Aprobado` |
| **Fecha** | `2026-11-06` |
| **Módulo** | Direcciones de entrega del cliente |
| **Ubicación** | `doc/specs/direcciones_spec.md` |

---

## 1. Objetivo

Permitir que cada cliente registre, consulte, actualice y elimine sus direcciones de entrega, y que reutilice esas direcciones en el checkout y en la vista de mapa, sin depender de una lista fija de ejemplo. Cada dirección puede tener un punto exacto confirmado sobre el mapa.

## 2. Alcance funcional

- El menú lateral del rol **CLIENTE** incluye la opción **"Mis direcciones"**.
- `DireccionesScreen` lista las direcciones del cliente con las acciones:
  - **Agregar nueva dirección** → `AgregarDireccionScreen`.
  - **Editar** → `EditarDireccionScreen`.
  - **Eliminar** con diálogo de confirmación.
  - **Ver ubicación** → `VerUbicacionScreen` (Google Maps).
- `SeleccionDireccionScreen` (checkout) muestra las direcciones guardadas del cliente en lugar de la lista fija anterior.
- Desde el checkout se puede agregar una dirección nueva y editar una existente; al volver se conserva la selección si la dirección sigue existiendo.
- El botón **CONTINUAR** del checkout permanece deshabilitado hasta que se selecciona una dirección guardada válida.
- Las direcciones se eliminan de la base de datos; no se usa borrado lógico.
- **Selección del punto exacto**:
  - En los formularios de alta y edición, el cliente escribe primero la dirección y luego pulsa **"Seleccionar en el mapa"**.
  - `SeleccionarUbicacionScreen` abre Google Maps centrado en la dirección escrita (geocodificada) y muestra un marcador aproximado.
  - El cliente puede tocar cualquier punto del mapa para fijar la ubicación exacta y confirmar con **"Usar esta ubicación"**.
  - Al tocar el mapa, la app hace **geocodificación inversa** y muestra el nombre de la calle o lugar del punto elegido.
  - Al confirmar, ese texto reemplaza el contenido del campo **"Dirección de entrega"** y queda asociado a las coordenadas guardadas.
  - Si el texto no se puede geocodificar, el mapa abre en Lima y permite marcar el punto manualmente.
  - Al modificar el texto de la dirección, la ubicación confirmada se invalida para evitar guardar un punto que no corresponde.
  - La confirmación del punto es obligatoria antes de guardar.
  - `VerUbicacionScreen` usa las coordenadas guardadas cuando existen; si no, geocodifica el texto como antes.

## 3. Modelo de datos

Colección Firestore: `direcciones`.

| Campo | Tipo | Obligatorio | Descripción |
| :--- | :--- | :---: | :--- |
| `id` | string | Sí | UUID del documento; coincide con el ID de Firestore |
| `usuarioId` | string | Sí | Cliente propietario de la dirección |
| `alias` | string | Sí | Etiqueta corta: Casa, Trabajo, etc. (máx. 30) |
| `direccion` | string | Sí | Texto de entrega (5 a 160 caracteres) |
| `referencia` | string | No | Referencia adicional (máx. 120) |
| `fechaCreacion` | timestamp | Sí | Fecha ISO-8601 del alta |
| `latitud` | double | Sí* | Latitud del punto exacto confirmado (-90 a 90) |
| `longitud` | double | Sí* | Longitud del punto exacto confirmado (-180 a 180) |

\* Obligatorias para las direcciones nuevas y para cualquier edición. Las direcciones antiguas sin coordenadas se siguen leyendo y su mapa se resuelve por geocodificación hasta que el cliente las edite.

## 4. API REST (Firestore v1)

| Operación | Método y ruta |
| :--- | :--- |
| Crear | `POST direcciones?documentId=<uuid>` |
| Listar por usuario | `POST ../documents:runQuery` con filtro `usuarioId == <id>` |
| Leer | `GET direcciones/{id}` |
| Actualizar | `PATCH direcciones/{id}?updateMask.fieldPaths=alias&...` |
| Eliminar | `DELETE direcciones/{id}` |

La máscara de edición solo incluye `alias`, `direccion`, `referencia`, `latitud` y `longitud`; `id`, `usuarioId` y `fechaCreacion` no son modificables desde la edición.

## 5. Validaciones y reglas

- `alias` y `direccion` son obligatorios; se recortan espacios antes de guardar.
- El alias no puede repetirse dentro del mismo cliente (comparación sin distinguir mayúsculas, tildes ni espacios repetidos).
- La dirección no puede repetirse dentro del mismo cliente con la misma normalización.
- Toda lectura, edición y eliminación verifica que el documento pertenezca al cliente autenticado.
- El ID de la dirección debe coincidir con el último segmento del `name` del documento y no puede contener `/`.
- Si falla la red después de un alta, el ViewModel reutiliza el mismo ID al reintentar para no duplicar la dirección.
- Los botones de guardado y eliminación se deshabilitan mientras hay una operación en curso.
- `latitud` y `longitud` son obligatorias: si faltan, la app pide confirmar el punto exacto en el mapa.
- Las coordenadas deben ser finitas y estar dentro de los rangos terrestres (-90 a 90 y -180 a 180).
- El par de coordenadas se guarda o se omite completo; una sola coordenada se considera un documento inválido.
- Si el cliente cambia el texto de la dirección, el punto confirmado se descarta y debe volver a marcarse.
- El texto de la dirección se actualiza con el nombre de la calle detectado al confirmar el punto; si la geocodificación inversa falla, se conserva el texto escrito y las coordenadas siguen siendo válidas.
- El texto detectado se recorta al límite de 160 caracteres del formulario.
- El pedido copia las coordenadas de la dirección elegida en el checkout; si el pedido no las tiene (pedidos antiguos), el mapa del detalle se resuelve por geocodificación del texto.
- Un pedido con solo una de las dos coordenadas se considera un documento inválido.

## 6. RBAC y privacidad

| Funcionalidad | CLIENTE | ADMIN |
| :--- | :---: | :---: |
| Ver y gestionar direcciones propias | ✅ | ❌ |
| Seleccionar dirección en checkout | ✅ | ❌ |
| Ver ubicación en mapa | ✅ | ❌ |
| Marcar el punto exacto de entrega | ✅ | ❌ |

- Las rutas `direcciones`, `agregarDireccion` y `editarDireccion` pertenecen únicamente a `RUTAS_CLIENTE`.
- Un administrador que restaure una de esas rutas es redirigido a su pantalla inicial.
- No se exponen direcciones de otros usuarios: la consulta filtra por `usuarioId` y cada documento se valida antes de interpretarse.

## 7. Experiencia de usuario

- Estados explícitos de carga, lista vacía, error y reintento.
- La lista vacía invita a agregar la primera dirección.
- El checkout indica "Toca una dirección para seleccionarla" cuando aún no hay una selección válida.
- La dirección elegida se refleja en el pedido mediante el campo existente `direccionEntrega`; sus coordenadas se copian a `latitud`/`longitud` del pedido.
- En el detalle del pedido (cliente y admin) se muestra un mapa de solo lectura con el punto exacto confirmado y un botón para abrirlo a pantalla completa.
- Los pedidos antiguos sin coordenadas muestran solo el botón **"Ver ubicación"** y el mapa se resuelve geocodificando el texto de entrega.
- El mapa se muestra como capa a pantalla completa y el botón Atrás regresa a la lista sin perderla.
- El selector de ubicación se abre como capa dentro del formulario, de modo que el alias, la dirección y la referencia escritos no se pierden.
- El formulario indica si el punto exacto ya fue confirmado y permite cambiarlo en cualquier momento.
- Al abrir el selector con una dirección escrita, el mapa se centra en el resultado aproximado y muestra ese punto como marcador inicial.
- Mientras se resuelve el nombre de la calle, el botón de confirmación se deshabilita; si la consulta falla o tarda más de 8 segundos, se habilita y se conserva el texto escrito.
- El nombre detectado se muestra en una tarjeta sobre el mapa antes de confirmar, para que el cliente sepa qué texto se guardará.

## 8. Pruebas

`DireccionesCrudTest` cubre:

- Conversión documento → entidad y escritura de todos los campos.
- Rechazo de documentos ajenos y de IDs que no coinciden.
- Obligatoriedad de alias y dirección; rechazo de referencias largas.
- Obligatoriedad del punto exacto y rechazo de coordenadas fuera de rango o no finitas.
- Lectura de direcciones antiguas sin coordenadas y rechazo de pares incompletos.
- Duplicados con normalización de mayúsculas, tildes y espacios.
- Exclusión del documento actual durante la edición.
- Filtro `usuarioId` de la consulta.
- Acceso de las nuevas rutas solo al rol CLIENTE.
- Composición del texto de dirección a partir del geocodificador inverso: línea completa, calle + número + distrito + ciudad, deduplicación, límite de longitud y caso sin datos.

## 9. Fuera de alcance

- Direcciones compartidas entre cuentas.
- Direcciones predeterminadas o favoritas.
- Sincronización offline ni caché local de direcciones.
- Geocodificación inversa para reescribir el texto de la dirección a partir del punto marcado; el campo **"Referencia"** no se autocompleta.
- Seguimiento en tiempo real del repartidor o ruta de entrega.
