package com.mycompany.signals.model;

/**
 * Filtro IIR definido por una ecuación de diferencias con coeficientes b (entrada) y a (salida).
 * Implementa la relación: y[n] = sum(b[i]*x[n-i]) - sum(a[j]*y[n-j]), con a[0] = 1.
 */
public class FiltroDiferencias implements Filtro {

    private final double[] b; // Coeficientes del numerador (entradas x)
    private final double[] a; // Coeficientes del denominador (salidas pasadas y)

    /**
     * @param b Arreglo de coeficientes feedforward (b0, b1, b2...)
     * @param a Arreglo de coeficientes feedback. OJO: a[0] suele ser 1.0 y se ignora en la resta,
     *          los coeficientes efectivos empiezan en a[1].
     */
    public FiltroDiferencias(double[] b, double[] a) {
        this.b = b;
        this.a = a;
    }

    @Override
    public Signal aplicar(Signal entrada) {
        double[] x = entrada.getFt(); // Señal original x[n]
        double[] y = new double[x.length]; // Señal filtrada y[n]

        // Iteramos sobre toda la señal
        for (int n = 0; n < x.length; n++) {
            double sumaX = 0;
            double sumaY = 0;

            // 1. Sumatoria de las entradas pasadas: b_i * x[n-i]
            for (int i = 0; i < b.length; i++) {
                if (n - i >= 0) { // Evitar índices negativos (condiciones iniciales cero)
                    sumaX += b[i] * x[n - i];
                }
            }

            // 2. Sumatoria de las salidas pasadas: a_j * y[n-j]
            // Empezamos en j=1 porque a[0] corresponde a y[n], que es lo que estamos calculando.
            for (int j = 1; j < a.length; j++) {
                if (n - j >= 0) {
                    sumaY += a[j] * y[n - j];
                }
            }

            // 3. Ecuación de diferencias final
            // y[n] = (suma de entradas) - (suma de salidas pasadas)
            // (Asumiendo que los coeficientes a están normalizados para que a[0] = 1)
            y[n] = sumaX - sumaY;
        }

        // Devolvemos la nueva señal, copiando el eje de tiempo original
        return new Signal(entrada.getT().clone(), y);
    }
}
