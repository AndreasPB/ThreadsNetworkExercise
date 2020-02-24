import org.apache.commons.lang3.StringUtils;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.List;

public class ServerWorker extends Thread {

    private Socket clientSocket;
    private String login = null;
    private Server server;
    private OutputStream outputStream;

    public ServerWorker(Server server, Socket clientSocket) {
        this.server = server;
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try {
            handleClientSocket();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }


    private void handleClientSocket() throws IOException, InterruptedException {
        InputStream inputStream = clientSocket.getInputStream();
        this.outputStream = clientSocket.getOutputStream();

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        while ( (line = reader.readLine()) != null) {
            String[] tokens = StringUtils.split(line);
            if (tokens != null && tokens.length > 0) {
                String cmd = tokens[0];
                if ("logoff".equals(cmd) || "quit".equalsIgnoreCase(cmd)) {
                    handleLogoff();
                    break;
                } else if ("login".equalsIgnoreCase(cmd)) {
                    handleLogin(outputStream, tokens);
                } else if ("msg".equalsIgnoreCase(cmd)) {
                    handleMessage(tokens);
                }
                
                else {
                    String msg = "Ukendt: " + cmd + "\n\r";
                    outputStream.write(msg.getBytes());
                }
                //String msg = "You typed: " + line + "\n";
                //outputStream.write(msg.getBytes());
            }
        }
        clientSocket.close();
    }

    // format: "msg" "login" msg
    private void handleMessage(String[] tokens) throws IOException {
        String sentTo = tokens[1];
        String body = tokens[2];

        List<ServerWorker> workerList = server.getWorkerList();
        for (ServerWorker worker : workerList) {
            if (sentTo.equalsIgnoreCase(worker.getLogin())) {
                String outMsg = "msg " + login + " " + body + "\n\r";
                worker.send(outMsg);
            }

        }
    }

    private void handleLogoff() throws IOException {
        server.removeWorker(this);
        List<ServerWorker> workerList = server.getWorkerList();

        // send other online user current user's status
        String onlineMsg = login + " gik offline \n\r";
        for (ServerWorker worker : workerList) {
            if (!login.equals(worker.getLogin())) {
                worker.send(onlineMsg);
            }
        }
        clientSocket.close();
    }

    public String getLogin() {
        return login;
    }

    private void handleLogin(OutputStream outputStream, String[] tokens) throws IOException {
        if (tokens.length == 3) {

            String login = tokens[1];
            String password = tokens[2];

            if ((login.equals("guest") && password.equals("guest")) ||
                    (login.equals("jens") && password.equals("1234")) ||
                    ((login.equals("ole") && password.equals("1234")) )) {

                String msg = "Godkendt login!\n\r";
                outputStream.write(msg.getBytes());
                this.login = login;
                System.out.println("User logged in successfully " + login);

                List<ServerWorker> workerList = server.getWorkerList();

                // send current user all other online logins
                send("Brugere online: \n\r");
                for (ServerWorker worker : workerList) {
                    if (worker.getLogin() != null) {
                        if (!login.equals(worker.getLogin())) {
                            String msg2 = worker.getLogin() + "\n\r";
                            send(msg2);
                        }
                    }
                }

                // send other online user current user's status
                String onlineMsg = login + "\n\r";
                for (ServerWorker worker : workerList) {
                    if (!login.equals(worker.getLogin())) {
                        worker.send(onlineMsg);
                    }
                }
            } else {
                String msg = "Fejl login\n\r";
                outputStream.write((msg.getBytes()));
            }
        }
    }
    private void send(String msg) throws IOException {
        if (login != null) {
            outputStream.write(msg.getBytes());
        }
    }
}