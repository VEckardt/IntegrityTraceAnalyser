/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package traceanalyser;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 *
 * @author veckardt
 */
public class TraceAnalyser extends Application {

    public static Stage stage;
    public static Image warnImage;

    public static void main(String[] args) {
        Application.launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("TraceAnalyser.fxml"));
        root.autosize();
        Scene scene = new Scene(root);
        TraceAnalyser.stage = stage;
        stage.setTitle(Copyright.title);

        Image applicationIcon = new Image(getClass().getResourceAsStream("resources/Move.png"));
        stage.getIcons().add(applicationIcon);
        
        warnImage = new Image(getClass().getResourceAsStream("resources/warn.png"));
        
        // stage.setWidth(700);
        // stage.setHeight(300);
        stage.setScene(scene);
        stage.show();
    }
}