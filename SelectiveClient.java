package selectiveRepeat;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

class Segment {
   int index;
   Segment next;
   String data;

   public Segment(int index, String data) {
      this.index = index;
      this.next = null;
      this.data = data;
   }
}

public class SelectiveClient {
   private static Segment head;

   public SelectiveClient() {
      head = null;
   }

   public static void main(String[] args) throws IOException {
   	// inputs
      System.out.println("This is Selective Repeat client");
      Scanner scan = new Scanner(System.in);
      String hostname = scan.next();
      int port = scan.nextInt();
      String filename = scan.next();
      int N = scan.nextInt();
      int mss = scan.nextInt();
      scan.close();
   
      int[] marker = new int[N];
   
   	// Socket
      DatagramSocket clientSocket = null;
      try {
         clientSocket = new DatagramSocket();
      } catch (SocketException e1) {
         e1.printStackTrace();
      }
      InetAddress serverIP = null;
      try {
         serverIP = InetAddress.getByName(hostname);
      } catch (UnknownHostException e1) {
         e1.printStackTrace();
      }
   
      Path filePath = Paths.get(filename);
      byte[] dataPacket = null;
      try {
         dataPacket = Files.readAllBytes(filePath);
      } catch (IOException e1) {
      	// TODO Auto-generated catch block
         e1.printStackTrace();
      }
      chunksDivision(dataPacket, mss);
      int currentIndex = 0;
      int pointer = 0;
      int seqAck = -1;
      int m = 0;
      long startTime = System.currentTimeMillis();
      while ((currentIndex * mss) < dataPacket.length) {
         for (m = 0; m < N; m++) {// sending
            if ((currentIndex * mss) > dataPacket.length)
               break;
            if (marker[m] == 2) {
               currentIndex++;
               continue;
            }
            Segment temp = head;
            while (temp!=null &&temp.index != currentIndex) // searching for data to send
               temp = temp.next;
            if(temp==null)
               break;
            String data = temp.data;
            byte[] header = createHeader(currentIndex, data); // creating
         														// header
            byte[] dataBytes = data.getBytes();
            byte[] packetToSend = new byte[header.length + dataBytes.length];
            for (int i = 0, j = 0; i < packetToSend.length; i++) { // copying
            	// header + data
               if (i < header.length)
                  packetToSend[i] = header[i];
               else {
                  packetToSend[i] = dataBytes[j];
                  j++;
               }
            }
            DatagramPacket toReceiver = new DatagramPacket(packetToSend, packetToSend.length, serverIP, port);
            try {// sending packet to server
               clientSocket.send(toReceiver);
               System.out.println("Packet sent : " + currentIndex);
               marker[m] = 1;
               currentIndex++;
            	// pointer++;
            } catch (IOException e) {
               e.printStackTrace();
            }
         }
         System.out.println("current index: " + currentIndex + " with m: " + m);
      	// in receiving mode
         int timeout = 1000;// in milliseconds
         byte[] receive = new byte[1024];
         DatagramPacket fromReceiver = new DatagramPacket(receive, receive.length);
         boolean flag = true;
      
         currentIndex = currentIndex - m;
         try {
            clientSocket.setSoTimeout(timeout);
            while (flag) {
               clientSocket.receive(fromReceiver);
               seqAck = ackHandler(fromReceiver.getData());
               System.out.println("Ack received for : " + seqAck);
               if (seqAck != -1) { // any other acknowledgement
                  int index = seqAck - currentIndex;
                  marker[index] = 2;
                  if (index == 0) {
                     while(marker[index]==2){
                        for (int i = 1; i < N; i++) {
                           marker[i - 1] = marker[i];
                        }
                        marker[N - 1] = -1;
                        currentIndex++;
                     }
                  }
               }
            }
         } catch (SocketTimeoutException ste) {// timeout
            System.out.println("Timeout, sequence number = " + seqAck);
         }
      }
   	// EOF
      String eof = "000000000000000000000000000000000000000000000000000000000000000000000000000";
      byte[] sendeof = eof.getBytes();
      DatagramPacket eofPacket = new DatagramPacket(sendeof, sendeof.length, serverIP, port);
      clientSocket.send(eofPacket);
      long endTime = System.currentTimeMillis();
      System.out.println("Total Time of transfer: " + (endTime - startTime));
   }

   public static void chunksDivision(byte[] dataPacket, int MSS) {
      int totalPackets = (int) Math.ceil((double) dataPacket.length / MSS);
      System.out.println("Total packets: " + totalPackets);
      String dataString = new String(dataPacket);
      for (int i = 0; i < totalPackets; i++) {
         int j = MSS * (i + 1);
         if (j > dataString.length()) {
            j = dataString.length();
         }
         String seg = dataString.substring(MSS * i, j);
         Segment s = new Segment(i, seg);
         if (head == null) {
            head = s;
         } else {
            Segment temp = head;
            while (temp.next != null) {
               temp = temp.next;
            }
            temp.next = s;
         }
      
      }
   }

   public static byte[] createHeader(int sequence, String data) {
      String sequenceStr = Integer.toBinaryString(sequence);
      String checksum = checksumCalculation(data);
      String fixedVal = "0101010101010101";
      for (int i = sequenceStr.length(); i < 32; i++) {
         sequenceStr = "0" + sequenceStr;
      }
      String header = sequenceStr + checksum + fixedVal;
   	// System.out.println("header is"+header);
      return header.getBytes();
   }

   public static String checksumCalculation(String data) {
      String hexString = new String();
      int value, i, result = 0;
      for (i = 0; i < data.length() - 2; i = i + 2) {
         value = (int) (data.charAt(i));
         hexString = Integer.toHexString(value);
         value = (int) (data.charAt(i + 1));
         hexString = hexString + Integer.toHexString(value);
         value = Integer.parseInt(hexString, 16);
         result += value;
      }
      if (data.length() % 2 == 0) {
         value = (int) (data.charAt(i));
         hexString = Integer.toHexString(value);
         value = (int) (data.charAt(i + 1));
         hexString = hexString + Integer.toHexString(value);
         value = Integer.parseInt(hexString, 16);
      } else {
         value = (int) (data.charAt(i));
         hexString = "00" + Integer.toHexString(value);
         value = Integer.parseInt(hexString, 16);
      }
      result += value;
      hexString = Integer.toHexString(result);
      if (hexString.length() > 4) {
         int carry = Integer.parseInt(("" + hexString.charAt(0)), 16);
         hexString = hexString.substring(1, 5);
         result = Integer.parseInt(hexString, 16);
         result += carry;
      }
      result = Integer.parseInt("FFFF", 16) - result;
      String padding = Integer.toBinaryString(result);
      for (int h = padding.length(); h < 16; h++) {
         padding = "0" + padding;
      }
      return padding;
   }

   public static int ackHandler(byte[] data) {
      String ACK = "";// Arrays.toString(data);
      for (int i = 0; i < 64; i++) {
         if (data[i] == 48) {
            ACK += "0";
         } else {
            ACK += "1";
         }
      }
      String packetType = ACK.substring(48, 64);
      if (packetType.equals("1010101010101010")) {
         return binToDec(ACK.substring(0, 32));
      }
      return -1;
   }

   private static int binToDec(String substring) {
      int dec = 0;
      int power = 0;
      for (int i = substring.length() - 1; i >= 0; i--) {
         if (substring.charAt(i) == '1')
            dec += Math.pow(2, power);
         power++;
      }
      return dec;
   }
}