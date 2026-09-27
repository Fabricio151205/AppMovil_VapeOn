# Directrices para Agentes de IA - VapeON Móvil (`AGENTS.md`)

Este archivo define las reglas, protocolos operativos, directrices arquitectónicas y restricciones éticas que cualquier agente de inteligencia artificial (Antigravity u otros) debe acatar al trabajar en el proyecto **VapeON Móvil**.

---

## 1. Protocolo Operativo y Comunicación con el Usuario ⚠️

> [!IMPORTANT]
> **REGLA OBLIGATORIA ANTES DE MODIFICAR CÓDIGO:**
> 1. **Confirmación Previa**: Antes de realizar cualquier cambio en el código fuente, el agente debe preguntar al usuario o informarle explícitamente en qué archivo(s) se implementarán los cambios.
> 2. **Modificación de Múltiples Archivos**: Si una tarea requiere editar más de un archivo:
>    - El agente **debe listar todos los archivos** que planea intervenir.
>    - Debe **especificar claramente el propósito y la justificación** de tocar cada archivo.
>    - Debe **esperar la aprobación explícita** del usuario antes de proceder con las modificaciones.
> 3. **Integridad del Código**: Preservar siempre los comentarios, docstrings y lógica previa no relacionada con la tarea. No eliminar código sin autorización.

---

## 2. Contexto General y Stack del Proyecto

