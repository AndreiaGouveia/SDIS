import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.HashMap;

public class Server{

    //Server Variables 
    String mscastAdress , srvcPort;
    int mcastPort;
    DatagramPacket multicast;
    MulticastSocket multicastSocket;

    protected int TIME_OUT = 1000;
    static Map<String, String> map = new HashMap<String, String>();

    public static String dealWithPacket(String recieved){
        String[] filtered = recieved.split(" ");
        String oper = filtered[0], name = filtered[1];

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

        if (args.length != 3) {
           System.out.println("Usage: java Echo <srvc_port> <mcast_addr> <mcast_port> ");
           return;
       }

       new Server(args[1],args[0],Integer.parseInt(args[2]));

    }

    public Server (String address,String port , int mport){
        mscastAdress = address;
        srvcPort = port;
        mcastPort = mport;

        MulticastServer();
    }
    
    public void MulticastServer(){

       startThread();

       byte[] b = this.srvcPort.getBytes();
       try {
            

            multicast = new DatagramPacket(b, b.length, InetAddress.getByName(mscastAdress), mcastPort);
            
        } catch (UnknownHostException e) {
           e.printStackTrace();
       }

       try {
           multicastSocket = new MulticastSocket(mcastPort);
           multicastSocket.joinGroup(InetAddress.getByName(mscastAdress));
           multicastSocket.setTimeToLive(1);
           multicastSocket.setLoopbackMode(false);

           startMulticastServer();

           multicastSocket.leaveGroup(InetAddress.getByName(mscastAdress));
       } catch (IOException e) {
           e.printStackTrace();
       }
   }

   public void startThread() {
    new Thread(new Runnable() {
        @Override
        public void run() {
            DatagramSever(Integer.parseInt(srvcPort));
        }
    }).start();
    }

    public void DatagramSever(int port){

       // send request
       try{
        //create socket
       DatagramSocket socket = new DatagramSocket(port);
         
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

       //client shuts down Server
        if(reply.equals("Time to shut down"))
            break;

       replyBuf = reply.getBytes();

       DatagramPacket replyPacket = new DatagramPacket(replyBuf, replyBuf.length,packet.getAddress(),packet.getPort());

       socket.send(replyPacket);
    
       }

       socket.close();}
       catch(IOException e){
        e.printStackTrace();
       }
   
    }

    private void startMulticastServer() {
        while (true) {
            try {
                multicastSocket.send(multicast);
                System.out.printf("multicast: %s %d: %s %s\n", mscastAdress, mcastPort, multicast.getAddress(), srvcPort);
                Thread.sleep(1000);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
