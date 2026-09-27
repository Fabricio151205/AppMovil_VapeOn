---
name: spec-writer
description: >-
  Use this skill whenever the user requests creating, updating, or reviewing technical, functional, or ethical specification documents (spec.md) inside the doc/ directory of the project, ensuring alignment with industry standards (RFC, ISO/IEEE, ethical software guidelines, and regulatory compliance).
---

# Skill: Generador de Especificaciones Técnicas y Éticas (`spec.md`)

Este skill define el procedimiento estándar para documentar especificaciones técnicas y éticas completas en proyectos de software, ubicadas convencionalmente en la carpeta `doc/` en la raíz del repositorio.

---
## IMPORTANTE:
Siempre preguntar al usuario en cual archivo desea agregar lo descrito, antes de hacer cualquier cambio en el codigo.
Si se van a tocar mas de un archivo, especificado por el usuario el sistema debe informar cuales son los archivos y siempre especificar para que se tocara ese archivo, luego se debera esperar a que el usuario afirme o niegue la peticion hecha.

## 1. Convención de Ubicación del Directorio `doc/`

En la industria del software (estándares de repositorios en GitHub, proyectos de código abierto de Google, Apache y Linux Foundation), la documentación de diseño y especificaciones técnicas se ubica en la **raíz del repositorio**:

```text
<raíz-del-proyecto>/
├── doc/
│   ├── spec.md                 # Especificación general del sistema o módulo principal
│   └── specs/                  # (Opcional) Especificaciones por feature o épica
│       ├── auth_spec.md
│       └── catalog_spec.md
```

**Regla**: Si la carpeta `doc/` no existe en la raíz, créala antes de generar el documento.

---

## 2. Estructura Obligatoria de un `spec.md`

Todo documento `spec.md` generado por este skill debe contener las siguientes secciones:

### I. Metadatos y Control de Versiones
- **Título del Sistema / Feature**
- **Versión**: SemVer (ej. `1.0.0`)
- **Estado**: `Borrador` | `En Revisión` | `Aprobado` | `Deprecado`
- **Fecha de Creación / Actualización**
- **Autores / Stakeholders**

### II. Resumen y Contexto del Negocio
- **Objetivo**: Qué problema resuelve la solución.
- **Alcance (Scope)**: Qué está dentro y fuera del alcance (*In-Scope* vs *Out-of-Scope*).
- **Usuarios Objetivo y Personas**: Perfiles que interactuarán con el sistema.

### III. Especificación Ética y Cumplimiento Normativo (Ethical Spec)
*Esta sección es fundamental para sistemas que manejan datos sensibles, comercio regulado (ej. vapeo, productos para mayores de edad) o algoritmos de decisión.*
- **Principio de No Maleficencia y Responsabilidad Social**: Restricciones de uso, protección de poblaciones vulnerables (ej. verificación estricta de mayoría de edad +18).
- **Privacidad y Protección de Datos (Data Privacy & Compliance)**: Cumplimiento de leyes de protección de datos (GDPR, Habeas Data, encriptación en tránsito y en reposo).
- **Transparencia y Consentimiento Informado**: Avisos legales, advertencias sanitarias claras, políticas de cookies y términos visibles.
- **Seguridad Ética y Mitigación de Riesgos**: Medidas contra suplantación, accesos no autorizados y uso ilícito.

### IV. Especificación Técnica y Arquitectura (Technical Spec)
- **Patrón de Arquitectura**: Single-Activity, MVVM, Clean Architecture, etc.
- **Stack y Dependencias**: Lenguaje, frameworks, librerías y versiones.
- **Modelo de Datos y Esquemas**: Entidades, tipos, validaciones y relaciones.
- **Contratos de API / Endpoints**: Rutas, métodos HTTP, cabeceras, payloads de entrada/salida y códigos de estado.
- **Requerimientos No Funcionales (NFR)**: Rendimiento, latencia, escalabilidad, disponibilidad y compatibilidad de plataforma (Android minSdk).

### V. Interfaz de Usuario y Experiencia (UI/UX Spec)
- Flujo de pantallas y diagrama de navegación.
- Componentes clave y estados (Cargando, Vacío, Error, Éxito).
- Criterios de accesibilidad (contraste de colores, soporte para TalkBack, tamaños táctiles mínimos de 48dp).

### VI. Criterios de Aceptación y Pruebas (QA Spec)
- Criterios de aceptación en formato BDD (*Given-When-Then* / *Dado-Cuando-Entonces*).
- Casos de prueba críticos (Happy Path, Edge Cases, Error Cases).

---

## 3. Procedimiento Paso a Paso para el Agente

Cuando se active este skill:

1. **Analizar el Contexto del Proyecto**:
   - Inspeccionar el código fuente del proyecto (`app/`, modelos, servicios, controladores) para extraer información verídica y actualizada sobre la arquitectura y funcionalidades.
2. **Identificar Consideraciones Éticas Específicas**:
   - Para aplicaciones de vapeo o productos con restricción: enfatizar validación de edad (+18), advertencias de salud y publicidad responsable.
   - Para aplicaciones con credenciales: políticas de contraseñas seguras, almacenamiento encriptado y prevención de fugas de datos.
3. **Redactar `doc/spec.md`**:
   - Utilizar el archivo de plantilla ubicado en `references/template.md`.
   - Llenar cada sección con detalles concretos del proyecto, sin placeholders genéricos (`TODO` o `TBD`).
4. **Validar Formato y Enlaces**:
   - Asegurarse de que el archivo markdown esté bien formateado, incluya diagramas Mermaid cuando aporte valor y cuente con tablas legibles.
