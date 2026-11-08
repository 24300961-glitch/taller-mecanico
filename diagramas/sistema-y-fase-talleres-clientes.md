# Sistema actual y fase de talleres/clientes

```mermaid
flowchart LR
    subgraph AUTH[Autenticación y seguridad existentes]
        Login[Login / recuperación] --> AuthStrategy[Strategy de autenticación y recuperación]
        AuthStrategy --> Users[(users)]
        UserAdmin[Creación de usuarios] --> UserStrategy[Strategy de creación por rol]
        UserStrategy --> Users
        Users --> Roles[(roles y permisos)]
        Roles --> DashboardStrategy[Strategy de dashboard por rol]
        DashboardStrategy --> Views[Vistas por rol]
    end

    subgraph FASE[Fase de talleres y clientes]
        Admin[Administrador] --> TallerReg[Registro y gestión de talleres]
        TallerReg --> Taller[(talleres)]
        Sepomex[Catálogo local SEPOMEX] --> TallerReg
        Admin --> ClienteReg[Registro de clientes]
        Recepcion[Recepcionista] --> ClienteReg
        Taller --> ClienteReg
        ClienteReg --> Cliente[(clientes)]
        Cliente --- TallerCliente[(asociación taller-cliente)]
        Taller --- TallerCliente
        Cliente --> Vehiculo[(vehiculos existentes)]
        Taller --- VehiculoTaller[(asignación vehículo-taller)]
        Vehiculo --- VehiculoTaller
        ClienteVista[Cliente consulta taller asignado a su coche] --> VehiculoTaller
        PersonalVista[Administrador/recepción consulta taller asignado y propietario] --> VehiculoTaller
    end

    AUTH --> FASE
```

La Strategy se muestra únicamente en autenticación, creación de cuentas, recuperación y dashboard, donde ya existe. Talleres, clientes, sus asociaciones, vehículos y SEPOMEX se representan sin Strategy. Un cliente puede asociarse a varios talleres; cada vehículo tiene una asignación propia a un taller específico, independiente de esa asociación.
