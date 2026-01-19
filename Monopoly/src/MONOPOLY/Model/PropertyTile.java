package MONOPOLY.Model;

public class PropertyTile extends Tile {

    private int price;
    private Player owner;

    public PropertyTile(String name, int price) {
        super(name);
        this.price = price;
        this.owner = null;
    }

    @Override
    public String onLand(Player player) {
        if (owner == null) {
            if (player.getMoney() >= price) {
                owner = player;
                player.removeMoney(price);
                return player.getName() + " compra " + name;
            }
            return player.getName() + " non ha abbastanza soldi";
        }

        if (owner != player) {
            player.removeMoney(100);
            owner.addMoney(100);
            return player.getName() + " paga affitto a " + owner.getName();
        }

        return player.getName() + " è sulla sua proprietà";
    }

    public Player getOwner() {
        return owner;
    }
}
