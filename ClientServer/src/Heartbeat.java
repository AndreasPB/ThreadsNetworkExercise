import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.TimerTask;

/**
 * This class is used by the client to send out a heartbeat, which tells the server that the client is active
 * and should not be disconnected. It extends the TimerTask class, meaning that it can be executed as a task by the
 * Timer class to execute at fixed intervals. To accomplish this, it takes the clients socket as a parameter and
 * uses an object of type 'PrintWriter' to output via the sockets outputStream.
 * @author Christian Grye Skydt
 */

class Heartbeat extends TimerTask
{
    private Socket socket;

    Heartbeat(Socket socket)
    {
        this.socket = socket;
    }

    @Override
    public void run()
    {
        PrintWriter messageToServer;

        try
        {
            messageToServer = new PrintWriter(socket.getOutputStream(), true); // will flush buffer each call.
            messageToServer.println("IMAV");                                            // Protocol message to indicate activity to server.
        }
        catch (IOException ioe)
        {
            ioe.printStackTrace();
        }
    }
}