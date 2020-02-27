import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.TimerTask;

/**
 * Ikke implementeret, men skal holde aktive brugere i live
 */

class Heartbeat extends TimerTask {
    private Socket socket;

    Heartbeat(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        PrintWriter messageToServer;

        try {
            messageToServer = new PrintWriter(socket.getOutputStream(), true); // will flush buffer each call.
            messageToServer.println("IMAV");                                            // Protocol message to indicate activity to server.
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}