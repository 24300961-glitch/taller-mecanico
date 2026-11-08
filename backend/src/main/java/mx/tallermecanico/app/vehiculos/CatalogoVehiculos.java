package mx.tallermecanico.app.vehiculos;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CatalogoVehiculos {

    private static final Map<String, List<String>> MARCAS = new LinkedHashMap<>();

    static {
        MARCAS.put("Nissan", List.of("Versa", "Sentra", "March", "NP300", "X-Trail", "Kicks"));
        MARCAS.put("Chevrolet", List.of("Aveo", "Onix", "Spark", "Silverado", "Tracker", "Equinox"));
        MARCAS.put("Volkswagen", List.of("Jetta", "Vento", "Golf", "Tiguan", "Polo", "Taos"));
        MARCAS.put("Toyota", List.of("Corolla", "Camry", "Hilux", "RAV4", "Yaris", "Tacoma"));
        MARCAS.put("Honda", List.of("Civic", "Accord", "CR-V", "City", "HR-V", "Fit"));
        MARCAS.put("Ford", List.of("Ranger", "Escape", "Mustang", "Bronco", "Maverick", "Explorer"));
        MARCAS.put("Mazda", List.of("Mazda3", "Mazda6", "CX-5", "CX-30", "CX-50", "MX-5"));
        MARCAS.put("Kia", List.of("Rio", "Forte", "Sportage", "Sorento", "Seltos", "K3"));
        MARCAS.put("Hyundai", List.of("Accent", "Elantra", "Tucson", "Creta", "Santa Fe", "Venue"));
        MARCAS.put("Jeep", List.of("Compass", "Wrangler", "Grand Cherokee", "Renegade", "Gladiator"));
        MARCAS.put("BMW", List.of("Serie 1", "Serie 3", "X1", "X3", "X5"));
        MARCAS.put("Mercedes-Benz", List.of("Clase A", "Clase C", "GLA", "GLC", "Sprinter"));
    }

    private CatalogoVehiculos() {}

    public static Map<String, List<String>> marcas() {
        return MARCAS;
    }

    public static boolean esValido(String marca, String modelo) {
        List<String> modelos = MARCAS.get(marca);
        return modelos != null && modelos.contains(modelo);
    }
}
