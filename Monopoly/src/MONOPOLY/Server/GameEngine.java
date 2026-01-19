package MONOPOLY.Server;

import MONOPOLY.Model.*;
import java.util.Random;

public class GameEngine {
    private GameState state;
    private Random random = new Random();

    public GameEngine(GameState state) {
        this.state = state;
    }

    public synchronized String rollDice() {
        Player p = state.getCurrentPlayer();
        int dice = random.nextInt(6) + 1;

        p.move(dice, state.getBoard().size());
        Tile tile = state.getBoard().getTile(p.getPosition());

        String moveResult = tile.onLand(p);
        return "ha ottenuto " + dice + "! -> " + moveResult;
    }

    public synchronized void endTurn() {
        state.nextTurn();
    }
}