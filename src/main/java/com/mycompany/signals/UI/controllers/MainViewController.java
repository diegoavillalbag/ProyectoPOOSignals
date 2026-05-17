package com.mycompany.signals.UI.controllers;

import com.mycompany.signals.model.Filtro;
import javafx.scene.control.TextField;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ChoiceBox;
import javafx.scene.layout.GridPane;
import javafx.scene.chart.LineChart;
import javafx.event.ActionEvent;
import javafx.scene.chart.XYChart;

import com.mycompany.signals.model.Signal;
import com.mycompany.signals.model.GeneradorFiltros;
import com.mycompany.signals.UI.GeneradorGraficasFX;
import com.mycompany.signals.model.GeneradorRuido;

/**
 * Controlador de la vista principal: señal de entrada, ruido, filtro y cuatro gráficos.
 * Enlaza los controles FXML con el modelo de señales y {@link GeneradorGraficasFX}.
 */
public class MainViewController implements Initializable {

    // --- Constantes del programa ---
    private final int N = 65536; // Cantidad de puntos de señales y FFT
    private final int paso = 7; // Muestreo temporal: cada cuántos índices se grafica un punto (1 = todos)
    private final int pasoFft = 32; // FFT: tamaño de ventana de submuestreo (pico por bloque)
    private final int pasoRespuestaFiltro = 50; // Respuesta del filtro en gráfico FFT
    private final int factorRecorte = 100; // Recorte temporal: se grafican los primeros N/factorRecorte puntos
    private final int factorRecorteFFT = 4; // Recorte frecuencias
    private final int fs = 65536; // Frecuencia de muestreo (Hz)

    // --- Estado de las señales ---
    private Signal senalEntrada;
    private Signal senalEntradaFFT;
    private Signal ruidoEntrada;
    private Signal respuestaFiltro;
    private Signal senalSalida;
    private Signal senalSalidaFFT;
    private Filtro filtroActual;

    private final GeneradorGraficasFX Graficador = new GeneradorGraficasFX();

    // --- Gráficos (se crean una vez y se actualizan) ---
    @FXML private GridPane SignalGrid;
    private LineChart<Number, Number> chartEntrada;
    private LineChart<Number, Number> chartFFTEntrada;
    private LineChart<Number, Number> chartSalida;
    private LineChart<Number, Number> chartFFTSalida;
    private XYChart.Series<Number, Number> serieRespuestaFiltro;
    
    
    // --- Menú señal de entrada ---
    @FXML private ChoiceBox<String> SignalSelector;
    private final String[] opcionesTipoSenalEntrada = {"Senoidal", "Cuadrada"};
    @FXML private TextField SignalFreq;
    @FXML private TextField SignalAmp;

    // --- Menú ruido ---
    @FXML private ChoiceBox<String> NoiseSelector; 
    private final String[] opcionesTipoRuido = {"Ruido Aleatorio Plano", "Ruido Aleatorio Porcentual", 
                                                "Ruido Interferencia (Senoidal)"};
    @FXML private TextField NoiseFreq;
    @FXML private TextField NoiseAmp;
    @FXML private TextField NoisePrctg;

    // --- Menú filtros ---
    @FXML private ChoiceBox<String> FilterSelector;
    private final String[] opcionesTipoFiltro = {"Pasa Bajos 1er Orden", "Pasa Altos 1er Orden",
            "Pasa Bajos 2do Orden", "Pasa Altos 2do Orden", "Pasa Banda 2do Orden"};
    @FXML private TextField FilterFc;
    @FXML private TextField FilterQ; // Solo habilitado para Pasa Banda 2do Orden
    
    
    @FXML
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Valores por defecto: senoidal + ruido porcentual + pasa-bajos
        double signalAmp = 1;
        double signalFreq = 1000;
        senalEntrada = Signal.crearSenoidal(signalAmp, signalFreq, fs, N);
        
        double noiseAmp = 0.3;
        double noiseFreq = 200;
        double noisePrctg = 0.15;
        ruidoEntrada = GeneradorRuido.aplicarRuidoPorcentual(senalEntrada, noisePrctg);
        
        double filterFc = 1000;
        double filterQ = 10;
        filtroActual = GeneradorFiltros.crearPasaBajos(filterFc, fs);

