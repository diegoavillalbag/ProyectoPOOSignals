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

// Importar los paquetes de señales y filtros
import com.mycompany.signals.model.Signal;
import com.mycompany.signals.model.GeneradorFiltros;
import com.mycompany.signals.UI.GeneradorGraficasFX;


public class MainViewController implements Initializable {

    // Constantes del programa
    private final int N = 4096; // Cantidad de puntos de señales y fft
    private final int n = 4; // Reduccion para no graficar todos los puntos
    private final int fs = 4096; 
    
    // Variables del programa
    private Signal senalEntrada;
    private Signal senalEntradaFFT;
    private Signal respuestaFiltro;
    private Signal senalSalida;
    private Signal senalSalidaFFT;
    private Filtro filtroActual; // posiblemente se deba modificar a un arreglo de filtros luego
    
    // Objetos de la interfaz
    GeneradorGraficasFX Graficador = new GeneradorGraficasFX(); // Objeto de tipo graficador para manejar los graficos
    
    // Graficos
    @FXML private GridPane SignalGrid;
    
    
    // Menu señal de entrada
    @FXML private ChoiceBox<String> SignalSelector; // Para seleccionar el tipo de señal a usar
    @FXML private TextField SignalFreq; // Entrada de la frecuencia de la señal de entrada
    @FXML private TextField SignalAmp; // Entrada de la amplitud de la señal de entrada
    
    @FXML
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Al iniciar la interfaz se carga una señal con ruido por defecto
        // Se aplica un filtro paso bajo de 1er orden y se muestra en pantalla
        
        int signalAmp = 1;
        int signalFreq = 10;
        double noiseAmp = 0.15;
        int noiseFreq = 500;
        
        senalEntrada = Signal.crearSenoidal(signalAmp, signalFreq, fs, N);
        Signal ruidoEntrada = Signal.crearSenoidal(noiseAmp, noiseFreq, fs, N);
        senalEntrada = ruidoEntrada.sumar(senalEntrada);
        
        senalEntradaFFT = senalEntrada.calcularFFT();
        
        filtroActual = GeneradorFiltros.crearPasaBajos(100, fs);
        respuestaFiltro = filtroActual.obtenerRespuestaFrecuencia(N, fs);
        
        senalSalida = filtroActual.aplicar(senalEntrada);
        senalSalidaFFT = senalSalida.calcularFFT();
        
        this.actualizarGraficas();
        
        // Al inicializar se deben cargar los datos necesarios dentro de la interfaz
        // Definir las opciones
        String[] opcionesTipoEntrada = {"Senoidal", "Cuadrada"};

        // Cargar las opciones al ChoiceBox
        SignalSelector.getItems().addAll(opcionesTipoEntrada);

        // Opcional: Seleccionar una por defecto
        SignalSelector.setValue(opcionesTipoEntrada[0]);
        
        // Cargar datos a los textfield
        SignalFreq.setText("10");
        SignalAmp.setText("1");
        
        
    }    
    
    public void actualizarGraficas(){
        
        // 1. Limpiar la cuadrícula por si hay gráficos viejos
        SignalGrid.getChildren().clear();

        // 2. Obtener los gráficos de tus métodos
        LineChart<Number, Number> chart1 = Graficador.crearGrafica("Señal de Entrada", "Entrada", senalEntrada, n);
        LineChart<Number, Number> chart2 = Graficador.crearGrafica("FFT de la Señal de Entrada", "FFT Entrada", senalEntradaFFT, 1);
        LineChart<Number, Number> chart3 = Graficador.crearGrafica("Señal de Salida", "Salida", senalSalida, n);
        LineChart<Number, Number> chart4 = Graficador.crearGrafica("FFT de la Señal de Salida", "FFt Salida", senalSalidaFFT, 1);
        XYChart.Series<Number, Number> serieFiltro = Graficador.convertirSignalASerie("Respuesta Filtro", respuestaFiltro, 1);
    
        // Añadir la serie al gráfico existente
        chart2.getData().add(serieFiltro);
        
        // 3. Posicionar en el Grid (Columna, Fila)
        // Fila 0
        SignalGrid.add(chart1, 0, 0); // Arriba Izquierda
        SignalGrid.add(chart2, 1, 0); // Arriba Derecha
        
        // Fila 1
        SignalGrid.add(chart3, 0, 1); // Abajo Izquierda
        SignalGrid.add(chart4, 1, 1); // Abajo Derecha
    }
    
    // Metodos OnAction
    @FXML
    private void SignalApplyOnAction(ActionEvent event) { 
        // Metodo del boton para cambiar la senal de entrada
        
        // Obtener los datos de la interfaz
        String seleccionSenalEntrada = SignalSelector.getValue();
        String freqEntrada = SignalFreq.getText();
        String ampEntrada = SignalAmp.getText();
        
        // Verificar que los input sean numericos (establecer limites)
        if( !(freqEntrada.chars().allMatch(Character::isDigit) && ampEntrada.chars().allMatch(Character::isDigit)) ){
            SignalFreq.setText("ERROR");
            SignalAmp.setText("ERROR");
            return;
        }
        
        // Convertir las entradas a enteros
        int freqEntradaInt = Integer.parseInt(freqEntrada);
        int ampEntradaInt = Integer.parseInt(ampEntrada);
        
        // Si la opcion seleccionada es senoidal
        if( seleccionSenalEntrada.equals("Senoidal") ){
            // Ya verificadas las entradas falta actualizar los graficos con los nuevos datos
            senalEntrada = Signal.crearSenoidal(ampEntradaInt, freqEntradaInt, fs, N);
        }
        
        // Si la opcion seleccionada es cuadarada
        if( seleccionSenalEntrada.equals("Cuadrada") ){
            // Ya verificadas las entradas falta actualizar los graficos con los nuevos datos
            senalEntrada = Signal.crearCuadrada(ampEntradaInt, freqEntradaInt, fs, N);
        }
        
        // Actualizar la FFT de la entrada
        senalEntradaFFT = senalEntrada.calcularFFT();
        
        // Actualizar las salidas
        senalSalida = filtroActual.aplicar(senalEntrada);
        senalSalidaFFT = senalSalida.calcularFFT();
        
        actualizarGraficas();
        
    }
    
    @FXML
    private void SignalDeleteOnAction(ActionEvent event) { 
        // Tu lógica aquí
    }
    
}
