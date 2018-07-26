import java.util.ArrayList;
import java.util.TimerTask;
  
class PacketTimer extends TimerTask {
   public Packet packet;
      
   public void addPacket(Packet newPacket) {
      packet = newPacket;
   }
      
   public void run() {
      System.out.println("Cool.");
      ArrayList<Packet> packetList = new ArrayList<Packet>();
      packetList.add(packet);
      //UDPServer.send(packetList);
      cancel();
   }
}