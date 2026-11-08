---
title: Arquitectura del Sistema - Taller Mecánico
---
graph TB
    subgraph Docker["🐳 Infraestructura Docker"]
        direction TB
        subgraph Contenedor_App["Contenedor taller-app :8080"]
            subgraph Presentacion["🖥️ Presentación (Thymeleaf)"]
                V1[login.html]
                V2[registro.html]
                V3[dashboard.html]
                V4[talleres.html]
                V5[clientes.html]
                V6[vehiculos.html]
                V7[usuarios.html]
                V8[citas.html]
                V9["fragments/layout.html"]
            end
            subgraph Controladores["🔀 Controladores (@RestController / @Controller)"]
                C1[TallerController]
                C2[ClienteController]
                C3[VehiculoController]
                C4[RolController]
                C5[AuthController]
                C6[CodigoPostalController]
                C7[WebViewController]
                C8[ApiExceptionHandler]
            end
            subgraph Servicios["⚙️ Servicios (@Service)"]
                S1[TallerService]
                S2[ClienteService]
                S3[VehiculoService]
                S4[RolService]
                S5[AuthService]
                S6[CodigoPostalService]
                S7[AuditService]
                S8[SesionService]
                S9[FotoTallerStorage]
                subgraph Estrategias["🔌 Estrategias (Strategy)"]
                    E1[DashboardStrategyFactory]
                    E2[AdministradorDashboardStrategy]
                    E3[ClienteDashboardStrategy]
                    E4[RecepcionistaDashboardStrategy]
                    E5[SmsRecoveryStrategy]
                    E6[UserCreationService]
                end
            end
            subgraph Repositorios["🗄️ Repositorios (JPA)"]
                R1[TallerRepository]
                R2[ClienteRepository]
                R3[VehiculoRepository]
                R4[RolRepository]
                R5[UserRepository]
                R6[CodigoPostalRepository]
                R7[AuditLogRepository]
            end
            subgraph Modelos["📦 Modelos (@Entity)"]
                M1[Taller]
                M2[Cliente]
                M3[Vehiculo]
                M4[Rol / Permiso]
                M5[UserAccount]
                M6[CodigoPostal]
                M7[AuditLog]
            end
            subgraph Seeders["🌱 Seeders"]
                SE1[SepomexCsvSeeder]
                SE2[InicializadorSistema]
            end
        end
        subgraph Contenedor_MySQL["Contenedor taller-mysql :3306"]
            DB[(MySQL 8.4\ntaller_mecanico)]
        end
    end
    subgraph Almacenamiento_Host["📁 Host (volúmenes montados)"]
        FS1[storage/talleres/ - fotos]
        FS2[logs/ - taller-mecanico.log]
        FS3[data/sepomex/CPdescarga.txt]
    end

    Presentacion --> Controladores
    Controladores --> Servicios
    Servicios --> Repositorios
    Repositorios --> Modelos
    Modelos --> DB
    Seeders --> DB
    S9 --> FS1
    S7 --> FS2
    SE1 --> FS3

    style Presentacion fill:#1565C0,color:#fff
    style Controladores fill:#6A1B9A,color:#fff
    style Servicios fill:#1B5E20,color:#fff
    style Estrategias fill:#2E7D32,color:#fff
    style Repositorios fill:#E65100,color:#fff
    style Modelos fill:#880E4F,color:#fff
    style Seeders fill:#37474F,color:#fff
    style DB fill:#0D47A1,color:#fff
    style Almacenamiento_Host fill:#263238,color:#fff
    style Docker fill:#01579B,color:#fff
