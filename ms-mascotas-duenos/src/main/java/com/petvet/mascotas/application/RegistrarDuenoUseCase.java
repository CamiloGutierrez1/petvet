package com.petvet.mascotas.application;

import com.petvet.mascotas.domain.ConsultaDuenos;
import com.petvet.mascotas.domain.CorreoRepetidoException;
import com.petvet.mascotas.domain.Dueno;
import com.petvet.mascotas.domain.RegistroDuenos;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrarDuenoUseCase {
    /** La misma expresión con la que Citas valida el correo de la ficha (MascotasRestClient). */
    private static final Pattern CORREO = Pattern.compile("[^\\s@]+@[^\\s@]+\\.[^\\s@]+");

    private final ConsultaDuenos consulta;
    private final RegistroDuenos registro;

    public RegistrarDuenoUseCase(ConsultaDuenos consulta, RegistroDuenos registro) {
        this.consulta = consulta;
        this.registro = registro;
    }

    /**
     * Guarda el correo sin espacios y en minúsculas, así es único sin distinguir mayúsculas. Si dos
     * registros simultáneos pasan la consulta, la restricción UNIQUE de la base responde el mismo 409.
     */
    @Transactional
    public Dueno ejecutar(DatosDueno datos) {
        if (datos == null) {
            throw new IllegalArgumentException("Los datos del dueño son obligatorios.");
        }
        if (datos.email() == null || datos.email().isBlank()) {
            throw new IllegalArgumentException("El correo del dueño es obligatorio.");
        }
        if (datos.nombre() == null || datos.nombre().isBlank()) {
            throw new IllegalArgumentException("El nombre del dueño es obligatorio.");
        }
        String correo = datos.email().strip().toLowerCase(Locale.ROOT);
        if (!CORREO.matcher(correo).matches()) {
            throw new IllegalArgumentException("El correo del dueño no es válido. Ejemplo: ana@correo.com.");
        }
        if (consulta.buscarPorCorreo(correo).isPresent()) {
            throw new CorreoRepetidoException();
        }
        return registro.guardar(new Dueno(null, datos.nombre().strip(), opcional(datos.documento()), correo,
                opcional(datos.telefono()), opcional(datos.direccion())));
    }

    private static String opcional(String texto) {
        return texto == null || texto.isBlank() ? null : texto.strip();
    }
}
