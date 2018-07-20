import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;

class UDPServer {

   private static int portNumber = 10052;
   private static String correctArgUsage = "Args should be in the following order:\n"
    + "<portNumber>\nValid ports are 10052-10055.";

   public static void main(String args[]) throws Exception     
   {
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
         System.out.println("Ready to recieve packets");
         serverSocket.receive(receivePacket);
         int port = receivePacket.getPort();
         System.out.println("Recieved a packet on port " + port + "!");
         String request = new String(receivePacket.getData());
         InetAddress IPAddress = receivePacket.getAddress();
         
                     
         ArrayList<Packet> packets = segmentation(request);
         
         //System.out.println("File name: " + fileName);
         //System.out.println("parseR[0]: " + result[0] + "\nparseR[1]: " + result[1]);
         
         /*
         byte[][] packets = segmentation(result[1]);
         String response = result[0];
         for (byte[] packet : packets) {
            DatagramPacket sendPacket = new DatagramPacket(packet, packet.length, IPAddress, port);
            serverSocket.send(sendPacket);
            System.out.println("Sent a packet back.");
         }
         */
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
      
      while ((bytesRead = fileInput.read(packetData)) != -1) {
         Packet temp = new Packet(sequenceNum, packetData);
         System.out.println(temp);
         packetList.add(new Packet(sequenceNum, Arrays.copyOfRange(packetData, 0, bytesRead)));
         sequenceNum++;
      }
      
      fileInput.close();
      
      return packetList;
      
      /*
      int fileSize = 0;
      
      
      
      File f = new File(fileName);
   
      fileSize = (int) f.length();
   
      byte[][] segmentationMatrix = new byte[(int)(fileSize / 253.0) + 1][256];
      int numBytes = segmentationMatrix.length - 1;
   
      byte[] fileInBytes = new byte[fileSize];
      FileInputStream fis = new FileInputStream(f);
   
      fis.read(fileInBytes);
      fis.close();
   
      for (int i = 0; i < numBytes; i++) {
         for (int j = 3; j < 256; j++) {
            segmentationMatrix[i][j] = fileInBytes[(i * 253) + j - 3];
         }
      }
   
      // Get last packet size:
      int lastPacketSize = fileSize % 253;
   
      // Header info:
      for (int k = 0; k < numBytes; k++) {
         segmentationMatrix[k][1] = (byte) k; // Sequence number
         // Packet Size
         if (k == fileInBytes.length - 1) {
            segmentationMatrix[k][2] = (byte) (lastPacketSize - 1);
         }
         else {
            segmentationMatrix[k][2] = (byte) 255;
         }
         segmentationMatrix[k][0] = checkSum(segmentationMatrix[k]); // Checksum
      
      }
      byte[] lastPacket = {0, (byte) numBytes, 4, 0b0};
      lastPacket[0] = checkSum(lastPacket);
      segmentationMatrix[numBytes] = lastPacket;
   
      return segmentationMatrix;
   
      */
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
   
   /*
      File f = new File(file);
      
      String fileText = "";
      try
      {
         BufferedReader br = new BufferedReader(new FileReader(file));
         for (String line; (line = br.readLine()) != null;) {
            System.out.print(line);
            fileText += line + "\n";
         }
         br.close();
      }
      catch (Exception e)
      {
         System.out.println("Oh no.");
         return s;
      }
   
      if (f.exists()) {
         s[0] = "HTTP/1.0 200 Document Follows \r\nContent-Type: text/plain\r\nContent-Length: " + f.length() + "\r\n\r\n" + fileText;
         s[1] = file;
      }
      else {
         s[0] = "Error: File not found";
      }
      return s;
      
      */
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