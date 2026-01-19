package MONOPOLY.Server;

import MONOPOLY.Model.*;
import MONOPOLY.Protocol.*;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ClientHandler implements Runnable {

    private Socket socket;
    private GameState state;
    private GameEngine engine;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private Player player;

    private static final List<ClientHandler> clients = new ArrayList<>();

    public ClientHandler(Socket socket, GameState state, GameEngine engine) {
        this.socket = socket;
        this.state = state;
        this.engine = engine;
        synchronized (clients) {
            clients.add(this);
        }
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            while (true) {
                Message msg = (Message) in.readObject();
                if (msg == null) break;

                switch (msg.getType()) {
                    case SET_PLAYER_COUNT:
                        state.setMaxPlayers((int) msg.getPayload());
                        break;

                    case START_GAME:
                        if (player == state.getOwner() && state.getPlayers().size() >= 2) {
                            state.startGame();
                            broadcast(new Message(MessageType.START_GAME, "Oyun Başladı!"));
                            broadcast(new Message(MessageType.YOUR_TURN, "Sıra ilk oyuncuda."));
                        }
                        break;

                    case JOIN_GAME:
                        player = new Player((String) msg.getPayload());

                        state.addPlayer(player);

                        if (state.getPlayers().size() == 1) {
                            state.setOwner(player);
                        }

                        String role = (state.getOwner() == player) ? "OWNER" : "PLAYER";

                        out.writeObject(new Message(MessageType.JOIN_GAME, role));
                        out.flush();
                        logSystem(player.getName() + " katıldı. Rolü: " + role);
                        break;

                    case ROLL_DICE:
                        if (!state.isGameStarted() || state.getCurrentPlayer() != player) {
                            out.writeObject(new Message(MessageType.ERROR, "Şu an zar atamazsınız!"));
                            break;
                        }
                        String engineResult = engine.rollDice();
                        int pIdx = state.getPlayers().indexOf(player);
                        int newPos = player.getPosition();
                        int currentMoney = player.getMoney();

                        Tile currentTile = state.getBoard().getTile(newPos);
                        int ownerIdx = -1;

                        if (currentTile instanceof PropertyTile property) {
                            Player owner = property.getOwner();
                            if (owner != null) {
                                ownerIdx = state.getPlayers().indexOf(owner);
                            }
                        }

                        String payload = pIdx + ":" + newPos + ":" + currentMoney + ":" + ownerIdx + ":" + player.getName() + " " + engineResult;

                        broadcast(new Message(MessageType.GAME_STATE_UPDATE, payload));
                        break;

                    case END_TURN:
                        engine.endTurn();
                        broadcast(new Message(MessageType.YOUR_TURN, "È il tuo turno."));
                        break;
                }
            }
        } catch (Exception e) {
            System.out.println("Bağlantı kesildi: " + (player != null ? player.getName() : "Bilinmiyor"));
        } finally {
            closeConnection();
        }
    }

    private void broadcast(Message msg) {
        synchronized (clients) {
            for (ClientHandler client : clients) {
                try {
                    client.out.writeObject(msg);
                    client.out.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void logSystem(String msg) {
        System.out.println("[SERVER] " + msg);
    }

    private void closeConnection() {
        try {
            synchronized (clients) {
                clients.remove(this);
            }
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}