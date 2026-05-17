package com.mycompany.signals.model;

/**
 * Fábrica de filtros IIR discretos (Butterworth y pasa-banda)
 * mediante transformación bilineal y coeficientes b, a.
 */
public class GeneradorFiltros {

    /**
     * Crea un filtro Pasa-Bajos Butterworth de 1er orden.
     * @param fc Frecuencia de corte en Hz.
     * @param fs Frecuencia de muestreo en Hz.
     * @return Devuelve un FiltroDiferencias
     */
    public static Filtro crearPasaBajos(double fc, double fs) {
        // La magia matemática: Transformación Bilineal
        // Convertimos la frecuencia de Hz a radianes y normalizamos
        double gamma = Math.tan(Math.PI * fc / fs);
        double denominador = 1.0 + gamma;

        // Coeficientes para la ecuación de diferencias
        double b0 = gamma / denominador;
        double b1 = gamma / denominador;
        double a1 = (gamma - 1.0) / denominador;

        double[] b = {b0, b1};
        double[] a = {1.0, a1}; // El 1.0 es a0

        return new FiltroDiferencias(b, a);
    }

    /**
     * Crea un filtro Pasa-Altos Butterworth de 1er orden.
     * @param fc Frecuencia de corte en Hz.
     * @param fs Frecuencia de muestreo en Hz.
     * @return Devuelve un FiltroDiferencias
     */
    public static Filtro crearPasaAltos(double fc, double fs) {
        double gamma = Math.tan(Math.PI * fc / fs);
        double denominador = 1.0 + gamma;

        double b0 = 1.0 / denominador;
        double b1 = -1.0 / denominador;
        double a1 = (gamma - 1.0) / denominador;

        double[] b = {b0, b1};
        double[] a = {1.0, a1};

        return new FiltroDiferencias(b, a);
    }

    /**
     * Crea un filtro Pasa-Bajos Butterworth de 2do orden (Biquad).
     * Corte más abrupto (-40 dB/década) y respuesta plana en la banda de paso.
     * @param fc Frecuencia de corte en Hz.
     * @param fs Frecuencia de muestreo en Hz.
     * @return Un objeto FiltroDiferencias configurado.
     */
    public static Filtro crearPasaBajos2doOrden(double fc, double fs) {
        // 1. Calcular la frecuencia angular normalizada usando la tangente (Pre-warping)
        double omega = 2.0 * Math.PI * fc / fs;
        double K = Math.tan(omega / 2.0);

        // Factor de calidad para Butterworth de 2do orden
        double Q = 1.0 / Math.sqrt(2.0); // aprox 0.7071

        // 2. Denominador común (equivale a a0 antes de normalizar)
        double K2 = K * K;
        double denominador = K2 + (K / Q) + 1.0;

        // 3. Calcular coeficientes normalizados
        double b0 = K2 / denominador;
        double b1 = 2.0 * K2 / denominador;
        double b2 = K2 / denominador;

        double a1 = 2.0 * (K2 - 1.0) / denominador;
        double a2 = (K2 - (K / Q) + 1.0) / denominador;

        // 4. Empaquetar arreglos
        double[] b = {b0, b1, b2};
        double[] a = {1.0, a1, a2}; // a0 es siempre 1.0

        return new FiltroDiferencias(b, a);
    }

    /**
     * Crea un filtro Pasa-Altos Butterworth de 2do orden (Biquad).
     * Elimina frecuencias bajas de forma abrupta y maximiza la ganancia en Nyquist.
     * @param fc Frecuencia de corte en Hz.
     * @param fs Frecuencia de muestreo en Hz.
     * @return Un objeto FiltroDiferencias configurado.
     */
    public static Filtro crearPasaAltos2doOrden(double fc, double fs) {
        // 1. Calcular la frecuencia angular normalizada usando la tangente (Pre-warping)
        double omega = 2.0 * Math.PI * fc / fs;
        double K = Math.tan(omega / 2.0);

        // Factor de calidad para Butterworth de 2do orden
        double Q = 1.0 / Math.sqrt(2.0);

        // 2. Denominador común (equivale a a0 antes de normalizar)
        double K2 = K * K;
        double denominador = K2 + (K / Q) + 1.0;

        // 3. Coeficientes específicos para Pasa-Altos
        double b0 = 1.0 / denominador;
        double b1 = -2.0 / denominador;
        double b2 = 1.0 / denominador;

        double a1 = 2.0 * (K2 - 1.0) / denominador;
        double a2 = (K2 - (K / Q) + 1.0) / denominador;

        // 4. Empaquetar arreglos
        double[] b = {b0, b1, b2};
        double[] a = {1.0, a1, a2};

        return new FiltroDiferencias(b, a);
    }

    /**
     * Crea un filtro Pasa-Banda de 2do orden (Biquad).
     * Sintoniza una frecuencia central específica atenuando todo lo demás a los lados.
     * @param f0 Frecuencia central de sintonía en Hz.
     * @param Q Factor de Calidad (Determina qué tan estrecha es la banda de paso).
     * @param fs Frecuencia de muestreo en Hz.
     * @return Un objeto FiltroDiferencias configurado.
     */
    public static Filtro crearPasaBanda(double f0, double Q, double fs) {
        // 1. Calcular la frecuencia angular central normalizada (Pre-warping)
        double omega0 = 2.0 * Math.PI * f0 / fs;

        // Términos intermedios de la Transformación Bilineal
        double sen0 = Math.sin(omega0);
        double cos0 = Math.cos(omega0);
        double alpha = sen0 / (2.0 * Q);

        // 2. Denominador común para normalizar (equivale a a0)
        double denominador = 1.0 + alpha;

        // 3. Calcular coeficientes normalizados para el pasa-banda (Ganancia pico constante = 0 dB)
        double b0 = alpha / denominador;
        double b1 = 0.0;
        double b2 = -alpha / denominador;

        double a1 = -2.0 * cos0 / denominador;
        double a2 = (1.0 - alpha) / denominador;

        // 4. Empaquetar arreglos para FiltroDiferencias
        double[] b = {b0, b1, b2};
        double[] a = {1.0, a1, a2}; // a0 siempre es 1.0

        return new FiltroDiferencias(b, a);
    }

}
