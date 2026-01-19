package MONOPOLY.Server;

import MONOPOLY.Model.*;

import java.util.ArrayList;
import java.util.List;

public class GameState {

    private int maxPlayers = 4;
    private List<Player> players = new ArrayList<>();
    private Board board = new Board();
    private int currentPlayerIndex = 0;
    private boolean gameStarted = false;
    private Player owner;

    public synchronized void setOwner(Player p) { this.owner = p; }
    public Player getOwner() { return owner; }

    public synchronized void startGame() { this.gameStarted = true; }
    public boolean isGameStarted() { return gameStarted; }

    public synchronized void addPlayer(Player p) { players.add(p); }
    public List<Player> getPlayers() { return players; }

    public Board getBoard() { return board; }

    public synchronized Player getCurrentPlayer() {
        return players.get(currentPlayerIndex);
    }

    public synchronized void nextTurn() {
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
    }

    private int currentPlayer;

    public GameState() {
        players = new ArrayList<>();
        board = new Board();
        currentPlayer = 0;
    }

    public synchronized void setMaxPlayers(int n) {
        maxPlayers = n;
    }

    public synchronized boolean isReady() {
        return players.size() == maxPlayers;
    }
}
