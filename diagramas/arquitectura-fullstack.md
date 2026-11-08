---
title: Arquitectura Fullstack - Taller Norte (React + Spring Boot)
---
graph TB
    subgraph Navegador["Navegador - frontend/ (React 18 + Vite)"]
        direction TB
        Rutas["React Router (12 rutas)<br/>guardias por permiso"]
        Paginas["Paginas<br/>Login, Registro, Recuperar, Reset,<br/>Dashboard, Clientes, Vehiculos,<br/>Talleres, Historial, Usuarios, Roles"]
        Comp["Componentes<br/>Layout, BrandPanel, Modal,<br/>PasswordConOjo, Toasts"]
        Api["api.js + AuthContext<br/>fetch + cookie JSESSIONID + CSRF"]
        Build["vite build - dist/<br/>index.html + assets/ con hash"]
    end
    subgraph Contenedor_App["Contenedor taller-app :8080 - backend/ (Spring Boot)"]
        direction TB
        Spa["WebViewController<br/>forward:/index.html<br/>(conserva @PreAuthorize)"]
        Rest["REST /api/*<br/>auth, clientes, vehiculos, talleres,<br/>roles, sepomex, dashboard, storage"]
        Seg["Security<br/>sesion + CSRF + formLogin /login<br/>logout POST /logout"]
        Svc["Servicios + JPA + Flyway V1-V5"]
        Static["static/ servido por Spring<br/>index.html, assets/, diagramas/<br/>error.html (fallback /error)"]
    end
    subgraph Contenedor_MySQL["Contenedor taller-mysql :3306"]
        DB[(MySQL 8.4<br/>taller_mecanico)]
    end
    subgraph Host["Host (volúmenes y repo)"]
        FS1[storage/talleres/ - fotos]
        FS2[logs/ - taller-mecanico.log]
        FS3[data/sepomex/ - catalogo]
        DG[diagramas/ - SVG + .md]
    end

    Rutas --> Paginas
    Paginas --> Comp
    Comp --> Api
    Api --> Rest
    Build --> Static
    Spa --> Static
    Rest --> Seg
    Seg --> Svc
    Svc --> DB
    Svc --> FS1
    DG -. visible en .-> Rest

    style Navegador fill:#1565C0,color:#fff
    style Contenedor_App fill:#0f172a,color:#fff
    style Contenedor_MySQL fill:#0D47A1,color:#fff
    style Host fill:#263238,color:#fff
    style DB fill:#0D47A1,color:#fff
