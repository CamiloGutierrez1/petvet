package com.petvet.mascotas.infrastructure.persistence;

import com.petvet.mascotas.domain.FotoGuardada;
import com.petvet.mascotas.domain.FotoMascota;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Formula;

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

    // Versión de mascotas.foto_version, leída en la misma consulta que los bytes: aunque otra subida termine en ese
    // momento, la ETag corresponde a la foto que se sirve. Es solo lectura; la escribe JpaAlmacenFotosMascota.
    @Formula("(SELECT m.foto_version FROM mascotas m WHERE m.id = mascota_id)")
    private Long version;

    protected FotoMascotaEntity() {
    }

    FotoMascotaEntity(long mascotaId, FotoMascota foto) {
        this.mascotaId = mascotaId;
        this.tipoContenido = foto.tipoContenido();
        this.datos = foto.datos();
    }

    FotoGuardada aDominio() {
        return new FotoGuardada(new FotoMascota(tipoContenido, datos), version);
    }
}
