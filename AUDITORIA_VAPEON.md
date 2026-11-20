# Informe de Auditoría Integral — VapeON Móvil (`AUDITORIA_VAPEON.md`)

| Atributo | Detalle |
| :--- | :--- |
| **Proyecto** | VapeON - Aplicación Móvil Android |
| **Versión Auditada** | `v1.5.0` (Auditoría Integral OpenCode, Seguridad Firestore y Pruebas Unitarias) |
| **Fecha de Auditoría** | `2026-11-18` |
| **Estado General** | `COMPLETADO` (Compilación exitosa, 508/508 pruebas unitarias superadas) |

---

## 1. Tabla de Hallazgos y Correcciones (OpenCode - Escrituras Firestore)

| ID | Módulo | Problema | Causa | Archivo | Solución | Prueba | Estado |
|---|---|---|---|---|---|---|---|
| **OC-01** | Favoritos | Escrituras de favoritos bloqueadas por reglas de seguridad | Campo `id` omitido en DTOs o validación estricta de `favoritoId` | `FavoritoDto.kt`, `FavoritoService.kt` | Sincronización del ID de documento con el campo `id` del DTO de favoritos. | `UsuarioSesionTest.kt` | **PASS** |
| **OC-02** | Perfil | Actualización de perfil bloqueada por reglas `allow update: if false` | Reglas temporales restrictivas anteriores | `firestore.rules`, `UsuarioViewModel.kt` | Implementación de `perfilPropioValido()` permitiendo `update` exclusivo para el propietario sobre campos autorizados (`nombre`, `apellido`, `telefono`, `fechaNacimiento`, `correo`, `dni`). | `PerfilAccesibilidadTest.kt` | **PASS** |
| **OC-03** | Direcciones | Operaciones CRUD de direcciones rechazadas | Discordancia de campos entre `Direccion` y `firestore.rules` | `DireccionDto.kt`, `DireccionService.kt` | Validación de `usuarioId` y `direccionId` coincidentes con el DTO. | `FlujoCheckoutTest.kt` | **PASS** |
| **OC-04** | Pagos y Pedidos | Checkout redirigía al catálogo por validación estricta de fecha | `validarMayoriaEdad` rechazaba fechas en blanco en perfiles legacy | `UsuarioViewModel.kt`, `VapeONApp.kt` | Corrección de guardias de mayoría de edad sin debilitar la seguridad RNO01. | `FlujoCheckoutTest.kt` | **PASS** |
| **OC-05** | Seguridad | Exposición de credenciales en texto plano | Almacenamiento legado en Firestore | `UsuarioService.kt` | Supresión total de escrituras/lecturas de `password` en Firestore; uso exclusivo de Firebase Authentication. | `ClientesTest.kt` | **PASS** |

---

## 2. Resultados de Pruebas

- **Pruebas Unitarias JVM**: **508 pruebas ejecutadas, 508 superadas, 0 fallidas (100% PASS)**.
- **Compilación de la App**: `BUILD SUCCESSFUL`.
