package proyectopsp_cliente;

import java.io.*;
import java.net.*;

public class ProyectoPSP_Cliente {

    public static void main(String args[]) throws Exception {
        int port = 1469;
        boolean active = true;
        Socket s = new Socket("localhost", port);
        DataInputStream din = new DataInputStream(s.getInputStream());
        DataOutputStream dout = new DataOutputStream(s.getOutputStream());
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String str = "", str2 = "";

        while (active) {
            System.out.println("");
            str2 = din.readUTF();
            if (str2.equals("ERR_LOGIN\n")) {
                System.out.println(str2);
                active = false;
            } else {
                System.out.println("Server says: " + str2);
                str = br.readLine();
                dout.writeUTF(str);
                dout.flush();
                if (str.equals("quit") || str.equals("QUIT")) {
                    active = false;
                }
            }
        }

        if (!str2.equals("ERR_LOGIN\n")) {
            System.out.println(din.readUTF());
        }
        din.close();
        dout.close();
        //s.close();
    }
}
