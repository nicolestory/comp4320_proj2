import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;

class UDPClient {

   private static String serverHostname = "tux059";   // Arg 1
   private static int portNumber = 10052;             // Arg 2
   private static double corruptionProb = 0.0;        // Arg 3
   private static double lossProb = 0.0;              // Arg 4
   private static String fileName = "TestFile.html";  // Arg 5
   private static String correctArgUsage = "Args should be in the following order:\n"
    + "<serverHostname> <portNumber> <corruption> <loss> <fileName>";
    
   private static DatagramSocket clientSocket;
   private static InetAddress IPAddress;

   /**
    * Main. This is where the magic happens.
    * 
    * @param args: the command line arguments
    */
   public static void main(String args[]) throws Exception {
      if (!parse_args(args)) {
         System.out.println(correctArgUsage);
         return;
      }
      
      System.out.println("UDP Server Hostname: " + serverHostname);
      System.out.println("Port Number: " + portNumber);
      System.out.println("Corruption Probability: " + corruptionProb);
      System.out.println("Loss Probability: " + lossProb);
      System.out.println("File Name: " + fileName);
      
      String request = "GET " + fileName + " HTTP/1.0";
      System.out.println("Request: " + request);
      
      clientSocket = new DatagramSocket();
      IPAddress = InetAddress.getByName(serverHostname);
      byte[] sendData = new byte[1024];
      sendData = request.getBytes();
      
      DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, portNumber);
      clientSocket.send(sendPacket);
      System.out.println("Sent a packet!\n");
      ArrayList<Packet> packets = recievePackets(clientSocket);
   
      clientSocket.close();
      
