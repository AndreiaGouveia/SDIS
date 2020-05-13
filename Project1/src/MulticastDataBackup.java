import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Random;

public class MulticastDataBackup implements Runnable {

    private String mscastAdress; //Multicast address of the channel
    private int mcastPort; //Multicast port of the channel
    private MulticastSocket multicastSocket; //multicast socket

    private static byte CR = 0xD; //CR Byte corresponding to the CRLF flag
    private static byte LF = 0xA; //LF Byte corresponding to the CRLF flag

    protected int peerID; //ID of the current peer

    /**
     *
     * Constructor of MulticastChannel, the multicast channel that deals with the DELETE, RESTORE and RECLAIM protocols
     *
     * @param address multicast channel address
     * @param mport multicast channel port
     */
    public MulticastDataBackup(String address, int mport) {
        mscastAdress = address;
        mcastPort = mport;
        peerID = Peer.getPeerId();
    }

    /**
     * Deals and treats the information within the packet received
     */
    @Override
    public void run() {
        //gets messages
        byte[] b = new byte[65000];
        try {
            multicastSocket = new MulticastSocket(mcastPort);
            multicastSocket.joinGroup(InetAddress.getByName(mscastAdress));

            while (true) {
                try {
                    DatagramPacket  received = new DatagramPacket(b, b.length);
                    multicastSocket.receive(received);
                    DealWithPacketBackup deal = new DealWithPacketBackup(received , multicastSocket , Peer.getPeerId());
                    Peer.getExecutor().execute(deal);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Function responsible for sending requests into the MulticastDataBackup ("PUTCHUNK")
     * @param request request to be sent
     */
    public void sendRequest(byte[] request) {
        // send request
        try {
            //get file id and chunk number
            String[] req = new String(request).split(" ",5);
            String fileID = req[3] , chunkNo = req[4];

            //create socket
            MulticastSocket socket = new MulticastSocket(this.mcastPort);

            socket.setTimeToLive(1);
            socket.joinGroup(InetAddress.getByName(mscastAdress));

            int time = new Random().nextInt(400);
            socket.setSoTimeout(time); //max time it will listen to

            //listen for x time
            long oldTime = System.currentTimeMillis();
            byte[] b = new byte[65000];

            DatagramPacket receive = new DatagramPacket(b,b.length);
            while(System.currentTimeMillis()-oldTime < time){

                try {
                    socket.receive(receive);
                }catch (IOException e)
                {
                    System.out.println("\nReached time out - no one has sent PUTCHUNK!");
                    break;
                }

                String[] ms = new String(receive.getData()).split(" ",6);

                if(ms[1]=="PUTCHUNK" && ms[3]==fileID && ms[4]==chunkNo){
                   System.out.println("Someone already sent it!");
                   socket.close();
                   return;//exit
                }
            }

            //sending request
            DatagramPacket replyPacket = new DatagramPacket(request, request.length, InetAddress.getByName(mscastAdress), mcastPort);
            socket.send(replyPacket);

            System.out.println("In PUTCHUNK - Sent packet, no one has sent it");
            socket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}