import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

public class Client {

    private static final String AUTHENTICATED = "authenticated";
    private static final String UNAUTHORIZED = "unauthorized";
    private static final String INVALID_COMMAND = "invalid command";
    private static final String ADMIN_ONLY = "must be admin";

    // COMMANDS
    private static final String PLAY = "play";
    private static final String STOP = "stop";
    private static final String PAUSE = "pause";
    private static final String RESUME = "resume";
    private static final String LIBRARY = "library";
    private static final String ADD = "add";
    private static final String DELETE = "delete";
    private static final String CREATE = "create";
    private static final String REMOVE = "remove";
    private static final String LOGOUT = "logout";

    static BufferedReader inBuffer;
    static PrintWriter outBuffer;
    static Socket clientSocket;
    static BufferedReader inFromUser;
    static Play player;

    public static void main(String args[]) throws Exception {
        if (args.length != 2) {
            System.out.println("Usage: Client <Server IP> <Server Port>");
            System.exit(1);
        }

        // Initialize a client socket connection to the server
        clientSocket = new Socket(args[0], Integer.parseInt(args[1]));

        // Initialize input and an output stream for the connection(s)
        outBuffer = new PrintWriter(clientSocket.getOutputStream(), true);

        inBuffer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

        // Initialize user input stream
        String line;
        inFromUser = new BufferedReader(new InputStreamReader(System.in));

        // Assign the audio player to the current client
        player = new Play(clientSocket);

        authenticateUser();

        while (true) {
            // Get user input and send to the server
            System.out.print("\nPlease enter a message to be sent to the server ('logout' to terminate): ");
            line = inFromUser.readLine();
            if (player.getThreadOver()) {
                player.interrupt();
            }
            // split command into two parts (at the space)
            String[] splitCmd = line.split(" ", 3);


            if (splitCmd[0].equals(PLAY) && player.getStopped()) {
                playSong(line);
            } else if (line.contains(RESUME) && !player.getPlaying()) {
                System.out.println("Resuming Playback...");
                player.resumeAudio();
            } else if (line.contains(PAUSE) && player.getPlaying()) {
                System.out.println("Paused Playback");
                player.pauseAudio();
            } else if (line.equals(STOP)) {
                stopSong(line);
            } else if (line.equals(LIBRARY)) {
                outBuffer.println(LIBRARY);

                System.out.println("\n");
                System.out.println("\n \uD83C\uDFA7 \uD83D\uDCFB - LIBRARY - \uD83D\uDCFB \uD83C\uDFA7 \n");
                String response;
                while (!(response = inBuffer.readLine()).equals("eof")) {
                    System.out.println(response); // print file names to the user
                }
                System.out.println("\n");

            } else if (splitCmd[0].equals(ADD) && player.getStopped()) {

                outBuffer.println(line); // send to Server
                String response = inBuffer.readLine();

                if (response.equals(INVALID_COMMAND)) {
                    System.out.println("invalid command: add_song <song name>");
                } else if (response.equals(AUTHENTICATED)) {

                    String songName = splitCmd[1];

                    if (checkFileExists(songName)) // check that the song exists on the user side
                    {
                        sendAudioFile(songName); //
                    } else {
                        System.out.println("no such song exists in your current directory");
                    }
                } else if (response.equals(UNAUTHORIZED)) {
                    System.out.println("You do not have permission to perform this action");
                }

            } else if (splitCmd[0].equals(DELETE) && player.getStopped()) {

                outBuffer.println(line); // send to Server
                String response = inBuffer.readLine();

                if (response.equals(INVALID_COMMAND)) {
                    System.out.println("invalid command: remove_song <song name>");
                } else if (response.equals(AUTHENTICATED)) {
                    String songName = splitCmd[1];
                    System.out.println("The song '" + songName + "' was deleted from the server");
                } else if (response.equals(UNAUTHORIZED)) {
                    System.out.println("You do not have permission to perform this action");
                }
            } else if (splitCmd[0].equals(CREATE) && player.getStopped()) {

                outBuffer.println(line); // send to Server
                String response = inBuffer.readLine();

                if (response.equals(INVALID_COMMAND)) {
                    System.out.println("invalid command: create_user <username> <password>");
                } else if (response.equals("account already exists")) {
                    String account = splitCmd[1];
                    System.out.println("'" + account + "' already exists");
                } else if (response.equals(ADMIN_ONLY)) {
                    System.out.println(response);
                } else if (response.equals("user created")) {
                    String account = splitCmd[1];
                    System.out.println("'" + account + "' was created");
                }
            } else if (splitCmd[0].equals(REMOVE) && player.getStopped()) {

                outBuffer.println(line); // send to Server
                String response = inBuffer.readLine();

                if (response.equals(INVALID_COMMAND)) {
                    System.out.println("invalid command: remove_user <username>");
                } else if (response.equals("account doesn't exist")) {
                    String account = splitCmd[1];
                    System.out.println("'" + account + "' already exists");
                } else if (response.equals(ADMIN_ONLY)) {
                    System.out.println(response);
                } else if (response.equals("user removed")) {
                    String account = splitCmd[1];
                    System.out.println("'" + account + "' was removed");
                }
            } else if (line.contains(LOGOUT)) {
                outBuffer.println(LOGOUT);
                break;
            } else {
                System.out.println("unavailable command");
            }
        }

        clientSocket.close();
    }// end method main


