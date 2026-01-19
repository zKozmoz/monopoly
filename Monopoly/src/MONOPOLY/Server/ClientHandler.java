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
                    case JOIN_GAME:
                        player = new Player((String) msg.getPayload());
                        state.addPlayer(player);

                        // Eğer ilk giren oyuncuysa Host yap
                        if (state.getPlayers().size() == 1) {
                            state.setOwner(player);
                            sendMessage(new Message(MessageType.JOIN_GAME, "OWNER"));
                        }

                        broadcast(new Message(MessageType.GAME_STATE_UPDATE, "-1:-1:0:-1:" + player.getName() + " è entrato!"));
                        break;

                    case START_GAME:
                        if (player == state.getOwner() && state.getPlayers().size() >= 1) {
                            state.startGame();
                            broadcast(new Message(MessageType.START_GAME, "La partita è iniziata!"));
                            sendTurnNotification();
                        }
                        break;

                    case ROLL_DICE:
                        if (state.isGameStarted() && player == state.getCurrentPlayer()) {
                            int dice = new java.util.Random().nextInt(6) + 1;
                            player.move(dice, state.getBoard().size());

                            int newPos = player.getPosition();
                            Tile tile = state.getBoard().getTile(newPos);

                            if (tile instanceof PropertyTile pt) {
                                if (pt.getOwner() == null) {
                                    // Mülk sahipsiz -> Oyuncuya sor
                                    sendMessage(new Message(MessageType.PROMPT_PURCHASE, pt.getName() + ":" + pt.getPrice()));
                                    // Not: Burada sırayı henüz DEĞİŞTİRMİYORUZ, cevap bekliyoruz.
                                } else if (pt.getOwner() != player) {
                                    // Mülk başkasının -> KİRA ÖDE
                                    String rentMsg = pt.onLand(player); // Kira mantığı zaten PropertyTile.onLand içinde var
                                    broadcastUpdate(newPos, player.getName() + " " + rentMsg);
                                    state.nextTurn();
                                    sendTurnNotification();
                                } else {
                                    // Kendi mülkü
                                    broadcastUpdate(newPos, player.getName() + " è sulla sua proprietà.");
                                    state.nextTurn();
                                    sendTurnNotification();
                                }
                            } else {
                                // Arsa olmayan bir yer (Şimdilik boş geçiyoruz)
                                broadcastUpdate(newPos, player.getName() + " tira " + dice);
                                state.nextTurn();
                                sendTurnNotification();
                            }
                        }
                        break;

                    case BUY_PROPERTY:
                        Tile currentTile = state.getBoard().getTile(player.getPosition());
                        if (currentTile instanceof PropertyTile pt && pt.getOwner() == null) {
                            pt.setOwner(player);
                            player.removeMoney(pt.getPrice());
                            broadcastUpdate(player.getPosition(), player.getName() + " ha comprato " + pt.getName());
                        }
                        state.nextTurn();
                        sendTurnNotification();
                        break;

                    case SKIP_PROPERTY:
                        state.nextTurn();
                        sendTurnNotification();
                        break;
                }
            }
        } catch (Exception e) {
            System.out.println("Connessione persa: " + (player != null ? player.getName() : "Unknown"));
        } finally {
            closeConnection();
        }
    }

    private void sendMessage(Message msg) {
        try {
            out.writeObject(msg);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void broadcast(Message msg) {
        synchronized (clients) {
            for (ClientHandler client : clients) {
                client.sendMessage(msg);
            }
        }
    }

    private void sendTurnNotification() {
        synchronized (clients) {
            for (ClientHandler client : clients) {
                if (client.player == state.getCurrentPlayer()) {
                    client.sendMessage(new Message(MessageType.YOUR_TURN, "È il tuo turno!"));
                }
            }
        }
    }

    private void closeConnection() {
        synchronized (clients) {
            clients.remove(this);
        }
        try { if (socket != null) socket.close(); } catch (IOException e) { e.printStackTrace(); }
    }

    private void broadcastUpdate(int pos, String logMessage) {
        int pIdx = state.getPlayers().indexOf(this.player);
        int money = this.player.getMoney();

        Tile currentTile = state.getBoard().getTile(pos);
        int ownerIdx = -1;
        if (currentTile instanceof PropertyTile pt && pt.getOwner() != null) {
            ownerIdx = state.getPlayers().indexOf(pt.getOwner());
        }

        String payload = pIdx + ":" + pos + ":" + money + ":" + ownerIdx + ":" + logMessage;
        broadcast(new Message(MessageType.GAME_STATE_UPDATE, payload));
    }
}