import java.time.LocalDate;

public class Mascota {

    private Long id;
    private String nombre;
    private String especie;
    private String raza;
    private LocalDate fechaNacimiento;
    private double peso;
    private Long duenoId;
    private String fotoUrl;

    public Mascota(Long id, String nombre,
                   String especie, String raza,
                   LocalDate fechaNacimiento,
                   double peso, Long duenoId,
                   String fotoUrl) {

        this.id = id;
        this.nombre = nombre;
        this.especie = especie;
        this.raza = raza;
        this.fechaNacimiento = fechaNacimiento;
        this.peso = peso;
        this.duenoId = duenoId;
        this.fotoUrl = fotoUrl;
    }

    public String getFotoUrl() {
        return fotoUrl;
    }

    public void setFotoUrl(String fotoUrl) {
        this.fotoUrl = fotoUrl;
    }

    public Long getDuenoId() {return duenoId;}
}