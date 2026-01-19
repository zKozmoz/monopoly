package MONOPOLY.Server;

import MONOPOLY.Model.*;

import java.util.ArrayList;
import java.util.List;

public class GameState {

    private int maxPlayers;
    private List<Player> players;
    private Board board;
    private int currentPlayer;

    public GameState() {
        players = new ArrayList<>();
        board = new Board();
        currentPlayer = 0;
    }

    private boolean gameStarted = false;
    private Player owner;

    public synchronized void startGame() { this.gameStarted = true; }
    public boolean isGameStarted() { return gameStarted; }

    public void setOwner(Player p) { this.owner = p; }
    public Player getOwner() { return owner; }

    public synchronized void setMaxPlayers(int n) {
        maxPlayers = n;
    }

    public synchronized boolean addPlayer(Player p) {
        if (players.size() < maxPlayers) {
            players.add(p);
            return true;
        }
        return false;
    }

    public synchronized boolean isReady() {
        return players.size() == maxPlayers;
    }

    public synchronized Player getCurrentPlayer() {
        return players.get(currentPlayer);
    }

    public synchronized void nextTurn() {
        currentPlayer = (currentPlayer + 1) % players.size();
    }

    public Board getBoard() {
        return board;
    }

    public List<Player> getPlayers() {
        return players;
    }
}
