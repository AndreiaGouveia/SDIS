import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class DealWithPacketBackup implements Runnable{

    private static byte CR = 0xD;
    private static byte LF = 0xA;

    protected int peerID;
    DatagramPacket received;
    MulticastSocket socket;

    /**
     *
     * Constructor of DealWithPacketBackup, the class that deals with the packets related to the BACKUP protocol
     *
     * @param received datagram packet received from the multicast backup channel
     * @param socket multicast socket used to send messages
     * @param peerID id of the current peer
     */
    public DealWithPacketBackup(DatagramPacket received , MulticastSocket socket , int peerID){
        this.received = received;
        this.socket = socket;
        this.peerID = peerID;
    }

    /**
     * Deals and treats the information within the packet received
     */
    @Override
    public void run() {

            byte[] smallerData = new byte[received.getLength()];

            byte[] data = received.getData();
            System.arraycopy(data, 0, smallerData, 0, received.getLength());
            data = smallerData;

            ArrayList<byte[]> separation = separateBodyAndHeader(data);

            String dataStr = new String(separation.get(0));

            byte[] content = separation.get(1);
            String[] dataArray = dataStr.split(" ",9);

            //getting local variables

            int senderID = Integer.parseInt(dataArray[2]);

            //check if we sent it
            if(senderID == peerID)//to do, check if restore doesn't need this
            {
                return;
            }

            //else deal with message
            //check type of instruction

            String messageType = dataArray[1];

            ConcurrentHashMap<String,Chunk> chunks = Peer.getChunks();
            String chunkId = dataArray[3] + " " + dataArray[4];
            switch (messageType){
                case "PUTCHUNK":

                    System.out.println("\n PUTCHUNK - Initial Number of chunks: " + chunks.size());
                    //see if chunk already exists

                    double memory = Peer.getUsedMemory();
                    double addedMemory = (double) content.length;

                    if(Peer.getMaxCapacity() < (memory + addedMemory))
                        return;

                    if (!chunks.containsKey(chunkId)) {
                        chunks.put(chunkId , new Chunk(content, Integer.parseInt(dataArray[5]),1));
                        memory += addedMemory;
                        Peer.setUsedMemory(memory);
                    }

                    SendStored stored = new SendStored(dataArray , socket);

                    Peer.getExecutor().schedule(stored , new Random().nextInt(400) , TimeUnit.MILLISECONDS);//sleep between 0 to 400 ms
                    break;

                default:
                    System.out.println("couldn't translate message");
            }

            return;
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
