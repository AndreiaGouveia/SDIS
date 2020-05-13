import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


import static java.lang.StrictMath.abs;

public class BackgroundSender implements Runnable{
    
    private FileInfo fileInfo; //Class that contains the information relative to a file
    private String channel; //Operation to be performed
    private static byte CR = 0xD; //CR Byte corresponding to the CRLF flag
    private static byte LF = 0xA; //LF Byte corresponding to the CRLF flag
    public int size; //Reclaim protocol maximum storage size
    public String path; //Path of the file

    /**
     *
     * Constructor of Background sender, class responsible to execute the protocols and supervise them, this contructor applies to every operation except RECLAIM
     *
     * @param fileInfo  the information about the file in question
     * @param channel   the operation to be made
     */
    public BackgroundSender(FileInfo fileInfo,String channel){
            this.fileInfo= fileInfo;
            this.channel = channel;
        }

    /**
     *
     * Constructor of Background sender only used for RECLAIM
     *
     * @param size      the new maximum size
     * @param channel   the operation to be made
     */
    public BackgroundSender(int size, String channel){
            this.size = size;
            this.channel = channel;
    }

    /**
     * Responsible for deciding the action to take 
     */
    @Override
    public void run(){
        switch (channel){
            case "PUTCHUNK":
                //mdb
                PutChunkHandler();
                break;

            case "GETCHUNK":
                //mdr
                System.out.println("onn get chunk");
                RestoreHandler();
                break;

            case "DELETE":
                //mc
                DeleteHandler();
                break;

            case "REMOVED":
                RemoveHandler();
                break;

            default:
                System.out.println("Could not find operation in backgroundSender");
        }
    }

    /**
     * Handler of putchunk operations, envia todos os chunks originados pelo ficheiro, cria a mensagem putchunk e espera pela respetiva confirmação (STORED)
     */
    public void PutChunkHandler(){///to do ver a cena versao
        //format : <version> <Message type>(this.channel) <sender_id> <File_ID> <ChunkNo> <ReplicationDeg> <CRLF>(OxD e 0xA) <CRLF> <Body>(chunk content)
        String message_str = fileInfo.getFileVersion() +  " " + this.channel + " " + Peer.getPeerId() + " " + fileInfo.getId() + " ";

        ConcurrentHashMap<Integer , byte[]> chunks = fileInfo.getChunks(); //getting the chunks

        for (Integer chunkNumber: chunks.keySet()){

            //finnish creating message
            String message = message_str + chunkNumber + " " + fileInfo.getReplication_degree() + " ";
            byte[] content = chunks.get(chunkNumber);

            //creating th crlf byte
            byte[] crlf = new byte[] {CR, LF, CR , LF};

            //joining the bytes into a single message ( header(message) + crlf + body do chunk(content))
            byte[] m = new byte[message.getBytes().length + crlf.length];
            System.arraycopy(message.getBytes(), 0, m, 0, message.getBytes().length);
            System.arraycopy(crlf, 0 , m , message.getBytes().length , crlf.length);

            byte[] finalMessage = new byte[m.length + content.length];
            System.arraycopy(m, 0, finalMessage, 0, m.length);
            System.arraycopy(content, 0, finalMessage, m.length, content.length);

            //send message
            boolean confirmed = true;
            int tries = 0;
            int time = 1000;
            int confirmResponses = 0;

            while(tries!=5){
               Peer.getMdb().sendRequest(finalMessage);//sends the request

                confirmResponses = Peer.getMc().ListenToStored(time,(fileInfo.getId() + " " +chunkNumber),fileInfo.getReplication_degree());//checks confirmed responses

               if(confirmResponses >= fileInfo.getReplication_degree())
               {
                   System.out.println("Replication degree was reached");
                   break;
               }
               time*=2;
               tries++;
            }

            fileInfo.getReplicationPerceived().set(chunkNumber, confirmResponses);
        }
    }

    /**
     * Handler of DELETE a file.
     */
    public void DeleteHandler(){

        //format : <version> <Message type>(this.channel) <sender_id> <File_ID> <CRLF>(OxD e 0xA) <CRLF>
        String message_str = fileInfo.getFileVersion() + " " + this.channel + " " + Peer.getPeerId() + " " + fileInfo.getId() + " " ;

        byte[] crlf = new byte[] {CR, LF, CR , LF};

        //joining the bytes into a single message ( header(message) + crlf + body do chunk(content))
        byte[] message = new byte[message_str.getBytes().length + crlf.length];
        System.arraycopy(message_str.getBytes(), 0, message, 0, message_str.getBytes().length);
        System.arraycopy(crlf, 0 , message , message_str.getBytes().length , crlf.length);

        Peer.getMc().sendRequest(message);//send the request
        System.out.println("\n DELETE - sent delete message");
    }

