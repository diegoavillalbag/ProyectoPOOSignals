package com.mycompany.signals;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * Punto de entrada de la aplicación JavaFX.
 * Carga la vista principal y aplica los estilos CSS.
 */
public class SignalsMain extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // 1. Cargar el FXML (crea la interfaz y enlaza el controlador)
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/fxml/MainView.fxml"));
        Parent root = loader.load();

        // 2. Crear la escena
        Scene scene = new Scene(root);

        // 3. Aplicar estilos CSS
        scene.getStylesheets().add(getClass().getResource("/ui/styles/mainview.css").toExternalForm());

        // 4. Configurar y mostrar la ventana
        primaryStage.setTitle("Simulador de Señales y Filtros");
        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/ui/images/logo3.png")));
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
