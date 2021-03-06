import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Timer;

class UDPServer {

   private static int portNumber = 10052;
   private static String correctArgUsage = "Args should be in the following order:\n"
    + "<portNumber>";
    
   private static DatagramSocket serverSocket;
   private static int port;
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
      
      System.out.println("Starting UDP Server on port " + portNumber);
      
      serverSocket = new DatagramSocket(portNumber);
      byte[] receiveData;
      
      receiveData = new byte[1024];
      DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
      System.out.println("Ready to recieve HTTP requests");
      serverSocket.receive(receivePacket);
      port = receivePacket.getPort();
      System.out.println("Recieved a packet on port " + port + "!");
      String request = new String(receivePacket.getData());
      IPAddress = receivePacket.getAddress();
         
         // Segment packets and send them
      ArrayList<Packet> packets = segmentation(request);
      sendPackets(packets);
         
      System.out.println();
   }
   
   
   /**
    * Selective Repeat, sends packets.
    * 
    * @param packetList: a list of packets to be sent
    * @param serverSocket: a DatagramSocket to send packets through
    * @param IPAddress: the IP of the client
    * @param port: the port of the client process
    */
   public static void sendPackets(ArrayList<Packet> packetList) throws Exception {
      int firstSNinWindow = 0;
      final int numAcks = Packet.numAcksAllowed;
      final int maxSN = Packet.maxSequenceNum;
      boolean[] sentCorrectly = new boolean[Packet.maxSequenceNum];
      Timer[] timers = new Timer[maxSN];
      boolean totallyDone = false;
      int numOfAcks = numAcks;
      
      System.out.println("Number of packets: " + packetList.size() + "\n\n");
      
      while (!totallyDone) {   
         // Figure out which packets to send:
         ArrayList<Packet> packetsToSend = new ArrayList<Packet>();
         for (int i = 0; i < numAcks; i++) {
            if (!sentCorrectly[(firstSNinWindow + i) % maxSN] && (firstSNinWindow + i) < packetList.size()) {
               Packet sendPacket = packetList.get(firstSNinWindow + i);
               packetsToSend.add(sendPacket);
               if (timers[(firstSNinWindow + i) % maxSN] == null) {
                  Timer time = new Timer();
                  time.schedule(new PacketTimer(sendPacket, time), 40);
                  timers[(firstSNinWindow + i) % maxSN] = time;
               }
            }
         }
         
         // Send the packets:
         send(packetsToSend);
         
         // Wait for ACK/NAK vector:
         Packet ACKvector = waitForACK();
         boolean[] newACKs = ACKvector.decodeACK();
         int firstACK = ACKvector.getACKNum();
         numOfAcks = ACKvector.getNumOfAcks();
         
         // Adjust ACK bools:
         if (Math.abs(firstACK - (firstSNinWindow % maxSN)) > numAcks) {
            firstACK += firstSNinWindow / maxSN; // Adjust the firstACK number
         }
         for (int i = 0; i < numOfAcks; i++) {
            int index = (firstSNinWindow + i) % maxSN;
            sentCorrectly[index] = sentCorrectly[index] || newACKs[i];
            if (newACKs[i] && timers[i] != null) {
               timers[i].cancel();
            }
         }
         
         // Move window forwards:
         while (sentCorrectly[firstSNinWindow % maxSN]) {
            sentCorrectly[(firstSNinWindow + numAcks) % maxSN] = false;
            if (timers[(firstSNinWindow + numAcks) % maxSN] != null) {
               timers[(firstSNinWindow + numAcks) % maxSN].cancel();
               timers[(firstSNinWindow + numAcks) % maxSN] = null;
            }
            firstSNinWindow++;
         }
         
         // Check if we're totally done:
         if (firstSNinWindow >= packetList.size()) {
            totallyDone = true;
         }
      }
      
      System.out.println("All packets have been transmitted correctly!");
      
      // Make sure all timers are stopped:
      for (int i = 0; i < maxSN; i++) {
         if (timers[i] != null) {
            timers[i].cancel();
         }
      }
   }
   
   /**
    * Sends each packet to the client server.
    */
   public static void send(ArrayList<Packet> packets) {
      DatagramPacket sendPacket;
      for (Packet packet : packets) {
         byte[] packetByte = packet.toByteArray();
         sendPacket = new DatagramPacket(packetByte, packetByte.length, IPAddress, port);
         try {
            serverSocket.send(sendPacket);
         }
         catch (IOException e) {
            System.out.println("IOException while trying to send packet " + packet.getSequenceNum());
         }
         System.out.println("Sent a packet back.");
         System.out.println(packet.toString());
      }
   }
   
   /**
    * Waits for an ACK to come back from the client server, and returns that ACK
    * as a Packet object.
    */
   public static Packet waitForACK() throws Exception {
      System.out.println("Waiting for ACK");
      byte[] receiveData = new byte[Packet.maxPacketSize];
      DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
      serverSocket.receive(receivePacket);
      receiveData = receivePacket.getData();
      
      Packet ACKvector = new Packet(receiveData);
   
      return ACKvector;
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
      if (fileName == null) {
         System.out.println("That's not an HTTP request!");
         return null;
      }
      InputStream fileInput = new FileInputStream(fileName);
      int bytesRead = 0;
      String headerInfo = "HTTP/1.0 200 Document Follows \r\nContent-Type: text/plain\r\nContent-Length: " 
         + new File(fileName).length() + "\r\n\r\n";
      int headerSize = headerInfo.length();
      int numDataBytes = Packet.maxDataBytes - headerSize;
      byte[] packetData = Arrays.copyOfRange(headerInfo.getBytes(), 0, Packet.maxDataBytes);
      
      // Read in file in segments, add to packetList
      while ((bytesRead = fileInput.read(packetData, headerSize, numDataBytes)) != -1) {
         int dataLength = bytesRead + headerSize;
         packetList.add(new Packet(sequenceNum, Arrays.copyOfRange(packetData, 0, dataLength), dataLength));
         sequenceNum++;
         headerSize = 0;
         numDataBytes = Packet.maxDataBytes;
      }
      
      fileInput.close();
      System.out.println("Closed the file.");
      
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
