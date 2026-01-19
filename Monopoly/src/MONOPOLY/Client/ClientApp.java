package MONOPOLY.Client;

import MONOPOLY.Client.network.NetworkClient;
import MONOPOLY.Client.ui.MainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ClientApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/MONOPOLY/Client/ui/main.fxml"));
        Parent root = loader.load();

        MainController controller = loader.getController();

        NetworkClient networkClient = new NetworkClient(controller);
        try {
            networkClient.connect("localhost", 12345);
            controller.setNetworkClient(networkClient);
        } catch (Exception e) {
            System.err.println("Sunucuya bağlanılamadı: " + e.getMessage());
        }

        Scene scene = new Scene(root);
        stage.setTitle("Monopoly Client");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}