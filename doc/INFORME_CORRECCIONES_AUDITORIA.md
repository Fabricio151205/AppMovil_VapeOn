# Informe de correcciones pendientes — VapeON
Fecha: 14/10/2026 (America/Lima).

## Resultado y alcance
Se atendieron I5–I12, interfaces incompletas que presentaban éxito ficticio, datos comerciales de relleno y redundancias comprobadas. Se mantuvo Compose → ViewModel → Service Retrofit → Firestore REST. No se agregaron librerías, repositorios, Services, ViewModels, Navigation Compose, Room, DataStore, Firebase Authentication ni Firebase Firestore SDK.

C1/C2, las guardias de navegación de C3, el aislamiento por usuario de C4, las validaciones de I1/I2, logout e I4 se conservaron. Las extensiones de ciclo de vida y limpieza son necesarias para I5/I6/I12; no sustituyen esos bloques. FirestoreDto.kt y PedidoService.kt no se modificaron en esta entrega.

## Comparación verificable antes/después
| Comprobación | Antes de modificar | Resultado final |
| --- | --- | --- |
| Unitarias debug | 76 aprobadas, 0 fallos | **102 aprobadas, 0 fallos** |
| Instrumentadas | 1 de 2 aprobada; rotación fallaba sin jerarquía Compose, con el teléfono bloqueado | **4 de 4 aprobadas en V2430, Android 16, desbloqueado** |
| Total final | 77 aprobadas de 78 ejecutadas inicialmente | **106 aprobadas, 0 fallos, 0 omitidas** |
| APK debug | Proyecto existente compilaba | **assembleDebug correcto** |
| Lint debug | 0 errores, 29 advertencias | **0 errores, 29 advertencias; sin nuevas categorías ni aumento** |
| Diff | Cambios anteriores del usuario ya presentes | git diff --check correcto; se conservaron esos cambios |

Se ejecutaron las unitarias al finalizar cada grupo importante: I5: 80; I6: 87; I7/I8: 90; I9: 91; I10: 95; I11: 98; I12: 99; cierre con pruebas de registro/recreación: 102.
Las cuatro instrumentadas finales fueron: contexto de la aplicación; ruta y campo del registro tras tres recreaciones; fallo de limpieza y recuperación de compra confirmada; cambio de usuario con inventario local aislado. Los intentos iniciales fallidos de rotación no se contabilizaron como aprobados. Después de desbloquear el teléfono se repitieron con éxito.

Comando final:
```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:connectedDebugAndroidTest :app:lintDebug --offline --console=plain
```

Artefactos:
- [APK debug](C:/Users/fabri/Desktop/AppMovil_VapeOn/app/build/outputs/apk/debug/app-debug.apk)
- [Informe unitario](C:/Users/fabri/Desktop/AppMovil_VapeOn/app/build/reports/tests/testDebugUnitTest/index.html)
- [Informe de dispositivo](C:/Users/fabri/Desktop/AppMovil_VapeOn/app/build/reports/androidTests/connected/debug/index.html)
- [Informe Lint](C:/Users/fabri/Desktop/AppMovil_VapeOn/app/build/reports/lint-results-debug.html)

Las advertencias restantes se relacionan con versiones/dependencias, recursos sin uso declarado, ubicación de iconos, estilo KTX/TOML, SDK y preferencias. No se actualizaron dependencias ni se borraron recursos futuros por una advertencia. No se sustituyó indiscriminadamente commit() por apply(): la finalización necesita comprobar el resultado de la persistencia.

