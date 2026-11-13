public class RegistrarMascotaUseCase {

    private final MascotaRepository mascotaRepository;
    private final DuenoRepository duenoRepository;

    public RegistrarMascotaUseCase(
            MascotaRepository mascotaRepository,
            DuenoRepository duenoRepository) {

        this.mascotaRepository = mascotaRepository;
        this.duenoRepository = duenoRepository;
    }

    public Mascota registrar(Mascota mascota) {

        if (mascota == null) {
            throw new IllegalArgumentException(
                    "Los datos de la mascota son obligatorios."
            );
        }

        if (duenoRepository.buscarPorId(mascota.getDuenoId()) == null) {
            throw new IllegalArgumentException(
                    "El dueño no existe."
            );
        }

        return mascotaRepository.guardar(mascota);
    }
}
