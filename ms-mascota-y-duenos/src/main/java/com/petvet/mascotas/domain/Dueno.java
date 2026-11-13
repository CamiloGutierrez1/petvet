public class Dueno {

    private Long duenoId;
    private String nombreDueno;
    private String direccion;
    private int numeroTelefono;
    private int numeroTelefonoEmergencias;
    private String correoDueno;

    public Dueno(Long duenoId, String nombreDueno, String direccion,
                   int numeroTelefono,
                   int numeroTelefonoEmergencias,
                 String correoDueno) {

        this.duenoId = duenoId;
        this.nombreDueno = nombreDueno;
        this.direccion = direccion;
        this.numeroTelefono = numeroTelefono;
        this.numeroTelefonoEmergencias = numeroTelefonoEmergencias;
        this.correoDueno = correoDueno;

    }

    public String getNombreDueno() {return nombreDueno;}
    public String getCorreoDueno() {return correoDueno;}
    public String getDireccion() {return direccion;}

}
