# PROGRESO VAPEON — Autenticación y seguridad

> Fuente principal para reanudar el trabajo. Actualizar después de cada archivo o grupo pequeño de cambios.

## Estado general

- **Objetivo**: unificar autenticación con Firebase Authentication (login, registro, Google, recuperación, sesión persistente) y dejar reglas seguras de Firestore.
- **Fase actual**: Fase 7 — pruebas (manuales pendientes).
- **Fases completadas**: 1 (análisis), 2 (login), 3 (registro), 4 (Google), 5 (recuperación/persistencia/logout), 6 (reglas generadas, sin publicar).
- **Fases pendientes**: validación manual en dispositivo, validación de reglas con Firebase Emulator Suite, decisión sobre Cloud Functions.
- **Avance aproximado**: 85 % (código y reglas listos; faltan pruebas reales y publicación controlada).

## Decisiones de diseño

- Se creó la abstracción `AutenticacionFirebase` con implementación `FirebaseAutenticacion` y un proveedor perezoso de `FirebaseAuth`, para poder probar la lógica en JVM sin inicializar Firebase.
- El login usa `signInWithEmailAndPassword`, obtiene el UID y lee `usuarios/{uid}`. El rol ADMIN se lee de Firestore; la autorización definitiva la aplican las reglas del servidor.
- El registro crea la identidad en Auth, usa el **UID como `documentId`**, no escribe `password`, envía verificación de correo y cierra la sesión provisional.
- Reintento de registro parcial: si Auth ya existe y `registroPendiente.correo` coincide, se inicia sesión con la misma contraseña para recuperar el UID y crear el perfil faltante. Nunca se vincula por correo a una cuenta ajena.
- Google Sign-In crea el perfil solo si no existe (`404`), sin duplicar ni enlazar cuentas por correo.
- Recuperación de contraseña con `sendPasswordResetEmail()`, en un solo paso y sin revelar si el correo existe.
- `restaurarSesion()` se ejecuta al abrir la app; cierra Firebase solo si el perfil no existe o no hay permiso, y conserva la sesión ante errores de red.
- `cerrarSesion()` cierra Firebase Auth además de limpiar el estado local.
- La edición de perfil usa `UsuarioPerfilService` con `updateMask` (`nombre`, `apellido`, `telefono`, `fechaNacimiento`, `correo`) y sincroniza el correo con Firebase Auth (`updateEmail`) antes del PATCH.
- El cambio administrativo de contraseña se reemplazó por envío de correo de restablecimiento: el ADMIN nunca escribe ni ve contraseñas.
- Las reglas usan `resource == null` en las lecturas para que la app reciba `404` (y no `403`) al comprobar existencia de perfil/pago/pedido/fidelidad.
- Compra del cliente: la regla solo permite bajar `stock` y conservar las mismas claves de `stockPorSabor`. Limitación documentada; se recomienda Cloud Functions.

## Archivos modificados

