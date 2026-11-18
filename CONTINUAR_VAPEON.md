# CONTINUAR VAPEON

## Objetivo
Unificar VapeOn con Firebase Authentication y dejar Firestore protegido con reglas seguras, sin romper productos, pedidos, pagos, fidelidad ni administración.

## Estado exacto
- Fases 1–6 completadas: análisis, login, registro, Google, recuperación/persistencia/logout y reglas generadas.
- Código nuevo compila y las pruebas unitarias JVM pasan (482, 0 fallos).
- `firestore.rules` existe en la raíz pero **no se publicó**.
- Faltan las pruebas manuales en dispositivo y la validación de reglas con el emulador.
- `PROGRESO_VAPEON.md` contiene el detalle completo, decisiones y limitaciones.

## Últimos archivos modificados
- `firestore.rules` (creado, sin publicar)
- `PROGRESO_VAPEON.md` (actualizado)
- `CONTINUAR_VAPEON.md` (actualizado)
- Código de autenticación: `auth/AutenticacionFirebase.kt`, `auth/GoogleAuthManager.kt`, `utils/FirebaseAuthInterceptor.kt`, `utils/RetrofitClient.kt`
- UI: `LoginScreen.kt`, `ForgotPasswordScreen.kt`, `DetalleClienteScreen.kt`, `VapeONApp.kt`
- ViewModels: `UsuarioViewModel.kt`, `ClientesViewModel.kt`
- Servicios: `UsuarioService.kt`
- Pruebas: `FakeAutenticacionFirebase.kt`, `UsuarioSesionTest.kt`, `PerfilAccesibilidadTest.kt`, `ClientesTest.kt`, `ClientesUiTest.kt` (instrumentado, no ejecutado)

## Cambios pendientes inmediatos
1. Ejecutar el checklist manual de `PROGRESO_VAPEON.md` en un dispositivo.
2. Instalar Firebase CLI y validar `firestore.rules` con Firebase Emulator Suite.
3. Publicar las reglas solo con aprobación del equipo.
4. Decidir el paso de inventario/pagos a Cloud Functions.

## Errores que quedan
- Ninguno conocido en compilación ni en pruebas unitarias.
- Riesgos abiertos: importes calculados en cliente, `stockPorSabor` acotado solo por claves y campo `password` legado sin eliminar.
- Las pruebas manuales de Auth (login, registro, Google, correo, persistencia) no se ejecutaron.

## Última compilación
`.\gradlew.bat :app:compileDebugKotlin :app:testDebugUnitTest --offline --console=plain` → **BUILD SUCCESSFUL** (tareas UP-TO-DATE). Última corrida completa de pruebas: 482 pruebas, 0 fallos.

## Siguiente tarea concreta
Probar en dispositivo el botón original **INICIAR SESIÓN** con una cuenta real de Firebase Auth y verificar que lee `usuarios/{uid}`.

## Advertencias de seguridad
- No volver a comparar ni escribir contraseñas en Firestore.
- No publicar reglas automáticamente ni usar `allow read, write: if true`.
- No eliminar usuarios ni datos de Firestore.
- No introducir claves administrativas en Android.
- Las reglas protegen `rol`, `password`, pedidos, pagos y fidelidad; la autorización real vive en el servidor.
- No afirmar que una prueba pasó si no se ejecutó.

## Cómo continuar sin repetir trabajo
Leer `PROGRESO_VAPEON.md`, revisar el estado de las fases y continuar desde "Próxima acción". No repetir el análisis ya documentado ni reconstruir la app.
