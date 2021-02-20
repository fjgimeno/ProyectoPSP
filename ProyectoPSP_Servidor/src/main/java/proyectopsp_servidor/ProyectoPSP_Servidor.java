package proyectopsp_servidor;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.*;

public class ProyectoPSP_Servidor {

    public static final String DBROUTE = "./data/database/database.json";
    public static final String SEPARATOR = System.getProperty("file.separator");
    public static String LOGPATH;
    public static File LOGFILE;
    public static FileWriter LOGFW;
    public static BufferedWriter LOGBW;

    public static void createUserFolder(String username) throws IOException {
        Process p;
        try {
            String classPath = new File("").getAbsolutePath();
            if (SEPARATOR.equals("\\")) {
                p = Runtime.getRuntime().exec("cmd /c mkdir " + classPath + SEPARATOR + "data" + SEPARATOR + "USERS" + SEPARATOR + username);
            } else {
                p = Runtime.getRuntime().exec("mkdir " + classPath + SEPARATOR + "data" + SEPARATOR + "USERS" + SEPARATOR + username);
            }
            p.waitFor();
            p.destroy();
            p = null;
        } catch (IOException | InterruptedException ex) {
            LOGBW.newLine();
            LOGBW.write("ERROR!" + ex);
        }
    }

    public static void deleteUserFolder(String username) throws IOException {
        Process p;
        try {
            String classPath = new File("").getAbsolutePath();
            if (SEPARATOR.equals("\\")) {
                p = Runtime.getRuntime().exec("cmd /c rmdir /Q /S " + classPath + SEPARATOR + "data" + SEPARATOR + "USERS" + SEPARATOR + username);
            } else {
                p = Runtime.getRuntime().exec("rm -r " + classPath + SEPARATOR + "data" + SEPARATOR + "USERS" + SEPARATOR + username);
            }
            p.waitFor();
            p.destroy();
            p = null;
        } catch (IOException | InterruptedException ex) {
            LOGBW.newLine();
            LOGBW.write("ERROR!" + ex);
        }
    }

    private static int addUser(String username, String password) throws IOException {
        JSONArray currentUsers = getUsers();
        //Checks if username already exists
        for (Object user : currentUsers) {
            JSONObject us = (JSONObject) user;
            if (us.get("username").equals(username)) {
                return 1;   //1 equals ERR_DUPLICATED
            }
        }

        //Checks if using a weak password
        if (password.length() < 8) {
            return 2;   //2 equals ERR_PASSWORD
        }

        //Inserts a new user>
        JSONObject obj = new JSONObject();
        obj.put("id", String.valueOf(currentUsers.length()));
        obj.put("username", username);
        obj.put("password", password);

        currentUsers.put(obj);

        JSONArray users = new JSONArray();
        for (int i = 0; i < currentUsers.length(); i++) {
            users.put(currentUsers.get(i));
        }
        JSONObject root = new JSONObject();
        root.put("user", users);

        try {
            FileWriter file = new FileWriter(DBROUTE);
            file.write(root.toString(4)); // 4 són els espais d'indentació
            file.close();
            LOGBW.newLine();
            LOGBW.write("User inserted successfully!");
            return 0;   //0 equals OK
        } catch (IOException e) {
            LOGBW.newLine();
            LOGBW.write("ERROR! File cannot be created" + e);
            return 3;
        }
    }

    private static int delUser(String username) throws IOException {
        JSONArray currentUsers = getUsers();
        JSONArray users = new JSONArray();
        for (int i = 0; i < currentUsers.length(); i++) {
            JSONObject us = (JSONObject) currentUsers.get(i);
            if (!us.get("username").equals(username)) {
                users.put(currentUsers.get(i));
            }
        }
        JSONObject root = new JSONObject();
        root.put("user", users);

        try {
            FileWriter file = new FileWriter(DBROUTE);
            file.write(root.toString(4)); // 4 són els espais d'indentació
            file.close();
            LOGBW.newLine();
            LOGBW.write("User inserted successfully!");
            return 0;   //0 equals OK
        } catch (IOException e) {
            LOGBW.newLine();
            LOGBW.write("ERROR! File cannot be created: " + e);
            return 3;
        }
    }

