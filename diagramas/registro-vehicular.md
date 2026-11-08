---
title: Proceso de Registro Vehicular
---
flowchart TD
    A([Inicio: Admin o Recepcionista\ncon sesión activa]) --> B{¿Existe al menos\nun taller activo?}
    B -- No --> Z([Error: no se puede registrar\nun vehículo sin taller activo])
    B -- Sí --> C{¿El cliente\nya existe?}
    C -- No --> D[Registrar cliente primero\ncon asociación a taller]
    D --> E[POST /api/clientes con tallerIds]
    E --> F([Cliente creado y\nasociado al taller])
    C -- Sí --> G[Seleccionar cliente\nexistente]
    F --> H
    G --> H[Abrir formulario de vehículo:\nplaca, marca, modelo, año,\nVIN opcional, color, taller]
    H --> I{¿Validación\nfrontend OK?\nPlaca formato, VIN 17 chars,\naño 1900-actual+1}
    I -- Fallo --> J([Marcar campo erróneo,\nno enviar])
    I -- OK --> K[POST /api/vehiculos\ncon clienteId y tallerId]
    K --> L{¿Placa o VIN\nya existe?}
    L -- Sí --> M([409: Front muestra\nvehículo existente])
    L -- No --> N[Normalizar placa\na mayúsculas sin espacios]
    N --> O[Persistir en\ntabla vehiculos\ncon taller_id]
    O --> P[Devolver 201\ncon datos del vehículo]
    P --> Q([Front confirma;\ndetalle cliente\nmuestra nuevo vehículo])

    style A fill:#2196F3,color:#fff
    style Q fill:#4CAF50,color:#fff
    style Z fill:#F44336,color:#fff
    style J fill:#F44336,color:#fff
    style M fill:#FF9800,color:#fff
