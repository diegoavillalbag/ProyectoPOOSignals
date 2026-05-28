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
import javafx.scene.control.Button;
import javafx.scene.control.Alert;

// Paquetes de input/output
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

// Paquetes del programa
import com.mycompany.signals.model.Signal;
import com.mycompany.signals.model.GeneradorFiltros;
import com.mycompany.signals.UI.GeneradorGraficasFX;
import com.mycompany.signals.model.GeneradorRuido;
import com.mycompany.signals.model.EstadoPrograma;


/**
 * Controlador de la vista principal: señal de entrada, ruido, filtro y cuatro gráficos.
 * Enlaza los controles FXML con el modelo de señales y {@link GeneradorGraficasFX}.
 */
public class MainViewController implements Initializable {

    // --- Constantes del programa ---
    private final int N = 65536; // Cantidad de puntos de señales y FFT
    private final int paso = 1; // Muestreo temporal: cada cuántos índices se grafica un punto (1 = todos)
    private final int pasoFft = 32; // FFT: tamaño de ventana de submuestreo (pico por bloque)
    private final int pasoRespuestaFiltro = 50; // Respuesta del filtro en gráfico FFT
    private final int fs = 65536; // Frecuencia de muestreo (Hz)
    private double tiempoInicio = 0;
    private double tiempoFin = 0.01;
    private double freqInicio = 0;
    private double freqFin = 8000;

    
    
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
    private final String[] opcionesTipoSenalEntrada = {"Señal Continua",
                                                       "Señal Senoidal", 
                                                       "Señal Cuadrada",
                                                       "Señal Triangular", 
                                                       "Señal Diente de Sierra"};
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
    private final String[] opcionesTipoFiltro = {
            "Pasa Bajos 1er Orden", "Pasa Altos 1er Orden",
            "Pasa Bajos 2do Orden", "Pasa Altos 2do Orden", "Pasa Banda 2do Orden",
            "Rechaza Banda 2do Orden",
            "Filtro Bilateral",
            "Filtro Mediana",
            "Filtro Hampel (Mediana Mejorado)",
            "Filtro Media Movil",
            "Filtro Savitzky-Golay 5 Puntos", //"Filtro Savitzky-Golay 7 Puntos",
            "Filtro Savitzky-Golay 11 Puntos", //"Filtro Savitzky-Golay 15 Puntos",
            "Filtro Savitzky-Golay 21 Puntos"};
    @FXML private TextField FilterFc;
    @FXML private TextField FilterQ; // Solo habilitado para Pasa Banda 2do Orden
    @FXML private TextField FilterPuntos; // Solo habilitado para Media y Mediana
    @FXML private TextField FilterSigma; // Solo habilitado para Hampel
    
    // --- Botones de Guardado ---
    @FXML private Button BotonGuardar1;
    @FXML private Button BotonGuardar2;
    @FXML private Button BotonGuardar3;
    
    
    
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
        int filterPuntos = 3;
        double filterSigma = 1.5; // Para filtro Hamel
        filtroActual = GeneradorFiltros.crearPasaBajos(filterFc, fs);

        inicializarGraficas();
        actualizarGraficas();
        
