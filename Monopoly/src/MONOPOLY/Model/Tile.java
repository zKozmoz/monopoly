package MONOPOLY.Model;

import java.io.Serializable;

public abstract class Tile implements Serializable {

    protected String name;

    public Tile(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public abstract String onLand(Player player);
}
