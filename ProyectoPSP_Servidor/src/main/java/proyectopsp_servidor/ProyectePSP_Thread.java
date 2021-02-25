package proyectopsp_servidor;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.Socket;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import java.time.format.DateTimeFormatter;  
import java.time.LocalDateTime;   

class ProyectePSP_Thread extends Thread {

    final DataInputStream dis;
    final DataOutputStream dos;
    public static final String SEPARATOR = System.getProperty("file.separator");
    public static final String DBROUTE = "./data/database/database.json";
    final Socket s;
    public String username = "";

    // Constructor 
    public ProyectePSP_Thread(Socket s, DataInputStream dis, DataOutputStream dos) {
        logger("User " + username + " connected successfully!");
        this.s = s;
        this.dis = dis;
        this.dos = dos;
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
            Logger.getLogger(ProyectoPSP_Servidor.class.getName()).log(Level.SEVERE, null, ex);
        }
        return jsa;
    }

    @Override
    public void run() {
        String received;
        String toreturn;
        String[] command;
        String password = "";
        JSONArray users;
        boolean loggedIn = false;

        do {
            try {
                users = getUsers();
                // Ask user what he wants 
                System.out.println("Sending login request to client...");
                toreturn = "LOGIN\n";
                dos.writeUTF(toreturn);
                dos.flush();
                username = dis.readUTF();
                for (Object user : users) {
                    JSONObject us = (JSONObject) user;
                    if (us.get("username").equals(username)) {
                        System.out.println("User " + username + " was found in the system");
                        toreturn = "PASS\n";
                        dos.writeUTF(toreturn);
                        dos.flush();
                        password = dis.readUTF();
                        if (us.get("password").equals(password)) {
                            System.out.println("User \'" + username + "\' successfully logged in!");
                            toreturn = "OK\n?\n";
                            dos.writeUTF(toreturn);
                            dos.flush();
                            loggedIn = true;
                        } else {
                            toreturn = "ERR_LOGIN\n";
                            dos.writeUTF(toreturn);
                            dos.flush();
                            this.s.close();
                            System.out.println("Connection closed");
                            this.stop();
                            break;
                        }
                        if (loggedIn) {
                            break;
                        } else {
                            toreturn = "ERR_LOGIN\n";
                            dos.writeUTF(toreturn);
                            dos.flush();
                            this.s.close();
                            System.out.println("Connection closed");
                            this.stop();
                            break;
                        }
                    }
                }
                break;
            } catch (IOException ex) {
                Logger.getLogger(ProyectePSP_Thread.class.getName()).log(Level.SEVERE, null, ex);
            }
        } while (loggedIn == false);

        while (true && loggedIn) {
            try {
                // receive the answer from client 
                received = dis.readUTF();
                command = received.split(";");

                if ((received.toLowerCase().equals("quit"))) {
                    System.out.println("Client " + this.s + " sends quit...");
                    System.out.println("Closing this connection.");
                    dos.writeUTF("THANKS " + username + " BYE\n");
                    System.out.println("Connection closed");
                    break;
                }

                switch (command[0]) {
                    case "adduser":
                    case "ADDUSER":
                        if (command.length != 3) {
                            toreturn = "ERR_BADNAME\n?\n";
                            dos.writeUTF(toreturn);
                            dos.flush();
                        } else {
                            if (username.equals("admin") && command.length == 3) {
                                switch (addUser(command[1], command[2])) {
                                    case 0:
                                        createUserFolder(command[1]);
                                        users = getUsers();
                                        dos.writeUTF("OK\n?\n");
                                        dos.flush();
                                        break;
                                    case 1:
                                        dos.writeUTF("ERR_DUPLICATED\n?\n");
                                        dos.flush();
                                        break;
                                    case 2:
                                        dos.writeUTF("ERR_PASSWORD\n?\n");
                                        dos.flush();
                                        break;
                                    default:
                                        dos.writeUTF("ERR_UNKNOWN\n?\n");
                                        dos.flush();
                                        break;
                                }

                                toreturn = "OK\n?\n";
                                dos.writeUTF(toreturn);
                                dos.flush();
                            } else if (command.length != 3) {
                                dos.writeUTF("ERR_WRONG_ARGS_NUMBER\n?\n");
                                dos.flush();
                            } else {
                                System.out.println("Telling client that he/she does not have permision");
                                dos.writeUTF("ERR_PERMISION\n?\n");
                                dos.flush();
                            }
                        }
                        break;

                    case "deluser":
                    case "DELUSER":
                        if (command.length != 2) {
                            toreturn = "ERR_WRONG_ARGS_NUMBER\n?\n";
                            dos.writeUTF(toreturn);
                            dos.flush();
                        } else {
                            if (username.equals("admin")) {
                                if (!command[1].equals("admin")) {
                                    int notok = delUser(command[1]);
                                    if (notok == 0) {
                                        deleteUserFolder(command[1]);
                                        dos.writeUTF("OK\n?\n");
                                        dos.flush();
                                    } else if (notok == 1) {
                                        dos.writeUTF("ERR_USER_NAME\n?\n");
                                        dos.flush();
                                    } else {
                                        dos.writeUTF("ERR_UNKNOWN\n?\n");
                                        dos.flush();
                                    }
                                } else {
                                    toreturn = "ERR_NOTPERMITED\n?\n";
                                    dos.writeUTF(toreturn);
                                    dos.flush();
                                }
                            } else {
                                System.out.println("Telling client that he/she does not have permision");
                                dos.writeUTF("ERR_PERMISION\n?\n");
                                dos.flush();
                            }
                        }
                        break;

                    case "create":
                    case "CREATE":
                        if (command.length != 2) {
                            toreturn = "ERR_WRONG_ARGS_NUMBER\n?\n";
                            dos.writeUTF(toreturn);
                            dos.flush();
                        } else {
                            int notok = createFile(username, command[1]);
                            if (notok == 0) {
                                createFile(username, command[1]);
                                dos.writeUTF("OK\n?\n");
                                dos.flush();
                            } else if (notok == 1) {
                                dos.writeUTF("ERR_FILE\n?\n");
                                dos.flush();
                            } else {
                                dos.writeUTF("ERR_UNKNOWN\n?\n");
                                dos.flush();
                            }
                        }
                        break;

                    case "delete":
                    case "DELETE":
                        if (command.length != 2) {
                            toreturn = "ERR_WRONG_ARGS_NUMBER\n?\n";
                            dos.writeUTF(toreturn);
                            dos.flush();
                        } else {
                            int notok = deleteFile(username, command[1]);
                            if (notok == 0) {
                                dos.writeUTF("OK\n?\n");
                                dos.flush();
                            } else if (notok == 1) {
                                dos.writeUTF("ERR_FILE\n?\n");
                                dos.flush();
                            } else {
                                dos.writeUTF("ERR_UNKNOWN\n?\n");
                                dos.flush();
                            }
                        }
                        break;

                    case "append":
                    case "APPEND":
                        if (command.length != 3) {
                            toreturn = "ERR_WRONG_ARGS_NUMBER\n?\n";
                            dos.writeUTF(toreturn);
                            dos.flush();
                        } else {
                            int notok = insertFile(username, command[1], command[2]);
                            if (notok == 0) {
                                dos.writeUTF("OK\n?\n");
                                dos.flush();
                            } else if (notok == 1) {
                                dos.writeUTF("ERR_FILE\n?\n");
                                dos.flush();
                            } else {
                                dos.writeUTF("ERR_UNKNOWN\n?\n");
                                dos.flush();
                            }
                        }
                        break;

                    case "list":
                    case "LIST":
                        if (command.length != 1) {
                            toreturn = "ERR_WRONG_ARGS_NUMBER\n?\n";
                            dos.writeUTF(toreturn);
                            dos.flush();
                        } else {
                            String resul = listFiles(username);
                            if (!resul.equals("NULL")) {
                                dos.writeUTF("\n" + resul + "\nOK\n?\n");
                                dos.flush();
                            } else {
                                dos.writeUTF("ERR_NO_FILES\n?\n");
                                dos.flush();
                            }
                        }
                        break;

                    default:
                        toreturn = "ERR_COMMAND\n?\n";
                        dos.writeUTF(toreturn);
                        dos.flush();
                        break;
                }
            } catch (Exception ex) {
                System.out.println("An exception has occured: \n" + ex);
                try {
                    dis.close();
                    dos.close();
                    s.close();
                    this.stop();
                } catch (IOException e) {
                    System.out.println("Error!");
                }
            }
        }

        try {
            // closing resources 
            TimeUnit.SECONDS.sleep(2);
            this.s.close();
            this.dis.close();
            this.dos.close();
            logger("User " + username + " disconected!");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException ex) {
            Logger.getLogger(ProyectePSP_Thread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private int insertFile(String username, String filename, String text) {
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
                logger("User " + username + "'s file named '" + filename + "' was updated successfully!");
                return 0;
            } catch (IOException e) {
                return 1;
            }
        } else {
            return 1;
        }
    }

    private int deleteFile(String username, String filename) {
        String filePath = new File("").getAbsolutePath() + SEPARATOR + "data" + SEPARATOR + "USERS" + SEPARATOR + username + SEPARATOR + filename;
        File fil = new File(filePath);
        if (fil.delete()) {
            logger("User " + username + "'s file named '" + filename + "' deleted successfully!");
            return 0;
        } else {
            return 1;
        }
    }

    private String listFiles(String username) {
        String directory = FileSystems.getDefault().getPath("").toAbsolutePath().toString();
        String filePath = directory + SEPARATOR + "data" + SEPARATOR + "USERS" + SEPARATOR + username;
        File fil = new File(filePath);
        File[] paths;
        try {
            paths = fil.listFiles();
            String out = "";
            for (File path : paths) {
                out = out + path + " " + Files.size(Paths.get(path.toString())) / 1024 + "\n";
            }
            fil = null;
            for (File path : paths) {
                path = null;
            }
            logger("User " + username + "'s files listed successfully!");
            return out;
        } catch (IOException ex) {
            return "NULL";
        }
    }

    private int createFile(String username, String filename) {
        String filePath = new File("").getAbsolutePath() + SEPARATOR + "data" + SEPARATOR + "USERS" + SEPARATOR + username + SEPARATOR + filename;
        File fil = new File(filePath);
        if (fil.exists()) {
            return 1;
        } else {
            try {
                fil.createNewFile();
                logger("User " + username + "'s file named '" + filename + "' created successfully!");
                return 0;
            } catch (IOException ex) {
                return 1;
            }
        }
    }

    public void createUserFolder(String username) throws IOException {
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
            logger("User " + username + "'s folder created successfully!");
        } catch (IOException | InterruptedException ex) {
            Logger.getLogger(ProyectoPSP_Servidor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void deleteUserFolder(String username) throws IOException {
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
            logger("User " + username + "'s folder removed successfully!");
        } catch (IOException | InterruptedException ex) {
            Logger.getLogger(ProyectoPSP_Servidor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private int addUser(String username, String password) throws IOException {
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
            file.write(root.toString(4)); // 4 s�n els espais d'indentaci�
            file.close();
            logger("User '" + username + "' added successfully!");
            return 0;   //0 equals OK
        } catch (IOException ex) {
            Logger.getLogger(ProyectoPSP_Servidor.class.getName()).log(Level.SEVERE, null, ex);
            return 3;
        }
    }

    private int delUser(String username) throws IOException {
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
            file.write(root.toString(4)); // 4 s�n els espais d'indentaci�
            file.close();
            logger("User '" + username + "' removed successfully!");
            return 0;   //0 equals OK
        } catch (IOException ex) {
            Logger.getLogger(ProyectoPSP_Servidor.class.getName()).log(Level.SEVERE, null, ex);
            return 3;
        }
    }

    public void logger(String text) {
        String filePath = new File("").getAbsolutePath() + SEPARATOR + "data" + SEPARATOR + "logs" + SEPARATOR + "log";
        File fil = new File(filePath);
        if (fil.exists()) {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
            LocalDateTime now = LocalDateTime.now();
            try {
                FileWriter fw = new FileWriter(filePath, true);
                BufferedWriter bw = new BufferedWriter(fw);
                bw.newLine();
                bw.write("User " + username + "[" + s + "]" + dtf.format(now) + ": " + text);
                bw.close();
                fw.close();
            } catch (IOException e) {
                System.out.println("Exception: " + e);
            }
        }
    }

}