## Problemas corregidos
| Bloque | Comportamiento resultante |
| --- | --- |
| I5 | POST/PATCH/DELETE impiden operaciones simultáneas repetidas. Altas de productos y usuarios reutilizan documentId en los reintentos del intento activo. Errores no se presentan como éxito. Formularios conservan campos al recrear y los resultados de guardado los observa la composición vigente. |
| I6 | El carrito rechaza agotados y precios inválidos, limita incrementos al stock conocido, mantiene cantidad mínima 1 y permite eliminación explícita. Revisa JSON restaurado, duplicados, cantidades y disponibilidad. La caché se invalida al cambiar de usuario. RF22 mantiene su validación fresca contra Firebase. |
| I7 | El detalle utiliza los atributos reales. Datos ausentes muestran No disponible y stock no positivo muestra Sin stock. No inventa sabores ni disponibilidad. |
| I8 | Se presentan céntimos y dos decimales con un formato común, sin convertir precios a enteros. Se conserva BigDecimal y no se cambian subtotal, delivery, total ni vuelto de RF22. Altas/ediciones rechazan precios no finitos o con precisión superior a céntimos. |
| I9 | Se distinguen cargando, vacío y error; los fallos conservan mensaje y reintento. Permisos, pedido ausente y datos inválidos tienen mensajes diferenciados. |
| I10 | Usuarios y productos recorren nextPageToken hasta terminar. Una página fallida no se publica como listado completo; un token repetido se detecta. Pedidos continúa usando runQuery, sin inventarle tokens de documents.list. |
| I11 | Un documento inválido no elimina los pedidos válidos. Se informa la cantidad inválida y se conserva orden descendente. Los documentos ajenos no se interpretan ni exponen al cliente. |
| I12 | Un pedido confirmado conserva su comprobante local hasta comprobar la eliminación durable del carrito y del intento. Si falla, queda visible Pedido registrado y se bloquea otra compra. El reintento de limpieza o la recuperación de una confirmación persistida no vuelve a enviar el commit. |

## Finalización de RF22 y compatibilidad con C4
1. RF22 valida stock fresco, precio, monto y dirección como antes.
2. Guarda el intento por usuario y confirma el mismo commit atómico con sus precondiciones de stock.
3. Al confirmar el servidor, conserva localmente el mismo pedido/commit con la marca confirmado.
4. Solo termina la limpieza si la eliminación del carrito en SharedPreferences devuelve éxito.
5. Solo retira el comprobante después de la limpieza del carrito.
6. Si alguna escritura local falla, el pedido permanece confirmado en la interfaz, el carrito no queda disponible para una nueva compra y el usuario puede reintentar la limpieza.
7. Después de reiniciar, una confirmación local recuperada continúa únicamente esa limpieza. Un intento anterior sin esa marca sigue la comprobación existente de C4. B no recupera ni borra el intento de A.

La persistencia continúa en SharedPreferences; stockDisponible y confirmado son datos locales. No se agregan campos a los documentos de pedidos ni a sus productos comprados por estas marcas locales.

## Archivos modificados (31)
Lista relativa al estado encontrado al iniciar esta entrega, no a todos los cambios pendientes en Git desde trabajos anteriores.

