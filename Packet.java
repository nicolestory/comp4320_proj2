import java.util.Arrays;

class Packet {

   private byte checksum = -1;      // Index 0
   private int sequenceNum = -1;    // Index 1
   private int lastIndex = -1;      // Index 2-3
   private int ACKNum = -1;         // Index 4
   private int numOfAcks = -1;      // Index 5
   private byte[] data = null;
   public static final int maxPacketSize = 512;
   public static final int headerSize = 6;
   public static final int maxDataBytes = maxPacketSize - headerSize;
   public static final int maxSequenceNum = 24;
   public static final int numAcksAllowed = 8;  // AKA Window size
   
   private static byte True = 0x00;
   private static byte False = (byte) 0xFF;
   
   /**
    * From byte array (data AND headers) to Packet object.
    * For use from the client side, probably.
    */
   public Packet(byte[] packetBytes) throws Exception {
      if (packetBytes.length > maxPacketSize) {
         throw new Exception("Byte array is too long.");
      }
      // Assign header info to fields
      checksum = packetBytes[0];
      sequenceNum = packetBytes[1] % maxSequenceNum;
      lastIndex = (packetBytes[2] << 8) | (packetBytes[3] & 0xff);
      ACKNum = packetBytes[4];
      numOfAcks = packetBytes[5];
      data = Arrays.copyOfRange(packetBytes, headerSize, packetBytes.length);
   }
   
   /**
    * From byte array (data ONLY) and sequence number to Packet object
    */
   public Packet(int seqNum, byte[] packetData) throws Exception {
      if (packetData.length > maxDataBytes) {
         throw new Exception("Byte array for packet " + seqNum + " is too long.");
      }
      sequenceNum = seqNum % maxSequenceNum;
      lastIndex = packetData.length + headerSize - 1;
      data = packetData;
      checksum = generateChecksum();
   }
   
   /**
    * From byte array (data ONLY), sequence number, and index to Packet object
    */
   public Packet(int seqNum, byte[] packetData, int dataSize) throws Exception {
      if (packetData.length > maxDataBytes || dataSize > maxDataBytes) {
         throw new Exception("Packet data or index is too large.");
      }
      sequenceNum = seqNum % maxSequenceNum;
      lastIndex = dataSize + headerSize - 1;
      if (dataSize < packetData.length) {
         data = Arrays.copyOf(packetData, dataSize);
      }
      else {
         data = packetData;
      }
      checksum = generateChecksum();
   }
   
   /**
    * Creates a Packet object that represents an ACK/NAK vector.
    */
   public Packet(int firstAckNum, boolean[] ackArray, int numAcks) throws Exception {
      if (ackArray.length > numAcksAllowed) {
         throw new Exception("Number of ACKS must be less than or equal to " + numAcksAllowed);
      }
      if (firstAckNum < 0) {
         throw new Exception("Negative ACK number given: " + firstAckNum);
      }
      data = new byte[numAcks];
      for (int i = 0; i < numAcks; i++) {
         if (ackArray[i]) {
            data[i] = True;
         }
         else {
            data[i] = False;
         }
      }
      ACKNum = firstAckNum % maxSequenceNum;
      lastIndex = headerSize + numAcks - 1;
      numOfAcks = numAcks;
   }
   
   /**
    * Converts the Packet data into a byte array, ready to be sent.
    */
   public byte[] toByteArray() {
      byte[] packetBytes = new byte[lastIndex + 1];
      packetBytes[0] = checksum;
      packetBytes[1] = (byte) sequenceNum;
      packetBytes[2] = (byte) (lastIndex >> 8);
      packetBytes[3] = (byte) (lastIndex % 256);
      packetBytes[4] = (byte) ACKNum;
      packetBytes[5] = (byte) numOfAcks;
      for (int i = 0; i < data.length; i++) {
         packetBytes[headerSize + i] = data[i];
      }
      return packetBytes;
   }
   
