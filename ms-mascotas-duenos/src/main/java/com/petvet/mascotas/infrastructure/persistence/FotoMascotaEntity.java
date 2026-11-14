package com.petvet.mascotas.infrastructure.persistence;

import com.petvet.mascotas.domain.FotoMascota;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "fotos_mascotas")
public class FotoMascotaEntity {
    @Id
    @Column(name = "mascota_id")
    private Long mascotaId;

    @Column(name = "tipo_contenido", nullable = false, length = 20)
    private String tipoContenido;

    // byte[] sin @Lob: Hibernate lo valida contra BYTEA. Con @Lob esperaría una columna oid.
    @Column(nullable = false)
    private byte[] datos;

    protected FotoMascotaEntity() {
    }

    FotoMascotaEntity(long mascotaId, FotoMascota foto) {
        this.mascotaId = mascotaId;
        this.tipoContenido = foto.tipoContenido();
        this.datos = foto.datos();
    }

    FotoMascota aDominio() {
        return new FotoMascota(tipoContenido, datos);
    }
}
