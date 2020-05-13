import java.util.ArrayList;

public class Chunk {

    private byte[] content;
    private int replication_degree;
    private static int chunk_Count;
    private static ArrayList<Integer> confirmation_peers;

    /**
     *
     * Constructor of Chunk, the class representing a file chunk
     *
     * @param content   content of the chunk
     * @param replication_degree  replication degree of the chunk
     * @param chunk_Count perceived replication degree
     */
    public Chunk(byte[] content , int replication_degree , int chunk_Count){
        this.chunk_Count = chunk_Count;
        this.content = content;
        this.replication_degree = replication_degree;
        this.confirmation_peers = new ArrayList<Integer>();
    }

    /**
     *
     * Used to increase the perceived replication degree - when a new peer sends "STORED"
     *
     * @param idPeer  id of the Peer
     */
    public void increase_chunk_count( int idPeer){
        System.out.println("ON INCREASE CHUNK IN CHUNKS");
        for(int i = 0; i<confirmation_peers.size() ;i++){
            System.out.println("Peer: " + confirmation_peers.get(i));
        }
        if(!confirmation_peers.contains(idPeer))
        {
            chunk_Count++;
            confirmation_peers.add(idPeer);
        }
    }

    /**
     *
     * Used to decrease the perceived replication degree - when a peer sends "REMOVED"
     *
     * @param idPeer  id of the Peer
     * @return "true" if the replication degree of the chunk is bigger than the perceived, "false" otherwise
     */
    public boolean decrease_chunk_count( int idPeer){
        System.out.println("ON DECREASE CHUNK IN CHUNKS");
        for(int i = 0; i<confirmation_peers.size() ;i++){
            System.out.println("Peer: " + confirmation_peers.get(i));
        }

        if(confirmation_peers.contains(idPeer))
        {
            setChunk_Count(chunk_Count - 1);
            confirmation_peers.remove(Integer.valueOf(idPeer));
        }

        return (replication_degree > chunk_Count);
    }


    /**
     * @return perceived replication degree
     */
    public int getChunk_Count() {
        return chunk_Count;
    }

    /**
     *  Sets the perceived replication degree to the given parameter number
     *  @param chunkCount replication degree to update to
     */
    public static void setChunk_Count(int chunkCount){
        chunk_Count = chunkCount;
    }

    /**
     * @return replication degree of the chunk
     */
    public int getReplication_degree() {
        return replication_degree;
    }

    /**
     * @return content of the chunk
     */
    public byte[] getContent() {
        return content;
    }

}
