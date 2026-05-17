package com.mycompany.signals.model;

import java.util.Random;

/**
 * Fábrica de señales de ruido para sumar o mezclar con la entrada.
 * Incluye ruido gaussiano, porcentual e interferencia senoidal.
 */
public class GeneradorRuido {

    private static final Random random = new Random();

    /**
     * Genera ruido blanco gaussiano puro (Ruido Aleatorio).
     * @param amplitudMax La desviación estándar o "fuerza" del ruido.
     * @param fs Frecuencia de muestreo.
     * @param N Número de muestras (el constructor de Signal validará que sea potencia de 2).
     * @return Objeto Signal que contiene solo el ruido aleatorio.
     */
    public static Signal crearRuidoAleatorio(double amplitudMax, double fs, int N) {
        double[] t = new double[N];
        double[] ft = new double[N];
        double dt = 1.0 / fs;

        for (int i = 0; i < N; i++) {
            t[i] = i * dt;
            // nextGaussian() genera valores con distribución normal (media 0, varianza 1)
            ft[i] = random.nextGaussian() * amplitudMax;
        }

        return new Signal(t, ft);
    }

    /**
     * Genera un ruido porcentual aleatorio.
     * El ruido es dinámico: es mayor cuando la amplitud de la señal es mayor.
     * @param senalBase La señal original sobre la que se calcula el ruido.
     * @param porcentaje Fracción de ruido a aplicar (ej: 0.10 para un +/- 10%).
     * @return Nueva Signal del ruido (sin la señal base).
     */
    public static Signal aplicarRuidoPorcentual(Signal senalBase, double porcentaje) {
        double[] tOriginal = senalBase.getT();
        double[] ftOriginal = senalBase.getFt();
        int n = ftOriginal.length;

        double[] ftConRuido = new double[n];

        for (int i = 0; i < n; i++) {
            // Genera un multiplicador aleatorio entre -porcentaje y +porcentaje
            // Por ejemplo, si porcentaje = 0.10, el factor va de -0.10 a +0.10
            double factorAleatorio = (random.nextDouble() * 2.0 - 1.0) * porcentaje;

            // Se calcula el error (valor original * porcentaje aleatorio)
            ftConRuido[i] = ftOriginal[i] * factorAleatorio;
        }

        // Devolvemos una nueva señal clonando el eje temporal original por seguridad
        return new Signal(tOriginal.clone(), ftConRuido);
    }

    /**
     * Genera un ruido de interferencia tonal (una senoidal extra).
     * Utiliza el método nativo de la clase Signal.
     * @param amplitud Amplitud de la interferencia.
     * @param frecuenciaHz Frecuencia de la interferencia en Hz.
     * @param fs Frecuencia de muestreo.
     * @param N Número de muestras.
     * @return Objeto Signal con la interferencia.
     */
    public static Signal crearRuidoInterferencia(double amplitud, double frecuenciaHz, double fs, int N) {
        return Signal.crearSenoidal(amplitud, frecuenciaHz, fs, N);
    }
}
