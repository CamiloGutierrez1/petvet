package com.petvet.mascotas.infrastructure.persistence;

import com.petvet.mascotas.domain.Dueno;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "duenos")
public class DuenoEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 30)
    private String documento;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(length = 10)
    private String telefono;

    @Column(length = 200)
    private String direccion;

    protected DuenoEntity() {
    }

    static DuenoEntity nuevo(Dueno dueno) {
        DuenoEntity entity = new DuenoEntity();
        entity.nombre = dueno.nombre();
        entity.documento = dueno.documento();
        entity.email = dueno.email();
        entity.telefono = dueno.telefono();
        entity.direccion = dueno.direccion();
        return entity;
    }

    Dueno aDominio() {
        return new Dueno(id, nombre, documento, email, telefono, direccion);
    }
}
