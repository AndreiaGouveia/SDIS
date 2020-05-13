import java.io.IOException;
import java.net.*;
import java.io.*;

public class Client {
    public static void main(String[] args) throws IOException {
         if (args.length < 4) {
            System.out.println("Usage: java Echo <host> <port> <oper> <opnd>");
            return;
        }
        /* MEANINGS*/
        /*
            Host - Dns Name
            Oper - Register or Lookup
            Opnd - List of operands

            args.length = 4-> lookup
                          5-> register
        */
        
        //local variables
        String oper = args[2];
        int port=Integer.parseInt(args[1]);
        
        //getting DataPacketReady
        System.out.println("oper:"+oper);

        String request ="";


        //error handling and defining requests
        if(oper.equals( "register"))
        {
            request =  oper + " " + args[3];
        }
        else if(oper.equals("lookup")){
            request = oper + " " + args[3];
        }
        else {
            System.out.println("shutting server down");

            request = oper + " " + args[3];
        }

        InetAddress address = InetAddress.getLocalHost();

        Socket socket = new Socket(address, port);

        // send request
        PrintWriter out = new PrintWriter(socket.getOutputStream(),true);
        out.println(request);


        //get response
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String response = in.readLine();
        if(response != null) {

            System.out.println("Client: " + args[2] + " " + args[3] + " : " + response);
        }
    }
}
