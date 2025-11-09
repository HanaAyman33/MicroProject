package guc.edu;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.geometry.Rectangle2D;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                MainApp.class.getResource("MainView.fxml")
        );

        Parent root;
        try {
            root = loader.load();
        } catch (Exception ex) {
            System.err.println("Failed to load MainView.fxml: " + ex.getMessage());
            ex.printStackTrace();
            throw ex;
        }
        Scene scene = new Scene(root);

        // Ensure standard OS window controls are visible
        primaryStage.initStyle(StageStyle.DECORATED);
        primaryStage.setTitle("Tomasulo Simulator");
        primaryStage.setScene(scene);

        // Size the window within the current screen's visual bounds
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        double targetWidth = Math.min(1280, bounds.getWidth() * 0.95);
        double targetHeight = Math.min(900, bounds.getHeight() * 0.90);

        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
        primaryStage.setWidth(targetWidth);
        primaryStage.setHeight(targetHeight);

        // Center on screen (respecting taskbar/menu bar)
        primaryStage.setX(bounds.getMinX() + (bounds.getWidth() - targetWidth) / 2);
        primaryStage.setY(bounds.getMinY() + (bounds.getHeight() - targetHeight) / 2);

        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
