
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class MyHTTPServer {
	
    private static final int PORT = 8090;
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
	public static void main(String[] args) {
    	
        try {
        	
            ServerSocket server = new ServerSocket(PORT);
            
            Socket requestSocket;
            
            System.out.println("MiniServer active " + PORT);
            
            while (true) {
            	
            	requestSocket = server.accept(); 
            	PrintWriter out = new PrintWriter(requestSocket.getOutputStream());		
            	InputStream is = requestSocket.getInputStream();
                BufferedReader in = new BufferedReader(new InputStreamReader(is));
                String line = in.readLine();
                
	                 
            out.println("HTTP/1.0 200 OK");
            out.println("Content-Type: application/json; charset=utf-8");
            out.println("Server: HTTPServer");
            out.println("");
            out.println("dvsvdsvsf");
            out.println();
       	
            out.close();
       	in.close();
       	requestSocket.close();
       }
} catch (Exception e) {
}}}