        inicializarGraficas();
        actualizarGraficas();
        
        // Poblar ChoiceBox y TextField de cada sección
        SignalSelector.getItems().addAll(opcionesTipoSenalEntrada);
        SignalSelector.setValue(opcionesTipoSenalEntrada[0]);
        SignalFreq.setText(String.valueOf(signalFreq));
        SignalAmp.setText(String.valueOf(signalAmp));

        NoiseSelector.getItems().addAll(opcionesTipoRuido);
        NoiseSelector.setValue(opcionesTipoRuido[1]);
        NoiseFreq.setText(String.valueOf(noiseFreq));
        NoiseAmp.setText(String.valueOf(noiseAmp));
        NoisePrctg.setText(String.valueOf(noisePrctg));

        FilterSelector.getItems().addAll(opcionesTipoFiltro);
        FilterSelector.setValue(opcionesTipoFiltro[0]);
        FilterFc.setText(String.valueOf(filterFc));
        FilterQ.setText(String.valueOf(filterQ));

        // Listeners: habilitar campos según tipo de ruido o filtro
        // Ruido porcentual: solo porcentaje activo al inicio
        NoiseFreq.setDisable(true);
        NoiseAmp.setDisable(true);
        NoisePrctg.setDisable(false);
        
        // Listener
        NoiseSelector.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            // Si la nueva opción seleccionada es "Ruido Aleatorio Porcentual"
            if (newValue != null && newValue.equals("Ruido Aleatorio Porcentual")) {
                NoiseFreq.setDisable(true);
                NoiseAmp.setDisable(true);
                NoisePrctg.setDisable(false);
            } else if (newValue != null && newValue.equals("Ruido Aleatorio Plano")) {
                NoiseFreq.setDisable(true);
                NoiseAmp.setDisable(false);
                NoisePrctg.setDisable(true);
            } else {
                NoiseFreq.setDisable(false);
                NoiseAmp.setDisable(false);
                NoisePrctg.setDisable(true);
            }
        });
        
        FilterQ.setDisable(true);
        FilterSelector.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && newValue.equals("Pasa Banda 2do Orden")) {
                FilterQ.setDisable(false);
            } else {
                FilterQ.setDisable(true);
            }
        });
    }

    /**
     * Crea los cuatro LineChart una sola vez y los coloca en la cuadrícula.
     */
    private void inicializarGraficas() {
        chartEntrada = Graficador.crearGraficaVacia("Señal de Entrada", "Tiempo", "Amplitud", "Entrada");
        chartFFTEntrada = Graficador.crearGraficaVacia("FFT de la Señal de Entrada", "Frecuencia (Hz)", "Amplitud", "FFT Entrada");
        chartSalida = Graficador.crearGraficaVacia("Señal de Salida", "Tiempo", "Amplitud", "Salida");
        chartFFTSalida = Graficador.crearGraficaVacia("FFT de la Señal de Salida", "Frecuencia (Hz)", "Amplitud", "FFT Salida");
        serieRespuestaFiltro = Graficador.agregarSerieVacia(chartFFTEntrada, "Respuesta Filtro");

        SignalGrid.add(chartEntrada, 0, 0);
        SignalGrid.add(chartFFTEntrada, 1, 0);
        SignalGrid.add(chartSalida, 0, 1);
        SignalGrid.add(chartFFTSalida, 1, 1);
    }

    /**
     * Recalcula señales/FFT/filtro y refresca las series de los gráficos existentes.
     */
    public void actualizarGraficas() {
        Signal entradaConRuido = senalEntrada.sumar(ruidoEntrada);
        senalEntradaFFT = entradaConRuido.calcularFFT();
        senalSalida = filtroActual.aplicar(entradaConRuido);
        senalSalidaFFT = senalSalida.calcularFFT();
        respuestaFiltro = filtroActual.obtenerRespuestaFrecuencia(N, fs);
        // Escalar H(w) al pico de la FFT de entrada para comparar en el mismo gráfico
        respuestaFiltro = respuestaFiltro.multiplicarPorEscalar(senalEntradaFFT.obtenerAmplitudMaxima());

        // Actualizar series (sin recrear los LineChart)
        Graficador.actualizarSerie(chartEntrada.getData().get(0), entradaConRuido, paso, factorRecorte, false);
        Graficador.actualizarSerie(chartFFTEntrada.getData().get(0), senalEntradaFFT, pasoFft, factorRecorteFFT, true);
        Graficador.actualizarSerie(serieRespuestaFiltro, respuestaFiltro, pasoRespuestaFiltro, factorRecorteFFT, true);
        Graficador.actualizarSerie(chartSalida.getData().get(0), senalSalida, paso, factorRecorte, false);
        Graficador.actualizarSerie(chartFFTSalida.getData().get(0), senalSalidaFFT, pasoFft, factorRecorteFFT, true);
    }
    
    // --- Handlers FXML: señal de entrada ---

    @FXML
    private void SignalApplyOnAction(ActionEvent event) {
        // Obtener los datos de la interfaz
        String seleccionSenalEntrada = SignalSelector.getValue();
        String freqEntrada = SignalFreq.getText();
        String ampEntrada = SignalAmp.getText();
        
        // Expresión regular para números decimales positivos (ej: 10, 3.14, 0.5)
        String regexDecimalPositivo = "^[0-9]+(\\.[0-9]+)?$";

        // Verificar que ambos inputs cumplan con el formato decimal positivo
        if (!freqEntrada.matches(regexDecimalPositivo) || !ampEntrada.matches(regexDecimalPositivo)) {
            SignalFreq.setText("ERROR: Debe ser decimal positivo");
            SignalAmp.setText("ERROR");
            return;
        }
        
        // Convertir las entradas a double
        double freqEntradaDouble = Double.parseDouble(freqEntrada);
        double ampEntradaDouble = Double.parseDouble(ampEntrada);
        
        if (seleccionSenalEntrada.equals("Senoidal")) {
            senalEntrada = Signal.crearSenoidal(ampEntradaDouble, freqEntradaDouble, fs, N);
        }
        if (seleccionSenalEntrada.equals("Cuadrada")) {
            senalEntrada = Signal.crearCuadrada(ampEntradaDouble, freqEntradaDouble, fs, N);
        }

        actualizarGraficas();
        
    }
    
    @FXML
    private void SignalDeleteOnAction(ActionEvent event) { 
        senalEntrada = Signal.crearSenoidal(0, 0, fs, N);
        
        SignalFreq.setText("0");
        SignalAmp.setText("0");
        SignalSelector.setValue(opcionesTipoSenalEntrada[0]);
        
        actualizarGraficas();
        
    }
    
    
    // --- Handlers FXML: filtros ---

    @FXML
    private void FilterApplyOnAction(ActionEvent event) {
        // Obtener datos de la interfaz
        String seleccionFiltro = FilterSelector.getValue();
        String filterFc = FilterFc.getText();
        String filterQ = FilterQ.getText();
        
        // Expresión regular para números decimales positivos (ej: 10, 3.14, 0.5)
        String regexDecimalPositivo = "^[0-9]+(\\.[0-9]+)?$";

        // Verificar que ambos inputs cumplan con el formato decimal positivo
        if (!filterFc.matches(regexDecimalPositivo) || !filterQ.matches(regexDecimalPositivo)) {
            FilterFc.setText("ERROR");
            FilterQ.setText("ERROR");
            return;
        }
        
        // Convertir las entradas a double
        double filterFcDouble = Double.parseDouble(filterFc);
        double filterQDouble = Double.parseDouble(filterQ);
        
        if (seleccionFiltro.equals("Pasa Bajos 1er Orden")) {
            filtroActual = GeneradorFiltros.crearPasaBajos(filterFcDouble, fs);
        }
        if (seleccionFiltro.equals("Pasa Altos 1er Orden")) {
            filtroActual = GeneradorFiltros.crearPasaAltos(filterFcDouble, fs);
        }
        if (seleccionFiltro.equals("Pasa Bajos 2do Orden")) {
            filtroActual = GeneradorFiltros.crearPasaBajos2doOrden(filterFcDouble, fs);
        }
        if (seleccionFiltro.equals("Pasa Altos 2do Orden")) {
            filtroActual = GeneradorFiltros.crearPasaAltos2doOrden(filterFcDouble, fs);
        }
        if (seleccionFiltro.equals("Pasa Banda 2do Orden")) {
            filtroActual = GeneradorFiltros.crearPasaBanda(filterFcDouble, filterQDouble, fs);
        }

        actualizarGraficas();
        
    }
    
    @FXML
    private void FilterDeleteOnAction(ActionEvent event){
        filtroActual = GeneradorFiltros.crearPasaAltos(0, fs);
        actualizarGraficas();
    }

    // --- Handlers FXML: ruido ---

    @FXML
    private void NoiseSumOnAction(ActionEvent event) {
        // Obtener datos de la interfaz
        String seleccionRuido = NoiseSelector.getValue();
        String noiseFreq = NoiseFreq.getText();
        String noiseAmp = NoiseAmp.getText();
        String noisePrctg = NoisePrctg.getText();
        
        String regexDecimalPositivo = "^[0-9]+(\\.[0-9]+)?$";
        if (!noiseFreq.matches(regexDecimalPositivo) || !noiseAmp.matches(regexDecimalPositivo) || !noisePrctg.matches(regexDecimalPositivo)) {
            NoiseFreq.setText("ERROR");
            NoiseAmp.setText("ERROR");
            NoisePrctg.setText("ERROR");
            return;
        }

        double noiseFreqDouble = Double.parseDouble(noiseFreq);
        double noiseAmpDouble = Double.parseDouble(noiseAmp);
        double noisePrctgDouble = Double.parseDouble(noisePrctg);

        // Acumular nuevo ruido sobre el ya existente
        if (seleccionRuido.equals("Ruido Aleatorio Plano")) {
            ruidoEntrada = ruidoEntrada.sumar(GeneradorRuido.crearRuidoAleatorio(noiseAmpDouble, fs, N));
        }
        if (seleccionRuido.equals("Ruido Aleatorio Porcentual")) {
            ruidoEntrada = ruidoEntrada.sumar(GeneradorRuido.aplicarRuidoPorcentual(senalEntrada, noisePrctgDouble));
        }
        if (seleccionRuido.equals("Ruido Interferencia (Senoidal)")) {
            ruidoEntrada = ruidoEntrada.sumar(GeneradorRuido.crearRuidoInterferencia(noiseAmpDouble, noiseFreqDouble, fs, N));
        }

        actualizarGraficas();
    }

    @FXML
    private void NoiseApplyOnAction(ActionEvent event) {
        String seleccionRuido = NoiseSelector.getValue();
        String noiseFreq = NoiseFreq.getText();
        String noiseAmp = NoiseAmp.getText();
        String noisePrctg = NoisePrctg.getText();

        String regexDecimalPositivo = "^[0-9]+(\\.[0-9]+)?$";
        if (!noiseFreq.matches(regexDecimalPositivo) || !noiseAmp.matches(regexDecimalPositivo) || !noisePrctg.matches(regexDecimalPositivo)) {
            NoiseFreq.setText("ERROR");
            NoiseAmp.setText("ERROR");
            NoisePrctg.setText("ERROR");
            return;
        }

        double noiseFreqDouble = Double.parseDouble(noiseFreq);
        double noiseAmpDouble = Double.parseDouble(noiseAmp);
        double noisePrctgDouble = Double.parseDouble(noisePrctg);

        // Reemplazar el ruido actual por uno nuevo del tipo seleccionado
        if (seleccionRuido.equals("Ruido Aleatorio Plano")) {
            ruidoEntrada = GeneradorRuido.crearRuidoAleatorio(noiseAmpDouble, fs, N);
        }
        if (seleccionRuido.equals("Ruido Aleatorio Porcentual")) {
            ruidoEntrada = GeneradorRuido.aplicarRuidoPorcentual(senalEntrada, noisePrctgDouble);
        }
        if (seleccionRuido.equals("Ruido Interferencia (Senoidal)")) {
            ruidoEntrada = GeneradorRuido.crearRuidoInterferencia(noiseAmpDouble, noiseFreqDouble, fs, N);
        }

        actualizarGraficas();
    }
    
    @FXML
    private void NoiseDeleteOnAction(ActionEvent event){
        ruidoEntrada = Signal.crearContinua(0, fs, N);
        
        actualizarGraficas();
    }
}
