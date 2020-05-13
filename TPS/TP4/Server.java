
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.io.*;
import java.net.ServerSocket;

public class Server{

    public static void main(String[] args) throws IOException {

        if (args.length != 1) {
           System.out.println("Usage: java Echo <port>");
           return;
       }

        // send request
        int port_number = Integer.parseInt(args[0]);
        ServerSocket server_socket = new ServerSocket(port_number);

        while (true) {
            // get response
            Socket client = server_socket.accept();
            PrintWriter out = new PrintWriter(client.getOutputStream(),true);
            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            String received = in.readLine();

            if(received != null){

                System.out.println("Server: " + received);

                String[] parts = received.split(" ");

                String response = "received";


                //send response
                out.println(response);
            }

        }
   }
}