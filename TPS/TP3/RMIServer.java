import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;

public class RMIServer extends UnicastRemoteObject implements RMI_interface {

    private final HashMap<String, String> data = new HashMap<String, String>();

    public RMIServer() throws RemoteException {
        super(0);
    }

    public static void main(String[] args) {

        if (args.length != 1) {
            System.out.println("Usage: java Echo <remote_object_name>");
            return;
        }

        String remote_object_name = args[0];

        try {
            RMIServer obj = new RMIServer();
           System.out.println("created rmi");
            // Bind the remote object's stub in the registry
            Registry registry = LocateRegistry.createRegistry(2025);
            System.out.println("created resgetry");
            registry.bind("//localhost/RMIServer" , obj);
    
            System.err.println("RMIServer ready");
        } catch (Exception e) {
            System.err.println("RMIServer exception: " + e.toString());
            e.printStackTrace();
        }

    }

    @Override
    public int register(String owner , String plate){
        System.out.println("Registing...: owner: " + owner + " and plate: " + plate);

        //check if exists
        if(data.containsKey(plate))
        {
            System.out.println("ERROR: Aleady registered");
            return -1;
        }

        //insert new plate
        data.put(plate, owner);
        System.out.println("Registered successfully");
        return 0;
    }

    @Override
    public String lookup(String plate){
        System.out.println("Looking up...: plate: " + plate);

         //check if exists
         if(data.containsKey(plate))
         {
            return data.get(plate);
         }
         
        System.out.println("ERROR: Aleady registered");
        return "ERROR";
    }
}