package MONOPOLY.Model;

import java.util.ArrayList;
import java.util.List;

public class Board {

    private List<Tile> tiles;

    public Board() {
        tiles = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            if (i == 0) tiles.add(new PropertyTile("START", 0)); // Başlangıç noktası
            else if (i % 5 == 0) tiles.add(new PropertyTile("Tax Station " + i, 100));
            else tiles.add(new PropertyTile("Property " + i, 50 + (i * 10)));
        }
    }

    public Tile getTile(int position) {
        return tiles.get(position % tiles.size());
    }

    public int size() {
        return tiles.size();
    }
}
