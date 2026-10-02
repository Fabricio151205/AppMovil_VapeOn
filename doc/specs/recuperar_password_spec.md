# Especificación Técnica y UI/UX - Recuperación de Contraseña (`recuperar_password_spec.md`)

| Atributo | Detalle |
| :--- | :--- |
| **Identificador** | `SPEC-VAPEON-RECUPERAR-001` |
| **Modulo / Pantalla** | `ForgotPasswordScreen` |
| **Versión** | `1.0.0` |
| **Estado** | `Aprobado` |
| **Fecha de Publicación** | `2026-10-02` |
| **Plataforma Objetivo** | Android Nativo (Jetpack Compose, Material 3) |
| **Ubicación del Documento** | `doc/specs/recuperar_password_spec.md` |

---

## 1. Resumen Ejecutivo y Alcance

### 1.1 Propósito
La pantalla `ForgotPasswordScreen` gestiona la solicitud de recuperación de credenciales de acceso para usuarios que han olvidado su contraseña. Guía al usuario en la introducción de su correo registrado para el envío de un código de verificación.

### 1.2 Alcance (Scope)
- **In-Scope**:
  - Presentación corporativa con logo oficial `R.drawable.logo_vapeon`.
  - Título dorado en negrita: **"¿Olvidaste tu Contraseña?"**.
  - Texto de instrucción claro para el usuario.
  - Campo de entrada estilizado `OutlinedTextField` para *"Correo Electronico"*.
  - Validación básica del campo de correo e informe visual en pantalla.
  - Botón rojo principal **"RECUPERAR CONTRASEÑA"** (`VapeOnRed`).
  - Divisor con la letra **"o"**.
  - Botón secundario rojo oscuro **"Volver"** (`VapeOnRedDark`) que redirige a `LoginScreen`.
  - Ajuste de márgenes con `statusBarsPadding()`.
- **Out-of-Scope**:
  - Servidor de correo SMTP o proveedor SMS para envío real de tokens en v1.0 (se simula la confirmación de envío).

---

## 2. Especificación de UI/UX y Componentes

- **Surface Container**: `VapeOnBackground` (`#0D0E15`) con `statusBarsPadding()`.
- **Logo**: `Image(painterResource(id = R.drawable.logo_vapeon))`, tamaño `170.dp`.
- **Título**: Text centrado `"¿Olvidaste tu\nContraseña?"` con `VapeOnGold` (`#EAA016`), tamaño `24.sp` y peso `FontWeight.Bold`.
- **Instrucciones**: Text centrado `"No te preocupes, es posible recuperarla.\nIngresa tu Email y recibiras un codigo de\nverificación"`.
- **Campo de Correo**: `OutlinedTextField` con fondo `#141620`, bordes `#4D3D22` y esquinas redondeadas `12.dp`.
- **Botones**:
  - Principal: `"RECUPERAR CONTRASEÑA"`, color `VapeOnRed` (`#ED2025`), forma píldora `RoundedCornerShape(50)`.
  - Secundario: `"Volver"`, color `VapeOnRedDark` (`#5B181C`), ejecuta el callback `volverLogin()`.

---

## 3. Criterios de Aceptación y Pruebas (BDD)

### Escenario 1: Solicitud de código de verificación
- **Dado que** el usuario ingresa su correo electrónico válido en `ForgotPasswordScreen`.
- **Cuando** pulsa el botón **"RECUPERAR CONTRASEÑA"**.
- **Entonces** el sistema valida el correo y muestra un mensaje informando que se ha enviado el código de verificación.

### Escenario 2: Retorno a la pantalla de Login
- **Dado que** el usuario decide no continuar con la recuperación.
- **Cuando** pulsa el botón **"Volver"**.
- **Entonces** la aplicación ejecuta `volverLogin()` y regresa a la pantalla de inicio de sesión.
