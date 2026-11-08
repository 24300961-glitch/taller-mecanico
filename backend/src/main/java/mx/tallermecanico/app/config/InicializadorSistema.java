package mx.tallermecanico.app.config;

import mx.tallermecanico.app.auth.UserRepository;
import mx.tallermecanico.app.usuarios.strategy.UserCreationService;
import mx.tallermecanico.app.common.Texto;
import mx.tallermecanico.app.seguridad.Rol;
import mx.tallermecanico.app.seguridad.RolRepository;
import mx.tallermecanico.app.seguridad.SeguridadProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.List;
import java.util.Locale;

@Component
@Order(0)
public class InicializadorSistema implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(InicializadorSistema.class);

    private final SeguridadProperties props;
    private final RolRepository roles;
    private final UserRepository users;
    private final UserCreationService userCreation;
    private final JdbcTemplate jdbc;

    @Value("${app.admin.email:}") private String adminEmail;
    @Value("${app.admin.password:}") private String adminPassword;
    @Value("${app.admin.nombre:Administrador de taller}") private String adminNombre;

    /** Construye el inicializador de catálogo y cuentas del taller.
     * @param props roles de inicio y equivalencias heredadas
     * @param roles repositorio del catálogo
     * @param users repositorio de cuentas
     * @param userCreation creación Strategy por rol
     * @param jdbc acceso SQL para migraciones heredadas
     */
    public InicializadorSistema(SeguridadProperties props, RolRepository roles,
                                UserRepository users, UserCreationService userCreation,
                                JdbcTemplate jdbc) {
        this.props = props;
        this.roles = roles;
        this.users = users;
        this.userCreation = userCreation;
        this.jdbc = jdbc;
    }

    /** Sincroniza roles, migra cuentas heredadas y crea únicamente el Administrador bootstrap.
     * @param args argumentos del proceso Spring Boot
     * @return No aplica; modifica datos de inicialización dentro de una transacción.
     * @throws IllegalStateException si la configuración esencial de roles es inválida
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        sincronizarRolesIniciales();
        migrarRolesLegado();
        crearAdministradorInicial();
    }

    /** Actualiza permisos y crea los roles declarados en configuración.
     * @return No aplica; guarda los roles configurados.
     */
    private void sincronizarRolesIniciales() {
        for (SeguridadProperties.RolInicial inicial : props.getRolesIniciales()) {
            String clave = Texto.clave(inicial.getNombre());
            if (clave == null) continue;
            Rol rol = roles.findByClave(clave).orElseGet(() -> {
                log.info("[Init] Creando rol inicial configurado: {}", inicial.getNombre());
                return new Rol(inicial.getNombre());
            });
            rol.otorgar(inicial.getPermisos());
            rol.setAsignable(inicial.isAsignable());
            roles.save(rol);
        }
    }

    /** Migra la columna histórica `users.role` al catálogo relacional de roles.
     * @return No aplica; las filas compatibles se actualizan y se quita la columna obsoleta.
     * @throws org.springframework.dao.DataAccessException si falla una consulta de migración
     */
    private void migrarRolesLegado() {
        String columna = buscarColumnaLegado();
        if (columna == null) return;
        log.warn("[Init] Se detectó la columna heredada users.{}; migrando a la tabla roles", columna);
        List<String> valores = jdbc.queryForList(
                "select distinct " + columna + " from users where rol_id is null and " + columna + " is not null", String.class);
        for (String valor : valores) {
            String destino = props.getEquivalenciasLegado().getOrDefault(valor, valor);
            Rol rol = roles.findByClave(Texto.clave(destino)).orElseGet(() -> roles.save(new Rol(destino)));
            int n = jdbc.update("update users set rol_id = ? where rol_id is null and " + columna + " = ?", rol.getId(), valor);
            log.info("[Init] {} usuario(s) migrados de '{}' a rol '{}'", n, valor, rol.getNombre());
        }
        jdbc.execute("alter table users drop column " + columna);
        log.info("[Init] Columna heredada users.{} eliminada", columna);
    }

    /** Detecta el nombre exacto de una columna heredada llamada role.
     * @return nombre SQL encontrado o null si no existe
     * @throws org.springframework.dao.DataAccessException si no se pueden leer metadatos
     */
    private String buscarColumnaLegado() {
        return jdbc.execute((java.sql.Connection con) -> {
            DatabaseMetaData md = con.getMetaData();
            for (String tabla : List.of("users", "USERS")) {
                try (ResultSet rs = md.getColumns(con.getCatalog(), null, tabla, null)) {
                    while (rs.next()) {
                        String nombre = rs.getString("COLUMN_NAME");
                        if ("role".equals(nombre.toLowerCase(Locale.ROOT))) return nombre;
                    }
                }
            }
            return null;
        });
    }

    /** Crea el administrador bootstrap a través de la Strategy del rol administrador.
     * @return No aplica; persiste la cuenta si las variables iniciales están definidas.
     * @throws IllegalStateException si la configuración de rol administrador no existe
     */
    private void crearAdministradorInicial() {
        if (adminEmail == null || adminEmail.isBlank() || adminPassword == null || adminPassword.isBlank()) {
            if (users.count() == 0) {
                log.info("[Init] No hay usuarios. Define APP_ADMIN_EMAIL y APP_ADMIN_PASSWORD para crear el administrador inicial.");
            }
            return;
        }
        if (adminPassword.length() < 16) {
            log.warn("[Init] No se creó el administrador inicial: APP_ADMIN_PASSWORD debe tener al menos 16 caracteres.");
            return;
        }
        String email = Texto.correo(adminEmail);
        if (users.existsByEmail(email)) {
            log.info("[Init] El administrador inicial {} ya existe.", email);
            return;
        }
        Rol rol = roles.findByClave(Texto.clave(props.getRolAdministradorInicial()))
                .orElseThrow(() -> new IllegalStateException("No se configuró el rol administrador inicial"));
        userCreation.createForRole(email, adminNombre, adminPassword, adminPassword, null, rol.getId());
        log.info("[Init] Administrador inicial creado: {} con rol {}", email, rol.getNombre());
    }

}
