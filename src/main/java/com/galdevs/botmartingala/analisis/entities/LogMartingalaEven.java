package com.galdevs.botmartingala.analisis.entities;


import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;



@Entity  
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogMartingalaEven {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String fechaApertura;

    private long timestampApertura;

    private String fechaCierre;

    private long timestampCierre;

    private String instId;

    private int sentido;

    private double gap;

    private int exito;
    
    private long ttl;
}
