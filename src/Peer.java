import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class Peer extends UnicastRemoteObject implements Service {

    private static ConcurrentHashMap<String,FileInfo> files; //contains the files that were backed up
    private static ArrayList<String> stateMessage; //message to be sent to the client regarding the state of a peer
    private static MulticastDataRestore mdr; //multicast data restore channel
    private static MulticastDataBackup mdb; //multicast data backup channel
    private static MulticastChannel mc; //multicast channel
    private static ConcurrentHashMap<String, Chunk> chunks; //contains the chunks - id (file_id + chunk number) and its content
    private static double version; //version of the peer
    private static int peer_id; //ID of the peer
    private static double maxCapacity; //Maximum disk storage capacity
    private static double usedMemory; //Used disk memory
    private static ScheduledThreadPoolExecutor executor; //Thread executor

    /**
     *
     * Constructor of Peer, responsible for processing the client's requests
     *
     * @param mdr_address multicast data restore channel address
     * @param mdrport multicast data restore channel port
     * @param mdb_address multicast data backup channel address
     * @param mdbport multicast data backup channel port
     * @param mc_address multicast channel address
     * @param mcport multicast channel port
     */
    public Peer(String mdr_address, int mdrport ,String mdb_address, int mdbport ,String mc_address,int mcport) throws RemoteException {

        mdr = new MulticastDataRestore(mdr_address, mdrport);
        mdb = new MulticastDataBackup(mdb_address, mdbport );
        mc = new MulticastChannel(mc_address, mcport);
        executor = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(200);
        this.maxCapacity = 64000000;
        this.usedMemory = 0;
    }

    /**
     * @param args user input (peer information)
     */
    public static void main(String[] args) {

        if (args.length != 9) {
            System.out.println("Usage: java Echo <version> <peer id> <remote_object_name> <mc_address> <mcport> <mdb_address> <mdbport> <mc_address> <mcport>");
            return;
        }

        try{
        //getting variables from the user input
        version = Double.parseDouble(args[0]);
        peer_id = Integer.parseInt(args[1]);
        String mdr_address = args[7],  mdb_address = args[5],  mc_address = args[3];
        int mdrport = Integer.parseInt(args[8]), mdbport = Integer.parseInt(args[6]), mcport = Integer.parseInt(args[4]);

        files = new ConcurrentHashMap<String , FileInfo>();
        chunks = new ConcurrentHashMap<String, Chunk>();
        String remote_object_name = args[2];
        

            Peer obj = new Peer(mdr_address, mdrport , mdb_address, mdbport , mc_address, mcport);

            Registry registry;
            try {
                registry = LocateRegistry.createRegistry(1099);
                System.out.println("created registry");
            }
            catch(Exception e){
                registry = LocateRegistry.getRegistry(1099);
            }

            registry.rebind(remote_object_name , obj);
    
            System.err.println("RMIServer ready");

            //starting multicast channels
            executor.execute(mdb);
            executor.execute(mdr);
            executor.execute(mc);

        } catch (Exception e) {
            System.err.println("RMIServer or number exception: " + e.toString());
            e.printStackTrace();
        }

    }

    /**
     * Executes the BACKUP protocol
     * @param path Path of the file to backup
     * @param replication_degree replication degree 
     */
    @Override
    public int backup(String path , int replication_degree){

        if(files.containsKey(path))
            return 1;

        FileInfo fileInfo = new FileInfo(path , version , replication_degree);

        files.put(path , fileInfo);

        executor.execute(new BackgroundSender(fileInfo, "PUTCHUNK"));

        return 0;
    }

    /**
     * Executes the RESTORE protocol 
     * @param path Path of the file to restore
     */
    @Override
    public int restore(String path){

           FileInfo file = CheckIfExists(path);

        if( file == null)// file has to be backedup
        {
            return 1;
        }

           executor.execute(new BackgroundSender(file , "GETCHUNK"));

        return 0;
    }

    /**
     * Executes the DELETE protocol 
     * @param path Path of the file to delete
     */
    @Override
    public int delete(String path){

        FileInfo fileInfo = CheckIfExists(path);

        if(fileInfo == null)// file has to be backedup
            return 1;

        executor.execute(new BackgroundSender(fileInfo,"DELETE"));

        return 0;
    }

    /**
     * Executes the RECLAIM protocol
     * @param size Maximum disk space used to store the chunks
     */
    @Override
    public int reclaim(int size){

        //size comes in kbytes
        if(size > 64000000 || size == maxCapacity)//if size surpasses the maximum possible or if it's already equal to peers max size
            return -1;

        maxCapacity = size;

        executor.execute(new BackgroundSender(size,"REMOVED"));

        return 0;
    }

    /**
     * Executes the STATE protocol 
     */
    @Override
    public ArrayList state(){

        stateMessage = new ArrayList<String>();

        String printState = "\n -----SERVICE STATE-----";
        String file_info = "File Info: \n";
        String chunk_infos = "Chunks Info: \n";
        String storage_info = "Storage Info: \n";

        Iterator<Map.Entry<String,FileInfo>> iter1 = files.entrySet().iterator();
        while (iter1.hasNext()) {
            Map.Entry<String,FileInfo> entry = iter1.next();
            file_info += "File pathname: " + entry.getKey() + "\n";
            file_info += "Id of the file: " + entry.getValue().getId() + "\n";
            file_info += "Replication degree: " + entry.getValue().getReplication_degree()+ "\n";
            
            ConcurrentHashMap<Integer, byte[]> fileChunks = entry.getValue().getChunks();
            Iterator<Map.Entry<Integer, byte[]>> iter2 = fileChunks.entrySet().iterator();
            while (iter2.hasNext()) {
                file_info += "For each chunk of the file...\n";
                Map.Entry<Integer, byte[]> entry1 = iter2.next();
                file_info += "Its id: " + entry1.getKey() + "\n";
                file_info += "Its perceived replication degree: " + entry.getValue().getReplicationPerceived().get(entry1.getKey()) + "\n";
            }
        }

        Iterator<Map.Entry<String,Chunk>> iter = chunks.entrySet().iterator();
        System.out.println("used memory: " + this.usedMemory);
        while (iter.hasNext()) {
            chunk_infos += "For each chunk stored: \n";
            Map.Entry<String,Chunk> entry = iter.next();
            chunk_infos += "Its id: " + entry.getKey() + "\n";
            chunk_infos += "Its size: " + (entry.getValue().getContent().length)/1000  + " kBytes" + "\n";
            chunk_infos += "Its replication degree: " + entry.getValue().getReplication_degree() + "\n";
        }

        storage_info += "Maximum amount of disk space that can be used to store chunks: " + this.maxCapacity + " kBytes" + "\n";
        storage_info += "Amount of storage used to backup the chunks: " + this.usedMemory/1000 + " kBytes" + "\n";

        stateMessage.add(printState);
        stateMessage.add(file_info);
        stateMessage.add(chunk_infos);
        stateMessage.add(storage_info);

        return stateMessage;
    }

    /**
     * Checks if the given file exists
     * @param path file path to be checked
     * @return file info if it exists, otherwise returns null
     */
    public FileInfo CheckIfExists(String path){
        if(files.containsKey(path))
            return files.get(path);

        return null;
    }

    /**
     * @return multicast channel
     */
    public static MulticastChannel getMc() {
        return mc;
    }

    /**
     * @return multicast data backup channel
     */
    public static MulticastDataBackup getMdb() {
        return mdb;
    }

    /**
     * @return multicast data restore channel
     */
    public static MulticastDataRestore getMdr() {
        return mdr;
    }

    /**
     * @return chunks of a file
     */
    public static ConcurrentHashMap<String,Chunk> getChunks(){
        return chunks;
    }

    /**
     * @return version of the peer
     */
    public static double getVersion(){
        return version;
    }

    /**
     * @return maximum disk storage capacity
     */
    public static double getMaxCapacity() {
        return maxCapacity;
    }

    /**
     * @return used disk memory
     */
    public static double getUsedMemory(){
        return usedMemory;
    }

    /**
     * Updated the disk memory to the given value
     * @param memory disk memory
     */
    public static void setUsedMemory(double memory){
        usedMemory = memory;
    }

    /**
     * @return id of the peer
     */
    public static int getPeerId(){
        return peer_id;
    }

    /**
     * @return thread executor
     */
    public static ScheduledThreadPoolExecutor getExecutor() {
        return executor;
    }
}