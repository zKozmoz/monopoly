package MONOPOLY.Client.network;

import MONOPOLY.Client.ui.MainController;
import MONOPOLY.Protocol.Message;
import javafx.application.Platform;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream; // Eklendi
import java.net.Socket;

public class NetworkClient {
    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out; // Eklendi
    private MainController controller;

    public NetworkClient(MainController controller) {
        this.controller = controller;
    }

    public void connect(String host, int port) throws Exception {
        socket = new Socket(host, port);

        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.out.flush();

        this.in = new ObjectInputStream(socket.getInputStream());

        new Thread(this::listen).start();
    }

    public void send(Message msg) {
        try {
            out.writeObject(msg);
            out.flush();
        } catch (Exception e) {
            controller.log("Send error: " + e.getMessage());
        }
    }

    private void listen() {
        try {
            while (true) {
                Message msg = (Message) in.readObject();
                handleMessage(msg);
            }
        } catch (Exception e) {
            Platform.runLater(() -> controller.log("Connection lost."));
        }
    }

    private void handleMessage(Message msg) {
        Platform.runLater(() -> {
            switch (msg.getType()) {

                case JOIN_GAME ->
                        controller.handleJoinResponse(msg.getPayload().toString());

                case START_GAME ->
                        controller.onGameStarted();

                case YOUR_TURN -> {
                    controller.log("Senin sıran!");
                    controller.setMyTurn(true);
                }

                case GAME_STATE_UPDATE ->
                        controller.updateGameState(msg.getPayload().toString());

                case END_TURN ->
                        controller.log("Turn ended.");

                case ERROR ->
                        controller.log("Error: " + msg.getPayload());

                default ->
                        controller.log("Unhandled message: " + msg.getType());
            }
        });
    }
}