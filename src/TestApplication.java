    import java.nio.file.Path;
    import java.rmi.registry.LocateRegistry;
    import java.rmi.registry.Registry;
    import java.util.ArrayList;

    /**
     * Class that contains the main method of the Client
     */
    public class TestApplication {

        public static void main(String[] args) {

            if (args.length < 2) {
                System.out.println("Usage: java Echo <peer_ap> <sub_protocol> <opnd_1> <opnd_2>");
                return;
            }
            /*
            *   Meanings:
            *       peer_ap : remote object name
            *       sub_protocol :  operation that the peer must execute : BACKUP, RESTORE, DELETE , RECLAIM, STATE
            *
            *       if state, then no opnd
            *
            *       opnd_1 : path to the file to delete/backup/restore
            *                if(reclaim) -> maximum amount of disk space (kbytes)
            *       opnd_2 : replication degree -- only backup or enhancement
            *
            */

            // Storing remote name and operation info
            String peer_ap = args[0], operation = args[1];
            if(peer_ap == null || operation == null){
                System.out.println("name of peer/operation");
                return;
            }


            try {

                System.out.println("Client Started...");

                Registry registry = LocateRegistry.getRegistry(1099);

                System.out.println("TestApp connecting..."+ peer_ap);
                Service rmi = (Service)registry.lookup(peer_ap);
                System.out.println("got it...");

                switch(operation){
                    case "BACKUP":

                            if(args.length != 4){
                                System.out.println("Usage: java Echo <peer_ap> <sub_protocol> <opnd_1> <opnd_2>");
                                return;
                            }

                            String path = args[2];

                            int replication_degree = Integer.parseInt(args[3]);

                            int backup = rmi.backup(path,replication_degree);

                            if(backup == 0)
                            {
                                System.out.println("backup done");
                            }

                        break;

                    case "RESTORE":

                            if(args.length != 3){
                                System.out.println("Usage: java Echo <peer_ap> <sub_protocol> <opnd_1>");
                                return;
                            }

                            int restore = rmi.restore(args[2]);

                            if(restore == 0)
                            {
                                System.out.println("restore done");
                            }
                        break;

                    case "DELETE":

                            if(args.length != 3){
                                System.out.println("Wrong arguments! Example: java TestApp 1923 DELETE test1.pdf");
                                return;
                            }

                            if(args[2] == null)
                            {
                                System.out.println("Path can't be null");
                                return;
                            }

                            int delete = rmi.delete(args[2]);

                            if(delete == 0)
                            {
                                System.out.println("delete done");
                            }
                        break;

                    case "RECLAIM":

                            //maximum amount of disk space (in KByte) that the service can use to store the chunks
                            if(args.length != 3){
                                System.out.println("Wrong arguments! Example: java TestApp 1923 RECLAIM 0");
                                return;
                            }

                            int size = Integer.parseInt(args[2]);
                            int reclaim = rmi.reclaim(size);

                            if(reclaim == 0)
                            {
                                System.out.println("reclaim done");
                            }
                        
                        break;

                    case "STATE":

                            if(args.length != 2){
                                System.out.println("Wrong arguments! Example: java TestApp 1923 STATE");
                                return;
                            }

                            ArrayList<String> state = rmi.state();

                            for(String message : state){
                                System.out.println(message);
                            }

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