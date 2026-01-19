package MONOPOLY.Model;

import java.io.Serializable;

public class Player implements Serializable {

    private String name;
    private int money;
    private int position;

    public Player(String name) {
        this.name = name;
        this.money = 1500;
        this.position = 0;
    }

    public String getName() {
        return name;
    }

    public int getMoney() {
        return money;
    }

    public void addMoney(int amount) {
        money += amount;
    }

    public void removeMoney(int amount) {
        money -= amount;
    }

    public int getPosition() {
        return position;
    }

    public void move(int steps, int boardSize) {
        position = (position + steps) % boardSize;
    }
}