| Archivo | Cambio |
| --- | --- |
| [app/src/androidTest/java/com/example/vapeon_movil/RotacionActivityTest.kt](C:/Users/fabri/Desktop/AppMovil_VapeOn/app/src/androidTest/java/com/example/vapeon_movil/RotacionActivityTest.kt) | Además de conservar la ruta, comprueba que el campo Nombres sobreviva a tres recreaciones de la Activity. |
| [app/src/main/java/com/example/vapeon_movil/VapeONApp.kt](C:/Users/fabri/Desktop/AppMovil_VapeOn/app/src/main/java/com/example/vapeon_movil/VapeONApp.kt) | Observa resultados de guardado desde la composición vigente; conecta el alta y el CRUD centralizado; actualiza el inventario conocido del carrito sin alterar las guardias de rol y propietario. |
| [app/src/main/java/com/example/vapeon_movil/components/ItemCarritoCard.kt](C:/Users/fabri/Desktop/AppMovil_VapeOn/app/src/main/java/com/example/vapeon_movil/components/ItemCarritoCard.kt) | Muestra céntimos, impide incrementar sobre el stock conocido y disminuir de una unidad, y amplía los controles de cantidad a 48 dp. |
| [app/src/main/java/com/example/vapeon_movil/components/ProductCard.kt](C:/Users/fabri/Desktop/AppMovil_VapeOn/app/src/main/java/com/example/vapeon_movil/components/ProductCard.kt) | Presenta el precio con dos decimales usando el formato monetario común. |
| [app/src/main/java/com/example/vapeon_movil/entities/ItemCarrito.kt](C:/Users/fabri/Desktop/AppMovil_VapeOn/app/src/main/java/com/example/vapeon_movil/entities/ItemCarrito.kt) | Añade stockDisponible opcional como dato local; los JSON antiguos continúan siendo legibles. |
| [app/src/main/java/com/example/vapeon_movil/services/ProductoService.kt](C:/Users/fabri/Desktop/AppMovil_VapeOn/app/src/main/java/com/example/vapeon_movil/services/ProductoService.kt) | Acepta documentId para un alta idempotente del intento activo; incorpora pageToken y lectura de todas las páginas. Conserva la precondición y máscara de I4. |
| [app/src/main/java/com/example/vapeon_movil/services/UsuarioService.kt](C:/Users/fabri/Desktop/AppMovil_VapeOn/app/src/main/java/com/example/vapeon_movil/services/UsuarioService.kt) | Acepta documentId y pageToken; reúne todas las páginas para login y comprobación de duplicados. |
| [app/src/main/java/com/example/vapeon_movil/ui/AdminHomeScreen.kt](C:/Users/fabri/Desktop/AppMovil_VapeOn/app/src/main/java/com/example/vapeon_movil/ui/AdminHomeScreen.kt) | Reutiliza ProductoViewModel para carga y eliminación; distingue error y vacío; sustituye caja y predicción ficticias por ausencia de datos y mensajes de módulo no disponible. |
| [app/src/main/java/com/example/vapeon_movil/ui/AgregarProductoScreen.kt](C:/Users/fabri/Desktop/AppMovil_VapeOn/app/src/main/java/com/example/vapeon_movil/ui/AgregarProductoScreen.kt) | Utiliza el ViewModel existente, conserva campos al recrear, muestra carga y errores, y evita altas repetidas del mismo intento. |
| [app/src/main/java/com/example/vapeon_movil/ui/CarritoScreen.kt](C:/Users/fabri/Desktop/AppMovil_VapeOn/app/src/main/java/com/example/vapeon_movil/ui/CarritoScreen.kt) | Presenta avisos incluso al quedar vacío; bloquea checkout cuando el carrito no es válido; utiliza formato monetario común y retira un clic de perfil vacío. |
| [app/src/main/java/com/example/vapeon_movil/ui/CatalogoScreen.kt](C:/Users/fabri/Desktop/AppMovil_VapeOn/app/src/main/java/com/example/vapeon_movil/ui/CatalogoScreen.kt) | Mantiene el error de carga con reintento, bloquea eliminaciones duplicadas y marca Nuevos/Promociones como no disponibles al carecer de datos reales. Permite desplazamiento horizontal de las categorías. |
| [app/src/main/java/com/example/vapeon_movil/ui/CheckoutScreen.kt](C:/Users/fabri/Desktop/AppMovil_VapeOn/app/src/main/java/com/example/vapeon_movil/ui/CheckoutScreen.kt) | Unifica importes mostrados y añade la recuperación de limpieza local después de un pedido confirmado. Mantiene el pago en efectivo y sus cálculos. |
| [app/src/main/java/com/example/vapeon_movil/ui/DetallePedidoScreen.kt](C:/Users/fabri/Desktop/AppMovil_VapeOn/app/src/main/java/com/example/vapeon_movil/ui/DetallePedidoScreen.kt) | Unifica el formato de todos los importes conservando los datos guardados, el propietario y el cambio exclusivo de estado. |
| [app/src/main/java/com/example/vapeon_movil/ui/DetalleProductoScreen.kt](C:/Users/fabri/Desktop/AppMovil_VapeOn/app/src/main/java/com/example/vapeon_movil/ui/DetalleProductoScreen.kt) | Elimina nombre, marca, precio, stock, descripción y sabores de relleno; usa No disponible/Sin stock; deshabilita la compra agotada y el favorito sin backend. |
| [app/src/main/java/com/example/vapeon_movil/ui/EditarProductoScreen.kt](C:/Users/fabri/Desktop/AppMovil_VapeOn/app/src/main/java/com/example/vapeon_movil/ui/EditarProductoScreen.kt) | Conserva campos y versión durante una rotación, utiliza el resultado observable de guardado y mantiene la recarga explícita de I4. |
| [app/src/main/java/com/example/vapeon_movil/ui/ForgotPasswordScreen.kt](C:/Users/fabri/Desktop/AppMovil_VapeOn/app/src/main/java/com/example/vapeon_movil/ui/ForgotPasswordScreen.kt) | Deja de afirmar que envió un código. Informa que la recuperación por correo no está disponible. |
| [app/src/main/java/com/example/vapeon_movil/ui/HistorialComprasScreen.kt](C:/Users/fabri/Desktop/AppMovil_VapeOn/app/src/main/java/com/example/vapeon_movil/ui/HistorialComprasScreen.kt) | Muestra la advertencia de pedidos inválidos sin ocultar las compras válidas; conserva orden y filtro por usuario; utiliza moneda común. |
| [app/src/main/java/com/example/vapeon_movil/ui/LoginScreen.kt](C:/Users/fabri/Desktop/AppMovil_VapeOn/app/src/main/java/com/example/vapeon_movil/ui/LoginScreen.kt) | Conserva el formulario al recrear la Activity y deshabilita las opciones sociales no implementadas. |
| [app/src/main/java/com/example/vapeon_movil/ui/MenuScreen.kt](C:/Users/fabri/Desktop/AppMovil_VapeOn/app/src/main/java/com/example/vapeon_movil/ui/MenuScreen.kt) | Identifica las opciones futuras y el perfil como no disponibles; conserva Catálogo, Carrito, Historial, Pedidos y Cerrar sesión. |
| [app/src/main/java/com/example/vapeon_movil/ui/PedidosAdminScreen.kt](C:/Users/fabri/Desktop/AppMovil_VapeOn/app/src/main/java/com/example/vapeon_movil/ui/PedidosAdminScreen.kt) | Muestra pedidos válidos y la advertencia por documentos inválidos; conserva detalle y administración de estados. |
| [app/src/main/java/com/example/vapeon_movil/ui/RegisterScreen.kt](C:/Users/fabri/Desktop/AppMovil_VapeOn/app/src/main/java/com/example/vapeon_movil/ui/RegisterScreen.kt) | Conserva campos, usa el resultado de registro observado por el contenedor y deshabilita las opciones sociales sin implementar. |
| [app/src/main/java/com/example/vapeon_movil/viewmodels/CarritoViewModel.kt](C:/Users/fabri/Desktop/AppMovil_VapeOn/app/src/main/java/com/example/vapeon_movil/viewmodels/CarritoViewModel.kt) | Controla cantidades y stock conocido; valida JSON restaurado; revisa inventario y productos eliminados; informa fallos de persistencia; comprueba la limpieza durable y aísla la caché al cambiar de usuario. |
| [app/src/main/java/com/example/vapeon_movil/viewmodels/PedidoViewModel.kt](C:/Users/fabri/Desktop/AppMovil_VapeOn/app/src/main/java/com/example/vapeon_movil/viewmodels/PedidoViewModel.kt) | Distingue errores de lectura, conserva pedidos válidos con avisos y mantiene un comprobante local confirmado hasta terminar la limpieza. No repite el commit al recuperar una confirmación ya guardada. |
| [app/src/main/java/com/example/vapeon_movil/viewmodels/ProductoViewModel.kt](C:/Users/fabri/Desktop/AppMovil_VapeOn/app/src/main/java/com/example/vapeon_movil/viewmodels/ProductoViewModel.kt) | Centraliza altas y eliminaciones con bloqueo de duplicados y resultados observables; carga todas las páginas; distingue carga/error/vacío; mantiene versión de edición y descarta la validez de caché al cambiar de sesión. |
| [app/src/main/java/com/example/vapeon_movil/viewmodels/UsuarioViewModel.kt](C:/Users/fabri/Desktop/AppMovil_VapeOn/app/src/main/java/com/example/vapeon_movil/viewmodels/UsuarioViewModel.kt) | Reutiliza el identificador del intento de registro, consulta todas las páginas, publica el resultado para la composición vigente y cancela operaciones de registro al cambiar la sesión. Conserva contraseña exacta y validaciones de I1/I2. |
| [app/src/main/java/com/example/vapeon_movil/services/dto/PedidoDto.kt](C:/Users/fabri/Desktop/AppMovil_VapeOn/app/src/main/java/com/example/vapeon_movil/services/dto/PedidoDto.kt) | Convierte cada documento por separado y devuelve los pedidos válidos con el número de inválidos; comprueba pertenencia antes de interpretar el contenido. |
| [app/src/main/java/com/example/vapeon_movil/services/dto/ProductoDto.kt](C:/Users/fabri/Desktop/AppMovil_VapeOn/app/src/main/java/com/example/vapeon_movil/services/dto/ProductoDto.kt) | Lee nextPageToken sin reemplazar los campos ni updateTime existentes. |
| [app/src/main/java/com/example/vapeon_movil/services/dto/UsuarioDto.kt](C:/Users/fabri/Desktop/AppMovil_VapeOn/app/src/main/java/com/example/vapeon_movil/services/dto/UsuarioDto.kt) | Lee nextPageToken manteniendo el transporte actual de usuarios. |
| [app/src/test/java/com/example/vapeon_movil/EdicionProductoTest.kt](C:/Users/fabri/Desktop/AppMovil_VapeOn/app/src/test/java/com/example/vapeon_movil/EdicionProductoTest.kt) | Adapta el Service y comprueba que recrear el formulario no toma silenciosamente una versión nueva para guardar cambios antiguos. |
| [app/src/test/java/com/example/vapeon_movil/HistorialPedidoTest.kt](C:/Users/fabri/Desktop/AppMovil_VapeOn/app/src/test/java/com/example/vapeon_movil/HistorialPedidoTest.kt) | Comprueba mezcla de pedidos válidos y corruptos, fechas inválidas, orden y aislamiento de pedidos ajenos. |
| [app/src/test/java/com/example/vapeon_movil/UsuarioSesionTest.kt](C:/Users/fabri/Desktop/AppMovil_VapeOn/app/src/test/java/com/example/vapeon_movil/UsuarioSesionTest.kt) | Adapta el Service y añade respuesta perdida y doble pulsación de registro. |