| Archivo | Cambio | Motivo | Compila |
| :--- | :--- | :--- | :---: |
| `app/src/main/java/com/example/vapeon_movil/auth/AutenticacionFirebase.kt` | Creado: interfaz, `FirebaseAutenticacion`, `SesionFirebase`, `ErrorAutenticacion`, `actualizarCorreo` | Abstraer Firebase Auth y permitir pruebas JVM | Sí |
| `app/src/main/java/com/example/vapeon_movil/auth/GoogleAuthManager.kt` | Ajustado al flujo de sesión | Google Sign-In con Firebase | Sí |
| `app/src/main/java/com/example/vapeon_movil/viewmodels/UsuarioViewModel.kt` | Login, registro, restauración, recuperación y perfil migrados a Firebase Auth + UID | Núcleo de la migración | Sí |
| `app/src/main/java/com/example/vapeon_movil/viewmodels/ClientesViewModel.kt` | `enviarRestablecimiento()`; sin escrituras de `password` | El ADMIN no maneja contraseñas | Sí |
| `app/src/main/java/com/example/vapeon_movil/ui/LoginScreen.kt` | Botón original conectado a Firebase; eliminado el botón temporal "PROBAR FIREBASE AUTH" | Requisito de prioridad | Sí |
| `app/src/main/java/com/example/vapeon_movil/ui/ForgotPasswordScreen.kt` | Recuperación real de un paso | Reemplaza la simulación local | Sí |
| `app/src/main/java/com/example/vapeon_movil/ui/DetalleClienteScreen.kt` | "RESTABLECER CONTRASEÑA" + "Enviar correo" | Cambio administrativo seguro | Sí |
| `app/src/main/java/com/example/vapeon_movil/VapeONApp.kt` | Restauración de sesión al iniciar y navegación | Persistencia de sesión | Sí |
| `app/src/main/java/com/example/vapeon_movil/services/UsuarioService.kt` | Contratos de perfil (`updateMask`), rol y perfil Google | Separar escrituras sensibles | Sí |
| `app/src/main/java/com/example/vapeon_movil/utils/FirebaseAuthInterceptor.kt` | Creado: adjunta `getIdToken(false)` a Retrofit | Autenticar Firestore REST | Sí |
| `app/src/main/java/com/example/vapeon_movil/utils/RetrofitClient.kt` | Registra el interceptor | Token en cada petición | Sí |
| `app/build.gradle.kts`, `app/google-services.json` | Dependencias/configuración de Firebase | Requisito de Auth | Sí |
| `app/src/main/res/values/strings.xml` | Textos de autenticación | Mensajes de error/estado | Sí |
| `app/src/test/java/com/example/vapeon_movil/FakeAutenticacionFirebase.kt` | Creado: fake compartido de Auth | Pruebas deterministas | Sí |
| `app/src/test/java/com/example/vapeon_movil/UsuarioSesionTest.kt` | Actualizado al flujo Firebase | Cobertura de login/registro/sesión | Sí |
| `app/src/test/java/com/example/vapeon_movil/PerfilAccesibilidadTest.kt` | Actualizado | Perfil con `updateMask` | Sí |
| `app/src/test/java/com/example/vapeon_movil/ClientesTest.kt` | Actualizado | Restablecimiento sin `password` | Sí |
| `app/src/androidTest/java/com/example/vapeon_movil/ClientesUiTest.kt` | Ajustado (instrumentado, no ejecutado) | Cambio de diálogo | Sin ejecutar |
| `firestore.rules` | Creado en la raíz | Seguridad de Firestore | No aplica (sin publicar) |
| `PROGRESO_VAPEON.md`, `CONTINUAR_VAPEON.md` | Actualizados | Checkpoint | N/A |

## Funcionalidades

| Funcionalidad | Estado |
| :--- | :--- |
| Análisis de autenticación | COMPLETADA |
| Login tradicional con Firebase Auth | COMPLETADA (pruebas JVM; falta prueba real) |
| Registro con Firebase Auth y UID | COMPLETADA (falta prueba real) |
| Google Sign-In | IMPLEMENTADO (sin probar en dispositivo) |
| Recuperación de contraseña real | COMPLETADA (falta probar el correo) |
| Persistencia/restauración de sesión | COMPLETADA (falta probar reinicio de app) |
| Cierre de sesión Firebase | COMPLETADA |
| Eliminación de escrituras de `password` | COMPLETADA en código nuevo |
| Reglas `firestore.rules` | GENERADAS, NO PUBLICADAS |
| Pruebas unitarias JVM | 482 pruebas, 0 fallos |
| Pruebas manuales en dispositivo | PENDIENTES |
| Validación de reglas con emulador | PENDIENTE (Firebase CLI no instalado) |

## Reglas de Firestore (`firestore.rules`)

