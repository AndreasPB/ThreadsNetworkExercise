public class ServerMain {

    /**
     * Man kommer på serveren ved at telnette til localhost:8818
     */

    public static void main(String[] args) {
        int port = 8818;
        Server server = new Server(port);
        server.start();
    }
}
