package multicast3tehtv2;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;

public class Server{

	public static void main(String[] args) {
		try {
			MulticastSocket mcSocket = new MulticastSocket(6666); // Luodaan multicast-soketti
			InetAddress osoite = InetAddress.getByName("239.0.0.1"); // Määritellään multicast IP-osoite
			byte[] data = new byte[256]; 
			DatagramPacket dgramPacket = new DatagramPacket(data, data.length);
			mcSocket.joinGroup(osoite); // Liitytään multicast ryhmään
			
			while(true) {
				mcSocket.receive(dgramPacket);			
				byte[] rcv = dgramPacket.getData();
				int versiotemp = rcv[0] >> 4;
				int viestitemp = rcv[0] & 0x0f;
				if(versiotemp == 1 | versiotemp == 2) {
					if(viestitemp >= 1 && viestitemp <= 3) System.out.println(new String(rcv, 0, dgramPacket.getLength()));
					else rcv = new byte[256];
				}
				if(versiotemp > 2) {
					if(viestitemp >= 1 && viestitemp <= 4) System.out.println(new String(rcv, 0, dgramPacket.getLength()));
					else rcv = new byte[256];
				}
				if(rcv.toString().contains("/leave")) break;
			}
			
			mcSocket.leaveGroup(osoite);
			mcSocket.close();
			
		}
		catch(Exception e){
			System.out.println(e.getMessage());
		}

	}

}
