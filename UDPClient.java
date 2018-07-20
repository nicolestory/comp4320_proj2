import java.io.*;
import java.net.*;
import java.util.ArrayList;

class UDPClient {
   // To run: javac UDPClient.java && 
   // java UDPClient <serverHostname> <portNumber> <corruption> <loss> <fileName>
   private static String serverHostname = "tux059";   // Arg 1
   private static int portNumber = 10052;             // Arg 2
   private static double corruptionProb = 0.0;        // Arg 3
   private static double lossProb = 0.0;              // Arg 4
   private static String fileName = "TestFile.html";  // Arg 5
   private static String correctArgUsage = "Args should be in the following order:\n"
    + "<serverHostname> <portNumber> <corruption> <loss> <fileName>";

   public static void main(String args[]) throws Exception
   {
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
      
      DatagramSocket clientSocket = new DatagramSocket();
      InetAddress IPAddress = InetAddress.getByName(serverHostname);
      byte[] sendData = new byte[1024];
      sendData = request.getBytes();
      
      DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, portNumber);
      clientSocket.send(sendPacket);
      System.out.println("Sent a packet!\n");
      ArrayList<Packet> packets = recievePackets(clientSocket);
   
      clientSocket.close();
   }
   
   private static boolean parse_args(String args[])
   {
      if (args.length >= 2) {
         serverHostname = args[0];
         int tempPortNumber = Integer.parseInt(args[1]);
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
   
   private static double validProbability(String probArg) {
      double tempProb = Double.parseDouble(probArg);
      if ((0.0 > tempProb) || (tempProb > 1.0)) {
         System.out.println("The probability " + tempProb + " is not valid.");
         System.out.println("Try a decimal value between 0.0 and 1.0");
         return -1.0;
      }
      return tempProb;
   }
   
   /**
    * TODO: Change this to Selective Repeat.
    * TODO: Add in Gremlin function for each packet.
    */
   public static ArrayList<Packet> recievePackets(DatagramSocket clientSocket) throws Exception {
      boolean receivedLastPacket = false;
      ArrayList<Packet> packetsList = new ArrayList<Packet>();
   // Read in packets, and sort them:
      do {
         byte[] receiveData = new byte[Packet.maxPacketSize];
         DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
         clientSocket.receive(receivePacket);
         System.out.println("Recieved a packet!"); 
         receiveData = receivePacket.getData();
         
         Packet packet = new Packet(receiveData);
         
         System.out.println(packet.toString()); 
         int sequenceNumber = packet.getSequenceNum();
         if (packetsList.size() == sequenceNumber) {
            packetsList.add(packet);
         }
         else if (packetsList.size() < sequenceNumber) {
            for (int i = sequenceNumber; i < packetsList.size(); i++) {
               packetsList.add(null);
            }
            packetsList.add(sequenceNumber, packet);
         }
         else {
            packetsList.add(sequenceNumber, packet);
         }
      
         receivedLastPacket = packet.lastPacket();
      } while (!receivedLastPacket);
   
      return null;
   }

}