## Archivos nuevos (7, incluido este informe)
| Archivo | Propósito |
| --- | --- |
| [app/src/androidTest/java/com/example/vapeon_movil/FinalizacionCompraTest.kt](C:/Users/fabri/Desktop/AppMovil_VapeOn/app/src/androidTest/java/com/example/vapeon_movil/FinalizacionCompraTest.kt) | Pruebas en dispositivo con preferencias locales: fallo de limpieza, recuperación del comprobante confirmado y cambio A→B sin heredar inventario. No escribe pedidos en Firebase. |
| [app/src/main/java/com/example/vapeon_movil/utils/Dinero.kt](C:/Users/fabri/Desktop/AppMovil_VapeOn/app/src/main/java/com/example/vapeon_movil/utils/Dinero.kt) | Formato común con dos decimales y BigDecimal para presentación; no altera fórmulas ni importes originales. |
| [app/src/test/java/com/example/vapeon_movil/CarritoStockTest.kt](C:/Users/fabri/Desktop/AppMovil_VapeOn/app/src/test/java/com/example/vapeon_movil/CarritoStockTest.kt) | Prueba agotados, límites, restauración, inventario, datos corruptos y limpieza local fallida. |
| [app/src/test/java/com/example/vapeon_movil/DineroTest.kt](C:/Users/fabri/Desktop/AppMovil_VapeOn/app/src/test/java/com/example/vapeon_movil/DineroTest.kt) | Prueba céntimos, ceros, independencia del idioma y números no finitos. |
| [app/src/test/java/com/example/vapeon_movil/OperacionesProductoTest.kt](C:/Users/fabri/Desktop/AppMovil_VapeOn/app/src/test/java/com/example/vapeon_movil/OperacionesProductoTest.kt) | Prueba doble pulsación, respuesta perdida, eliminación repetida, precio inválido y carga fallida frente a lista vacía. |
| [app/src/test/java/com/example/vapeon_movil/PaginacionTest.kt](C:/Users/fabri/Desktop/AppMovil_VapeOn/app/src/test/java/com/example/vapeon_movil/PaginacionTest.kt) | Prueba páginas completas, token real, fallo de página posterior y token repetido mediante Retrofit sin red real. |
| [doc/INFORME_CORRECCIONES_AUDITORIA.md](C:/Users/fabri/Desktop/AppMovil_VapeOn/doc/INFORME_CORRECCIONES_AUDITORIA.md) | Este inventario, resultados, limitaciones y pruebas manuales. |

