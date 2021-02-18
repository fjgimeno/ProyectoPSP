package proyectopsp_servidor;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author thinkpadx220
 */
public class ProyectoPSP_Servidor {
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        int port = 1500;
        if (args.length != 0) {
            System.err.println("Uso: java ReceptorUDP");
        } else {
            try {
                ServerSocket ss = new ServerSocket(port);
                try {
                    // Crea el  socket 
                    DatagramSocket sSocket = new DatagramSocket(port);
                    // Crea el espacio para los mensajes 
                    byte[] cadena = new byte[1000];
                    DatagramPacket mensaje = new DatagramPacket(cadena, cadena.length);
                    System.out.println("Esperando mensajes..");
                    while (true) {
                        // Recibe y muestra el mensaje 
                        sSocket.receive(mensaje);
                        String datos = new String(mensaje.getData(), 0, mensaje.getLength());
                        System.out.println("\tMensaje Recibido: " + datos);
                    }
                } catch (SocketException e) {
                    System.err.println("Socket: " + e.getMessage());
                } catch (IOException e) {
                    System.err.println("E/S: " + e.getMessage());
                }
            } catch (IOException ex) {
                Logger.getLogger(ProyectoPSP_Servidor.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

}
