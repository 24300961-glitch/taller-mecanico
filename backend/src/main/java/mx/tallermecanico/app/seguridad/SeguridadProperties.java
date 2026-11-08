package mx.tallermecanico.app.seguridad;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ConfigurationProperties(prefix = "app.seguridad")
public class SeguridadProperties {

    private String rolRegistroPublico;
    private String rolAdministradorInicial;
    private String actorSistema = "SISTEMA";
    private List<RolInicial> rolesIniciales = new ArrayList<>();
    private Map<String, String> equivalenciasLegado = new LinkedHashMap<>();

    public String getRolRegistroPublico() { return rolRegistroPublico; }
    public void setRolRegistroPublico(String v) { this.rolRegistroPublico = v; }
    public String getRolAdministradorInicial() { return rolAdministradorInicial; }
    public void setRolAdministradorInicial(String v) { this.rolAdministradorInicial = v; }
    public String getActorSistema() { return actorSistema; }
    public void setActorSistema(String v) { this.actorSistema = v; }
    public List<RolInicial> getRolesIniciales() { return rolesIniciales; }
    public void setRolesIniciales(List<RolInicial> v) { this.rolesIniciales = v; }
    public Map<String, String> getEquivalenciasLegado() { return equivalenciasLegado; }
    public void setEquivalenciasLegado(Map<String, String> v) { this.equivalenciasLegado = v; }

    public static class RolInicial {
        private String nombre;
        private Set<Permiso> permisos = EnumSet.noneOf(Permiso.class);
        private boolean asignable = true;

        public String getNombre() { return nombre; }
        public void setNombre(String v) { this.nombre = v; }
        public Set<Permiso> getPermisos() { return permisos; }
        public void setPermisos(Set<Permiso> v) { this.permisos = v == null ? EnumSet.noneOf(Permiso.class) : v; }
        public boolean isAsignable() { return asignable; }
        public void setAsignable(boolean v) { this.asignable = v; }
    }
}
