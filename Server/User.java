import java.util.*;
import java.io.FileWriter;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;


public class User {
    private String userType;
    private String userName;

    private String password;

    public User(String name) {
        this.userName = name;
    }

    public User(String name, String pass, String type) {
        this.userName = name;
        this.userType = type;
        this.password = pass;
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }

    public boolean checkUserName(String input) {
        return input.matches(userName);
    }

    public boolean checkPassword(String input) {
        return input.matches(password);
    }

    public boolean isAdmin() {
        return (userType.matches("admin"));
    }

    /**
     * Persistent Account Data
     * @throws IOException
     */
    public void saveAccountData() throws IOException {
        try {
            FileWriter fw = new FileWriter("USER-" + userName);
            fw.write("begindata:\n");
            fw.write(password + "\n");
            fw.write(userType + "\n");
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Load user data
     * @return
     * @throws IOException
     */
    public boolean loadAccountData() throws IOException {
        try {
            BufferedReader br = new BufferedReader(new FileReader("USER-" + userName));
            String line = "";
            if (!(line = br.readLine()).matches("begindata:")) {
                System.out.println("User data is corrupted");
                System.out.println(line);
                br.close();
                return false;
            }
            password = br.readLine();
            userType = br.readLine();
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

}