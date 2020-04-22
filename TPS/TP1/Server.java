import java.io.*;
import java.net.*;
import java.util.Map;
import java.util.HashMap;

public class Server{

    static Map<String, String> map = new HashMap<String, String>();

    public static String dealWithPacket(String recieved){
        String[] filtered = recieved.split(" ");
        String oper = filtered[0], name = filtered[1];

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            System.out.println(entry.getKey() + ":" + entry.getValue().toString());
        }

        if(oper.equals("register")){
            String ip = filtered[2];
            
            if(map.containsKey(name))
            return "-1" + name + ip;

            //insert
            map.put(name,ip);
            return "1" + name + ip;
        }
        else if(oper.equals("lookup")){
            //lookup
            if(map.containsKey(name)){
                return Integer.toString(map.size()) + name;
            }
            return "-1" + name;
        }
        else return "Time to shut down";


    }

    public static void main(String[] args) throws IOException {

        if (args.length != 1) {
           System.out.println("Usage: java Echo <port>");
           return;
       }

       int port = Integer.parseInt(args[0]);

       // send request
       DatagramSocket socket = new DatagramSocket(port);
       // byte[] sbuf = args[1].getBytes();
       InetAddress address = InetAddress.getLocalHost();
       // DatagramPacket packet = new DatagramPacket(sbuf, sbuf.length, address, 4445);
         
       while (true) {
       // get response
       byte[] rbuf = new byte[256];
       DatagramPacket packet = new DatagramPacket(rbuf, rbuf.length);
       socket.receive(packet);
       
       // display response
       String received = new String(packet.getData());
       System.out.println("Echoed Message: " + received);

       // send reply
       byte[] replyBuf = new byte[256];
       String reply = dealWithPacket(received);

       //client shuts down server
        if(reply.equals("Time to shut down"))
            break;

       replyBuf = reply.getBytes();

       DatagramPacket replyPacket = new DatagramPacket(replyBuf, replyBuf.length,packet.getAddress(),packet.getPort());

       socket.send(replyPacket);
    
       }

       socket.close();
   }
}