- **Nombre del Proyecto**: VapeON Móvil
- **Namespace / Package**: `com.example.vapeon_movil`
- **Plataforma**: Android Nativo (SDK Mínimo: 31 | SDK Objetivo: 37 | Compile SDK: 37)
- **Lenguaje**: Kotlin (v2.2.10)
- **UI Toolkit**: Jetpack Compose (BOM 2026.02.01) con Material Design 3
- **Concurrencia**: Kotlin Coroutines (`suspend`, `rememberCoroutineScope`, `LaunchedEffect`)
- **Capa de Red**: Square Retrofit (2.9.0) + Gson Converter
- **Backend / Persistencia**: Google Cloud Firestore mediante **REST API v1** oficial
- **Documentación de Referencia**:
  - [ARQUITECTURA.md](file:///d:/AppMovil_VapeOn/ARQUITECTURA.md): Arquitectura detallada del sistema y roadmap.
  - [doc/spec.md](file:///d:/AppMovil_VapeOn/doc/spec.md): Especificación técnica, funcional y marco ético.

---

## 3. Directrices Arquitectónicas

El proyecto sigue una arquitectura **Single-Activity** con interfaz declarativa y desacoplamiento en capas:

### 3.1 Capa de Presentación (UI Layer)
- **Host Único**: `MainActivity.kt` debe mantenerse limpio como host (`ComponentActivity` + `setContent { VapeONApp() }`).
- **Enrutamiento y Estado de Pantallas**:
  - La navegación principal se gestiona en `VapeONApp.kt` mediante elevación de estado (*State Hoisting*) con `mutableStateOf("pantalla")`.
  - La comunicación entre pantallas hijas y el contenedor se realiza mediante **lambdas de callback** (`irRegistro`, `irHome`, `irAdmin`, `volver`, etc.).
- **Diseño y Tematización**:
  - Utilizar **exclusivamente** los tokens y paletas definidas en `ui/theme/`:
    - `Color.kt`: Colores corporativos (`VapeOnRed`, `VapeOnGold`, `VapeOnBackground`, `VapeOnSurface`, `VapeOnCardBackground`, etc.).
    - `Type.kt`: Escala tipográfica oficial de Material 3 (`titleLarge`, `titleMedium`, `bodyLarge`, etc.).
    - `Theme.kt`: Tema `VapeON_MovilTheme` adaptado a Dark Mode.
  - **Prohibido** crear estilos o colores *hardcodeados* cuando ya exista un token en la paleta corporativa.
- **Componentes Reutilizables**:
  - Los componentes visuales modulares deben residir en el paquete `components/` (ej. `ProductCard.kt`, `MarcaSection.kt`).
  - Todo elemento interactivo debe contar con un tamaño táctil mínimo accesible (mínimo 48dp) y descripción de contenido para accesibilidad (`contentDescription`).

### 3.2 Capa de Dominio (Domain / Entities)
- Los modelos limpios de negocio se ubican en el paquete `entities/`.
- Deben definirse como `data class` inmutables sin dependencias a librerías de UI o frameworks de red:
  - `Producto`: `id`, `nombre`, `marca`, `precio`, `stock`, `descripcion`.
  - `Usuario`: `id`, `nombre`, `apellido`, `telefono`, `fechaNacimiento`, `correo`, `password`, `rol`.

### 3.3 Capa de Red y Acceso a Datos (Data & Network Layer)
- **Cliente HTTP**: Configurado como Singleton en `utils/RetrofitClient.kt`.
- **Servicios de Red (`services/`)**:
  - `ProductoService.kt` y `UsuarioService.kt` definen los contratos HTTP contra Firestore REST.
  - Toda llamada a la red debe declararse como función suspendida (`suspend fun`).
  - Respetar la estructura de transporte de Google Cloud Firestore (campos envueltos en `{ "fields": { "campo": { "stringValue": "..." } } }`):
    - Usar `FirestoreFieldsContainer`, `FirestoreStringValue`, `FirestoreProductosResponse`, etc.
    - El ID del documento se extrae del path devuelto por Firebase: `doc.name.substringAfterLast("/")`.

---

## 4. Marco Ético y Reglas de Negocio 🛡️

Dado que VapeON es una aplicación para productos de vapeo y cigarrillos electrónicos:

1. **Protección a Menores de Edad (+18)**:
   - Toda cuenta de usuario debe contar obligatoriamente con el registro de `fechaNacimiento`.
   - El sistema no debe permitir acceso a compras a usuarios que no verifiquen mayoría de edad legal.
2. **Control de Acceso Basado en Roles (RBAC)**:
   - Rol `CLIENTE`: Navegación en catálogo y visualización de productos en modo solo lectura.
   - Rol `ADMIN`: Acceso a `AdminHomeScreen` y permisos para crear (`AgregarProductoScreen`), actualizar (`EditarProductoScreen`) y eliminar productos (`DELETE`).
   - El agente no debe exponer controles de edición o eliminación a usuarios con rol `CLIENTE`.
3. **Transparencia en el Catálogo**:
   - La información de precios, stock y especificaciones de marcas (**LifePod**, **Oxbar**, **Nexa**) debe ser veraz y fiel a los datos del inventario.
4. **Seguridad y Privacidad de Datos**:
   - Todas las llamadas REST deben ejecutarse exclusivamente sobre conexiones seguras **HTTPS / TLS**.
   - No exponer contraseñas en logs de depuración (`println` o `Log.d`).

---

## 5. Convenciones de Código y Estilo

- **Nomenclatura**:
  - Funciones `@Composable`: PascalCase (ej. `CatalogoScreen`, `ProductCard`).
  - Variables de estado y lambdas: camelCase (ej. `productosFirebase`, `cargando`, `irDetalleProducto`).
  - Constantes: SCREAMING_SNAKE_CASE (ej. `BASE_URL`, `PROJECT_ID`).
- **Manejo de Errores y UI States**:
  - Toda corrutina que invoque servicios de red (`scope.launch`) debe estar protegida con bloques `try-catch`.
  - Debe reflejarse visualmente el estado de carga (`CircularProgressIndicator`) y el estado de error (`Text` con color `MaterialTheme.colorScheme.error`).
- **Gestión de Recursos**:
  - Textos descriptivos e íconos en `res/` cuando corresponda.
  - Para íconos estándar, priorizar `androidx.compose.material.icons.Icons`.

---

## 6. Documentación y Especificaciones (`doc/`)

- Al crear o refactorizar funcionalidades de gran escala, se debe consultar o actualizar la especificación en `doc/spec.md`.
- Para generar nuevas especificaciones técnicas y éticas, utilizar el skill configurado en `.agents/skills/spec-writer/`.
