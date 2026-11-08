package mx.tallermecanico.app.web;

import mx.tallermecanico.app.auth.UserAccount;
import mx.tallermecanico.app.auth.UserRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Set;
import java.util.stream.Collectors;

@ControllerAdvice(assignableTypes = WebViewController.class)
public class VistaModelAdvice {

    private final UserRepository users;

    public VistaModelAdvice(UserRepository users) {
        this.users = users;
    }

    @ModelAttribute
    public void datosUsuario(Authentication auth, Model model) {
        if (auth == null || auth instanceof AnonymousAuthenticationToken || !auth.isAuthenticated()) {
            model.addAttribute("permisos", Set.of());
            return;
        }
        Set<String> permisos = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> !a.startsWith("ROLE_"))
                .collect(Collectors.toSet());
        model.addAttribute("permisos", permisos);
        UserAccount u = users.findByEmail(auth.getName()).orElse(null);
        if (u != null) {
            model.addAttribute("nombre", u.getFullName());
            model.addAttribute("email", u.getEmail());
            model.addAttribute("rol", u.getRol() == null ? "Sin rol" : u.getRol().getNombre());
        }
    }
}
