import javax.annotation.processing.SupportedSourceVersion;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.security.cert.CRL;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentNavigableMap;


public class MulticastChannel implements Runnable {

    private String mscastAdress; //Multicast address of the channel
    private int mcastPort; //Multicast port of the channel

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
    public MulticastChannel(String address, int mport) {
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
            MulticastSocket multicastSocket = new MulticastSocket(mcastPort);
            multicastSocket.joinGroup(InetAddress.getByName(mscastAdress));

            while (true) {
                try {
                    DatagramPacket  received = new DatagramPacket(b, b.length);
                    multicastSocket.receive(received);
                    DealWithPacketMc mc = new DealWithPacketMc(multicastSocket , received , Peer.getPeerId());
                    Peer.getExecutor().execute(mc);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Function responsible for sending requests into the MulticastDataRestore ("CHUNK")
     * @param request request to be sent
     */
    public void sendRequest(byte[] request) {
        // send request
        try {
            //get file id
            String[] s = (new String(request)).split(" ");
            String fileId = s[3];

            //create socket
            MulticastSocket socket = new MulticastSocket(this.mcastPort);

            socket.setTimeToLive(1);
            socket.joinGroup(InetAddress.getByName(mscastAdress));

            //forming packet
            DatagramPacket replyPacket = new DatagramPacket(request, request.length, InetAddress.getByName(mscastAdress), mcastPort);

            //sending request
            socket.send(replyPacket);
            //closing socket
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Function responsible for dealing with the received packet
     * @param received received packet
     * @param socket multicast socket
     * @return returns the id of the chunk if the packet is stored, otherwise returns an empty string
     */
    public String dealWithPacket(DatagramPacket received , MulticastSocket socket){

        //reduce the size of the packet to its true size
        byte[] smallerData = new byte[received.getLength()];

        byte[] data = received.getData();
        System.arraycopy(data, 0, smallerData, 0, received.getLength());
        data = smallerData;

        ArrayList<byte[]> separation = separateBodyAndHeader(data);

        String dataStr = new String(separation.get(0));

        String[] dataArray = dataStr.split(" ",5);

        //getting local variables
        int senderID = Integer.parseInt(dataArray[2]);

        //check if we sent it

        if(senderID == peerID)//to do, check if restore doesn't need this
        {
            //ignore message
            return  "";
        }

        //else deal with message
        //check type of instruction
        String messageType = dataArray[1];


        ConcurrentHashMap<String,Chunk> chunks = Peer.getChunks();

        switch (messageType){

            case "STORED":
                dataArray = dataStr.split(" ",6);
                String chunkId = dataArray[3] + " " + dataArray[4];

                if(chunks.containsKey(chunkId)){//check if we already have it stored
                    Chunk updatedChunk = chunks.get(chunkId);//update local chunk count
                    updatedChunk.increase_chunk_count(Integer.parseInt(dataArray[2]));
                    chunks.replace(chunkId, updatedChunk);//complete update
                    System.out.println(updatedChunk.getChunk_Count());
                }
                return chunkId;

            default:
                System.out.println("couldn't translate message");
        }

        return "";
    }

    /**
     * Function responsible for listening to the multicast channel for a limited period of time, waiting for stored responses
     * @param time maximum listening time
     * @param chunkID id of the chunk
     * @param replication_degree replication degree
     * @return number of confirmed responses
     */
    public int ListenToStored( int time , String chunkID , int replication_degree){
        int confirmed_responses = 0;

        try {
            //create socket
            MulticastSocket socket = new MulticastSocket(this.mcastPort);

            socket.setTimeToLive(1);
            socket.joinGroup(InetAddress.getByName(mscastAdress));

            byte[] b = new byte[65000];
            DatagramPacket packet = new DatagramPacket(b, b.length);

            long initialTime = System.currentTimeMillis();

            socket.setSoTimeout(time);
            while ((System.currentTimeMillis() - initialTime) < time && confirmed_responses != replication_degree) { //cycles for 1s then waits for more time
                //receive packet and check its content
                socket.receive(packet);
                String r = dealWithPacket(packet, socket);

                //check if it is confirmation of the chunk that we want
                if (r.equals(chunkID)) {
                    confirmed_responses++;
                }
            }

            System.out.println("Confirmed responses: "+ confirmed_responses + "  + replication degree: " + replication_degree);

        }catch (IOException exception){
            System.out.println("Socket timeout has been achieved!\n");
        }

        return confirmed_responses;
    }

    /**
     *
     * Used to separate the header and body of a message
     *
     * @param data message to separate
     * @return ArrayList "response", containing first the header and then the body of the message
     */
    private ArrayList<byte[]> separateBodyAndHeader(byte[] data ) {

        int i;
        for (i = 0; i < data.length - 4; i++) {
            if (data[i] == 0xD && data[i + 1] == 0xA && data[i + 2] == 0xD && data[i + 3] == 0xA) {
                break;
            }
        }

        byte[] header = Arrays.copyOfRange(data, 0, i);

        byte[] body = Arrays.copyOfRange(data, i + 4, data.length);

        ArrayList<byte[]> response = new ArrayList<byte[]>();

        response.add(header);
        response.add(body);

        return response;
    }

}