package mx.tallermecanico.app.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mx.tallermecanico.app.audit.AuditService;
import mx.tallermecanico.app.auth.UserRepository;
import mx.tallermecanico.app.auth.strategy.StrategyAuthenticationProvider;
import mx.tallermecanico.app.common.Texto;
import mx.tallermecanico.app.seguridad.Rol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    /** Crea el PasswordEncoder adaptativo que cifra irreversiblemente las contraseñas.
     * @return encoder BCrypt con costo 12
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /** Crea el registro de sesiones para expirar sesiones tras cambios de rol o contraseña.
     * @return registro en memoria asociado a las sesiones HTTP
     */
    @Bean
    SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    /** Publica eventos de sesión del contenedor servlet para Spring Security.
     * @return publisher de eventos HTTP de sesión
     */
    @Bean
    HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    /** Carga cuentas y permisos desde los repositorios.
     * @param users repositorio de usuarios
     * @return servicio para resolver cuentas por correo normalizado
     */
    @Bean
    UserDetailsService userDetailsService(UserRepository users) {
        return email -> users.findByEmail(Texto.correo(email))
                .map(u -> User.withUsername(u.getEmail())
                        .password(u.getPasswordHash())
                        .authorities(autoridades(u.getRol()))
                        .disabled(!u.isEnabled())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
    }

    /** Convierte el rol y sus permisos en autoridades de Spring Security.
     * @param rol rol persistido; null produce una lista vacía
     * @return autoridades ROLE_* y permisos funcionales
     */
    static List<GrantedAuthority> autoridades(Rol rol) {
        List<GrantedAuthority> lista = new ArrayList<>();
        if (rol == null) return lista;
        lista.add(new SimpleGrantedAuthority("ROLE_" + rol.getClave()));
        rol.getPermisos().forEach(p -> lista.add(new SimpleGrantedAuthority(p.name())));
        return lista;
    }

    /** Configura sesión, formulario de acceso, control CSRF, permisos y el proveedor Strategy.
     * @param http constructor de la cadena de filtros web
     * @param users repositorio utilizado por el manejador de acceso exitoso
     * @param audit servicio para eventos de inicio de sesión
     * @param sessionRegistry registro para invalidar sesiones cambiadas
     * @param strategyProvider proveedor que delega credenciales a la estrategia del rol
     * @return cadena de filtros de Spring Security
     * @throws Exception si Spring no puede construir la cadena de seguridad
     */
    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, UserRepository users, AuditService audit,
                                    SessionRegistry sessionRegistry, StrategyAuthenticationProvider strategyProvider) throws Exception {
        return http
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/", "/login", "/registro", "/recuperar", "/reset-password", "/error",
                                "/favicon.ico", "/favicon.svg", "/index.html").permitAll()
                        .requestMatchers("/api/auth/register", "/api/auth/forgot-password",
                                "/api/auth/reset-password", "/api/auth/csrf",
                                "/api/auth/registro-info", "/api/auth/validar-token").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/assets/**").permitAll()
                        .anyRequest().authenticated()
                )
                .csrf(csrf -> csrf.ignoringRequestMatchers(AntPathRequestMatcher.antMatcher("/h2-console/**")))
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .sessionManagement(s -> s
                        .sessionFixation(f -> f.migrateSession())
                        .maximumSessions(-1)
                        .sessionRegistry(sessionRegistry)
                        .expiredUrl("/login?expirada=true"))
                .authenticationProvider(strategyProvider)
                .formLogin(f -> f
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .successHandler((req, res, auth) -> {
                            users.findByEmail(auth.getName()).ifPresent(u -> {
                                u.touchLastLogin();
                                users.save(u);
                            });
                            audit.recordLogin(auth.getName(), true, "Inicio de sesión correcto");
                            res.sendRedirect("/dashboard");
                        })
                        .failureHandler((req, res, ex) -> {
                            String email = req.getParameter("email");
                            boolean desactivada = ex instanceof DisabledException;
                            audit.recordLogin(email == null ? "" : Texto.correo(email), false,
                                    desactivada ? "Cuenta desactivada" : "Credenciales inválidas");
                            res.sendRedirect(desactivada ? "/login?error=desactivada" : "/login?error=credenciales");
                        })
                        .permitAll()
                )
                .logout(l -> l
                        .logoutRequestMatcher(AntPathRequestMatcher.antMatcher(org.springframework.http.HttpMethod.POST, "/logout"))
                        .logoutSuccessUrl("/login?logout=true")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> {
                            log.warn("[Security] Acceso sin sesión a: {} desde {}", req.getRequestURI(), req.getRemoteAddr());
                            if (esApi(req)) {
                                json(res, HttpServletResponse.SC_UNAUTHORIZED,
                                        "Tu sesión expiró o no has iniciado sesión. Inicia sesión para continuar.");
                            } else {
                                res.sendRedirect("/login?sesion=requerida");
                            }
                        })
                        .accessDeniedHandler((req, res, ex) -> {
                            log.warn("[Security] Acceso denegado a: {} para usuario: {}", req.getRequestURI(), req.getRemoteUser());
                            if (esApi(req)) {
                                json(res, HttpServletResponse.SC_FORBIDDEN,
                                        "No tienes permiso para realizar esta acción o la página expiró. Recarga e inténtalo de nuevo.");
                            } else {
                                res.sendError(HttpServletResponse.SC_FORBIDDEN);
                            }
                        })
                )
                .build();
    }

    /** Comprueba si la solicitud solicita una ruta de API.
     * @param req solicitud servlet
     * @return true si la ruta empieza por /api/
     */
    private static boolean esApi(HttpServletRequest req) {
        return req.getRequestURI().startsWith("/api/");
    }

    /** Escribe una respuesta JSON pequeña para rechazos de seguridad.
     * @param res respuesta servlet a completar
     * @param status código HTTP a usar
     * @param mensaje descripción segura para el cliente
     * @throws IOException si el flujo de respuesta no puede escribirse
     */
    private static void json(HttpServletResponse res, int status, String mensaje) throws IOException {
        res.setStatus(status);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String cuerpo = "{\"timestamp\":\"" + Instant.now() + "\",\"status\":" + status
                + ",\"message\":\"" + mensaje.replace("\"", "\\\"") + "\"}";
        res.getWriter().write(cuerpo);
    }
}
