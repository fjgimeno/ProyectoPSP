package proyectopsp_servidor;

import java.io.*;
import java.net.*;

public class ProyectoPSP_Servidor {
    public static boolean boolContinue = true;
    public static final String SEPARATOR = System.getProperty("file.separator");
    public static DataInputStream din;
    public static DataOutputStream dout;

    public static void main(String args[]) throws Exception {
        int port = 1469;
        ServerSocket ss = new ServerSocket(port);

        while (true) {
            Socket s = null;
            try {
                System.out.println("Waiting for client connection...");
                s = ss.accept();
                System.out.println("A new client is connected : " + s);
                DataInputStream dis = new DataInputStream(s.getInputStream());
                DataOutputStream dos = new DataOutputStream(s.getOutputStream());
                System.out.println("Assigning new thread for this client");
                Thread t = new ProyectePSP_Thread(s, dis, dos);
                t.start();

            } catch (Exception e) {
                s.close();
                System.out.println("Exception: \n" + e);
            }
        }

    }
}
