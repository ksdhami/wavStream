/*
 * http://www.tutorialspoint.com/javaexamples/net_multisoc.htm
 */

import java.util.*;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class Server implements Runnable {

    // COMMANDS
    private static final String PLAY = "play";
    private static final String STOP = "stop";
    private static final String LIBRARY = "library";
    private static final String ADD = "add";
    private static final String DELETE = "delete";
    private static final String CREATE = "create";
    private static final String REMOVE = "remove";
    private static final String LOGOUT = "logout";

    Socket clientSocket;

    private static List<User> users; // A List of the users connecting to the Server
    private User currentUser; // the current User
    boolean isAdmin;

    private static final String INVALID_COMMAND = "invalid command";

    Server(Socket csocket) {
        this.clientSocket = csocket;
        isAdmin = false; // false by default
    }

    public static void main(String args[]) throws Exception {

        if (args.length != 1) {
            System.out.println("Usage: Server <Server Port>");
            System.exit(1);
        }

        ServerSocket serverSocket = new ServerSocket(Integer.parseInt(args[0]));
        System.out.println("Listening");

        loadUsers();

        while (true) {
            Socket sock = serverSocket.accept();
            System.out.println("Connected to client at Port #" + sock.getPort());
            new Thread(new Server(sock)).start();
        }
    }

    /**
     * Run thread on server for client
     */
    public void run() {

        try {
            PrintWriter outBuffer = new PrintWriter(clientSocket.getOutputStream(), true);
            BufferedReader inBuffer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String line = "";
            StreamAudio streamer = null;

            authenticateUser(inBuffer, outBuffer);

            while (true) {

                line = inBuffer.readLine();
                if (line != null) {
                    System.out.println("Client @ Port#" + clientSocket.getPort() + ": " + line);
                    String[] splitCmd = line.split(" ", 3);

                    if (splitCmd[0].equals(PLAY)) {
                        if (splitCmd.length != 2 || splitCmd[1].length() < 1) {
                            outBuffer.println(INVALID_COMMAND);
                        } else {
                            String fileName = splitCmd[1];

                            if (checkFileExists(fileName)) {
                                System.out.println("song available");
                                outBuffer.println("song available");
                                streamer = new StreamAudio(fileName, clientSocket);
                                streamer.start();
                            } else {
                                outBuffer.println("song unavailable");
                                System.out.println("song unavailable");
                            }
                        }

                    } else if (line.equals(STOP)) {
                        streamer.emptyBuffer();
                    } else if (line.equals(LIBRARY)) {
                        listSongs(outBuffer);
                    } else if (splitCmd[0].equals(CREATE)) {
                        if (splitCmd.length == 3) {
                            createUser(outBuffer, splitCmd[1], splitCmd[2]);
                        } else {
                            outBuffer.println(INVALID_COMMAND); // send error back to client
                        }
                    } else if (splitCmd[0].equals(REMOVE)) {
                        if (splitCmd.length == 2) {
                            removeUser(outBuffer, splitCmd[1]);
                        } else {
                            outBuffer.println(INVALID_COMMAND); // send error back to client
                        }
                        removeUser(outBuffer, splitCmd[1]);
                    } else if (splitCmd[0].equals(ADD)) {
                        if (splitCmd.length != 2 || splitCmd[1].length() < 1) {
                            outBuffer.println(INVALID_COMMAND);
                        } else if (isAdmin) {
                            outBuffer.println("authenticated");
                            String songName = splitCmd[1];
                            saveFile(songName);
                        } else {
                            outBuffer.println("unauthorized");
                        }
                    } else if (splitCmd[0].equals(DELETE)) {

                        if (splitCmd.length != 2 || splitCmd[1].length() < 1) {
                            outBuffer.println(INVALID_COMMAND);
                        } else if (isAdmin) {
                            String songName = splitCmd[1];
                            if (checkFileExists(songName)) {
                                deleteFile(songName);
                                outBuffer.println("authenticated");
                            }
                        } else {
                            outBuffer.println("unauthorized");
                        }
                    } else if (line.equals(LOGOUT)) {
                        saveUsers();
                        break;
                    }
                    updateCurrentUser();
                    saveUsers();
                    System.out.println("updating user");
                }
            } // end of while loop

            clientSocket.close();
            System.out.println("Client @ Port#" + clientSocket.getPort() + ": Disconnected");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    } // end method run

    /**
     * Server authenticates user
     * @param inBuffer
     * @param outBuffer
     */
    public void authenticateUser(BufferedReader inBuffer, PrintWriter outBuffer) throws IOException {

        boolean authenticated = false;

        while (!authenticated) {
            String userPass = "";

            while (userPass.equals("")) {
                userPass = inBuffer.readLine(); 
            }

            String[] splitAuth = userPass.split(" ", 2);

            if ((splitAuth.length != 2) || splitAuth[0].length() < 1 || splitAuth[1].length() < 1) {
                outBuffer.println("invalid entry");
            } else {
                String username = splitAuth[0];
                String password = splitAuth[1];

                for (User user : users) {
                    if (user.checkUserName(username) && user.checkPassword(password)) {
                        authenticated = true;
                        outBuffer.println("authenticated");
                        currentUser = user;
                        isAdmin = user.isAdmin();
                        System.out.println(username + " authenticated");
                        break;
                    }
                }
                if (!authenticated) {
                    outBuffer.println("invalid entry");
                }
            }
        }

    }// end method authenticateUser

    /**
     * Save song file to server
     * @param socket
     * @throws Exception
     */
    public void saveFile(String fileName) throws Exception {
        String fileNameNew = fileName;

        byte[] mybytearray = new byte[1024];
        InputStream is = clientSocket.getInputStream();
        FileOutputStream fos = new FileOutputStream(fileNameNew);
        BufferedOutputStream bos = new BufferedOutputStream(fos);
        int bytesRead;

        while ((bytesRead = is.read(mybytearray, 0, mybytearray.length)) != -1) {
            bos.write(mybytearray, 0, bytesRead);

            if (bytesRead != 1024)
                break;
        }
        bos.close();
    }

    /**
     * Remove song from server
     * @param fileName
     * @throws IOException
     */
    public void deleteFile(String fileName) throws IOException {
        String filePath = System.getProperty("user.dir");
        Path path = Paths.get(filePath, fileName);
        System.out.println(path.toString());
        Files.delete(path);
    }

    /**
     * Check if file exists to play or remove
     * @param fileName
     * @return
     * @throws IOException
     */
    public boolean checkFileExists(String fileName) throws IOException {
        String filePath = System.getProperty("user.dir");
        File folder = new File(filePath);
        File[] listOfFiles = folder.listFiles();
        // Send each file name in server directory
        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile() && listOfFiles[i].getName().equals(fileName) && fileName.contains(".wav")) {
                return true;
            }
        }

        return false;
    }

    /**
     * Lists all songs in the Server
     * @param clientChannel
     * @param buff
     * @throws IOException
     */
    public void listSongs(PrintWriter outBuffer) throws IOException {
        String filePath = System.getProperty("user.dir");
        String outputString = "";
        File folder = new File(filePath);
        File[] listOfFiles = folder.listFiles();

        // Send each file name in server directory
        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile() && listOfFiles[i].getName().contains(".wav")) {
                outputString = listOfFiles[i].getName();
                outBuffer.println(outputString);
            }
        }
        String eof = "eof";
        outBuffer.println(eof);
    }

    /**
     * Load users 
     * @throws IOException
     */
    public static void loadUsers() throws IOException {
        users = new ArrayList<User>(); 
        String filePath = System.getProperty("user.dir");
        File folder = new File(filePath);
        File[] listOfFiles = folder.listFiles();

        // Send each file name in server directory
        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile() && listOfFiles[i].getName().matches("^USER-.+")) {
                User tempUser = new User(listOfFiles[i].getName().substring(5));
                if (tempUser.loadAccountData()) { 
                    users.add(tempUser);
                }
            }
        }

        // Add default admin and user
        for (User user : users) {
            if (user.checkUserName("admin")) {
                return;
            }
        }

        User admin = new User("admin", "pass", "admin");
        User newUser = new User("user", "pass", "user");
        users.add(admin);
        users.add(newUser);
    }

    /**
     * Users are saved to file
     * @throws IOException
     */
    public static void saveUsers() throws IOException {
        for (User user : users) {
            user.saveAccountData();
        }
    }

    /**
     * Admin updates user if it exists
     * @throws IOException
     */
    public void updateCurrentUser() throws IOException {
        int j = 0;
        for (int i = 0; i < users.size(); i++) {
            System.out.println(users.get(i).getUserName());
            if (users.get(i).checkUserName(currentUser.getUserName())) {
                j = i;
            }
        }
        users.remove(j);
        users.add(currentUser);
    }

    /**
     * Admin creates new user
     * @param outBuffer
     * @param name
     * @param pass
     * @throws IOException
     */
    public void createUser(PrintWriter outBuffer, String name, String pass) throws IOException {
        if (!isAdmin) {
            outBuffer.println("must be admin");
            return;
        }

        for (User user : users) {
            if (user.checkUserName(name)) {
                outBuffer.println("account already exists");
                return;
            }
        }
        User newUser = new User(name, pass, "user");
        users.add(newUser);
        outBuffer.println("user created");
    }

    /**
     * Admin remove user
     * @param outBuffer
     * @param name
     * @throws IOException
     */
    public void removeUser(PrintWriter outBuffer, String name) throws IOException {
        if (!isAdmin) {
            outBuffer.println("must be admin");
            return;
        }

        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).checkUserName(name)) {
                users.remove(i);
                deleteFile("USER-" + name);
                System.out.println("user removed");
                outBuffer.println("user removed");
                return;
            }
        }
        outBuffer.println("account doesn't exist");
    }

} // End of Server class


/**
 * Class for threading the server-side audio stream
 */
class StreamAudio extends Thread {

    private Socket clientSocket;
    private String fileName;
    private OutputStream out;

    public StreamAudio(String fileName, Socket clientSocket) {
        this.fileName = fileName;
        this.clientSocket = clientSocket;
    }

    public void run() {
        streamAudio(fileName);
    }

    /**
     * Stream audio to client
     * @param fileName
     */
    public void streamAudio(String fileName) {
        try {
            FileInputStream in = new FileInputStream(fileName);

            out = clientSocket.getOutputStream(); // get the output stream to the client
            byte buffer[] = new byte[4096];
            int count;
            while ((count = in.read(buffer)) != -1) { // write the audio to the client
                out.write(buffer, 0, count);
            }
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    public void emptyBuffer() throws IOException {
        out.flush();
    }
}
