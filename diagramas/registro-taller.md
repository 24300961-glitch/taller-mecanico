---
title: Proceso de Registro de Taller
---
flowchart TD
    A([Inicio: Admin con sesión activa]) --> B[Accede a /talleres]
    B --> C[Rellena formulario:\nnombre, RFC, email, teléfono]
    C --> D[Ingresa Código Postal]
    D --> E{¿CP en catálogo\nSEPOMEX?}
    E -- Sí --> F[Autocompletar colonia,\nmunicipio, estado]
    E -- No --> G[Admin escribe colonia,\nmunicipio, estado manualmente]
    F --> H[Adjunta foto opcional\nJPG/JPEG/PNG/WEBP ≤15 MB]
    G --> H
    H --> I{¿Validación\nfrontend OK?}
    I -- Fallo --> J([Marcar campo con error,\nno enviar])
    I -- OK --> K[POST /api/talleres\nmultipart/form-data]
    K --> L{Backend\nvalida RFC duplicado\no nombre+dirección}
    L -- 409 Conflicto --> M([Front muestra taller\nexistente como referencia])
    L -- OK --> N{¿Foto adjunta?}
    N -- Sí --> O[FotoTallerStorage:\nvalidar extensión y\nfirma binaria]
    O -- Fallo --> P([400 Bad Request])
    O -- OK --> Q[Guardar en\nstorage/talleres/uuid.ext]
    N -- No --> R
    Q --> R[Persistir registro\nen tabla talleres]
    R --> S[Devolver 201 con datos\ndel taller creado]
    S --> T([Front confirma con\ntodos los datos guardados])

    style A fill:#4CAF50,color:#fff
    style T fill:#4CAF50,color:#fff
    style J fill:#F44336,color:#fff
    style M fill:#FF9800,color:#fff
    style P fill:#F44336,color:#fff
