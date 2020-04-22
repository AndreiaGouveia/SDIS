import java.rmi.Remote;
import java.rmi.RemoteException;

interface RMI_interface extends Remote {
    int register(String owner, String plate_nr) throws RemoteException;

    String lookup(String plate_nr) throws RemoteException;
}