   /**
    * Decodes an ACK vector.
    */
   public boolean[] decodeACK() throws Exception {
      if (ACKNum < 0) {
         throw new Exception("This packet is not an ACK/NAK packet.");
      }
      boolean[] ACKArray = new boolean[numAcksAllowed];
      for (int i = 0; i < numOfAcks; i++) {
         if (data[i] == True) {
            ACKArray[i] = true;
         }
         else {
            ACKArray[i] = false;
         }
      }
      return ACKArray;
   }
      
   /**
    * Generates a checksum for a byte array of the entire packet.
    * (For use from client side, maybe.)
    */   
   public static byte generateChecksum(byte[] packetBytes) {
      int sum = 0;
      for (int i = 1; i < packetBytes.length; i++) {
         sum += (int) (packetBytes[i] & 0xFF);
      }
      return (byte) (sum % 256);
   } 
   
   /**
    * Generates a checksum for a Packet object.
    */
   public byte generateChecksum() {
      int sum = 0;
      for (int i = 0; i < data.length; i++) {
         sum += (int) (data[i] & 0xFF);
      }
      sum += sequenceNum;
      sum += (byte) (lastIndex >> 8) + (byte) (lastIndex % 256);
      sum += (byte) ACKNum;
      sum += (byte) numOfAcks;
      return (byte) (sum % 256);
   }
   
   /**
    * Returns the packet data in a String object.
    */
   public String getDataString() {
      try {
         return new String(data, "UTF-8");
      }
      catch (Exception e) {
         System.out.println("Error in decoding packet data.");
         return "";
      }
   }
   
   /**
    * Returns a formatted String representing the Packet.
    */
   public String toString() {
      String result = "Sequence Number: " + sequenceNum;
      result += "\nChecksum: " + checksum;
      result += "\nLast Index: " + lastIndex;
      result += "\nData: " + getDataString();
      if (ACKNum >= 0) {
         result += "\nACK Number: " + ACKNum;
      }
      result += "\n\n";
      return result;
   }
   
   /**
    * Prints ACK info.
    */
   public void printACK() throws Exception {
      String result = "";
      for (int i = 0; i < numOfAcks; i++) {
         result += (ACKNum+i) + "    ";
      }
      result += "\n";
      boolean[] arr = this.decodeACK();
      for (int i = 0; i < numOfAcks; i++) {
         result += arr[i] + " ";
      }
      System.out.println(result);
   }
   
   /**
    * Returns true if this is the last packet, and false otherwise.
    */
   public boolean lastPacket() {
      if (lastIndex < headerSize + 3 && data[lastIndex - headerSize] == 0b0) {
         return true;
      }
      return false;
   }
   
   /**
    * Returns Packet's checksum.
    */
   public int getChecksum() {
      return checksum;
   }
   
   /**
    * Returns Packet's sequence number.
    */
   public int getSequenceNum() {
      return sequenceNum;
   }
   
   /**
    * Returns the index of the Packet's lastIndex field.
    */
   public int getLastIndex() {
      return lastIndex;
   }
   
   /**
    * Returns Packet's sequence number.
    */
   public byte[] getData() {
      return data;
   }
   
   /**
    * Returns Packet's ACK number.
    */
   public int getACKNum() {
      return ACKNum;
   }
   
   /**
    * Returns the number of ACKs in the packet.
    */
   public int getNumOfAcks() {
      return numOfAcks;
   }
   
   /**
    * Calculates and sets Packet's checksum field.
    */
   public void setChecksum() {
      checksum = generateChecksum();
   }
   
   /**
    * Sets Packet's sequence number.
    */
   public void setSequenceNumber(int newSeqNum) {
      sequenceNum = newSeqNum % maxSequenceNum;
   }
   
   /**
    * Sets Packet's ACK number.
    */
   public void setACKNum(int newACKNum) {
      ACKNum = newACKNum % maxSequenceNum;
   }
   
   /**
    * Sets Packet's lastIndex field.
    */
   public void setLastIndex(int newLastIndex) {
      lastIndex = newLastIndex;
   }
   
   /**
    * Sets Packet's data field.
    */
   public void setData(byte[] newData) {
      data = newData;
   }
   
   /**
    * Compares packets by sequence number.
    */
   public int compareTo(Packet packet) {
      return this.sequenceNum - packet.sequenceNum;
   }
}