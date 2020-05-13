import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MulticastDataRestore implements Runnable{

    private String mscastAdress; //Multicast address of the channel
    private int mcastPort; //Multicast port of the channel
    private String fileID; //ID of the file

    private static byte CR = 0xD; //CR Byte corresponding to the CRLF flag
    private static byte LF = 0xA; //LF Byte corresponding to the CRLF flag

    /**
     *
     * Constructor of MulticastDataRestore, the multicast channel that deals with the RESTORE subprotocol
     *
     * @param address multicast channel address
     * @param mport multicast channel port
     */
    public MulticastDataRestore(String address, int mport) {
        mscastAdress = address;
        mcastPort = mport;
        fileID = "";
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
                    dealWithPacket(received, multicastSocket);

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

            //get file id and chunk number
            String[] req = new String(request).split(" ",6);
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
                    System.out.println("Reached time out on mdr");
                    break;
                }

                String[] ms = new String(receive.getData()).split(" ",6);
                
                if(ms[1].equals("CHUNK") && ms[3].equals(fileID) && ms[4].equals(chunkNo)){
                   System.out.println("Someone already sent it! -- CHUNK");
                   socket.close();
                   return;//exit
                }
            }

            //sending request
            DatagramPacket replyPacket = new DatagramPacket(request, request.length, InetAddress.getByName(mscastAdress), mcastPort);
            System.out.println("None chunk sent, sending Chunk size: " + replyPacket.getLength());
            socket.send(replyPacket);
            socket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Function responsible for dealing with the received packet
     * @param received received packet
     * @param socket multicast socket
     * @return either returns an empty byte in case of an operation that isn't stored or the response from the stored operation
     */
    public byte[] dealWithPacket(DatagramPacket received , MulticastSocket socket){

        byte[] smallerData = new byte[received.getLength()];

        byte[] data = received.getData();
        System.arraycopy(data, 0, smallerData, 0, received.getLength());
        data = smallerData;

        ArrayList<byte[]> separation = separateBodyAndHeader(data);

        String dataStr = new String(separation.get(0));

        String[] dataArray = dataStr.split(" ",8);

        byte[] content = separation.get(1);

        int senderID = Integer.parseInt(dataArray[2]);

        //check if we sent it

        if(senderID == Peer.getPeerId())
        {
            //ignore message
            return new byte[0];
        }

        //else deal with message
        //check type of instruction

        String messageType = dataArray[1];

        ConcurrentHashMap<String,Chunk> chunks = Peer.getChunks();
        String chunkId = dataArray[3] + " " + dataArray[4];
        byte[] messageT = (new String(messageType + " ")).getBytes();

        switch (messageType){

            case "CHUNK":

                if(chunkId.equals(fileID))
                {   
                    
                    System.out.println("Number chunk received: " + dataArray[4] + " with respective content: " + content.length);
                    return content;
                }

                messageT = (new String(messageType + "1")).getBytes();
                
                return messageT;


            default:
                System.out.println("couldn't translate message");
        }

        return new byte[0];
    }

    /**
     * Function responsible for listening to the chunks on the MulticastDataRestore channel and waits until it has received the chunk that it wants
     * @param chunk_id id of the chunk
     * @return either returns a "resend" message or the content of the chunk
     */
    public byte[] listenToChunks(String chunk_id){
        fileID = chunk_id;

        //gets messages
        byte[] b = new byte[65000];
        try {
            MulticastSocket multicastSocket = new MulticastSocket(mcastPort);
            multicastSocket.joinGroup(InetAddress.getByName(mscastAdress));
            multicastSocket.setSoTimeout(1000);

            while (true) {
                    DatagramPacket  received = new DatagramPacket(b, b.length);
                    try{
                    multicastSocket.receive(received);}

                    catch (IOException e){
                        System.out.println("Got time out ON CHUNK, will resend");
                        multicastSocket.close();
                        return "resend".getBytes();
                    }

                    byte[] r_byte = dealWithPacket(received, multicastSocket);
                    if((!(new String(r_byte)).equals("CHUNK1")) && r_byte.length!=0){
                        return r_byte;
                    }

            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return new byte[0];
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