## Archivos eliminados (1)
- app/src/main/java/com/example/vapeon_movil/RegisterScreen.kt: pantalla duplicada sin uso. VapeONApp importa explícitamente ui.RegisterScreen; se verificaron las referencias antes de eliminar la copia de la raíz.

## Redundancias e interfaces incompletas
| Caso | Clasificación y decisión |
| --- | --- |
| CRUD de productos duplicado en AdminHome | Funcionalidad existente: ahora usa ProductoViewModel como el catálogo; se retiran las llamadas de red y la eliminación duplicadas de esa pantalla. |
| RegisterScreen de la raíz | Código muerto comprobado: eliminado; se mantiene la pantalla activa en ui/. |
| SaborPillButton y sabores inventados | Datos ficticios y componente que quedó sin uso: se retiran; el apartado indica No disponible. |
| Imports sin referencias en los archivos intervenidos | Retirados sin modificar lógica ajena. |
| Recuperación de contraseña | Interfaz sin backend de correo: deja de afirmar que envió un código. Implementar envío/verificación real exige un servicio adicional fuera del alcance autorizado; queda explícitamente no disponible. |
| Google/Facebook, favorito del detalle | Funciones futuras: controles deshabilitados e identificados como no disponibles; no se implementan servicios. |
| Yape/tarjeta | Se mantienen no disponibles, sin pasarela ni pagos nuevos. |
| Caja, predicción, cupones, nueva venta administrativa, estadísticas e inversionistas | Módulos futuros: interfaz conservada y mensajes honestos de no disponibilidad. Caja y predicción no presentan cifras inventadas. |
| Nuevos/Promociones del catálogo | No existen metadatos reales para esas clasificaciones; se retiran heurísticas de longitud del ID y precio y se identifican como no disponibles. Todos, búsqueda y marcas reales siguen funcionando. |
| Perfil sin implementación | No se presenta un clic vacío como una función real; el menú informa que no está disponible y el avatar del carrito es decorativo. |
| Parámetros callback opcionales sin acción por defecto | Son puntos de conexión de componentes, no operaciones de backend incompletas. No se construyeron módulos para rellenarlos. |

