package com.mycompany.signals.model;

public interface Filtro {
    
    /**
     * Aplica el filtro a una señal dada.
     * @param entrada La señal original.
     * @return Una nueva instancia de Signal con el filtro aplicado.
     */
    Signal aplicar(Signal entrada);

    /**
     * Genera la Respuesta en Frecuencia (Magnitud) del filtro.
     * Utiliza el teorema DSP: H(w) = FFT(h[n]) donde h[n] es la respuesta al impulso.
     * @param N Número de puntos para la resolución (Debe ser potencia de 2, ej. 1024).
     * @param fs Frecuencia de muestreo en Hz.
     * @return Arreglo con las magnitudes de atenuación/ganancia de 0 a fs/2.
     */
    default Signal obtenerRespuestaFrecuencia(int N, double fs) {
        // 1. Fabricar el Impulso Unitario (Delta de Dirac discreta)
        double[] t = new double[N];
        double[] impulso = new double[N];
        
        double dt = 1.0 / fs;
        for (int i = 0; i < N; i++) {
            t[i] = i * dt;
            impulso[i] = 0.0; 
        }
        impulso[0] = (double) N / 2.0; // El golpe del impulso en t=0

        Signal senalImpulso = new Signal(t, impulso);

        // 2. Pasar el impulso por el filtro para obtener h[n] (Respuesta al impulso)
        Signal h_n = this.aplicar(senalImpulso);

        // 3. La FFT de h[n] nos da directamente la Respuesta en Frecuencia H(w)
        return h_n.calcularFFT();
    }

}