    /**
     * Play chosen song given by line
     * @param line
     * @throws IOException
     */
    public static void playSong(String line) throws IOException {

        outBuffer.println(line);
        String response = inBuffer.readLine();

        if (response.equals("song available")) {
            // Activate a thread to play the song
            player = new Play(clientSocket);
            player.start();
        } else if (response.equals("song unavailable")) {
            System.out.println("That song does not exist!");
        } else if (response.equals(INVALID_COMMAND)) {
            System.out.println("Invalid command: play <song name>");
        }
    }

    /**
     * Stop current song
     * @param line
     */
    public static void stopSong(String line) {
        if (!player.getStopped()) {
            player.stopAudio();// stop audio playback
            outBuffer.println(line); // send the stop command to the server
        }
    }


    /**
     * Authenticate users' username and password
     * @throws IOException
     */
    public static void authenticateUser() throws IOException {

        boolean authenticated = false;
        String response = "";

        while (!authenticated) {
            System.out.print("Please enter a username: "); // prompt for username
            String username = inFromUser.readLine();

            System.out.print("Please enter a password: "); // prompt for password
            String password = inFromUser.readLine();

            outBuffer.println(username + " " + password); // send the user info to the server to be authenticated
            response = "";

            while (response.equals("")) {
                response = inBuffer.readLine();

                if (response.equals(AUTHENTICATED)) {
                    authenticated = true;
                }
            }

            if (!authenticated) {
                System.out.println("Invalid username or password");
            }
        }
    }


    /**
     * Check if song exists on client's side
     * @param fileName
     * @return
     * @throws IOException
     */
    public static boolean checkFileExists(String fileName) {
        String filePath = System.getProperty("user.dir");
        File folder = new File(filePath);
        File[] listOfFiles = folder.listFiles();

        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile() && listOfFiles[i].getName().equals(fileName) && fileName.contains(".wav")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Send audio file to server
     * @param songName
     * @throws IOException
     */
    public static void sendAudioFile(String songName) throws IOException {
        File myFile = new File(songName);
        byte[] byteArray = new byte[(int) myFile.length()];
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(myFile));
        bis.read(byteArray, 0, byteArray.length);
        OutputStream os = clientSocket.getOutputStream();
        os.write(byteArray, 0, byteArray.length);
        os.flush();
    }
} // end class client


/**
 * This class handles both the streaming and the playing of the audio file
 */
class Play extends Thread {

    private Socket clientSocket;
    private SourceDataLine sdl;
    private volatile boolean playing;
    private volatile boolean stopped;
    private AudioInputStream din;
    private Boolean threadOver;

    public Play(Socket sock) {
        this.clientSocket = sock;
        playing = false;
        stopped = true;
        sdl = null;
        threadOver = false;
    }

    public void pauseAudio() {
        if (playing)
            playing = false;
    }

    public void resumeAudio() {
        if (!playing)
            playing = true;
    }

    public void stopAudio() {
        sdl.close();
        stopped = true;
    }

    public boolean getStopped() {
        return stopped;
    }

    public boolean getPlaying() {
        return playing;
    }

    public boolean getThreadOver() {
        return threadOver;
    }

    /**
     * Control streaming and playback
     * https://docs.oracle.com/javase/tutorial/sound/sampled-overview.html
     */
    public void run() {
        playing = true;
        stopped = false;
        din = null;

        try {
            // Read the audio from the server
            InputStream inFromServer = new BufferedInputStream(clientSocket.getInputStream());

            AudioInputStream ais = AudioSystem.getAudioInputStream(inFromServer);

            AudioFormat baseFormat = ais.getFormat();
            AudioFormat decodedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, baseFormat.getSampleRate(), 16,
                    baseFormat.getChannels(), baseFormat.getChannels() * 2, baseFormat.getSampleRate(), false);
            din = AudioSystem.getAudioInputStream(decodedFormat, ais);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, decodedFormat);
            sdl = (SourceDataLine) AudioSystem.getLine(info);

            if (sdl != null) {
                sdl.open(decodedFormat);
                byte[] data = new byte[4096];

                sdl.start();

                int nBytesRead;
                while (((nBytesRead = din.read(data, 0, data.length)) != -1)) {

                    while (!playing) {
                        try {
                            Thread.sleep(1000);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    sdl.write(data, 0, nBytesRead);
                }
                sdl.drain();
                sdl.stop();
                sdl.close();
                stopped = true;
                din.close();
            }

        } catch (IOException | LineUnavailableException | UnsupportedAudioFileException e) {
            System.out.println(e);
        }
        threadOver = true;
    } // end method run
} // end classPlayWAV