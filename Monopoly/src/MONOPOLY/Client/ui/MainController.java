package MONOPOLY.Client.ui;

import MONOPOLY.Client.network.NetworkClient;
import MONOPOLY.Model.Board;
import MONOPOLY.Protocol.Message;
import MONOPOLY.Protocol.MessageType;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.scene.control.ListView;
import javafx.scene.control.Button;

import java.util.HashMap;
import java.util.Map;

public class MainController {

    @FXML private GridPane board;
    @FXML private TextArea logArea;
    @FXML private HBox decisionBox;
    @FXML private Button rollDiceButton;

    private final Map<Integer, StackPane> tileMap = new HashMap<>();
    private final Circle[] playerTokens = new Circle[4];
    private NetworkClient networkClient;
    private Board gameBoard = new Board();

    @FXML
    private ListView<String> playerListView;


    private final Map<Integer, String> playerInfoMap = new HashMap<>();

    @FXML
    private Button startButton;
    private boolean isOwner = false;
    private boolean gameStarted = false;



    public void handleJoinResponse(String role) {
        if ("OWNER".equals(role)) {
            this.isOwner = true;
            log("Oda sahibi sensin. Oyuncular hazır olduğunda başlatabilirsin.");
            Platform.runLater(() -> {
                startButton.setVisible(true);
                startButton.setManaged(true);
                rollDiceButton.setDisable(true);
            });
        } else {
            this.isOwner = false;
            log("Oyuna katıldın. Sahibin başlatması bekleniyor...");
            Platform.runLater(() -> {
                startButton.setVisible(false);
                startButton.setManaged(false);
                rollDiceButton.setDisable(true);
            });
        }
    }

    @FXML
    private void startGame() {
        log("Game Starting...");
        networkClient.send(new Message(MessageType.START_GAME, null));
    }

    public void onGameStarted() {
        log("Oyun başladı!");
        this.gameStarted = true;
        Platform.runLater(() -> {
            startButton.setVisible(false);
            startButton.setManaged(false);
            rollDiceButton.setText("Zar At");
            log("İyi şanslar!");
        });
    }

    public void setMyTurn(boolean myTurn) {
        Platform.runLater(() -> rollDiceButton.setDisable(!myTurn));
    }

    public void updatePlayerStatus(int pIdx, String name, int money) {
        playerInfoMap.put(pIdx, name + ": " + money + " $");
        Platform.runLater(() -> {
            playerListView.getItems().setAll(playerInfoMap.values());
        });
    }

    @FXML
    public void initialize() {
        buildBoard();
        setupTokens();
        log("Accesso in corso...");

        this.networkClient = new NetworkClient(this);
        try {
            networkClient.connect("localhost", 12345);
            networkClient.send(new Message(MessageType.JOIN_GAME, "Player " + (int)(Math.random()*100)));
        } catch (Exception e) {
            log("Errore di connessione: " + e.getMessage());
        }
    }

    private void buildBoard() {
        for (int i = 0; i < 11; i++) {
            board.getColumnConstraints().add(new ColumnConstraints(55));
            board.getRowConstraints().add(new RowConstraints(55));
        }

        int tileIndex = 0;
        for (int i = 0; i <= 10; i++) addTile(10 - i, 10, tileIndex++);
        for (int i = 1; i <= 9; i++) addTile(0, 10 - i, tileIndex++);
        for (int i = 0; i <= 10; i++) addTile(i, 0, tileIndex++);
        for (int i = 1; i <= 9; i++) addTile(10, i, tileIndex++);
    }

    private void addTile(int col, int row, int index) {
        StackPane tile = new StackPane();
        tile.setStyle("-fx-border-color: #333; -fx-background-color: beige;");

        String name = (index < gameBoard.size()) ? gameBoard.getTile(index).getName() : String.valueOf(index);
        Text label = new Text(name + "\n(" + index + ")");
        label.setStyle("-fx-font-size: 9px; -fx-text-alignment: center;");

        tile.getChildren().add(label);
        board.add(tile, col, row);
        tileMap.put(index, tile);
    }

    private void setupTokens() {
        Color[] colors = {Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW};
        for (int i = 0; i < 4; i++) {
            Circle token = new Circle(10, colors[i]);
            token.setStroke(Color.BLACK);
            playerTokens[i] = token;
            tileMap.get(0).getChildren().add(token);
        }
    }

    @FXML
    private void rollDice() {
        log("Lancio dei dadi...");
        networkClient.send(new Message(MessageType.ROLL_DICE, null));
    }

    public void log(String msg) {
        Platform.runLater(() -> logArea.appendText(msg + "\n"));
    }

    public void updateGameState(Object payload) {
        if (payload instanceof String content) {
            try {
                String[] parts = content.split(":", 5);
                if (parts.length == 5) {
                    int pIdx = Integer.parseInt(parts[0]);
                    int newPos = Integer.parseInt(parts[1]);
                    int money = Integer.parseInt(parts[2]);
                    int ownerIdx = Integer.parseInt(parts[3]);
                    String message = parts[4];

                    log(message);
                    Platform.runLater(() -> {
                        Circle token = playerTokens[pIdx];
                        tileMap.values().forEach(pane -> pane.getChildren().remove(token));
                        tileMap.get(newPos).getChildren().add(token);

                        updatePlayerStatus(pIdx, "Giocatore " + (pIdx + 1), money);

                        if (ownerIdx != -1) {
                            updateTileColor(newPos, ownerIdx);
                        }
                    });
                    setMyTurn(false);
                }
            } catch (Exception e) {
                log("Veri hatası: " + content);
            }
        }
    }

    private void updateTileColor(int tileIndex, int ownerIdx) {
        Color[] playerColors = {Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW};
        StackPane tile = tileMap.get(tileIndex);
        String hex = String.format("#%02X%02X%02X",
                (int)(playerColors[ownerIdx].getRed() * 255),
                (int)(playerColors[ownerIdx].getGreen() * 255),
                (int)(playerColors[ownerIdx].getBlue() * 255));
        tile.setStyle("-fx-border-color: #333; -fx-background-color: " + hex + ";");
    }

    private String toHexString(Color color) {
        return String.format("#%02X%02X%02X",
                (int)(color.getRed() * 255),
                (int)(color.getGreen() * 255),
                (int)(color.getBlue() * 255));
    }

    public void showPurchaseDecision(String propertyInfo) {
        Platform.runLater(() -> {
            log("Vuoi comprare " + propertyInfo.replace(":", " per ") + "$?");
            rollDiceButton.setVisible(false);
            decisionBox.setVisible(true);
            decisionBox.setManaged(true);
        });
    }

    @FXML
    private void handleBuy() {
        networkClient.send(new Message(MessageType.BUY_PROPERTY, null));
        resetButtons();
    }

    @FXML
    private void handleSkip() {
        networkClient.send(new Message(MessageType.SKIP_PROPERTY, null));
        resetButtons();
    }

    private void resetButtons() {
        Platform.runLater(() -> {
            decisionBox.setVisible(false);
            decisionBox.setManaged(false);
            rollDiceButton.setVisible(true);
            rollDiceButton.setDisable(true);
        });
    }
}