      writeToFile(packets);
      launchBrowser("new_" + fileName);
   }
   
   /**
    * Selective repeat, receives packets
    * 
    * Recieves packets from UDPServer until a packet with a null byte on the end is recieved.
    * 
    * @param clientSocket: a DatagramSocket through which to recieve packets
    * @return a list of recieved packets
    */
   public static ArrayList<Packet> recievePackets(DatagramSocket clientSocket) throws Exception {
      boolean receivedLastPacket = false;
      ArrayList<Packet> packetsList = new ArrayList<Packet>();
      int numAcks = Packet.numAcksAllowed;
      int maxSN = Packet.maxSequenceNum;
      int firstSNinWindow = 0;
      boolean[] receivedCorrectly = new boolean[maxSN];
      Packet[] packetArray = new Packet[maxSN];
      int lastFlag = 9;
      int lastSN = -1;
      
      boolean totallyDone = false;
      
      // Read in packets, and sort them:
      do {
         int numExpected = 0;
         for (int i = 0; i < numAcks; i++) {
            if (!receivedCorrectly[(firstSNinWindow + i) % maxSN]) {
               numExpected++;
            }
         }
         
         // Read in packets:
         numExpected = Math.min(lastFlag, numExpected);
         for (int newPacNum = 0; newPacNum < numExpected; newPacNum++) {
         
            // Receive a packet:
            byte[] receiveData = new byte[Packet.maxPacketSize];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            clientSocket.receive(receivePacket);
            System.out.println("Recieved a packet!");
            receiveData = receivePacket.getData();
         
            Packet packet = new Packet(receiveData);
            int sequenceNumber = (firstSNinWindow / maxSN) * maxSN + packet.getSequenceNum();
            
            // Gremlin attack!
            packet = gremlin(packet);
         
            // Error checking:
            if ((packet == null || errorDetected(packet)) ) {
               if (packet != null && (packet.getLastIndex() < Packet.headerSize + 2)) {
                  break;
               }
               continue;
            }
            
            receivedCorrectly[sequenceNumber % maxSN] = true;
            packetArray[sequenceNumber % maxSN] = packet;
         
            System.out.println(packet.toString()); 
         
            // Check if this is the last packet:
            receivedLastPacket = packet.lastPacket();
            if (receivedLastPacket) {
               System.out.println("Found the last packet!");
               lastFlag = 1;
               lastSN = sequenceNumber;
               break;
            }
         }
         
         // Make ACK/NAK vector
         boolean[] ACKvector = new boolean[numExpected];
         for (int index = firstSNinWindow; index < firstSNinWindow + numExpected; index++) {
            ACKvector[(index - firstSNinWindow) % maxSN] = receivedCorrectly[index % maxSN];
         }
         
         Packet packetACK = new Packet(firstSNinWindow, ACKvector, numExpected);
         
         // Send ACK/NAK vector
         byte[] packetBytes = packetACK.toByteArray();
         DatagramPacket sendPacket = new DatagramPacket(packetBytes, packetBytes.length, IPAddress, portNumber);
         clientSocket.send(sendPacket);
         System.out.println("Sent an ACK vector!");
         
         // Check if we're totally done
         if (receivedLastPacket) {
            totallyDone = true;
            for (int i = firstSNinWindow; i < lastSN; i++) {
               totallyDone = totallyDone && receivedCorrectly[i % maxSN];
            }
         }
         
         // Move window forward
         while (receivedCorrectly[firstSNinWindow % maxSN]) {
            receivedCorrectly[(firstSNinWindow + numAcks) % maxSN] = false;
            packetsList.add(packetArray[firstSNinWindow % maxSN]);
            packetArray[firstSNinWindow % maxSN] = null;
            firstSNinWindow++;
         }
         
      } while (!totallyDone);
      
      System.out.println("All packets have been received correctly!");
   
      return packetsList;
   }
   
   /**
    * Detects errors in packets
    * 
    * @param packet: the packet to error-check
    * @return true if the packet has an error or is null, and false if 
    *    the packet has no detectable errors.
    */
   public static boolean errorDetected(Packet packet) throws Exception {
      if (packet == null) {
         System.out.println("Packet was lost!");
         return true;
      }
      
      if (packet.getChecksum() != packet.generateChecksum()) {
         System.out.println("An error was detected in packet " + packet.getSequenceNum() + ":");
         System.out.println(packet.toString());
         return true;
      }
      return false;
   }
   
   /**
    * Gremlins attack your packets. >:)
    * They corrupt or lose packets, depending on probabilities given in command line args.
    * 
    * @param packet: the packet to be corrupted or lost
    * @return the corrupted packet, or null for a lost packet
    */
   public static Packet gremlin(Packet packet) throws Exception {
      double changePacketProbability = Math.random();
      if (changePacketProbability <= corruptionProb) {
         System.out.println("A gremlin corrupted a packet!");
         packet = damagePacket(packet);
      }
      else if (Math.random() <= lossProb) {
         System.out.println("A gremlin stole a packet!");
         return null;
      }
      
      return packet;
   }
   
   /**
    * Corrupts the packet. XORs 1, 2, or 3 bytes of the packet.
    * 
    * @param packet: the packet to be damaged
    * @return the damaged packet
    */
   public static Packet damagePacket(Packet packet) throws Exception {
      int numBytesDamaged = 3;
      double randNumBytesDamaged = Math.random();
      if (randNumBytesDamaged < 0.5) {
         numBytesDamaged = 1;
      } else if (randNumBytesDamaged < 0.8) {
         numBytesDamaged = 2;
      }
      
      byte[] packetData = packet.getData();
      
      for (int i = 0; i < numBytesDamaged; i++) {
         int byteToChange = (int) (Math.random() * packetData.length);
         packetData[byteToChange] = (byte) (packetData[byteToChange] ^ 0xFF);
         System.out.println("Changed a byte!");
      }
      
      packet.setData(packetData);
      
      return packet;
   }

   
   /**
    * Outputs packet array to file.
    * 
    * @param packetsList: a list of Packets
    */
   private static void writeToFile(ArrayList<Packet> packetsList) throws Exception {
      FileOutputStream out = new FileOutputStream("new_" + fileName);
      
      for (int i = 0; i < packetsList.size() - 1; i++) {
         out.write(packetsList.get(i).getData());
      }
      out.close();
      System.out.println("Successfully wrote to file new_" + fileName);
   }
   
   /**
    * Opens the file in FireFox
    */
   public static void launchBrowser(String fileName) {
      Process p;
      try {
         p = Runtime.getRuntime().exec("firefox " + fileName);
      } catch (Exception e) {
         System.out.println("Error opening file " + fileName);
      }
   }
   
   /**
    * Parses input args and assigns values to class variables.
    * 
    * @param args[]: command line args
    * @return true if args were parsed correctly, false otherwise.
    */
   private static boolean parse_args(String args[])
   {
      if (args.length >= 1) {
         serverHostname = args[0];
      }
      if (args.length >= 2) {
         int tempPortNumber;
         try {
            tempPortNumber = Integer.parseInt(args[1]);
         }
         catch (Exception e) {
            return false;
         }
         if ((10052 > tempPortNumber) || (tempPortNumber > 10055)) {
            System.out.println("Port " + tempPortNumber + " is out of our port range.");
            System.out.println("Try a port between 10052 and 10055");
            return false;
         }
         portNumber = tempPortNumber;
      }
         
      if (args.length >= 3) {
         corruptionProb = validProbability(args[2]);
      }
      if (args.length >= 4) {
         lossProb = validProbability(args[3]);
      }
      if (corruptionProb == -1.0 || lossProb == 1.0) {
         return false;
      }
         
      if (args.length >= 5) {
         fileName = args[4];
      }
      
      return true;
   }
   
   /**
    * Parses inputted probability String to a double, and checks
    * that it is between 0.0 and 1.0, inclusive.
    * 
    * @param probArg: the probability String to be parsed
    * @return tempProb: the parsed probability, or -1.0 if it cannot be parsed
    */
   private static double validProbability(String probArg) {
      double tempProb;
      try {
         tempProb = Double.parseDouble(probArg);
      }
      catch (Exception e) {
         return -1.0;
      }
      if ((0.0 > tempProb) || (tempProb > 1.0)) {
         System.out.println("The probability " + tempProb + " is not valid.");
         System.out.println("Try a decimal value between 0.0 and 1.0");
         return -1.0;
      }
      return tempProb;
   }
}