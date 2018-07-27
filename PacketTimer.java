import java.util.ArrayList;
import java.util.TimerTask;
import java.util.Timer;
  
class PacketTimer extends TimerTask {
   public Packet packet;
   public Timer timer;
   
   public PacketTimer(Packet pkt, Timer tmr) {
      packet = pkt;
      timer = tmr;
   }
   
   public void run() {
      ArrayList<Packet> pkt = new ArrayList<Packet>();
      pkt.add(packet);
      UDPServer.send(pkt);
      System.out.println("Timed Out!");
      cancel(); 
      timer.schedule(new PacketTimer(packet, timer), 40);
   }
}
