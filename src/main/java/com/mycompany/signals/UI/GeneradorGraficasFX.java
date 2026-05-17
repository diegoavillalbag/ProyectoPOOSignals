package com.mycompany.signals.UI;

import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import com.mycompany.signals.model.Signal;

/**
 * Construye y actualiza gráficos LineChart de JavaFX para señales en tiempo y FFT.
 * Los gráficos se crean una vez y luego solo se refrescan las series de datos.
 */
public class GeneradorGraficasFX {

    /** Umbral respecto al pico: max / 10^ORDENES (valores por debajo se grafican como 0). */
    private static final int ORDENES_MAGNITUD_MINIMAS = 3;
    /** Tolerancia fija entre puntos sucesivos en el reductor de cadenas. */
    private static final double EPSILON = 1e-5;

    /** Datos intermedios tras umbral y submuestreo FFT, listos para el reductor. */
    private record DatosFftGrafico(double[] x, double[] y, int limite, double amplitudMax) {
    }

    /**
     * Crea un LineChart vacío listo para actualizaciones sucesivas.
     * @param titulo Título del gráfico.
     * @param ejeX Etiqueta del eje horizontal.
     * @param ejeY Etiqueta del eje vertical.
     * @param nombreSerie Nombre de la serie principal (leyenda).
     */
    public LineChart<Number, Number> crearGraficaVacia(String titulo, String ejeX, String ejeY, String nombreSerie) {
        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel(ejeX);
        yAxis.setLabel(ejeY);

        LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle(titulo);
        lineChart.setCreateSymbols(false); // Mejor rendimiento sin marcadores por punto
        lineChart.setAnimated(false);

        XYChart.Series<Number, Number> serie = new XYChart.Series<>();
        serie.setName(nombreSerie);
        lineChart.getData().add(serie);
        return lineChart;
    }

    /**
     * Añade una serie vacía adicional (p. ej. respuesta del filtro sobre la FFT de entrada).
     */
    public XYChart.Series<Number, Number> agregarSerieVacia(LineChart<Number, Number> chart, String nombreSerie) {
        XYChart.Series<Number, Number> serie = new XYChart.Series<>();
        serie.setName(nombreSerie);
        chart.getData().add(serie);
        return serie;
    }

    /**
     * Reemplaza los puntos de una serie sin recrear el gráfico.
     * @param paso en FFT: tamaño de ventana de submuestreo; en tiempo: salto entre índices
     * @param factorRecorte divide la longitud visible (ej. 70 → primeros N/70 puntos en tiempo)
     * @param omitirRepetidos si es true (FFT): umbral → submuestreo → reductor de cadenas
     */
    public void actualizarSerie(XYChart.Series<Number, Number> serie, Signal signal, int paso, int factorRecorte, boolean omitirRepetidos) {
        actualizarSerie(serie, signal.getT(), signal.getFt(), paso, factorRecorte, omitirRepetidos);
    }

    /**
     * @see #actualizarSerie(XYChart.Series, Signal, int, int, boolean)
     */
    public void actualizarSerie(XYChart.Series<Number, Number> serie, double[] x, double[] y, int paso, int factorRecorte, boolean omitirRepetidos) {
        serie.getData().clear();
        if (x == null || y == null || x.length == 0) {
            return;
        }

        paso = Math.max(1, paso);
        factorRecorte = Math.max(1, factorRecorte);
        int longitud = Math.min(x.length, y.length);
        int limite = longitud / factorRecorte;

        if (omitirRepetidos) {
            DatosFftGrafico datos = prepararDatosFft(x, y, limite, paso);
            graficarConReductorCadenas(serie, datos);
        } else {
            graficarConMuestreo(serie, x, y, limite, paso);
        }
    }

    /**
     * Pipeline FFT: 1) umbral de magnitud → 2) submuestreo por ventanas → devuelve datos listos.
     */
    private DatosFftGrafico prepararDatosFft(double[] x, double[] y, int limite, int paso) {
        double amplitudMax = 0;
        double[] magnitudes = new double[limite];

        for (int i = 0; i < limite; i++) {
            amplitudMax = Math.max(amplitudMax, Math.abs(y[i]));
        }
        double umbral = umbralDesdeMax(amplitudMax);

        // 1. Poner a cero lo que queda por debajo del umbral respecto al máximo
        for (int i = 0; i < limite; i++) {
            magnitudes[i] = (amplitudMax > 0 && Math.abs(y[i]) < umbral) ? 0.0 : y[i];
        }

        // 2. Submuestrear por ventanas (conserva el pico de cada bloque)
        return submuestrearPorVentana(x, magnitudes, limite, paso, amplitudMax);
    }

    /**
     * max / 10^ORDENES_MAGNITUD_MINIMAS
     */
    private double umbralDesdeMax(double amplitudMax) {
        return amplitudMax * Math.pow(10, -ORDENES_MAGNITUD_MINIMAS);
    }

    /**
     * Por cada ventana de {@code paso} bins conserva el punto de mayor magnitud (no pierde picos).
     */
    private DatosFftGrafico submuestrearPorVentana(double[] x, double[] y, int limite, int paso, double amplitudMax) {
        if (paso <= 1) {
            return new DatosFftGrafico(x, y, limite, amplitudMax);
        }

        int nuevoLimite = (limite + paso - 1) / paso;
        double[] xSub = new double[nuevoLimite];
        double[] ySub = new double[nuevoLimite];
        int j = 0;

        for (int inicio = 0; inicio < limite; inicio += paso) {
            int fin = Math.min(inicio + paso, limite);
            int mejor = inicio;
            for (int i = inicio + 1; i < fin; i++) {
                if (Math.abs(y[i]) > Math.abs(y[mejor])) {
                    mejor = i;
                }
            }
            xSub[j] = x[mejor];
            ySub[j] = y[mejor];
            j++;
        }

        return new DatosFftGrafico(xSub, ySub, j, amplitudMax);
    }