        // Poblar ChoiceBox y TextField de cada sección
        SignalSelector.getItems().addAll(opcionesTipoSenalEntrada);
        SignalSelector.setValue(opcionesTipoSenalEntrada[1]);
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
        FilterPuntos.setText(String.valueOf(filterPuntos));
        FilterSigma.setText(String.valueOf(filterSigma));

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
        FilterPuntos.setDisable(true);
        FilterSigma.setDisable(true);
        FilterSelector.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && (newValue.equals("Pasa Banda 2do Orden") 
                || newValue.equals("Rechaza Banda 2do Orden"))) {
                FilterFc.setDisable(false);
                FilterQ.setDisable(false);
                FilterPuntos.setDisable(true);
                FilterSigma.setDisable(true);
            } else if(newValue != null && newValue.startsWith("Filtro Savitzky-Golay")){
                FilterFc.setDisable(true);
                FilterQ.setDisable(true);
                FilterPuntos.setDisable(true);
                FilterSigma.setDisable(true);
            } else if(newValue != null && (newValue.startsWith("Filtro Mediana") ||
                                           newValue.startsWith("Filtro Media Movil"))){
                FilterFc.setDisable(true);
                FilterQ.setDisable(true);
                FilterPuntos.setDisable(false);
                FilterSigma.setDisable(true);
            } else if(newValue != null && newValue.startsWith("Filtro Hampel")){
                FilterFc.setDisable(true);
                FilterQ.setDisable(true);
                FilterPuntos.setDisable(false);
                FilterSigma.setDisable(false);
            } else if(newValue != null && newValue.startsWith("Filtro Bilateral")){
                FilterFc.setDisable(true);
                FilterQ.setDisable(true);
                FilterPuntos.setDisable(false);
                FilterSigma.setDisable(false);
            } else {
                FilterFc.setDisable(false);
                FilterQ.setDisable(true);
                FilterPuntos.setDisable(true);
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
        double multiplicador = senalEntradaFFT.obtenerAmplitudMaxima();
        if (multiplicador==0.0) multiplicador = 1;
        respuestaFiltro = respuestaFiltro.multiplicarPorEscalar(multiplicador);

        // Actualizar series (sin recrear los LineChart)
        Graficador.actualizarSerie(chartEntrada.getData().get(0), entradaConRuido, paso, false, tiempoInicio, tiempoFin);
        Graficador.actualizarSerie(chartFFTEntrada.getData().get(0), senalEntradaFFT, pasoFft, true, freqInicio, freqFin);
        Graficador.actualizarSerie(serieRespuestaFiltro, respuestaFiltro, pasoRespuestaFiltro, false, freqInicio, freqFin);
        Graficador.actualizarSerie(chartSalida.getData().get(0), senalSalida, paso, false, tiempoInicio, tiempoFin);
        Graficador.actualizarSerie(chartFFTSalida.getData().get(0), senalSalidaFFT, pasoFft, true, freqInicio, freqFin);
    }
    
    // --- Handlers FXML: señal de entrada ---

    @FXML
    private void SignalApplyOnAction(ActionEvent event) {
        // Obtener los datos de la interfaz
        String seleccionSenalEntrada = SignalSelector.getValue();
        String freqEntrada = SignalFreq.getText();
        String ampEntrada = SignalAmp.getText();
        
        // Validar frecuencia
        String validationError = validarEntrada("Frecuencia de Señal", freqEntrada, 0.1, 8000, true);
        if (validationError != null) {
            mostrarError(validationError);
            return;
        }
        
        // Validar amplitud
        validationError = validarEntrada("Amplitud de Señal", ampEntrada, 0.01, 10, true);
        if (validationError != null) {
            mostrarError(validationError);
            return;
        }
        
        // Convertir las entradas a double
        double freqEntradaDouble = Double.parseDouble(freqEntrada);
        double ampEntradaDouble = Double.parseDouble(ampEntrada);
        
        if (seleccionSenalEntrada.equals("Señal Continua")) {
            senalEntrada = Signal.crearContinua(ampEntradaDouble, fs, N);
        }
        if (seleccionSenalEntrada.equals("Señal Senoidal")) {
            senalEntrada = Signal.crearSenoidal(ampEntradaDouble, freqEntradaDouble, fs, N);
        }
        if (seleccionSenalEntrada.equals("Señal Cuadrada")) {
            senalEntrada = Signal.crearCuadrada(ampEntradaDouble, freqEntradaDouble, fs, N);
        }
        if (seleccionSenalEntrada.equals("Señal Triangular")) {
            senalEntrada = Signal.crearTriangular(ampEntradaDouble, freqEntradaDouble, fs, N);
        }
        if (seleccionSenalEntrada.equals("Señal Diente de Sierra")) {
            senalEntrada = Signal.crearDienteDeSierra(ampEntradaDouble, freqEntradaDouble, fs, N);
        }

        actualizarGraficas();
        
    }
    
    /**
     * Borra la señal de entrada y restablece los valores de la UI a cero.
     */
    @FXML
    private void SignalDeleteOnAction(ActionEvent event) { 
        senalEntrada = Signal.crearContinua( 0, fs, N);
        
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
        String filterPuntos = FilterPuntos.getText();
        String filterSigma = FilterSigma.getText();
        
        // Validar Fc (frecuencia de corte)
        String validationError = validarEntrada("Frecuencia de Corte", filterFc, 1, 8000, true);
        if (validationError != null) {
            mostrarError(validationError);
            return;
        }
        
        // Validar Q
        validationError = validarEntrada("Factor Q", filterQ, 0.1, 100, true);
        if (validationError != null) {
            mostrarError(validationError);
            return;
        }
        
        // Validar Sigma
        validationError = validarEntrada("Sigma", filterSigma, 0.01, 10, true);
        if (validationError != null) {
            mostrarError(validationError);
            return;
        }
        
        // Validar Puntos
        validationError = validarEntrada("Puntos", filterPuntos, 1, 100, false);
        if (validationError != null) {
            mostrarError(validationError);
            return;
        }
        
        // Convertir las entradas a double
        double filterFcDouble = Double.parseDouble(filterFc);
        double filterQDouble = Double.parseDouble(filterQ);
        double filterSigmaDouble = Double.parseDouble(filterSigma);
        
        // Convertir las entradas a int
        int filterPuntosInt = Integer.parseInt(filterPuntos);
        
        // Primer Orden
        if (seleccionFiltro.equals("Pasa Bajos 1er Orden")) {
            filtroActual = GeneradorFiltros.crearPasaBajos(filterFcDouble, fs);
        }
        if (seleccionFiltro.equals("Pasa Altos 1er Orden")) {
            filtroActual = GeneradorFiltros.crearPasaAltos(filterFcDouble, fs);
        }
        
        // Segundo Orden
        if (seleccionFiltro.equals("Pasa Bajos 2do Orden")) {
            filtroActual = GeneradorFiltros.crearPasaBajos2doOrden(filterFcDouble, fs);
        }
        if (seleccionFiltro.equals("Pasa Altos 2do Orden")) {
            filtroActual = GeneradorFiltros.crearPasaAltos2doOrden(filterFcDouble, fs);
        }
        if (seleccionFiltro.equals("Pasa Banda 2do Orden")) {
            filtroActual = GeneradorFiltros.crearPasaBanda(filterFcDouble, filterQDouble, fs);
        }
        if (seleccionFiltro.equals("Rechaza Banda 2do Orden")) {
            filtroActual = GeneradorFiltros.crearRechazaBanda(filterFcDouble, filterQDouble, fs);
        }
        
        // Filtros en tiempo
        if (seleccionFiltro.equals("Filtro Mediana")) {
            filtroActual = GeneradorFiltros.crearFiltroMediana(filterPuntosInt);
        }
        if (seleccionFiltro.equals("Filtro Hampel (Mediana Mejorado)")) {
            filtroActual = GeneradorFiltros.crearFiltroHampel(filterPuntosInt, filterSigmaDouble);
        }
        if (seleccionFiltro.equals("Filtro Media Movil")) {
            filtroActual = GeneradorFiltros.crearMediaMovil(filterPuntosInt);
        }
        if (seleccionFiltro.equals("Filtro Bilateral")) {
            filtroActual = GeneradorFiltros.crearFiltroBilateral(filterPuntosInt, filterSigmaDouble);
        }
        
        // Filtros Savitzky-Golay
        if (seleccionFiltro.equals("Filtro Savitzky-Golay 5 Puntos")) {
            filtroActual = GeneradorFiltros.crearSavitzkyGolay5Puntos();
        }
        if (seleccionFiltro.equals("Filtro Savitzky-Golay 7 Puntos")) {
            filtroActual = GeneradorFiltros.crearSavitzkyGolay7Puntos();
        }
        if (seleccionFiltro.equals("Filtro Savitzky-Golay 11 Puntos")) {
            filtroActual = GeneradorFiltros.crearSavitzkyGolay11Puntos();
        }
        if (seleccionFiltro.equals("Filtro Savitzky-Golay 15 Puntos")) {
            filtroActual = GeneradorFiltros.crearSavitzkyGolay15Puntos();
        }
        if (seleccionFiltro.equals("Filtro Savitzky-Golay 21 Puntos")) {
            filtroActual = GeneradorFiltros.crearSavitzkyGolay21Puntos();
        }

        actualizarGraficas();
        
    }
    
    /**
     * Restaura un filtro neutro de pasa-altos con frecuencia cero.
     */
    @FXML
    private void FilterDeleteOnAction(ActionEvent event){
        filtroActual = GeneradorFiltros.crearPasaAltos(0, fs);
        actualizarGraficas();
    }

    // --- Handlers FXML: ruido ---


    /**
     * Añade ruido nuevo al ruido existente según el tipo seleccionado.
     */
    @FXML
    private void NoiseSumOnAction(ActionEvent event) {
        // Obtener datos de la interfaz
        String seleccionRuido = NoiseSelector.getValue();
        String noiseFreq = NoiseFreq.getText();
        String noiseAmp = NoiseAmp.getText();
        String noisePrctg = NoisePrctg.getText();
        
        // Validar frecuencia de ruido
        String validationError = validarEntrada("Frecuencia de Ruido", noiseFreq, 0.1, 8000, true);
        if (validationError != null) {
            mostrarError(validationError);
            return;
        }
        
        // Validar amplitud de ruido
        validationError = validarEntrada("Amplitud de Ruido", noiseAmp, 0.01, 10, true);
        if (validationError != null) {
            mostrarError(validationError);
            return;
        }
        
        // Validar porcentaje de ruido
        validationError = validarEntrada("Porcentaje de Ruido", noisePrctg, 0.01, 1, true);
        if (validationError != null) {
            mostrarError(validationError);
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

    /**
     * Reemplaza el ruido actual por uno nuevo según el tipo seleccionado.
     */
    @FXML
    private void NoiseApplyOnAction(ActionEvent event) {
        String seleccionRuido = NoiseSelector.getValue();
        String noiseFreq = NoiseFreq.getText();
        String noiseAmp = NoiseAmp.getText();
        String noisePrctg = NoisePrctg.getText();

        // Validar frecuencia de ruido
        String validationError = validarEntrada("Frecuencia de Ruido", noiseFreq, 0.1, 8000, true);
        if (validationError != null) {
            mostrarError(validationError);
            return;
        }
        
        // Validar amplitud de ruido
        validationError = validarEntrada("Amplitud de Ruido", noiseAmp, 0.01, 10, true);
        if (validationError != null) {
            mostrarError(validationError);
            return;
        }
        
        // Validar porcentaje de ruido
        validationError = validarEntrada("Porcentaje de Ruido", noisePrctg, 0.01, 1, true);
        if (validationError != null) {
            mostrarError(validationError);
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
    
    /**
     * Elimina todo el ruido actual dejando la señal de entrada limpia.
     */
    @FXML
    private void NoiseDeleteOnAction(ActionEvent event){
        ruidoEntrada = Signal.crearContinua(0, fs, N);
        
        actualizarGraficas();
    }
    
    /**
     * Guarda el estado completo del programa en un archivo .dat.
     */
    @FXML
    private void BotonGuardarOnAction(ActionEvent event){
        // 1. Crear la cápsula de estado y llenarla con los datos actuales
        EstadoPrograma estado = new EstadoPrograma();

        estado.senalEntrada = this.senalEntrada;
        estado.ruidoEntrada = this.ruidoEntrada;
        estado.filtroActual = this.filtroActual;

        estado.tipoSenal = SignalSelector.getValue();
        estado.freqSenal = SignalFreq.getText();
        estado.ampSenal = SignalAmp.getText();

        estado.tipoRuido = NoiseSelector.getValue();
        estado.freqRuido = NoiseFreq.getText();
        estado.ampRuido = NoiseAmp.getText();
        estado.prctgRuido = NoisePrctg.getText();

        estado.tipoFiltro = FilterSelector.getValue();
        estado.fcFiltro = FilterFc.getText();
        estado.qFiltro = FilterQ.getText();
        estado.puntosFiltro = FilterPuntos.getText();
        estado.sigmaFiltro = FilterSigma.getText();

        // 2. Abrir ventana de diálogo para elegir dónde guardar
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar Estado de Señales");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivos de Datos (*.dat)", "*.dat"));
        
        // Obtener el Stage actual desde el evento
        Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);

        // 3. Serializar y guardar en el archivo
        if (file != null) {
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
                oos.writeObject(estado);
                System.out.println("Datos guardados exitosamente en: " + file.getAbsolutePath());
            } catch (Exception e) {
                System.err.println("Error al guardar el archivo: " + e.getMessage());
            }
        }
    }
    
    /**
     * Valida que una entrada sea numérica y esté dentro de los límites establecidos.
     * @param nombreCampo Nombre del campo para mostrar en el error
     * @param valor Valor a validar
     * @param minimo Valor mínimo aceptado
     * @param maximo Valor máximo aceptado
     * @param esDecimal true si es decimal, false si es entero
     * @return null si la validación es exitosa, mensaje de error en caso contrario
     */
    private String validarEntrada(String nombreCampo, String valor, double minimo, double maximo, boolean esDecimal) {
        if (valor == null || valor.trim().isEmpty()) {
            return nombreCampo + ": El campo no puede estar vacío.";
        }
        
        try {
            double numValue = Double.parseDouble(valor);
            
            if (esDecimal) {
                // Para decimales
                if (numValue <= minimo || numValue > maximo) {
                    return nombreCampo + ": Debe estar entre " + minimo + " y " + maximo + ".";
                }
            } else {
                // Para enteros
                if (numValue != Math.floor(numValue)) {
                    return nombreCampo + ": Debe ser un número entero.";
                }
                int intValue = (int) numValue;
                if (intValue < minimo || intValue > maximo) {
                    return nombreCampo + ": Debe estar entre " + (int)minimo + " y " + (int)maximo + ".";
                }
            }
            return null; // Validación exitosa
        } catch (NumberFormatException e) {
            return nombreCampo + ": Debe ser un número válido.";
        }
    }
    
    /**
     * Muestra un diálogo de error al usuario.
     * @param mensaje Mensaje de error a mostrar
     */
    private void mostrarError(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error de Validación");
        alert.setHeaderText("Entrada Inválida");
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
    
    /**
     * Carga un estado previo del programa desde un archivo .dat.
     */
    @FXML
    private void BotonCargarOnAction(ActionEvent event){
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Abrir Estado de Señales");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivos de Datos (*.dat)", "*.dat"));
        
        Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                // 1. Deserializar el objeto
                EstadoPrograma estado = (EstadoPrograma) ois.readObject();

                // 2. Restaurar los modelos
                this.senalEntrada = estado.senalEntrada;
                this.ruidoEntrada = estado.ruidoEntrada;
                this.filtroActual = estado.filtroActual;

                // 3. Restaurar la interfaz gráfica (UI)
                SignalSelector.setValue(estado.tipoSenal);
                SignalFreq.setText(estado.freqSenal);
                SignalAmp.setText(estado.ampSenal);

                NoiseSelector.setValue(estado.tipoRuido);
                NoiseFreq.setText(estado.freqRuido);
                NoiseAmp.setText(estado.ampRuido);
                NoisePrctg.setText(estado.prctgRuido);

                FilterSelector.setValue(estado.tipoFiltro);
                FilterFc.setText(estado.fcFiltro);
                FilterQ.setText(estado.qFiltro);
                FilterPuntos.setText(estado.puntosFiltro);
                FilterSigma.setText(estado.sigmaFiltro);

                // 4. Refrescar los gráficos con los nuevos datos
                actualizarGraficas();
                System.out.println("Datos cargados exitosamente desde: " + file.getAbsolutePath());

            } catch (Exception e) {
                System.err.println("Error al cargar el archivo: " + e.getMessage());
            }
        }
    }
    
}
