import java.net.MulticastSocket;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class SendStored implements Runnable{

    private static byte CR = 0xD; //CR Byte corresponding to the CRLF flag
    private static byte LF = 0xA; //LF Byte corresponding to the CRLF flag

    private String[] dataArray; //Contains the header message
    private MulticastSocket socket; //Multicast socket

    /**
     *
     * Constructor of SendStored, class responsible for sending the "STORED" message
     *
     * @param dataArray contains the header message
     * @param socket multicast socket
     */
    public SendStored(String[] dataArray , MulticastSocket socket){
        this.dataArray = dataArray;
        this.socket = socket;
    }

    /**
     * Sends the "STORED" message
     */
    @Override
    public void run(){
        //compose stored message <Version> STORED <SenderID> <FileId> <ChunkNo> <CRLF><CRLF>
        String stored_str = dataArray[0] + " " + "STORED" + " " + Peer.getPeerId() + " " + dataArray[3] + " " + dataArray[4] + " ";

        //creating th crlf byte
        byte[] crlf = new byte[] {CR, LF, CR , LF};

        //joining the bytes into a single message ( header(message) + crlf + body do chunk(content))
        byte[] message = new byte[stored_str.getBytes().length + crlf.length];
        System.arraycopy(stored_str.getBytes(), 0, message, 0, stored_str.getBytes().length);
        System.arraycopy(crlf, 0 , message , stored_str.getBytes().length , crlf.length);

        Peer.getMc().sendRequest(message);

        System.out.println("Sent stored");


    }
}
