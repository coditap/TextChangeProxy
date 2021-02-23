import java.io.BufferedReader; //[4]
import java.io.InputStreamReader; //[4]'
import java.io.PrintWriter; //[3]
import java.net.ServerSocket; //[1]
import java.net.Socket; //[2]

public class EchoServer {

	public static void main(String[] args) throws Exception {
		
		int portNumber = 5433; //if i not terminate after use i getting denny
		String serverIP = "localhost";
		
			try {
				ServerSocket serverSocket = new ServerSocket(portNumber); //[1] SS object import java.net.ServerSocket;
				System.out.println("Server started at port "+portNumber+" IPs: "+serverIP);
				Socket clientSocket = serverSocket.accept(); //client accept connection form the server [2] import java.net.Socket; server waiting for client response
				System.out.println("Got connection from "+clientSocket.getInetAddress()+ " port number "+clientSocket.getPort()); //getInetAddress gives Ip address form the client on client port no.
				
				PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true); //[3] import java.io.PrintWriter; output stream from the socket
				
				BufferedReader in = new BufferedReader( new InputStreamReader(clientSocket.getInputStream())); //[4] import java.io.BufferedReader; Reading what the client has to say to server
				
				String inputLine;
				while ((inputLine = in.readLine()) != null){
					System.out.println("Got message "+inputLine+" from client");
					out.println(inputLine);
				}
			} catch (Exception e) {
				System.out.println("Something wrong in EchoServer class");
				e.printStackTrace();
			}
	}
}