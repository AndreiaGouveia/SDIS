import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class RMIClient{

    public static void main(String[] args) {

        if (args.length < 3) {
            System.out.println("Usage: java Echo <remote_object_name> <oper> <opnd>");
            return;
        }

        // Storing remote name and operation info
        String remote_object_name = args[0], operation = args[1], plate = args[2];

        if(plate == null){
            System.out.println("Expected valid plate");
            return;
        }

        try {

            System.out.println("Client Started...");

            Registry registry = LocateRegistry.getRegistry(2025);

            System.out.println("Client getting registree..."+remote_object_name);
            RMI_interface rmi = (RMI_interface)registry.lookup("//localhost/RMIServer");
            System.out.println("got it...");

            switch(operation){
                case "register":

                    System.out.println("On registered...");
                    String owner = args[3];

                    if(owner == null){
                        System.out.println("Expected owner name");
                        return;
                    }
                    System.out.println("Client getting info...");

                    int register = rmi.register(owner,plate);
                    System.out.println("Client got info...");

                    if(register == -1)
                    {
                        System.out.println("Already exists");
                    }

                    System.out.println("Registering name: " + owner + " -- plate: "+plate);
                    break;

                case "lookup":
                    System.out.println("Looking up name plate: "+plate);

                    String lookup = rmi.lookup(plate);
                    System.out.println("Onwer name is: "+ lookup);
                    break;

                default:

                    System.out.println("Invalid request ");
                    break;
            }
        } catch (Exception e) {
            System.err.println(" Something went wrong");
            e.printStackTrace();}
    }
}