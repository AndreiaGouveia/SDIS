import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DealWithPacketMc implements Runnable{

    private static byte CR = 0xD; //CR Byte corresponding to the CRLF flag
    private static byte LF = 0xA; //LF Byte corresponding to the CRLF flag

    protected int peerID; //The ID of the current peer

    MulticastSocket multicastSocket; //Multicast socket used to send messages
    DatagramPacket received; // Datagram packet received from each operation

    /**
     *
     * Constructor of DealWithPacketMc, the class that deals with the packets related to the DELETE, RESTORE and RECLAIM protocols
     *
     * @param multicastSocket multicast socket used to send messages
     * @param packet datagram packet received from each operation
     * @param peerID id of the current peer
     */
    public DealWithPacketMc(MulticastSocket multicastSocket , DatagramPacket packet , int peerID){
        this.multicastSocket = multicastSocket;
        this.received = packet;
        this.peerID = peerID;
    }

    /**
     * Deals and treats the information within the packet received
     */
    @Override
    public void run(){

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
            return;
        }

        //else deal with message
        //check type of instruction
        String messageType = dataArray[1];

        ConcurrentHashMap<String,Chunk> chunks = Peer.getChunks();

        switch (messageType){
            case "DELETE":
                //Go trough every entry and deleting all entries that have as a substring the id of a file
                Iterator<Map.Entry<String,Chunk>> iter = chunks.entrySet().iterator();

                while (iter.hasNext()) {
                    Map.Entry<String,Chunk> entry = iter.next();
                    if(entry.getKey().contains(dataArray[3])){
                        double memory = Peer.getUsedMemory();
                        memory -= (double) entry.getValue().getContent().length;
                        iter.remove();
                    }
                }

                System.out.println("\n Completed delete with chunk size: "+Peer.getChunks().size());
                break;

            case "STORED":
                dataArray = dataStr.split(" ",6);
                String chunkId = dataArray[3] + " " + dataArray[4];

                if(chunks.containsKey(chunkId)){//check if we already have it stored
                    Chunk updatedChunk = chunks.get(chunkId);//update local chunk count
                    updatedChunk.increase_chunk_count(Integer.parseInt(dataArray[2]));
                    chunks.replace(chunkId, updatedChunk);//complete update
                }
                return;

            case "GETCHUNK":
                dataArray = dataStr.split(" ",6);
                if(chunks.containsKey(dataArray[3] + " " + dataArray[4])) {//check if we have the chunk

                    //create message
                    //format: <Version> <CHUNK> <SenderId> <FileId> <ChunkNo> <CRLF><CRLF><Body>
                    String message = dataArray[0] + " " + "CHUNK" + " " + peerID + " " + dataArray[3] + " " + dataArray[4] + " ";
                    byte[] crlf = new byte[] {CR, LF, CR , LF};

                    //concatenating all of the bytes
                    byte[] content = chunks.get(dataArray[3] + " " + dataArray[4]).getContent();

                    byte[] m = new byte[message.getBytes().length + crlf.length];
                    System.arraycopy(message.getBytes(), 0, m, 0, message.getBytes().length);
                    System.arraycopy(crlf, 0 , m , message.getBytes().length , crlf.length);

                    byte[] finalMessage = new byte[m.length + content.length];
                    System.arraycopy(m, 0, finalMessage, 0, m.length);
                    System.arraycopy(content, 0, finalMessage, m.length, content.length);

                    Peer.getMdr().sendRequest(finalMessage);
                }
                break;

            case "REMOVED":
                dataArray = dataStr.split(" ", 6);
                String chunk_id = dataArray[3] + " " + dataArray[4];

                if(chunks.containsKey(chunk_id)){
                    Chunk chunk = chunks.get(chunk_id);
                    chunks.replace(chunk_id, chunk); //complete update
                    if(chunk.decrease_chunk_count(Integer.parseInt(dataArray[2]))){

                        //finnish creating message
                        String message = Peer.getVersion() +  " " + "PUTCHUNK" + " " + Peer.getPeerId() + " " + dataArray[3] + " " + dataArray[4] + " " + chunk.getReplication_degree() + " ";
                        byte[] content = chunk.getContent();

                        //creating th crlf byte
                        byte[] crlf = new byte[] {CR, LF, CR , LF};


                        //joining the bytes into a single message ( header(message) + crlf + body do chunk(content))
                        byte[] m = new byte[message.getBytes().length + crlf.length];
                        System.arraycopy(message.getBytes(), 0, m, 0, message.getBytes().length);
                        System.arraycopy(crlf, 0 , m , message.getBytes().length , crlf.length);

                        byte[] finalMessage = new byte[m.length + content.length];
                        System.arraycopy(m, 0, finalMessage, 0, m.length);
                        System.arraycopy(content, 0, finalMessage, m.length, content.length);
                        Peer.getMdb().sendRequest(finalMessage);

                    }

                }
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
