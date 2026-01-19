package MONOPOLY.Client.ui;

import MONOPOLY.Client.network.NetworkClient;
import MONOPOLY.Model.Board;
import MONOPOLY.Protocol.Message;
import MONOPOLY.Protocol.MessageType;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.geometry.Pos;

import java.util.HashMap;
import java.util.Map;

public class MainController {

    @FXML private GridPane board;
    @FXML private TextArea logArea;
    @FXML private Button startButton;
    @FXML private Button rollDiceButton;
    @FXML private HBox decisionBox;
    @FXML private ListView<PlayerInfo> playerListView;

    private final Map<Integer, StackPane> tileMap = new HashMap<>();
    private final Circle[] playerTokens = new Circle[4];
    private NetworkClient networkClient;
    private Board gameBoard = new Board();
    private boolean isOwner = false;

    public static class PlayerInfo {
        int id;
        String name;
        int money;
        public PlayerInfo(int id, String name, int money) {
            this.id = id; this.name = name; this.money = money;
        }
        @Override public String toString() { return name + ": " + money + " $"; }
    }

    @FXML
    public void initialize() {
        buildBoard();
        setupTokens();

        playerListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(PlayerInfo item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.toString());
                    setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

                    Circle c = new Circle(8);
                    Color[] colors = {Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW};
                    if (item.id >= 0 && item.id < colors.length) c.setFill(colors[item.id]);
                    else c.setFill(Color.GREY);

                    setGraphic(c);
                }
            }
        });

        log("Hoşgeldiniz! Bağlantı bekleniyor...");
        startButton.setVisible(false);
        startButton.setManaged(false);
        rollDiceButton.setDisable(true);
        if(decisionBox != null) { decisionBox.setVisible(false); decisionBox.setManaged(false); }
    }

    public void setNetworkClient(NetworkClient networkClient) {
        this.networkClient = networkClient;
        networkClient.send(new Message(MessageType.JOIN_GAME, "Player " + (int)(Math.random()*1000)));
    }

    public void handleJoinResponse(String role) {
        if ("OWNER".equals(role)) {
            this.isOwner = true;
            log("Odayı kurdunuz. Oyuncular bekleniyor...");
            Platform.runLater(() -> {
                startButton.setVisible(true);
                startButton.setManaged(true);
            });
        } else {
            log("Odaya katıldınız. Kurucunun başlatması bekleniyor.");
        }
    }

    @FXML
    private void startGame() {
        log("Game Starting...");
        networkClient.send(new Message(MessageType.START_GAME, null));
    }

    public void onGameStarted() {
        Platform.runLater(() -> {
            startButton.setVisible(false);
            startButton.setManaged(false);
            log("OYUN BAŞLADI!");
        });
    }

    @FXML private void rollDice() { networkClient.send(new Message(MessageType.ROLL_DICE, null)); }
    @FXML private void handleBuy() { networkClient.send(new Message(MessageType.BUY_PROPERTY, null)); closeDecision(); }
    @FXML private void handleSkip() { networkClient.send(new Message(MessageType.SKIP_PROPERTY, null)); closeDecision(); }

    public void showPurchaseDecision(String info) {
        Platform.runLater(() -> {
            log("Satın almak ister misin? " + info);
            rollDiceButton.setVisible(false);
            rollDiceButton.setManaged(false);
            if(decisionBox != null) { decisionBox.setVisible(true); decisionBox.setManaged(true); }
        });
    }

    private void closeDecision() {
        Platform.runLater(() -> {
            if(decisionBox != null) { decisionBox.setVisible(false); decisionBox.setManaged(false); }
            rollDiceButton.setVisible(true);
            rollDiceButton.setManaged(true);
            rollDiceButton.setDisable(true); // Karar verince sıra biter
        });
    }

    public void setMyTurn(boolean myTurn) {
        Platform.runLater(() -> {
            rollDiceButton.setDisable(!myTurn);
            if (myTurn) log(">>> SENİN SIRAN <<<");
        });
    }

    public void updateGameState(Object payload) {
        if (payload instanceof String content) {
            try {
                // pIdx:pos:money:ownerIdx:message
                String[] parts = content.split(":", 5);
                if (parts.length == 5) {
                    int pIdx = Integer.parseInt(parts[0]);
                    int newPos = Integer.parseInt(parts[1]);
                    int money = Integer.parseInt(parts[2]);
                    int ownerIdx = Integer.parseInt(parts[3]);
                    String message = parts[4];

                    log(message);

                    if (pIdx != -1) updatePlayerList(pIdx, "Player " + pIdx, money);

                    Platform.runLater(() -> {
                        if (pIdx != -1 && newPos != -1) {
                            Circle token = playerTokens[pIdx];
                            tileMap.values().forEach(pane -> pane.getChildren().remove(token));
                            if (tileMap.containsKey(newPos)) tileMap.get(newPos).getChildren().add(token);
                            StackPane.setAlignment(token, Pos.CENTER);
                        }
                        if (ownerIdx != -1 && newPos != -1) {
                            updateTileColor(newPos, ownerIdx);
                        }
                    });
                } else {
                    log(content);
                }
            } catch (Exception e) {
                log("Hata: " + content);
            }
        }
    }

    private void updatePlayerList(int id, String name, int money) {
        Platform.runLater(() -> {
            boolean found = false;
            for (PlayerInfo p : playerListView.getItems()) {
                if (p.id == id) {
                    p.money = money;
                    found = true;
                    break;
                }
            }
            if (!found) playerListView.getItems().add(new PlayerInfo(id, name, money));
            playerListView.refresh();
        });
    }

    private void updateTileColor(int tileIndex, int ownerIdx) {
        Color[] colors = {Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW};
        StackPane tile = tileMap.get(tileIndex);
        String hex = String.format("#%02X%02X%02X", (int)(colors[ownerIdx].getRed()*255), (int)(colors[ownerIdx].getGreen()*255), (int)(colors[ownerIdx].getBlue()*255));
        tile.setStyle("-fx-background-color: " + hex + "; -fx-border-color: black; -fx-opacity: 0.7;");
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
        tile.setStyle("-fx-border-color: #7f8c8d; -fx-background-color: #f1c40f;");
        String name = (index < gameBoard.size()) ? gameBoard.getTile(index).getName() : String.valueOf(index);
        Text label = new Text(name + "\n(" + index + ")");
        label.setStyle("-fx-font-size: 8px; -fx-text-alignment: center;");
        tile.getChildren().add(label);
        board.add(tile, col, row);
        tileMap.put(index, tile);
    }

    private void setupTokens() {
        Color[] colors = {Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW};
        for (int i = 0; i < 4; i++) {
            Circle token = new Circle(8, colors[i]);
            token.setStroke(Color.BLACK);
            token.setStrokeWidth(2);
            playerTokens[i] = token;
            tileMap.get(0).getChildren().add(token);
        }
    }

    public void log(String msg) { Platform.runLater(() -> logArea.appendText(msg + "\n")); }
}