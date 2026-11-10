package com.petvet.citas.infrastructure.persistence;

import com.petvet.citas.domain.Veterinario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "veterinarios")
public class VeterinarioEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String especialidad;

    protected VeterinarioEntity() {
    }

    Veterinario toDomain() {
        return new Veterinario(id, nombre, especialidad);
    }
}
