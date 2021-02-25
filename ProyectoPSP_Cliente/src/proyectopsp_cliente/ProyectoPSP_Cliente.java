package proyectopsp_cliente;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ProyectoPSP_Cliente {

    public static void main(String args[]) throws Exception {
        int port = 1469;
        try {
            Scanner scn = new Scanner(System.in);
            InetAddress ip = InetAddress.getByName("localhost");
            Socket s = new Socket(ip, port);
            DataInputStream dis = new DataInputStream(s.getInputStream());
            DataOutputStream dos = new DataOutputStream(s.getOutputStream());

            //Bucle de intercambio de informacion
            while (true) {
                String tosend;
                String received = dis.readUTF();
                if (received.equals("ERR_LOGIN\n")) {
                    System.out.println(received);
                    s.close();
                    System.out.println("Connection closed");
                    break;
                } else {
                    System.out.println("Server says: " + received);
                    tosend = scn.nextLine();
                    scn.reset();
                    if (tosend.toLowerCase().equals("quit")) {
                        dos.writeUTF(tosend);
                        received = dis.readUTF();
                        System.out.println(received);
                        System.out.println("Connection closed");
                        break;
                    } else {
                        dos.writeUTF(tosend);
                    }
                }
            }
            
            s.close();
            scn.close();
            dis.close();
            dos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
