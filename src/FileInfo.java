import javax.print.DocFlavor;
import java.io.*;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.nio.file.Files;
import java.nio.file.attribute.UserPrincipal;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;

import static java.lang.System.exit;

public class FileInfo{

    private String filePath; //Path of the file
    private String fileId; //ID of the file
    private double fileVersion; //Version of the peer
    private ConcurrentHashMap<Integer, byte[]> chunks; //Contains, as key, the number of the chunk and, as value, its content
    private static ArrayList<Integer> replicationPerceived; //Contains the perceived replication degrees of each chunk respectively
    private Integer number_of_chunks; //Number of chunks
    private int replication_degree; //Replication degree of the file
    private String file_name; //Name of the file

    /**
     *
     * Constructor of FileInfo, the class that holds all the information of a file
     *
     * @param file_path   path of the file
     * @param fileVersion  version of the peer
     * @param replication_degree replication degree of the file
     */
    FileInfo(String file_path , double fileVersion , int replication_degree){

        filePath = file_path;
        chunks =  new ConcurrentHashMap<Integer, byte[]>();
        this.fileVersion =fileVersion;
        this.replication_degree = replication_degree;

        //generate id and divide into chunks
        generate_ID();
        divideFileIntoChunks();
        replicationPerceived = new ArrayList<Integer>(Collections.nCopies(chunks.size(), 0));

    }

    /**
     * Creates an ID mixing the file name, modified date and its owner
     */
    public void generate_ID(){
        //mix of file name, date modified and owner
        File file = new File(filePath);

        System.out.println("FILE SIZE: " + file.length());

        String fileName = file.getName();
        String lastModified = file.lastModified() + "";

        String[] f = fileName.split("/");
        file_name = f[f.length-1];

        //get owner
        UserPrincipal user = null;
        try {
            user = Files.getOwner(file.toPath());
        } catch (Exception x) {
            System.err.println("Faled to get owner of: " + filePath);
            System.err.println("\tError is: " + x);
        }

        String owner = user.getName();

        //put it all together
        String file_id = fileName + " " + lastModified + " " + owner;

        //encrypt id sha-256
        try{

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] encrypted = digest.digest(file_id.getBytes(StandardCharsets.UTF_8));

        StringBuilder hexStr = new StringBuilder();

        for(byte singleByte : encrypted){
            String m = Integer.toHexString(0xff & singleByte);
            if(m.length() == 1)
                hexStr.append('0');
            hexStr.append(m);
        }

        fileId = hexStr.toString();
        } catch (NoSuchAlgorithmException exception){
            System.out.println("Error "+exception.getMessage());
        }
    }

    /**
     * Divides a file into multiple chunks
     */
    public void divideFileIntoChunks(){

        try{

            byte[] fileContent = Files.readAllBytes((new File(this.filePath)).toPath());

            int c = 0;
            for (int i = 0 ; i<fileContent.length ; i++){
                if((fileContent.length - (i+64000)) > 0)
                {
                    System.out.println("Chunk number: " + c + " Chunk size: 6400");
                    chunks.put(c , Arrays.copyOfRange(fileContent, i, (i+64000)));
                    i+=63999;
                }
                else{
                    System.out.println("Chunk number: " + c + " Chunk size: "+ (fileContent.length - i));
                    chunks.put(c, Arrays.copyOfRange(fileContent, i, fileContent.length));
                    c++;
                    break;
                }
                c++;
            }

            if(chunks.get(c-1).length == 64000)
            {
                chunks.put(c , new byte[0] );//create a last chunk with nothing
                number_of_chunks = c;
            }else number_of_chunks = c;

        }catch (java.lang.NullPointerException exception)
        {
            exception.printStackTrace();
        }
        catch (FileNotFoundException e){
            System.out.println("File not found");
        }
        catch(IOException ioe){
            System.out.println("Error "+ioe.getMessage());
        }
    }

    /**
     * @return path of the file
     */    
    public String getFileName() {
        return filePath;
    }

    /**
     * @return name of the file
     */
    public String getFileTag(){
        return file_name;
    }

    /**
     * @return id of the file
     */
    public String getId() {
        return fileId;
    }

    /**
     * @return concurrent hash map with the chunks
     */
    public ConcurrentHashMap<Integer, byte[]> getChunks() {
        return chunks;
    }

    /**
     * @return number of chunks
     */
    public Integer getNumber_of_chunks() {
        return number_of_chunks;
    }

    /**
     * @return replication degree of the file
     */
    public int getReplication_degree(){return replication_degree; }

    /**
     * @return version of the peer
     */
    public double getFileVersion() { return fileVersion; }

    /**
     * @return array list with the perceived replication degrees of each chunk
     */
    public static ArrayList<Integer> getReplicationPerceived(){
        return replicationPerceived;
    }
}