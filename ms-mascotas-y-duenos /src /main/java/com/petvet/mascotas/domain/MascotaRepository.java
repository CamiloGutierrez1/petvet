import java.util.List;
import java.util.Optional;

public interface MascotaRepository {

    Mascota guardar(Mascota mascota);

    Optional<Mascota> buscarPorId(Long id);

    List<Mascota> buscarPorDueno(Long duenoId);

    List<Mascota> listarTodas();

    Mascota actualizar(Mascota mascota);

    void eliminar(Long id);
}