    /**
     * Paso 3 FFT: colapsa tramos consecutivos con el mismo valor (tolerancia EPSILON).
     */
    private void graficarConReductorCadenas(XYChart.Series<Number, Number> serie, DatosFftGrafico datos) {
        for (int i = 0; i < datos.limite(); i++) {
            if (!conservarPuntoEnReductor(datos.y(), datos.limite(), i)) {
                continue;
            }
            serie.getData().add(new XYChart.Data<>(datos.x()[i], datos.y()[i]));
        }
    }

    /** Conserva inicio, fin y esquinas donde cambia la magnitud. */
    private boolean conservarPuntoEnReductor(double[] y, int limite, int i) {
        boolean esInicio = (i == 0);
        boolean esFin = (i == limite - 1);
        boolean cambioDesdeAnterior = i > 0 && Math.abs(y[i] - y[i - 1]) > EPSILON;
        boolean cambioHaciaSiguiente = i < limite - 1 && Math.abs(y[i + 1] - y[i]) > EPSILON;
        return esInicio || esFin || cambioDesdeAnterior || cambioHaciaSiguiente;
    }

    /**
     * Graficado en tiempo: utiliza "Peak Decimation" avanzado con interpolación.
     * Extrae el mínimo, el máximo, el valor medio, y si la señal cruza el eje X,
     * calcula el instante exacto del cruce mediante interpolación lineal.
     */
    private void graficarConMuestreo(XYChart.Series<Number, Number> serie, double[] x, double[] y, int limite, int paso) {
        if (paso <= 1) {
            for (int i = 0; i < limite; i++) {
                serie.getData().add(new XYChart.Data<>(x[i], y[i]));
            }
            return;
        }

        double ultimoX = -Double.MAX_VALUE; // Para evitar puntos duplicados globales

        for (int inicio = 0; inicio < limite; inicio += paso) {
            int fin = Math.min(inicio + paso, limite);
            
            // 1. Encontrar mínimo y máximo
            int idxMin = inicio;
            int idxMax = inicio;
            double minY = y[inicio];
            double maxY = y[inicio];
            
            for (int i = inicio + 1; i < fin; i++) {
                if (y[i] > maxY) { maxY = y[i]; idxMax = i; }
                if (y[i] < minY) { minY = y[i]; idxMin = i; }
            }
            
            // 2. Encontrar el valor más cercano al medio de los picos
            double medioY = (minY + maxY) / 2.0;
            int idxMedio = inicio;
            double menorDif = Double.MAX_VALUE;
            
            for (int i = inicio; i < fin; i++) {
                double dif = Math.abs(y[i] - medioY);
                if (dif < menorDif) {
                    menorDif = dif;
                    idxMedio = i;
                }
            }

            // Arreglos temporales para los puntos de este bloque (máximo 4 puntos)
            double[] blockX = new double[4];
            double[] blockY = new double[4];
            int count = 0;

            blockX[count] = x[idxMin];   blockY[count++] = y[idxMin];
            blockX[count] = x[idxMax];   blockY[count++] = y[idxMax];
            blockX[count] = x[idxMedio]; blockY[count++] = y[idxMedio];

            // 3. INTERPOLACIÓN LINEAL DEL CERO
            // Solo buscamos si hay un cruce estricto por cero (uno negativo y otro positivo)
            if (minY < 0 && maxY > 0) {
                for (int i = inicio; i < fin - 1; i++) {
                    // Detectar dónde ocurre exactamente el cambio de signo
                    if ((y[i] < 0 && y[i + 1] > 0) || (y[i] > 0 && y[i + 1] < 0)) {
                        double x1 = x[i];   double y1 = y[i];
                        double x2 = x[i+1]; double y2 = y[i+1];
                        
                        // Fórmula de interpolación lineal para y = 0
                        double xZero = x1 - y1 * (x2 - x1) / (y2 - y1);
                        
                        blockX[count] = xZero; 
                        blockY[count++] = 0.0;
                        break; // Solo necesitamos anclar un cruce por bloque
                    }
                }
            }

            // 4. Ordenar los puntos del bloque cronológicamente (eje X)
            // Usamos un simple Bubble Sort ya que son máximo 4 elementos (ultrarrápido)
            for (int i = 0; i < count - 1; i++) {
                for (int j = i + 1; j < count; j++) {
                    if (blockX[i] > blockX[j]) {
                        double tempX = blockX[i]; blockX[i] = blockX[j]; blockX[j] = tempX;
                        double tempY = blockY[i]; blockY[i] = blockY[j]; blockY[j] = tempY;
                    }
                }
            }

            // 5. Agregar a la gráfica filtrando duplicados exactos en el tiempo
            for (int i = 0; i < count; i++) {
                if (blockX[i] > ultimoX) {
                    serie.getData().add(new XYChart.Data<>(blockX[i], blockY[i]));
                    ultimoX = blockX[i];
                }
            }
        }
    }

}