    private static JSONArray getUsers() throws IOException {
        JSONArray jsa = new JSONArray();
        try {
            File usersdb = new File(DBROUTE);
            JSONTokener tokener;
            tokener = new JSONTokener(usersdb.toURL().openStream());
            JSONObject jso = new JSONObject(tokener);
            jsa = jso.getJSONArray("user");
        } catch (MalformedURLException ex) {
            Logger.getLogger(ProyectoPSP_Servidor.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            LOGBW.newLine();
            LOGBW.write("There was a problem with reading the database file");
        }
        return jsa;
    }

    public static void main(String args[]) throws Exception {
        LOGPATH = new File("").getAbsolutePath() + SEPARATOR + "data" + SEPARATOR + "logs" + SEPARATOR + "log.txt";
        LOGFILE = new File(LOGPATH);
        LOGFW = new FileWriter(LOGFILE, true);
        LOGBW = new BufferedWriter(LOGFW);
        int port = 1469;
        ServerSocket ss = new ServerSocket(port);
        System.out.println("Waiting for client connection...");
        LOGBW.newLine();
        LOGBW.write("Waiting for client connection...");
        Socket s = ss.accept();
        System.out.println("Client successfully connected!");
        LOGBW.newLine();
        LOGBW.write("Client successfully connected!");
        DataInputStream din = new DataInputStream(s.getInputStream());
        DataOutputStream dout = new DataOutputStream(s.getOutputStream());
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String str = "", str2 = "";
        boolean clientLoggedIn = false, logError = false, userExit = false;
        JSONArray users = getUsers();
        String username = "";

        //Iterator<String> keys = jso.keySet();
        //JSONArray users  = (JSONArray) obj;
        //System.out.println(users);
        //Server asks the user (connected socket) to log in to the server
        //By default, there is a user already creater with
        //'username = admin' & 'password = admin'
        while (!clientLoggedIn || !logError) {
            String password;
            boolean foundUser = false, wrongPass = false;

            System.out.println("Sending login request to client...");
            LOGBW.newLine();
            LOGBW.write("Sending login request to client...");
            str2 = "LOGIN\n";
            dout.writeUTF(str2);
            dout.flush();
            username = din.readUTF();
            for (Object user : users) {
                JSONObject us = (JSONObject) user;
                if (us.get("username").equals(username)) {
                    foundUser = true;
                    System.out.println("User " + username + " was found in the system");
                    LOGBW.newLine();
                    LOGBW.write("User " + username + " was found in the system");
                    str2 = "PASS\n";
                    dout.writeUTF(str2);
                    dout.flush();
                    password = din.readUTF();
                    if (us.get("password").equals(password)) {
                        clientLoggedIn = true;
                        System.out.println("User \'" + username + "\' successfully logged in!");
                        LOGBW.newLine();
                        LOGBW.write("User \'" + username + "\' successfully logged in!");
                        str2 = "OK\n?\n";
                        dout.writeUTF(str2);
                        dout.flush();
                    } else {
                        wrongPass = true;
                    }
                    break;
                } else {
                    foundUser = false;
                }
            }
            if (!foundUser || wrongPass) {
                str2 = "ERR_LOGIN\n";
                dout.writeUTF(str2);
                dout.flush();
                System.out.println("There was an error with the log-in, waiting for a new client to connect...");
                LOGBW.newLine();
                LOGBW.write("There was an error with the log-in, waiting for a new client to connect...");
                s = ss.accept();
                System.out.println("\nNew client successfully connected!");
                LOGBW.newLine();
                LOGBW.write("New client successfully connected!");
                din = new DataInputStream(s.getInputStream());
                dout = new DataOutputStream(s.getOutputStream());
            } else {
                clientLoggedIn = true;
                break;
            }
        }

        while (!userExit) {
            String command[] = din.readUTF().split(";");
            switch (command[0]) {
                case "QUIT":
                case "quit":
                    userExit = true;
                    dout.writeUTF("THANKS " + username + " BYE\n");
                    dout.flush();
                    din.close();
                    s.close();
                    ss.close();
                    System.exit(0);

                case "adduser":
                case "ADDUSER":
                    if (command.length != 3) {
                        str2 = "ERR_BADNAME\n?\n";
                        dout.writeUTF(str2);
                        dout.flush();
                    } else {
                        if (username.equals("admin") && command.length == 3) {
                            switch (addUser(command[1], command[2])) {
                                case 0:
                                    createUserFolder(command[1]);
                                    dout.writeUTF("OK\n?\n");
                                    dout.flush();
                                    break;
                                case 1:
                                    dout.writeUTF("ERR_DUPLICATED\n?\n");
                                    dout.flush();
                                    break;
                                case 2:
                                    dout.writeUTF("ERR_PASSWORD\n?\n");
                                    dout.flush();
                                    break;
                                default:
                                    dout.writeUTF("ERR_UNKNOWN\n?\n");
                                    dout.flush();
                                    break;
                            }

                            str2 = "OK\n?\n";
                            dout.writeUTF(str2);
                            dout.flush();
                        } else if (command.length != 3) {
                            dout.writeUTF("ERR_WRONG_ARGS_NUMBER\n?\n");
                            dout.flush();
                        } else {
                            System.out.println("Telling client that he/she does not have permision");
                            LOGBW.newLine();
                            LOGBW.write("Telling client that he/she does not have permision");
                            dout.writeUTF("ERR_PERMISION\n?\n");
                            dout.flush();
                        }
                    }
                    break;

                case "deluser":
                case "DELUSER":
                    if (command.length != 2) {
                        str2 = "ERR_WRONG_ARGS_NUMBER\n?\n";
                        dout.writeUTF(str2);
                        dout.flush();
                    } else {
                        if (username.equals("admin")) {
                            if (!command[1].equals("admin")) {
                                int notok = delUser(command[1]);
                                if (notok == 0) {
                                    deleteUserFolder(command[1]);
                                    dout.writeUTF("OK\n?\n");
                                    dout.flush();
                                } else if (notok == 1) {
                                    dout.writeUTF("ERR_USER_NAME\n?\n");
                                    dout.flush();
                                } else {
                                    dout.writeUTF("ERR_UNKNOWN\n?\n");
                                    dout.flush();
                                }
                            } else {
                                str2 = "ERR_NOTPERMITED\n?\n";
                                dout.writeUTF(str2);
                                dout.flush();
                            }
                        } else {
                            System.out.println("Telling client that he/she does not have permision");
                            LOGBW.newLine();
                            LOGBW.write("Telling client that he/she does not have permision");
                            dout.writeUTF("ERR_PERMISION\n?\n");
                            dout.flush();
                        }
                    }
                    break;

                case "create":
                case "CREATE":
                    if (command.length != 2) {
                        str2 = "ERR_WRONG_ARGS_NUMBER\n?\n";
                        dout.writeUTF(str2);
                        dout.flush();
                    } else {
                        int notok = createFile(username, command[1]);
                        if (notok == 0) {
                            createFile(username, command[1]);
                            dout.writeUTF("OK\n?\n");
                            dout.flush();
                        } else if (notok == 1) {
                            dout.writeUTF("ERR_FILE\n?\n");
                            dout.flush();
                        } else {
                            dout.writeUTF("ERR_UNKNOWN\n?\n");
                            dout.flush();
                        }
                    }
                    break;

                case "delete":
                case "DELETE":
                    if (command.length != 2) {
                        str2 = "ERR_WRONG_ARGS_NUMBER\n?\n";
                        dout.writeUTF(str2);
                        dout.flush();
                    } else {
                        int notok = deleteFile(username, command[1]);
                        if (notok == 0) {
                            dout.writeUTF("OK\n?\n");
                            dout.flush();
                        } else if (notok == 1) {
                            dout.writeUTF("ERR_FILE\n?\n");
                            dout.flush();
                        } else {
                            dout.writeUTF("ERR_UNKNOWN\n?\n");
                            dout.flush();
                        }
                    }
                    break;

                case "append":
                case "APPEND":
                    if (command.length != 3) {
                        str2 = "ERR_WRONG_ARGS_NUMBER\n?\n";
                        dout.writeUTF(str2);
                        dout.flush();
                    } else {
                        int notok = insertFile(username, command[1], command[2]);
                        if (notok == 0) {
                            dout.writeUTF("OK\n?\n");
                            dout.flush();
                        } else if (notok == 1) {
                            dout.writeUTF("ERR_FILE\n?\n");
                            dout.flush();
                        } else {
                            dout.writeUTF("ERR_UNKNOWN\n?\n");
                            dout.flush();
                        }
                    }
                    break;

                case "list":
                case "LIST":
                    if (command.length != 1) {
                        str2 = "ERR_WRONG_ARGS_NUMBER\n?\n";
                        dout.writeUTF(str2);
                        dout.flush();
                    } else {
                        String notok = listFiles(username);
                        if (!notok.equals("NULL")) {
                            dout.writeUTF("\n" + notok);
                            dout.flush();
                        } else {
                            dout.writeUTF("ERR_NO_FILES\n?\n");
                            dout.flush();
                        }
                        dout.writeUTF("OK\n?\n");
                        dout.flush();
                    }
                    break;

                default:
                    str2 = "ERR_COMMAND\n?\n";
                    dout.writeUTF(str2);
                    dout.flush();
                    break;
            }
        }
        LOGFW.close();
        LOGBW.close();
        din.close();
        dout.close();
        s.close();
        ss.close();
    }

    private static int createFile(String username, String filename) {
        String filePath = new File("").getAbsolutePath() + SEPARATOR + "data" + SEPARATOR + "USERS" + SEPARATOR + username + SEPARATOR + filename;
        File fil = new File(filePath);
        if (fil.exists()) {
            return 1;
        } else {
            try {
                fil.createNewFile();
                return 0;
            } catch (IOException ex) {
                return 1;
            }
        }
    }

    private static int insertFile(String username, String filename, String text) {
        String filePath = new File("").getAbsolutePath() + SEPARATOR + "data" + SEPARATOR + "USERS" + SEPARATOR + username + SEPARATOR + filename;
        File fil = new File(filePath);
        if (fil.exists()) {
            try {
                FileWriter fw = new FileWriter(filePath, true);
                BufferedWriter bw = new BufferedWriter(fw);
                bw.newLine();
                bw.write(text);
                bw.close();
                fw.close();
                return 0;
            } catch (IOException e) {
                return 1;
            }
        } else {
            return 1;
        }
    }

    private static int deleteFile(String username, String filename) {
        String filePath = new File("").getAbsolutePath() + SEPARATOR + "data" + SEPARATOR + "USERS" + SEPARATOR + username + SEPARATOR + filename;
        File fil = new File(filePath);
        if (fil.delete()) {
            return 0;
        } else {
            return 1;
        }
    }

    private static String listFiles(String username) {
        String filePath = new File("").getAbsolutePath() + SEPARATOR + "data" + SEPARATOR + "USERS" + SEPARATOR + username;
        File fil = new File(filePath);
        File[] paths;
        try {
            paths = fil.listFiles();
            String out = "";
            for (File path : paths) {
                out = out + path + " " + Files.size(Paths.get(path.toString())) / 1024 + "\n";
            }
            return out;
        } catch (IOException ex) {
            return "NULL";
        }
    }
}
