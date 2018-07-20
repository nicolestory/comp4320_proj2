import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;

class UDPServer {

   private static int portNumber = 10052;
   private static String correctArgUsage = "Args should be in the following order:\n"
    + "<portNumber>";

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
      
      System.out.println("Starting UDP Server on port " + portNumber);
      
      DatagramSocket serverSocket = new DatagramSocket(portNumber);
      byte[] receiveData;
      
      while(true)
      {
         receiveData = new byte[1024];
         DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
         System.out.println("Ready to recieve HTTP requests");
         serverSocket.receive(receivePacket);
         int port = receivePacket.getPort();
         System.out.println("Recieved a packet on port " + port + "!");
         String request = new String(receivePacket.getData());
         InetAddress IPAddress = receivePacket.getAddress();
         
         // Segment packets and send them
         ArrayList<Packet> packets = segmentation(request);
         sendPackets(packets, serverSocket, IPAddress, port);
         
         System.out.println();
      }
   }
   
   /**
    * TODO: Implement Selective Repeat here.
    * Current stuff is a working version that just sends the packets.
    * 
    * @param packetList: a list of packets to be sent
    * @param serverSocket: a DatagramSocket to send packets through
    * @param IPAddress: the IP of the client
    * @param port: the port of the client process
    */
   public static void sendPackets(ArrayList<Packet> packetList, DatagramSocket serverSocket,
   InetAddress IPAddress, int port) {
      DatagramPacket sendPacket;
      for (Packet packet : packetList) {
         byte[] packetByte = packet.toByteArray();
         sendPacket = new DatagramPacket(packetByte, packetByte.length, IPAddress, port);
         try {
            serverSocket.send(sendPacket);
         }
         catch (IOException e) {
            System.out.println("IOException while trying to send packet " + packet.getSequenceNum());
         }
         System.out.println("Sent packet " + packet.getSequenceNum() + " back.");
      }
   }
   
   /**
    * Gets request, parses request, reads file, segments data, returns Packets to be sent.
    * 
    * @param request: a String with the HTTP request
    * @return packetList: an ArrayList of packets to be sent
    */
   public static ArrayList<Packet> segmentation(String request) throws Exception {
      String fileName = parseRequest(request);
      ArrayList<Packet> packetList = new ArrayList<Packet>();
      int sequenceNum = 0;
      InputStream fileInput = new FileInputStream(fileName);
      int bytesRead = 0;
      byte[] packetData = new byte[Packet.maxDataBytes];
      
      // Read in file in segments, add to packetList
      while ((bytesRead = fileInput.read(packetData)) != -1) {
         packetList.add(new Packet(sequenceNum, Arrays.copyOfRange(packetData, 0, bytesRead)));
         sequenceNum++;
      }
      
      fileInput.close();
      
      // Add last packet with null character:
      packetList.add(new Packet(sequenceNum, new byte[] {0b0}));
      
      return packetList;
   }

   
   /**
    * Returns the file name from the HTTP GET request,
    * or null if the request is not in the correct format.
    */
   public static String parseRequest(String request) {
      String parsedSoFar = "";
      String file = "";
      int fileSize = 0;
      String s = null;
      //s[0] = "Invalid request";
      //s[1] = file;
   
      if (request.length() < 14) {
         return s;
      }
   	
      if (!request.substring(0, 4).equals("GET ")) {
         return s;
      }
   
      parsedSoFar = request.substring(4, request.length());
   	
      int counter = 0;
   
      while (parsedSoFar.charAt(counter) != ' ') {
         counter++;
      }
   
      file = parsedSoFar.substring(0, counter);
   
      parsedSoFar = parsedSoFar.substring(file.length() + 1, parsedSoFar.length()).trim();
   
      if (!parsedSoFar.equals("HTTP/1.0")) {
         return s;
      }
      
      return file;
   }
   
   private static boolean parse_args(String args[])
   {
      if (args.length >= 1) {
         int tempPortNumber;
         try {
            tempPortNumber = Integer.parseInt(args[0]);
         }
         catch (Exception e) {
            return false;
         }
         if ((10052 > tempPortNumber) || (tempPortNumber > 10055)) {
            System.out.println("Port " + tempPortNumber + " is out of our port range.");
            System.out.println("Try a port between 10052 and 10055.");
            return false;
         }
         portNumber = tempPortNumber;
      }
      return true;
   }
}