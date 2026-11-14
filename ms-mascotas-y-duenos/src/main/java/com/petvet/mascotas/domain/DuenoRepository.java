import java.util.List;
import java.util.Optional;

public interface DuenoRepository {

    // Register a new owner or save changes
    Dueno guardar(Dueno dueno);

    // Find an owner using their ID
    Optional<Dueno> buscarPorId(Long duenoId);

    // Find an owner using their email
    Optional<Dueno> buscarPorCorreo(String correoDueno);

    // List all registered owners
    List<Dueno> listar();

    // Update owner information
    Dueno actualizar(Dueno dueno);

    // Delete an owner using their ID
    void eliminar(Long duenoId);
}
