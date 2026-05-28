# ProyectoPOOSignals

## Descripción

ProyectoPOOSignals es una aplicación JavaFX para generar, visualizar y procesar señales discretas. Permite crear señales básicas, añadir ruido, aplicar filtros digitales y observar los resultados en tiempo y frecuencia.

## Estructura del proyecto

- `src/main/java/com/mycompany/signals/SignalsMain.java`
  - Punto de entrada JavaFX.
  - Carga la interfaz `MainView.fxml` y aplica estilos CSS.

- `src/main/java/com/mycompany/signals/UI/controllers/MainViewController.java`
  - Controlador principal de la UI.
  - Gestiona la creación de señales, ruido, filtros y la actualización de gráficos.
  - Maneja la validación de entradas, guardado y carga de estado.

- `src/main/java/com/mycompany/signals/UI/GeneradorGraficasFX.java`
  - Crea y actualiza gráficos de JavaFX.
  - Renderiza señales en el dominio del tiempo y espectros de frecuencia (FFT).

- `src/main/java/com/mycompany/signals/model/Signal.java`
  - Representa una señal discreta con ejes de tiempo y amplitud.
  - Genera formas de onda y calcula FFT.

- `src/main/java/com/mycompany/signals/model/GeneradorRuido.java`
  - Genera ruido aleatorio, ruido porcentual y ruido de interferencia senoidal.

- `src/main/java/com/mycompany/signals/model/GeneradorFiltros.java`
  - Fabrica filtros lineales y no lineales.
  - Incluye filtros Butterworth, pasa banda, rechaza banda, media móvil y Savitzky-Golay.

- `src/main/java/com/mycompany/signals/model/Filtro.java`
  - Interfaz común para filtros digitales.
  - Define el método `aplicar` y calcula la respuesta en frecuencia.

- `src/main/java/com/mycompany/signals/model/FiltroDiferencias.java`
  - Implementa filtros por ecuación de diferencias (IIR/FIR).

- `src/main/java/com/mycompany/signals/model/FiltroMediana.java`
- `src/main/java/com/mycompany/signals/model/FiltroHampel.java`
- `src/main/java/com/mycompany/signals/model/FiltroBilateral.java`
  - Filtros no lineales en el dominio del tiempo.

- `src/main/java/com/mycompany/signals/model/EstadoPrograma.java`
  - Contiene el estado serializable de la aplicación para guardar y cargar sesiones.

## Cómo se generan las señales

Las señales se crean en `Signal.java` mediante métodos estáticos como:

- `crearSenoidal(amplitud, frecuencia, fs, N)`
- `crearCuadrada(amplitud, frecuencia, fs, N)`
- `crearTriangular(amplitud, frecuencia, fs, N)`
- `crearDienteDeSierra(amplitud, frecuencia, fs, N)`
- `crearContinua(amplitud, fs, N)`

Cada señal construye un eje temporal uniforme `t[i] = i / fs` y un vector de amplitudes `ft`.

## Cómo se generan los filtros

Los filtros se generan en `GeneradorFiltros.java` y se dividen en dos tipos:

1. Filtros lineales por ecuación de diferencias con coeficientes `b` y `a`.
   - `crearPasaBajos`
   - `crearPasaAltos`
   - `crearPasaBajos2doOrden`
   - `crearPasaAltos2doOrden`
   - `crearPasaBanda`
   - `crearRechazaBanda`
   - `crearMediaMovil`
   - `crearSavitzkyGolay...`

2. Filtros no lineales en tiempo:
   - `FiltroMediana`
   - `FiltroHampel`
   - `FiltroBilateral`

Los filtros lineales se aplican con la ecuación:

- `y[n] = sum(b[i]*x[n-i]) - sum(a[j]*y[n-j])`

Los filtros no lineales procesan la ventana vecina de cada muestra para remover ruido de forma adaptativa.

## Cómo se genera el ruido

El ruido se produce en `GeneradorRuido.java`:

- `crearRuidoAleatorio(amplitudMax, fs, N)`
  - Ruido blanco gaussiano.

- `aplicarRuidoPorcentual(senalBase, porcentaje)`
  - Ruido proporcional a la amplitud de la señal base.

- `crearRuidoInterferencia(amplitud, frecuenciaHz, fs, N)`
  - Ruido tonal senoidal adicional.

## Interacción entre señales, ruido y filtros

El flujo principal se realiza en `MainViewController.actualizarGraficas()`:

1. Suma la señal de entrada con el ruido: `entradaConRuido = senalEntrada.sumar(ruidoEntrada)`.
2. Calcula la FFT de la señal con ruido: `senalEntradaFFT = entradaConRuido.calcularFFT()`.
3. Aplica el filtro seleccionado: `senalSalida = filtroActual.aplicar(entradaConRuido)`.
4. Calcula la FFT de la señal filtrada: `senalSalidaFFT = senalSalida.calcularFFT()`.
5. Obtiene la respuesta en frecuencia del filtro: `respuestaFiltro = filtroActual.obtenerRespuestaFrecuencia(N, fs)`.
6. Muestra los resultados en cuatro gráficos:
   - Señal de entrada en tiempo.
   - FFT de la señal de entrada.
   - Señal de salida en tiempo.
   - FFT de la señal de salida.
   - Respuesta del filtro superpuesta.

## Guía básica de uso

1. Abrir la aplicación.
2. En la sección **Señal de entrada**:
   - Seleccionar el tipo de señal.
   - Ajustar frecuencia y amplitud.
   - Presionar `Aplicar`.

3. En la sección **Ruido**:
   - Seleccionar el tipo de ruido.
   - Ajustar parámetros.
   - Presionar `Aplicar` para reemplazar el ruido o `Sumar` para acumular ruido.
   - Presionar `Eliminar` para quitar todo el ruido.

4. En la sección **Filtro**:
   - Seleccionar el filtro.
   - Ajustar los parámetros habilitados.
   - Presionar `Aplicar`.
   - Presionar `Eliminar` para restablecer el filtro.

5. Observar los gráficos que se actualizan automáticamente.
6. Guardar o cargar el estado completo con los botones correspondientes.

## Validación de entradas

La UI valida los valores del usuario y muestra diálogos de error si los datos son inválidos o están fuera de rango. Los valores no se sobrescriben en el campo cuando hay error.

## Requisitos

- Java 11+ con JavaFX.
- Maven para construir y ejecutar el proyecto.

## Ejecución

Usar Maven o ejecutar la clase `SignalsMain` desde el IDE.
