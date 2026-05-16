package com.mycompany.signals.UI;


import javafx.util.StringConverter;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import com.mycompany.signals.model.Signal; // Asegúrate de importar tu clase Signal

public class GeneradorGraficasFX {

    /**
     * Método 1: Orientado a Objetos. Recibe directamente un objeto Signal.
     * Ideal para graficar señales en el dominio del tiempo.
     */
    public static LineChart<Number, Number> crearGrafica(String titulo, String serie, Signal senal, int n) {
        // Reutilizamos el Método 2, extrayendo los arreglos del objeto
        return crearGrafica(titulo, serie, senal.getT(), senal.getFt(), n);
    }

    /**
     * Método 2: Genérico. Recibe arreglos crudos.
     * Ideal para graficar resultados matemáticos sueltos, como la FFT (Hz vs Magnitud).
     */
    public static LineChart<Number, Number> crearGrafica(String titulo, String serie, double[] x, double[] y, int n) {
        // Definir los ejes
        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();
        
        xAxis.setLabel("Amplitud");
        yAxis.setLabel("Tiempo");
        
        // Crear el gráfico
        LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle(titulo);
        lineChart.setCreateSymbols(false); // Apagar los puntitos por rendimiento
        
        // Cargar los datos
        XYChart.Series<Number, Number> dataSeries = new XYChart.Series<>();
        dataSeries.setName(serie);
        
        // Llenar la serie iterando los arreglos
        for (int i = 0; i < (x.length/n); i++) {
            dataSeries.getData().add(new XYChart.Data<>(x[i], y[i]));
        }
        
        lineChart.getData().add(dataSeries);
        
        return lineChart;
    }
    
    public LineChart<Number, Number> crearGraficaXLog(String titulo, String serie, Signal fftSignal) {

        // 1. Crear Ejes
        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();

        xAxis.setLabel("Frecuencias (log)");
        yAxis.setLabel("Amplitud");

        // 2. Formatear las etiquetas del eje X (Truco Logarítmico)
        xAxis.setTickLabelFormatter(new StringConverter<Number>() {
            @Override
            public String toString(Number logValue) {
                double valorReal = Math.pow(10, logValue.doubleValue());
                if (valorReal >= 1000) {
                    return String.format("%.1f k", valorReal / 1000.0);
                }
                return String.format("%.0f", valorReal);
            }

            @Override
            public Number fromString(String string) { return null; } 
        });

        xAxis.setTickUnit(1);
        xAxis.setMinorTickCount(10);

        // 3. Crear gráfico
        LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setCreateSymbols(false); 
        chart.setAnimated(false);
        chart.setTitle(titulo);

        // 4. Crear la serie y asignarle el título
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName(serie); // <-- Nombre de la serie para la leyenda

        double[] freqs = fftSignal.getT(); 
        double[] mags = fftSignal.getFt(); 

        // Empezamos en i=1 para evitar calcular Math.log10(0)
        for (int i = 1; i < freqs.length; i++) {
            double xLog = Math.log10(freqs[i]); // Eje horizontal logarítmico
            double yLinear = mags[i];           // Eje vertical lineal (sin alterar)

            series.getData().add(new XYChart.Data<>(xLog, yLinear));
        }

        // 5. Agregar la serie al gráfico
        chart.getData().add(series);

        return chart;
    }
    
    /**
     * Convierte un objeto Signal en una serie de datos lineal.
     * Útil para superponer datos en gráficos de tiempo (crearGrafica).
     */
    public XYChart.Series<Number, Number> convertirSignalASerie(String nombreSerie, Signal signal, int n) {
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName(nombreSerie);

        double[] x = signal.getT();
        double[] y = signal.getFt();

        for (int i = 0; i < (x.length / n); i++) {
            series.getData().add(new XYChart.Data<>(x[i], y[i]));
        }
        return series;
    }
    
    /**
     * Convierte un objeto Signal en una serie de datos logarítmica.
     * Útil para superponer datos en gráficos creados con crearGraficaXLog.
     */
    public XYChart.Series<Number, Number> convertirSignalASerieLog(String nombreSerie, Signal signal) {
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName(nombreSerie);

        double[] x = signal.getT();
        double[] y = signal.getFt();

        // Empezamos en 1 para evitar log10(0)
        for (int i = 1; i < x.length; i++) {
            if (x[i] > 0) {
                double xLog = Math.log10(x[i]);
                series.getData().add(new XYChart.Data<>(xLog, y[i]));
            }
        }
        return series;
    }
    
}