## Validaciones presentes y limitaciones pendientes
Presentes: teléfono obligatorio de 9 dígitos comenzando en 9; formato de correo y trim exterior; fecha real dd/mm/aaaa no futura; contraseñas iguales y comparación exacta; campos de productos y precio finito en céntimos; stock entero no negativo; cantidad del carrito entre 1 y el stock conocido; propietario del pedido; rol administrativo; estados de envío admitidos; versiones de edición; precondiciones y atomicidad del checkout; recuperación por usuario; protección de la finalización local.

Pendientes por alcance o arquitectura:
- **C5 sigue pendiente:** el almacenamiento actual de contraseñas en texto plano no se modificó. No se agregó hash, Auth ni OAuth.
- La consulta de correos duplicados ahora recorre todas las páginas, pero no garantiza unicidad atómica entre dos registros independientes simultáneos. Los identificadores de intento evitan duplicar su propio reintento, no crean una restricción única de correo entre dispositivos. No se cambia el modelo de autenticación para resolverlo.
- El stock del carrito es el último conocido, no una reserva. RF22 sigue comprobando el inventario fresco antes de confirmar.
- No se implementan mayoría de edad nueva, listeners, notificaciones, GPS, pasarelas, predicción ni estadísticas.
- Las pruebas automáticas de red usan dobles e interceptores. **No se hicieron altas, compras, eliminaciones ni cambios de estado contra el Firebase real durante esta verificación.**
- No se pretende cubrir en dispositivo todas las rutas de cada rol solo con las cuatro instrumentadas. Esa matriz completa requiere las comprobaciones manuales siguientes.

## Confirmación de funcionalidades existentes
| Funcionalidad | Evidencia y alcance |
| --- | --- |
| RF22 | Pasan sus 9 pruebas unitarias originales y la prueba de finalización local en dispositivo. Se preservan efectivo, monto, vuelto, instantánea, stock entero, commit y precondiciones. Compra real contra Firebase: manual pendiente. |
| RF26 | Pasan las pruebas de filtro, pertenencia, orden, detalle y documentos inválidos. No se recalculan los datos históricos. Consulta con cuentas reales: manual pendiente. |
| RF28 | Pasan las 4 pruebas de estados, permisos y escritura exclusiva de estado; PedidoService conserva su PATCH con updateMask. Cambio real administrador→cliente: manual pendiente. |
| C3 | Pasan 10 pruebas de navegación/selección y la instrumentada de ruta/campo tras tres recreaciones. Rotación de todas las pantallas autenticadas: manual pendiente. |
| C4 | Pasan sus 14 pruebas existentes y la finalización persistida; se mantiene el aislamiento de intentos por usuario. La prueba nueva de preferencias también comprueba el aislamiento de inventario A→B. |
| I1/I2/I3/I4 | Sus pruebas existentes se mantienen; se amplían registro idempotente y preservación de versión durante recreación. |

