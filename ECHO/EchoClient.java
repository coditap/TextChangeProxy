import java.io.BufferedReader;//[3]
import java.io.InputStreamReader;//[3]'
import java.io.PrintWriter;//[2]
import java.net.Socket;//[1]


public class EchoClient {

	public static void main(String[] args) throws Exception{
		
		String hostName = "localhost";
		int portNumber = 5433;
		
		Socket echoSocket = new Socket(hostName, portNumber); //import java.net.Socket; [1]
		System.out.println("Client: Conected to socket"+echoSocket);
		
		
		PrintWriter out = new PrintWriter(echoSocket.getOutputStream()); //import java.io.PrintWriter;//[2]; Writing to the server from the client
		System.out.println("Client: Type what do you want to pas to the server: ");
		
		BufferedReader in = new BufferedReader(new InputStreamReader(echoSocket.getInputStream())); //import java.io.BufferedReader;//[3]; import java.io.InputStreamReader;//[3]'; Getting messages from the server, waiting for response
		
		BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in)); //buffering data from the client to pass it to the server
		
		String userInput;
		while ((userInput = stdIn.readLine()) != null){
			out.println(userInput); //message is send to the output and again
			System.out.println("Client: Wthat do you whan to send");
			System.out.println("echo: " + in.readLine());
		}
	}
	
}
