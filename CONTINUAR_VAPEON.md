# CONTINUAR VAPEON

## Objetivo
Mantener la estabilidad, seguridad y correcta integración de VapeOn con Firebase Authentication, Cloud Firestore REST (`firestore.rules`) y Jetpack Compose.

## Estado exacto
- Fases 1 a 10 completadas: Auditoría OpenCode finalizada.
- Código compila perfectamente y las **508 pruebas unitarias pasan con 0 fallos**.
- Informes actualizados: `AUDITORIA_VAPEON.md`, `PROGRESO_VAPEON.md`, `CONTINUAR_VAPEON.md`.

## Última compilación y pruebas
- `.\gradlew.bat :app:compileDebugKotlin :app:testDebugUnitTest --offline --console=plain` → **BUILD SUCCESSFUL** (508 pruebas superadas, 0 fallos).

## Nueva venta administrativa — continuación del 19/11/2026

Los archivos de la función estaban parcialmente creados, fuera de la navegación del panel. Se completaron la ruta ADMIN, el registro en pedidos/Caja, la distinción de entrega, la lectura sin teléfono para tienda y la recuperación de envíos ambiguos con el mismo ID persistido por administrador. Se preparó una propuesta local de reglas. EFECTIVO y YAPE son registros manuales de cobro. El delivery conserva S/ 5. Las pruebas de interfaz con datos reales quedan pendientes; usar un proyecto de prueba al registrar ventas porque descuentan stock.

Verificación de esta continuación: :app:assembleDebug y :app:testDebugUnitTest completados con BUILD SUCCESSFUL en copia local de los mismos fuentes. 495 pruebas unitarias, 0 fallos, 0 errores y 0 omitidas. Las seis pruebas nuevas cubren tienda, delivery, validaciones, permisos de navegación y recuperación de una respuesta perdida. Siete comprobaciones de permisos de alta pasaron en Firestore Emulator, proyecto demo-vapeon-venta. Pendiente la prueba visual en Android Studio y la aprobación/publicación de las reglas locales, si las reglas remotas restrictivas actuales no admiten la venta en tienda.

## Inversionistas — 19/11/2026

Se agregó un CRUD de registros de inversionistas accesible desde el botón del panel ADMIN. Entidad Inversionista, InversionistaDto, InversionistaService, InversionistasViewModel e InversionistasScreen. Incluye búsqueda, detalle, validación, edición y eliminación con control de versión y limpieza de datos de sesión. La colección se llama inversionistas. La propuesta de permisos ADMIN está en firestore.rules, pendiente de revisión/publicación. El registro se limita a identidad y contacto.

Verificación del CRUD de inversionistas: :app:assembleDebug y :app:testDebugUnitTest completados con BUILD SUCCESSFUL en una copia local con los mismos fuentes. Se ejecutaron 502 pruebas unitarias con 0 fallos, 0 errores y 0 omitidas, incluidas 7 nuevas del CRUD. Firestore Emulator superó 17 comprobaciones con datos ficticios. Pendientes la prueba visual en Android Studio y la revisión/publicación de las reglas locales para inversionistas.

## Rediseño UI/UX de las pantallas nuevas — 19/11/2026

Se revisaron las pantallas y accesos añadidos recientemente y se ajustó su presentación sin cambiar la lógica de negocio ni las reglas de Firestore:

- **Panel ADMIN**: se descartó el rediseño; el panel quedó exactamente como estaba antes (botones tipo píldora y mismo texto). Solo se conservó la navegación a los módulos nuevos.
- **Nueva venta**: encabezado con subtítulo y acción de actualizar catálogo, selector visual de entrega (tienda/delivery), datos del cliente, catálogo con búsqueda y selector de sabor, carrito con control de cantidades, método de pago, cálculo de vuelto y barra inferior fija con subtotal, delivery, total y confirmación. El estado de venta pendiente de comprobación se muestra con un aviso y bloquea la edición para evitar duplicados.
- **Inversionistas**: resumen con contador y actualización, búsqueda con acción de limpiar, tarjetas con iniciales, documento y contacto, estados vacíos diferenciados, y diálogos de detalle, alta/edición y eliminación con jerarquía visual y áreas táctiles de al menos 48 dp.

Verificación ejecutada tras el rediseño:

- `.\gradlew.bat :app:compileDebugKotlin --offline --console=plain` → **BUILD SUCCESSFUL**.
- `.\gradlew.bat :app:testDebugUnitTest --offline --console=plain` → **BUILD SUCCESSFUL**, 502 pruebas, 0 fallos, 0 errores, 0 omitidas.

Pendiente: revisión visual en dispositivo/emulador; no se modificaron reglas ni datos remotos.

## Ajustes de diseño y validaciones — 19/11/2026

- **Panel ADMIN restaurado**: se revirtió el cambio de las tarjetas de acción. La sección conserva los botones rojos originales, el título "Más Acciones" y las mismas rutas; únicamente se eliminaron los avisos "módulo no disponible" para que los botones abran las pantallas nuevas.
- **Nueva venta — selector de entrega**: las opciones "Venta en tienda" y "Con delivery" ahora son tarjetas más altas (mínimo 152 dp), con título de hasta dos líneas y descripción de hasta cuatro líneas, para que no se corte ningún texto en pantallas estrechas.
- **Nueva venta — pago**: se añadió el método **TARJETA** junto a EFECTIVO y YAPE, con botones más grandes (72 dp) y un apartado de datos de tarjeta: titular, últimos 4 dígitos y vencimiento `MM/AA` con formato automático. Solo se guarda el método en el pedido; nunca se envían número completo, CVV ni datos del titular a Firestore.
- **Validaciones de Nueva venta**: nombre (2 a 100 caracteres), teléfono peruano de 9 dígitos, dirección de entrega (5 a 200 caracteres), productos y cantidades, método de pago, titular, últimos 4 dígitos, vencimiento vigente y monto que cubre el total. Los errores se muestran en el campo correspondiente después de intentar registrar la venta.
- **Validaciones de Inversionistas**: nombre (2 a 150 caracteres), DNI de 8 dígitos o RUC peruano con dígito verificador, teléfono, correo y observaciones (hasta 2000 caracteres). El formulario añade errores por campo y canóniza el tipo de documento y el correo antes de guardar.
- **`firestore.rules` (propuesta local, no publicada)**: la rama ADMIN de `pedidos` ahora admite `TARJETA` además de EFECTIVO e YAPE. No se ampliaron permisos para clientes ni se publicaron reglas remotas.

Verificación ejecutada:

- `.\\gradlew.bat :app:compileDebugKotlin --offline --console=plain` → **BUILD SUCCESSFUL**.
- `.\\gradlew.bat :app:testDebugUnitTest --offline --console=plain` → **BUILD SUCCESSFUL**, 508 pruebas, 0 fallos, 0 errores, 0 omitidas (6 pruebas nuevas: tarjeta, campos, normalización y validación de inversionistas).
- `.\\gradlew.bat :app:assembleDebug --offline --console=plain` → **BUILD SUCCESSFUL**.

Pendiente: prueba visual en dispositivo/emulador y revisión/publicación de la propuesta de reglas locales.