    /**
     * Handler of the RESTORE operation
     */
    public void RestoreHandler() {
        //format : <version> <Message type>(this.channel) <sender_id> <File_ID> <ChunkNo> <CRLF>(OxD e 0xA) <CRLF>
        System.out.println("number of chunks " + fileInfo.getNumber_of_chunks());

        try{

        int tries = 0;
        //creating the file to be written
            FileOutputStream newFile = new FileOutputStream(new File("restored-"+fileInfo.getFileTag()), true);

            System.out.println("no chunks: " + fileInfo.getNumber_of_chunks());
            for (int i = 0; i < fileInfo.getNumber_of_chunks(); i++) {//go through the number of chunks that exist in that file
                
                if(tries >= 5){
                    System.out.println("Aborted restore - chunk not found!");
                    return;
                }

                System.out.println("--------- WAITING FOR CHUNK NUMBER : " + i + "----------");

                //setting up the message of the request
                String message_str = fileInfo.getFileVersion() + " " + this.channel + " " + Peer.getPeerId() + " " + fileInfo.getId() + " " + i + " " ;

                byte[] crlf = new byte[] {CR, LF, CR , LF};

                //joining the bytes into a single message ( header(message) + crlf + body do chunk(content))
                byte[] message = new byte[message_str.getBytes().length + crlf.length];
                System.arraycopy(message_str.getBytes(), 0, message, 0, message_str.getBytes().length);
                System.arraycopy(crlf, 0 , message , message_str.getBytes().length , crlf.length);

                //send request
                Peer.getMc().sendRequest(message);

                byte[] received = Peer.getMdr().listenToChunks(fileInfo.getId() + " " + i); //listening for chunks

                if ((new String(received)).equals("resend") || (new String(received).equals("CHUNK1"))) {// chunk1 means that the message chunk wasnt of the chunk we wanted
                    i--;
                    tries++;
                    System.out.println("----------RESENT-------");
                    continue;
                }

                newFile.write(received);
            }

            System.out.println("GOT OUT");
            //closing file
            newFile.close();

            return;

        }catch (IOException exception){
            exception.printStackTrace();
        }
    }

    /**
     * Handler to deal with SPACE RECLAIMING
     */
    public void RemoveHandler(){

        double to_eliminate = abs(Peer.getMaxCapacity() - Peer.getUsedMemory());
        //getting our arrays
        ArrayList<ArrayList<Pair>> result = new ArrayList<ArrayList<Pair>>();
        result = getArrays();
        ArrayList<Pair> firstArray = result.get(0) , secondArray = result.get(1);

        //visit first array(doesn't require backup) and try to eliminate the smaller or equally sized chunks
        ListIterator<Pair> iterator = firstArray.listIterator();
            while(iterator.hasNext()){
                Pair a =  iterator.next();
                System.out.println("From the first array: " + a.getX() + " - still needs to eliminate: " + to_eliminate);
                    to_eliminate -= a.getX();
                    iterator.remove();
                    sendAndFormMessageRemoved(a.getChunkNumber(), a.getId());
                    if(to_eliminate <= 0) {
                        System.out.println("Reached Goal! Eliminated finished with value " + to_eliminate);
                        return;
                    }
            }
        //visit second array (requires backup) and try to eliminate the smaller or equally sized chunks
        ListIterator<Pair> iterator2 = secondArray.listIterator();
            while(iterator2.hasNext()){
                Pair a =  iterator2.next();
                System.out.println("From the second array: " + a.getX() + " - still needs to eliminate: " + to_eliminate);
                to_eliminate -= a.getX();
                iterator2.remove();
                sendAndFormMessageRemoved(a.getChunkNumber(), a.getId());
                if(to_eliminate <= 0) {
                    System.out.println("Reached Goal! Eliminated finished with value " + to_eliminate);
                    return;
                }
            }


    }

    /**
     * Prepares and sends the "REMOVED" message to the multicast channel 
     *
     * @param chunkNumber    number of the chunk
     * @param file_id        id of the file
     */
    public void sendAndFormMessageRemoved(int chunkNumber, String file_id){
        //format : <Version> REMOVED <SenderId> <FileId> <ChunkNo> <CRLF><CRLF>
        String message_str = Peer.getVersion() +  " " + this.channel + " " + Peer.getPeerId() + " " + file_id + " " + chunkNumber + " ";

        byte[] crlf = new byte[]{CR, LF, CR, LF};

        //joining the bytes into a single message ( header(message) + crlf + body do chunk(content))
        byte[] message = new byte[message_str.getBytes().length + crlf.length];
        System.arraycopy(message_str.getBytes(), 0, message, 0, message_str.getBytes().length);
        System.arraycopy(crlf, 0, message, message_str.getBytes().length, crlf.length);

        Peer.getMc().sendRequest(message);
    }

    /**
     * @return Two ArrayLists to be used on the RECLAIM protocol
     */   
    public ArrayList<ArrayList<Pair>> getArrays(){

        ArrayList<Pair> firstOne =  new ArrayList<Pair>(); //replication degree > perceived replication degree
        ArrayList<Pair> secondOne =  new ArrayList<Pair>(); //replication degree <= perceived replication degree

        String s ="";
        for (Map.Entry<String, Chunk> entry : Peer.getChunks().entrySet()) {
            if(entry.getValue().getReplication_degree() < entry.getValue().getChunk_Count()){
                firstOne.add(new Pair(entry.getValue().getContent().length, entry.getKey()));
            }else secondOne.add(new Pair(entry.getValue().getContent().length, entry.getKey()));
        }



        //bigger to smaller
        Collections.sort(firstOne);

        //smaller to bigger
        Collections.sort(secondOne);

        //add them to our array list
        ArrayList<ArrayList<Pair>> answer = new ArrayList<ArrayList<Pair>>();
        answer.add(firstOne);
        answer.add(secondOne);
        return answer;

    }

}