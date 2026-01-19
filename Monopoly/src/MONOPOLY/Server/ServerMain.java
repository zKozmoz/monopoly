package MONOPOLY.Server;

import java.net.ServerSocket;
import java.net.Socket;

public class ServerMain {
    public static void main(String[] args) throws Exception {
        ServerSocket serverSocket = new ServerSocket(12345);
        GameState state = new GameState();
        GameEngine engine = new GameEngine(state);

        System.out.println("Server 12345 portunda baslatildi...");

        while (true) {
            Socket socket = serverSocket.accept();
            new Thread(new ClientHandler(socket, state, engine)).start();
        }
    }
}