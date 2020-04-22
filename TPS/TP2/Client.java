import java.net.MulticastSocket;
import java.io.IOException;
import java.net.*;

public class Client {

    //local variables
    String mscastAddress,oper;
    int mcastPort;
    DatagramPacket packet;
    MulticastSocket multicastSocket;
    int port;

    public static void main(String[] args) throws IOException {
         if (args.length != 4) {
            System.out.println("Usage: java Echo <multicast_addr> <multicast_port> <operation> <operands>");
            return;
        }
        /* MEANINGS*/
        /*
            mscastAddress - Dns Name
            Oper - Register or Lookup
            Opnd - List of operands

            args.length = 4-> lookup
                          5-> register
        */
        //local variables
        new Client(args[0],args[1],args[2] , args[3]);
    }
    
    public Client( String mscastAddress1 ,String mcastPort1 ,String oper1, String x){
        this.mscastAddress = mscastAddress1;
        this.oper = oper1;
        this.mcastPort=Integer.parseInt(mcastPort1);

        packet = new DatagramPacket(new byte[128], 128);//tamanho max do packet e 128

        try {
            this.multicastSocket = new MulticastSocket(mcastPort);
            multicastSocket.joinGroup(InetAddress.getByName(mscastAddress));
            multicastSocket.setSoTimeout(4000);
            listenForBroadcast(x);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void listenForBroadcast( String x){//here we get the packet from the server, in order to get the port
        while (true) {
            try {
                this.multicastSocket.receive(this.packet);

                String str = new String(this.packet.getData(), 0, this.packet.getLength());
                System.out.println("DBG: Received from broadcast: " + str);

                port = Integer.parseInt(str);

                System.out.printf("multicast: %s %d : %s %d\n", this.mscastAddress, this.mcastPort, this.packet.getAddress(), port);
                this.oldClient(this.packet.getAddress() , x);
                break;

            } catch (SocketTimeoutException e) {
                System.out.println("Timeout ocurred, exiting");
                break;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void oldClient(InetAddress address , String x){
        //getting DataPacketReady
        try{
        byte[] sbuf;
        System.out.println("oper:"+oper);


        // send request
        DatagramSocket socket = new DatagramSocket();

        //error handling and defining packets
        if(oper.equals( "register"))
        {
            String both = oper + " " + x + " " + address.getHostAddress();
            sbuf = both.getBytes();
            packet = new DatagramPacket(sbuf, sbuf.length,address, port);
        }
        else if(oper.equals("lookup")){

            String both = oper + " " + x;
            sbuf = both.getBytes();
            packet = new DatagramPacket(sbuf, sbuf.length,address, port);
        }
        else {
            System.out.println("shutting server down");
            
            String both = oper + " " + x;
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
        catch(IOException e){
            e.printStackTrace();
        }
    }
}