Colecciones cubiertas: `usuarios`, `productos`, `marcas`, `sabores`, `proveedores`, `direcciones`, `tarjetas`, `favoritos`, `pedidos`, `pagos`, `fidelidades`, `eventosFidelidad`, `recompensasFidelidad`, `configuracionFidelidad`.

- Denegación por defecto; no existe `allow read, write: if true`.
- `esAdmin()` lee `usuarios/{uid}.rol == 'ADMIN'`.
- El cliente crea su perfil con `uid == auth.uid`, `rol == 'CLIENTE'`, sin `password` y con correo igual al del token (`lower()`).
- El ADMIN no puede escribir `password` ni quitarse su propio rol.
- Usuarios, pedidos, pagos y fidelidad no se eliminan desde la app.
- Pedido digital: el `pagoId` debe existir, ser del mismo usuario y estar `CONFIRMADO`.
- Pagos: transiciones `PENDIENTE → CONFIRMADO/RECHAZADO` y `estadoRegistroPedido → REGISTRADO`.
- Catálogo (`productos`, `marcas`, `sabores`) con lectura pública; proveedores solo ADMIN.

### Limitaciones conocidas (documentadas en el propio archivo)

- Los importes de pedidos/pagos se calculan en el cliente; las reglas validan forma, propiedad y estados, pero no recalculan precios.
- `stockPorSabor` solo se acota por claves; no se verifica valor por valor.
- Los documentos antiguos pueden conservar `password`; no se borran datos y las reglas impiden reescribirlo.
- Para cerrar estas brechas se recomienda mover inventario y confirmación de pagos a Cloud Functions.

## Pruebas ejecutadas

| Comando | Resultado |
| :--- | :--- |
| `.\gradlew.bat :app:compileDebugKotlin --console=plain` | BUILD SUCCESSFUL |
| `.\gradlew.bat :app:testDebugUnitTest --offline --console=plain` | 482 pruebas, 0 fallos |
| `.\gradlew.bat :app:compileDebugKotlin :app:testDebugUnitTest --offline --console=plain` | BUILD SUCCESSFUL (tareas UP-TO-DATE) |
| `firebase --version` | Firebase CLI no disponible |
| `npx --no-install firebase --version` | Falló (paquete no instalado) |

## Pruebas manuales pendientes (no ejecutadas)

1. **Login real**: cuenta existente en Firebase Auth → botón original **INICIAR SESIÓN** → entra con perfil de Firestore.
2. **Login inválido**: contraseña incorrecta y correo inexistente → mensaje claro, sin filtrar si la cuenta existe.
3. **Registro**: crear cuenta nueva → documento `usuarios/{uid}` con rol CLIENTE, sin campo `password`, correo de verificación recibido.
4. **Registro interrumpido**: cortar tras crear Auth y reintentar → se completa el perfil sin duplicar.
5. **Google**: cuenta nueva crea perfil; cuenta existente lo reutiliza; no se vincula por correo.
6. **Recuperación**: "Olvidé mi contraseña" → correo real → cambiar contraseña → iniciar sesión con la nueva.
7. **Persistencia**: cerrar y reabrir la app → sesión restaurada; cerrar sesión → vuelve a login y Firebase queda cerrado.
8. **ADMIN**: "RESTABLECER CONTRASEÑA" envía correo y no escribe contraseña; no puede quitarse su rol.
9. **Reglas**: validar con Firebase Emulator Suite los casos permitidos/denegados (requiere instalar Firebase CLI).
10. **Instrumentadas**: `ClientesUiTest` no se ejecutó (requiere emulador/dispositivo).

## Próxima acción

1. Ejecutar el checklist manual en un dispositivo con el proyecto Firebase real.
2. Instalar Firebase CLI y validar `firestore.rules` con el emulador antes de publicar.
3. Publicar las reglas solo con aprobación del equipo.
4. Decidir si inventario/pagos pasan a Cloud Functions para cerrar las limitaciones.
5. Confirmar si se eliminará el campo legado `password` de documentos antiguos (hoy no se toca).
