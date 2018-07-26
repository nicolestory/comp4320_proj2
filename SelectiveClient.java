import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;

/**
	 * @param local_Port - Port on client Side
	 * @param server_IP - The Server IP
	 * @param remote_Port - Port on Server side
	 * @param grem is the Gremlin Used inside SR
	 */

   public SelectiveRepeater(int local_Port,InetAddress server_IP, int remote_Port, Gremlin grem, boolean traceOPT){
      gremlin = grem;
   
      for(int i =0; i<acknowledgmentBuffer.length; i++){
         acknowledgmentBuffer[i] = "N"+i;
      }
      localPortNumber = local_Port;
      serverPortNumber = remote_Port;
      serverIP = server_IP;
      try {
         clientSocket = new DatagramSocket(localPortNumber);
      } catch (SocketException e) {
         e.printStackTrace();
      }
      trace = traceOPT;
   }
