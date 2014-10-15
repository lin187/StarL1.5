/**
 * Com is the communication method for traffic light algorithm
 * it keeps listening for acknowledge from destination until k tries
 * 
 * 
 * @author Yixiao Lin
 * @version 1.0
 */
import java.io.IOException;
import java.net.*;


public class Com {

	public long timer;
	public boolean acked;
	public int tries;
	int portNum;
	
	//constructor for the communication class
	public Com(){
		timer = 0;
		acked = false;
		tries = 1;
		portNum = 6653;
	}
	
	public void RUniCast(String message, String dest, int k, int RTT){
		start_receive(portNum);
		//start thread On_recieve
		send(message, dest);
		//start thread waitOver
		waitOver(message, dest, k, RTT);
	}
	
	void waitOver(final String message, final String dest, final int k, final int RTT) {
		//within time bound RTT*k, try send message k times if not received acknoledegment 
		(new Thread() { 
			public void run() 
			{
				timer = System.currentTimeMillis();
				while(true) 
				{ 
					long dtime = System.currentTimeMillis() - timer;
					if(dtime >= RTT){
						if(tries <= k && !acked){
							tries ++;
							timer = System.currentTimeMillis();
							send(message, dest);
						}
						else
							break;
						
					} else
						//optimazation
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
				}
			}	
		}).start();
	} /* close waitOver */
	
	void start_receive(final int port) {
		(new Thread() { 
			public void run() 
			{ 
				DatagramSocket self = null;
				//max byte length, can be increased
				byte[] receiveData = new byte[128]; 
				boolean listening = true;
				while(listening) 
				{ 
					if(self == null){
						try { 
							self = new DatagramSocket(port); 
						} catch (SocketException e) { 
							e.printStackTrace(); 
						}
					}
					else{
						// Listen on the receiver socket 
						DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length); 
						try { 
							self.receive(receivePacket); 
						} catch (IOException e) { e.printStackTrace(); } 
						// Only construct the string from the available data 
						String in_message = new String(receivePacket.getData(), 0, receivePacket.getLength());
						On_recieve(in_message);
					}
				}
			}	
		}).start();
	}
	
	private void On_recieve(String in_message) {
		if(in_message == "ack")
			acked =true;
		else{
			Rdeliver(in_message);
			//send acknoledgemeng
			send(in_message, "ip");
		}
	}
	  
	private void Rdeliver(String in_message) {
		System.out.println(in_message);
	}

	void send(String msg, String dest) { 
			  
		// create a client Socket for sending message 
		DatagramSocket clientSocket = null; 
		try { 
			clientSocket = new DatagramSocket(); 
		} catch (SocketException e) { 
			e.printStackTrace(); 
		} 
		// get the IP address of the destination process 
		InetAddress IPAddress = null; 
		try { 
			IPAddress = InetAddress.getByName(dest); 
		} catch (UnknownHostException e) { 
			e.printStackTrace(); 
		}         
		// set up the message pack using UDP protocol
		byte[] sendData = new byte[128]; 
		sendData = msg.toString().getBytes();
		DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, portNum); 
		try {
			clientSocket.send(sendPacket); 
		} catch (IOException e) { 
			e.printStackTrace(); 
		}
		clientSocket.close(); 
	} /* close send thread */
}
