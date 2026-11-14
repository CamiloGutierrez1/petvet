public class RegistrarDuenoUseCase {

    private final DuenoRepository duenoRepository;

    public RegistrarDuenoUseCase(DuenoRepository duenoRepository) {
        this.duenoRepository = duenoRepository;
    }

    public Dueno registrar(Dueno dueno) {

        // Validar la información de dueño
        if (dueno == null) {
            throw new IllegalArgumentException(
                    "Los datos del dueño son obligatorios."
            );
        }

        if (dueno.getCorreoDueno() == null ||
                dueno.getCorreoDueno().isBlank()) {
            throw new IllegalArgumentException(
                    "El correo del dueño es obligatorio."
            );
        }

        return duenoRepository.guardar(dueno);
    }
}