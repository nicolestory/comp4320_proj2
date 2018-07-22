import java.util.Arrays;

class Packet {

   private byte checksum;           // Index 0
   private int sequenceNum;         // Index 1-2
   private int lastIndex;           // Index 3-4
   private byte[] data;
   public static final int maxPacketSize = 512;
   public static final int headerSize = 5;
   public static final int maxDataBytes = maxPacketSize - headerSize;
   
   /**
    * From byte array (data AND headers) to Packet object.
    * For use from the client side, probably.
    */
   public Packet(byte[] packetBytes) throws Exception {
      if (packetBytes.length > maxPacketSize) {
         throw new Exception("Listen here friendo");
      }
      // Assign header info to fields
      checksum = packetBytes[0];
      sequenceNum = (packetBytes[1] << 8) | (packetBytes[2] & 0xff);
      lastIndex = (packetBytes[3] << 8) | (packetBytes[4] & 0xff);
      data = Arrays.copyOfRange(packetBytes, headerSize, packetBytes.length);
   }
   
   /**
    * From byte array (data ONLY) and sequence number to Packet object
    */
   public Packet(int seqNum, byte[] packetData) throws Exception {
      if (packetData.length > maxDataBytes) {
         throw new Exception("boi if you DONT");
      }
      sequenceNum = seqNum;
      lastIndex = packetData.length + headerSize - 1;
      data = packetData;
      checksum = generateChecksum();
   }
   
   /**
    * Initializes a Packet object with only a Sequence Number.
    * Please don't use this on accident.
    * Only on purpose, pls.
    */
   public Packet(int sn) {
      sequenceNum = sn;
   }
   
   /**
    * Converts the Packet data into a byte array, ready to be sent.
    */
   public byte[] toByteArray() {
      byte[] packetBytes = new byte[lastIndex + 1];
      packetBytes[0] = checksum;
      packetBytes[1] = (byte) (sequenceNum >> 8);
      packetBytes[2] = (byte) (sequenceNum % 256);
      packetBytes[3] = (byte) (lastIndex >> 8);
      packetBytes[4] = (byte) (lastIndex % 256);
      for (int i = 0; i < data.length; i++) {
         packetBytes[headerSize + i] = data[i];
      }
      return packetBytes;
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
      sum += (byte) (sequenceNum >> 8) + (byte) (sequenceNum % 256);
      sum += (byte) (lastIndex >> 8) + (byte) (lastIndex % 256);
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
         System.out.println("Oh no.");
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
      result += "\n\n";
      return result;
   }
   
   /**
    * Returns true if this is the last packet, and false otherwise.
    */
   public boolean lastPacket() {
      if (data[lastIndex - headerSize] == 0b0) {
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
    * Calculates and sets Packet's checksum field.
    */
   public void setChecksum() {
      checksum = generateChecksum();
   }
   
   /**
    * Sets Packet's sequence number.
    */
   public void setSequenceNumber(int newSeqNum) {
      sequenceNum = newSeqNum;
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