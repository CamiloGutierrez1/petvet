package com.petvet.mascotas.infrastructure.persistence;

import com.petvet.mascotas.domain.Especie;
import com.petvet.mascotas.domain.Mascota;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.hibernate.annotations.DynamicUpdate;

@Entity
@Table(name = "mascotas")
// El UPDATE lleva solo las columnas que cambiaron: editar los datos no pisa la versión de una foto
// subida al mismo tiempo, y subir una foto no pisa los datos.
@DynamicUpdate
public class MascotaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private Especie especie;

    @Column(nullable = false, length = 60)
    private String raza;

    @Column(name = "fecha_nacimiento", nullable = false)
    private LocalDate fechaNacimiento;

    // BigDecimal para NUMERIC(7,3): con Double, ddl-auto validate rechazaría la columna.
    @Column(nullable = false, precision = 7, scale = 3)
    private BigDecimal peso;

    // Foto de ejemplo, relativa al front. Nunca guarda la URL de una foto subida.
    @Column(name = "foto_url")
    private String fotoUrl;

    // Versión de la foto subida (null si no hay). Con ella se arma la fotoUrl al responder.
    @Column(name = "foto_version")
    private Long fotoVersion;

    @Column(name = "dueno_id", nullable = false)
    private long duenoId;

    protected MascotaEntity() {
    }

    /** Mascota nueva y sin foto. */
    static MascotaEntity nueva(Mascota mascota) {
        MascotaEntity entity = new MascotaEntity();
        entity.duenoId = mascota.duenoId();
        entity.actualizarDatos(mascota);
        return entity;
    }

    /** Solo los cinco campos editables: la foto y el dueño no cambian. */
    void actualizarDatos(Mascota datos) {
        nombre = datos.nombre();
        especie = datos.especie();
        raza = datos.raza();
        fechaNacimiento = datos.fechaNacimiento();
        peso = datos.peso();
    }

    void cambiarVersionFoto(long version) {
        fotoVersion = version;
    }

    Long fotoVersion() {
        return fotoVersion;
    }

    Mascota aDominio() {
        return new Mascota(id, nombre, especie, raza, fechaNacimiento, peso, fotoUrl, duenoId);
    }
}
