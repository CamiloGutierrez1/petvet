public class Dueno {

    private Long duenoId;
    private String nombreDueno;
    private String tipoDocumento;
    private String numeroDocumento;
    private String direccion;
    private String correoDueno;
    private String numeroTelefono;

    public Dueno(
            Long duenoId,
            String nombreDueno,
            String tipoDocumento,
            String numeroDocumento,
            String direccion,
            String correoDueno,
            String numeroTelefono) {

        this.duenoId = duenoId;
        this.nombreDueno = nombreDueno;
        this.tipoDocumento = tipoDocumento;
        this.numeroDocumento = numeroDocumento;
        this.direccion = direccion;
        this.correoDueno = correoDueno;
        this.numeroTelefono = numeroTelefono;
    }

    public Long getDuenoId() {
        return duenoId;
    }

    public String getNombreDueno() {
        return nombreDueno;
    }

    public String getTipoDocumento() {
        return tipoDocumento;
    }

    public String getNumeroDocumento() {
        return numeroDocumento;
    }

    public String getDireccion() {
        return direccion;
    }

    public String getCorreoDueno() {
        return correoDueno;
    }

    public String getNumeroTelefono() {
        return numeroTelefono;
    }
}
