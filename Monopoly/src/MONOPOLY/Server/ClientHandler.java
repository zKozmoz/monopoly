package MONOPOLY.Server;

import MONOPOLY.Model.*;
import MONOPOLY.Protocol.*;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

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
                        if (state.isGameStarted()) {
                            sendMessage(new Message(MessageType.ERROR, "La partita è già iniziata! Impossibile unirsi."));
                            return;
                        }
                        String pName = (String) msg.getPayload();
                        player = new Player(pName);
                        state.addPlayer(player);

                        if (state.getPlayers().size() == 1) {
                            state.setOwner(player);
                            sendMessage(new Message(MessageType.JOIN_GAME, "OWNER"));
                        } else {
                            sendMessage(new Message(MessageType.JOIN_GAME, "GUEST"));
                        }
                        broadcastUpdate(-1, pName + " si è unito alla partita!");
                        break;

                    case START_GAME:
                        if (player == state.getOwner()) {
                            state.startGame();
                            broadcast(new Message(MessageType.START_GAME, "La partita è iniziata!"));
                            sendTurnNotification();
                        }
                        break;

                    case ROLL_DICE:
                        if (state.isGameStarted() && player == state.getCurrentPlayer()) {
                            processDiceRoll();
                        }
                        break;

                    case BUY_PROPERTY:
                        processPurchase(true);
                        break;

                    case SKIP_PROPERTY:
                        processPurchase(false);
                        break;
                }
            }
        } catch (EOFException | java.net.SocketException e) {
            System.out.println("Giocatore disconnesso: " + (player != null ? player.getName() : "Sconosciuto"));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeConnection();
        }
    }

    private void processDiceRoll() {
        int dice = new java.util.Random().nextInt(6) + 1;
        player.move(dice, state.getBoard().size());
        int newPos = player.getPosition();
        Tile tile = state.getBoard().getTile(newPos);

        if (tile instanceof PropertyTile pt) {
            if (pt.getOwner() == null) {
                broadcastUpdate(newPos, player.getName() + " ha tirato " + dice + ". Può acquistare " + pt.getName());
                sendMessage(new Message(MessageType.PROMPT_PURCHASE, pt.getName() + ":" + pt.getPrice()));
            } else if (pt.getOwner() != player) {
                String rentResult = pt.onLand(player);
                broadcastUpdate(newPos, player.getName() + " ha tirato " + dice + ". Ha pagato l'affitto a " + pt.getOwner().getName());
                state.nextTurn();
                sendTurnNotification();
            } else {
                // Kendi mülkü
                broadcastUpdate(newPos, player.getName() + " ha tirato " + dice + ". È sulla sua proprietà.");
                state.nextTurn();
                sendTurnNotification();
            }
        } else {
            broadcastUpdate(newPos, player.getName() + " ha tirato " + dice);
            state.nextTurn();
            sendTurnNotification();
        }
    }

    private void processPurchase(boolean buying) {
        Tile tile = state.getBoard().getTile(player.getPosition());
        if (buying && tile instanceof PropertyTile pt && pt.getOwner() == null) {
            if (player.getMoney() >= pt.getPrice()) {
                pt.setOwner(player);
                player.removeMoney(pt.getPrice());
                broadcastUpdate(player.getPosition(), player.getName() + " ha acquistato " + pt.getName() + "!");
            } else {
                sendMessage(new Message(MessageType.ERROR, "Fondi insufficienti!"));
            }
        } else {
            broadcastUpdate(player.getPosition(), player.getName() + " ha deciso di non acquistare.");
        }
        state.nextTurn();
        sendTurnNotification();
    }

    private void sendMessage(Message msg) {
        try {
            out.writeObject(msg);
            out.reset();
            out.flush();
        } catch (IOException e) {}
    }

    private void broadcast(Message msg) {
        synchronized (clients) {
            Iterator<ClientHandler> it = clients.iterator();
            while (it.hasNext()) {
                ClientHandler client = it.next();
                try {
                    client.out.writeObject(msg);
                    client.out.reset();
                    client.out.flush();
                } catch (IOException e) {
                    it.remove();
                }
            }
        }
    }

    private void broadcastUpdate(int pos, String logMessage) {
        int pIdx = (player != null) ? state.getPlayers().indexOf(player) : -1;
        int money = (player != null) ? player.getMoney() : 0;

        int ownerIdx = -1;
        if (pos != -1) {
            Tile t = state.getBoard().getTile(pos);
            if (t instanceof PropertyTile pt && pt.getOwner() != null) {
                ownerIdx = state.getPlayers().indexOf(pt.getOwner());
            }
        }
        String payload = pIdx + ":" + pos + ":" + money + ":" + ownerIdx + ":" + logMessage;
        broadcast(new Message(MessageType.GAME_STATE_UPDATE, payload));
    }

    private void sendTurnNotification() {
        synchronized (clients) {
            for (ClientHandler client : clients) {
                if (client.player == state.getCurrentPlayer()) {
                    client.sendMessage(new Message(MessageType.YOUR_TURN, null));
                }
            }
        }
    }

    private void closeConnection() {
        synchronized (clients) { clients.remove(this); }
        try { if (socket != null) socket.close(); } catch (IOException e) {}
    }
}