SDIS Project 1 -- Distributed Backup Service

Java Version 11

-----------------------------------------------------------

 To compile the project, first type in the terminal:
        javac *.java

 To run the test application, then type:

    $ java TestApplication <peer_ap> <sub_protocol> <opnd_1> <opnd_2> 

 <peer_ap> - peer's access point
 <operation> - either BACKUP, DELETE, RESTORE, RECLAIM or STATE
 <opnd_1> - either path (BACKUP, DELETE, RESTORE) or maximum size of disk to store the chunks (RECLAIM)
 <opnd_2> - replication degree (BACKUP)

 To create a Peer:

    $ java Peer <version> <peerID> <peer_ap> <mc_address> <mc_port> <mdb_address> <mdb_port> <mdr_address> <mdr_port>

 <version> - protocol version
 <peerID> - ID of the peer
 <peer_ap> - peer's access point
 <mc_address> - IP multicast channel address
 <mc_port> - multicast channel port
 <mdb_address> - IP multicast data backup address
 <mdb_port> - multicast data backup port
 <mdr_address> - IP multicast data restore address
 <mdr_port> - multicast data restore port