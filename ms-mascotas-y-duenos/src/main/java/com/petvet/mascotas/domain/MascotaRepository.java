import java.util.List;
import java.util.Optional;

public interface MascotaRepository {

    // Register a new pet or save changes
    Mascota guardar(Mascota mascota);

    // Find a pet using its ID
    Optional<Mascota> buscarPorId(Long id);

    // Find all pets belonging to an owner
    List<Mascota> buscarPorDueno(Long duenoId);

    // List all registered pets
    List<Mascota> listarTodas();

    // Update pet information
    Mascota actualizar(Mascota mascota);

    // Delete a pet using its ID
    void eliminar(Long id);
}
