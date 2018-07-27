import java.util.TimerTask;
import java.util.Timer;
  
class PacketTimer extends TimerTask {
   public static long end;
   public static boolean timedOut;

   public PacketTimer(long _timeout) {
	end = System.currentTimeMillis() + _timeout;
	timedOut = false;
   }
   
   public void run() {
	if (System.currentTimeMillis() >= end) {
		timedOut = true;
		cancel();
	}
   }

   /*public static void main(String[] args) {
	PacketTimer p = new PacketTimer(5000);
	Timer t = new Timer();
	
	t.schedule(p, 0, 1);
	while (System.currentTimeMillis() < end + 100) {}
	t.cancel();
   }*/
}
