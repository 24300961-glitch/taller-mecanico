---
title: Visión General del Sistema - Encaje de Fases y Strategy
---
flowchart LR
    subgraph Fase0["Fase 0: Autenticación y Usuarios"]
        direction TB
        AUTH[Spring Security\nJSESSIONID + CSRF]
        LOGIN[login.html]
        REGISTRO[registro.html]
        RECOVER[recuperar / reset-password]
        ROLES[Roles y Permisos\n7 roles configurables]
        AUTH --> LOGIN
        AUTH --> REGISTRO
        AUTH --> RECOVER
        AUTH --> ROLES
    end

    subgraph Strategy["Patrón Strategy - Solo Autenticación y Dashboard"]
        direction TB
        subgraph AuthStrategy["Auth Strategy"]
            AS1[EmailRecoveryStrategy]
            AS2[SmsRecoveryStrategy]
            AS3[CreacionCuentaPorRolStrategy]
        end
        subgraph DashStrategy["Dashboard Strategy"]
            DS1[AdministradorDashboardStrategy]
            DS2[ClienteDashboardStrategy]
            DS3[RecepcionistaDashboardStrategy]
            DS4[MaestroMecanicoDashboardStrategy]
            DS5[AsesorServicioDashboardStrategy]
            DS6[TecnicoConsultaDashboardStrategy]
        end
    end

    subgraph Fase1["Fase 1: Talleres y Clientes"]
        direction TB
        TALLER[Taller\nRN-01 a RN-12]
        CLIENTE[Cliente\nN:M con Taller]
        VEHICULO[Vehículo\n1 taller por coche]
        SEPOMEX[SEPOMEX\ncatalogo_codigos_postales\n~145k registros]
        FOTO[Fotos\nJPG/JPEG/PNG/WEBP\n≤15 MB]
        TALLER --> CLIENTE
        TALLER --> VEHICULO
        SEPOMEX --> TALLER
        FOTO --> TALLER
    end

    subgraph Datos["Base de Datos MySQL"]
        direction TB
        DB_U[(users)]
        DB_R[(roles / rol_permisos)]
        DB_T[(talleres)]
        DB_C[(clientes)]
        DB_TC[(taller_cliente)]
        DB_V[(vehiculos)]
        DB_CP[(catalogo_codigos_postales)]
        DB_AL[(audit_logs)]
        DB_PR[(password_reset_tokens)]
    end

    Fase0 -->|Sesión activa\nPermiso ROLES_ASIGNAR| Strategy
    Fase0 -->|Precondición:\nsesión activa| Fase1
    Strategy -.->|No interviene en\ntalleres/clientes/vehículos| Fase1
    Fase1 --> Datos
    Fase0 --> Datos

    style Fase0 fill:#1A237E,color:#fff
    style Strategy fill:#1B5E20,color:#fff
    style Fase1 fill:#880E4F,color:#fff
    style Datos fill:#0D47A1,color:#fff
    style AuthStrategy fill:#2E7D32,color:#fff
    style DashStrategy fill:#2E7D32,color:#fff
