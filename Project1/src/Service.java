import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

interface Service extends Remote {
    /**
     * Initiates the BACKUP protocol in the peer
     * @param path Path of the file to backup
     * @param replication_degree replication degree 
     */
    int backup(String path , int replication_degree) throws RemoteException;

    /**
     * Initiates the RESTORE protocol in the peer
     * @param path Path of the file to restore
     */
    int restore(String path) throws RemoteException;

    /**
     * Initiates the DELETE protocol in the peer
     * @param path Path of the file to delete
     */
    int delete(String path) throws RemoteException;

    /**
     * Initiates the RECLAIM protocol in the peer
     * @param size Maximum disk space used to store the chunks
     */
    int reclaim(int size) throws RemoteException;

    /**
     * Initiates the STATE protocol in the peer
     */
    ArrayList<String> state() throws RemoteException;
}