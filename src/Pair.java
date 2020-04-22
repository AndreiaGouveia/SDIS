public class Pair implements Comparable{

    private int x; //Byte size of the content of a chunk
    private String fullkey; //ID + chunk_number 
    private String id; //ID of the file
    private int chunk_number; //number of the chunk

    /**
     *
     * Constructor of Pair, class responsible for storing the byte size of the content of a chunk and its chunk id
     *
     * @param x byte size of the content of a chunk
     * @param y ID + chunk_number
     */
    public Pair(int x , String y){
        this.x = x;
        fullkey = y;
        String[] separation = y.split(" ", 2);
        this.id = separation[0];
        this.chunk_number = Integer.parseInt(separation[1]);
    }


    /**
     * @return byte size of the content of a chunk
     */
    public int getX() {
        return x;
    }

    /**
     * @return ID of the file
     */
    public String getId() {
        return id;
    }

    /**
     * @return number of the chunk
     */
    public int getChunkNumber(){
        return chunk_number;
    }

    /**
     * @return ID + chunk_number 
     */
    public String getFullkey() {
        return fullkey;
    }

    /**
     * Used to override the compare operation, in order to use the "sort" operation
     */
    @Override
    public int compareTo(Object o) {

        int compareX = ((Pair)o).getX();

        return x - compareX;//descending order
    }
}
