package com.mycompany.signals.model;


import java.io.Serializable;

/**
 * Contenedor serializable del estado actual del programa.
 * Permite guardar y restaurar la configuración de señal, ruido y filtro.
 */
public class EstadoPrograma implements Serializable {
    private static final long serialVersionUID = 1L;

    // Instancias del modelo
    public Signal senalEntrada;
    public Signal ruidoEntrada;
    public Filtro filtroActual;

    // Valores de texto y selecciones de la Interfaz (Señal)
    public String tipoSenal;
    public String freqSenal;
    public String ampSenal;

    // Valores de texto y selecciones de la Interfaz (Ruido)
    public String tipoRuido;
    public String freqRuido;
    public String ampRuido;
    public String prctgRuido;

    // Valores de texto y selecciones de la Interfaz (Filtro)
    public String tipoFiltro;
    public String fcFiltro;
    public String qFiltro;
    public String puntosFiltro;
    public String sigmaFiltro;
}