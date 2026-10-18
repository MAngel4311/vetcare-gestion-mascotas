package com.vetcare.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/** F-01 / SWR-01: dueño de mascota. */
@Entity
@Table(name = "duenos")
@Getter
@Setter
public class Dueno extends Persona {

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false)
    private String direccion;

    @OneToMany(mappedBy = "dueno")
    private List<Mascota> mascotas = new ArrayList<>();
}