## Qué revisar en Firebase y en el teléfono
No se necesitan colecciones, índices ni cambios de esquema nuevos. nextPageToken y updateTime son datos de la API/metadatos; no deben crearse manualmente como campos de negocio. Mantener las reglas existentes necesarias para lectura y operaciones autorizadas en usuarios, productos y pedidos.

Usar cuentas y productos destinados a pruebas para la aceptación con Firebase real:

1. **Registro:** probar teléfono con letras, 8/10 dígitos, prefijo +51, espacios; correo inválido, fecha imposible/vacía/futura y confirmación distinta. No debe crearse cuenta. Crear una cuenta válida, rotar durante el formulario y confirmar que persiste. Probar contraseña con espacios exteriores: debe funcionar exactamente con ellos y fallar si se quitan.
2. **Login/logout:** entrar con A, abrir detalle/carrito/historial, cerrar sesión y entrar con B. B no debe recuperar ninguna selección ni intento de A. Probar login fallido después de una sesión. A debe recuperar su propio intento pendiente cuando corresponda.
3. **Productos:** alta con precio 70.90 y stock 3, doble pulsación rápida, verificar un solo documento; comprobar doubleValue e integerValue. Simular desconexión durante el alta y comprobar el mismo intento antes de empezar otro. Editar precio sin tocar stock y revisar campos adicionales del documento; eliminar una vez y comprobar carga/error.
4. **Edición concurrente (I4):** abrir stock 10 en administrador, comprar 3 como cliente en otro dispositivo y guardar la edición antigua. Debe fallar, conservar stock 7 y exigir Recargar datos. Repetir cambiando manualmente stock y después rotando mientras edita. No debe guardar con una versión tomada silenciosamente después.
5. **Carrito:** agotado no se agrega; con stock 3 no superar cantidad 3; disminuir mantiene mínimo 1; eliminar retira. Restaurar un carrito con cantidad 7 y stock 3, luego actualizar catálogo. Probar producto eliminado/agotado y verificar aviso incluso si queda vacío.
6. **Dinero:** comprobar 70.90 en catálogo, detalle, carrito, checkout, historial y detalle de pedido de ambos roles. Revisar precio unitario, subtotales, delivery, total, monto y vuelto; no debe perderse el cero final.
7. **RF22:** monto insuficiente, exacto y superior; stock insuficiente; desconexión antes/durante confirmación. Verificar un pedido real, descuento único y carrito vacío solo tras confirmar. Tras resultado incierto se comprueba el mismo ID.
8. **I12:** la escritura local fallida está simulada automáticamente en el teléfono, sin alterar permisos del sistema. Para una prueba integral de cierre de proceso, cerrar/reabrir durante la confirmación y comprobar que A recupera el mismo pedido y no vuelve a descontar stock.
9. **RF26/I11:** comprobar usuario sin compras, orden por fecha real, datos del detalle y rechazo de pedido ajeno. En un entorno de prueba introducir un pedido propio incompleto junto a dos válidos; deben verse los válidos y el aviso. El detalle inválido debe informar el problema.
10. **RF28:** administrador cambia REGISTRADO → EN CAMINO → ENTREGADO. Cliente pulsa Actualizar pedido y ve el valor real. Cliente no debe disponer de controles de actualización.
11. **Errores de carga/paginación:** desconectar red o usar reglas de prueba restrictivas para comprobar error y Reintentar, sin falso mensaje de colección vacía. Con suficientes documentos o páginas, verificar producto/usuario situado en una página posterior.
12. **Rotación/regreso:** repetir en cliente, administrador, alta/edición, carrito, checkout, historial y ambos detalles de pedido. Comprobar regreso y cerrar sesión después de rotar. Las cuatro pruebas físicas automáticas no sustituyen esta matriz completa.

Referencia del contrato de listado usado: [Firestore REST documents.list](https://firebase.google.com/docs/firestore/reference/rest/v1/projects.databases.documents/list).
