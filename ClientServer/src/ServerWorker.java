import org.apache.commons.lang3.StringUtils;
import java.io.*;
import java.net.Socket;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;

public class ServerWorker extends Thread {

    private Socket clientSocket;
    private String login = null;
    private Server server;
    private OutputStream outputStream;
    private HashSet<String> topicSet = new HashSet<>();

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

    /**
     * Tager imod inputs fra CMD og udnytter protokoller
     * @throws IOException
     * @throws InterruptedException
     */

    private void handleClientSocket() throws IOException, InterruptedException {
        InputStream inputStream = clientSocket.getInputStream();
        this.outputStream = clientSocket.getOutputStream();

        outputStream.write(("\n\rVelkommen til chatten\n\rCommands: login, msg, list, join, leave, quit\n\n\r").getBytes());

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;

        while ( (line = reader.readLine()) != null) {
            String[] tokens = StringUtils.split(line);
            if (tokens != null && tokens.length > 0) {
                String cmd = tokens[0];
                if ("logoff".equals(cmd) || "logout".equals(cmd) || "quit".equalsIgnoreCase(cmd)) {
                    handleLogoff();
                    break;
                } else if ("login".equalsIgnoreCase(cmd)) {
                    handleLogin(outputStream, tokens);
                } else if ("data".equalsIgnoreCase(cmd) || ("msg".equalsIgnoreCase(cmd))) {
                    String[] tokensMsg = StringUtils.split(line, null, 3);
                    handleMessage(tokensMsg);
                } else if ("join".equalsIgnoreCase(cmd)) {
                    handleJoin(tokens);
                } else if ("leave".equalsIgnoreCase(cmd)) {
                    handleLeave(tokens);
                } else if ("list".equalsIgnoreCase(cmd)) {
                    handleList(login);
                } else {
                    String msg = "Ukendt: " + cmd + "\n\r";
                    outputStream.write(msg.getBytes());
                }
            }
        }
        clientSocket.close();
    }

    private void handleLeave(String[] tokens) {
        if (tokens.length > 1) {
            String topic = tokens[1];
            topicSet.remove(topic);
        }
    }

    public boolean isMemberOfTopic(String topic) {
        return topicSet.contains(topic);
    }

    private void handleJoin(String[] tokens) {
        if (tokens.length > 1) {
            String topic = tokens[1];
            topicSet.add(topic);
        }
    }

    // format: "msg/data" "login" tekst...
    // format: "msg/data" "#topic" tekst...
    private void handleMessage(String[] tokens) throws IOException, ArrayIndexOutOfBoundsException {
        String sendTo = tokens[1];
        String body = tokens[2];

        boolean isTopic = sendTo.charAt(0) == '#';

        List<ServerWorker> workerList = server.getWorkerList();
        for (ServerWorker worker : workerList) {
            if (isTopic) {
                if (worker.isMemberOfTopic(sendTo)) {
                    String outMsg = "DATA " + sendTo + ":" + login + ": " + body + "\n\r";
                    worker.send(outMsg);
                }
            } else {
                if (sendTo.equalsIgnoreCase(worker.getLogin())) {
                    String outMsg = "DATA " + login + ": " + body + "\n\r";
                    worker.send(outMsg);
                }
            }
        }
    }

    private void handleLogoff() throws IOException {
        server.removeWorker(this);
        Vector<ServerWorker> workerList = server.getWorkerList();

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

    /**
     *
     * @param outputStream
     * @param tokens
     * @throws IOException
     */

    private void handleLogin(OutputStream outputStream, String[] tokens) throws IOException {
        if (tokens.length == 3) {

            String login = tokens[1];
            String password = tokens[2];

            if ((login.equals("guest") && password.equals("guest")) ||
                    (login.equals("jens") && password.equals("1234")) ||
                    ((login.equals("ole") && password.equals("1234")) )) {

                if (login.equals("guest")) {
                    this.login = handleGuests(login);
                } else {
                    this.login = login;
                }

                String msg = "J_OK!\n\r";
                outputStream.write(msg.getBytes());
                System.out.println("User logged in successfully " + login);

                Vector<ServerWorker> workerList = server.getWorkerList();

                // send current user all other online logins
                handleList(login);

                // send other online user current user's status
                String onlineMsg = login + " er online!\n\r";
                for (ServerWorker worker : workerList) {
                    if (!login.equals(worker.getLogin())) {
                        worker.send(onlineMsg);
                    }
                }
            } else {
                String msg = "J_ER - Forkert login!\n\r";
                outputStream.write((msg.getBytes()));
            }
        }
    }

    /**
     * Sørger for at alle gæster har et unikt login
     * @param login
     * @return
     */

    private String handleGuests(String login) {
        Vector<ServerWorker> workerList = server.getWorkerList();

        System.out.println(workerList);

        int count = 0;
        boolean guestFound = false;
        try {
            for (ServerWorker worker : workerList) {
                if (worker.getLogin().startsWith("guest")) {
                    count++;
                    guestFound = true;
                }
            }
        } catch (NullPointerException e) {
            System.out.println(e);
        }
        if (guestFound) {
            String newLogin = login + count;
            return newLogin;
        } else {
            return login;
        }
    }

    private void handleList(String login) throws IOException {
        Vector<ServerWorker> workerList = server.getWorkerList();
        send("Brugere online: \n\r");
        for (ServerWorker worker : workerList) {
            if (worker.getLogin() != null) {
                if (!login.equals(worker.getLogin())) {
                    String msg2 = worker.getLogin() + "\n\r";
                    send(msg2);
                }
            }
        }
    }

    private void send(String msg) throws IOException {
        if (login != null) {
            outputStream.write(msg.getBytes());
        }
    }
}