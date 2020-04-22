import java.io.IOException;
import java.net.*;

public class Client {
    public static void main(String[] args) throws IOException {
         if (args.length != 4) {
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
        String host = args[0], oper = args[2];
        int port=Integer.parseInt(args[1]);
        
        //getting DataPacketReady
        byte[] sbuf;
        DatagramPacket packet;
        InetAddress address = InetAddress.getLocalHost();
        System.out.println("oper:"+oper);


        // send request
        DatagramSocket socket = new DatagramSocket();

        //error handling and defining packets
        if(oper.equals( "register"))
        {
            String both = oper + " " + args[3] + " " + address.getHostAddress();
            sbuf = both.getBytes();
            packet = new DatagramPacket(sbuf, sbuf.length,address, port);
        }
        else if(oper.equals("lookup")){

            String both = oper + " " + args[3];
            sbuf = both.getBytes();
            packet = new DatagramPacket(sbuf, sbuf.length,address, port);
        }
        else {
            System.out.println("shutting server down");
            
            String both = oper + " " + args[3];
            sbuf = both.getBytes();
            packet = new DatagramPacket(sbuf, sbuf.length,address, port);
            
            socket.close();
            return ;
        }

        socket.send(packet);

        // get response
        byte[] rbuf = new byte[sbuf.length];
        packet = new DatagramPacket(rbuf, rbuf.length);
        socket.receive(packet);

        // display response
        String received = new String(packet.getData());
        System.out.println("Echoed Message: " + received);
        socket.close();
    }
}
