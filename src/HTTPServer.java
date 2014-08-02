import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.json.JSONObject;

public class HTTPServer {
	
    private final int PORT = 8090;
    
    private ConcurrentMap<String, Future> connectionsMap = new ConcurrentHashMap<String, Future>();
    
    private ConcurrentMap<String, ExecutingThreads> connectionsMap2 = new ConcurrentHashMap<String, ExecutingThreads>();
    
    private ExecutorService executor = null;
    
    private void bootUp() {

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
                
                System.out.println(line);
                String[] requestHeaderLine = line.split(" ");
                System.out.println(requestHeaderLine[1]);
                
                String requestedMethod = requestHeaderLine[0];
                String urlRequested = requestHeaderLine[1];
                String[] params = requestHeaderLine[1].split("\\?");
                
           	
                if(params[0].equals("/sleep")){
                	
                	String[] requestParams = params[1].split("&");
                	String timeout = requestParams[0].split("=")[1];
                	String connid = requestParams[1].split("=")[1];
                	
                	Future future = executor.submit(new MyCallable(requestSocket, requestedMethod, urlRequested, timeout, connid));
                	connectionsMap2.put(connid, new ExecutingThreads(future, new Date(), Long.parseLong(timeout)));
                	connectionsMap.put(connid, future);
                	System.out.println(connectionsMap);
                	
                } else if(params[0].equals("/kill")){
                	
                	line = "";
                	int postDataLength = -1;
                	String postDataContent = "";
                	
                	while ((line = in.readLine()) != null && (line.length() != 0)) {
                	    System.out.println(line);
                	    if (line.indexOf("Content-Length:") > -1) {
                	        postDataLength = new Integer(line.substring(line.indexOf("Content-Length:") + 16, line.length())).intValue();
                	    }
                	}

                	if (postDataLength > 0) {
                		char[] charArray = new char[postDataLength];
                        in.read(charArray, 0, postDataLength);
                        postDataContent = new String(charArray);
                		System.out.println(postDataContent);
                	}
                	
                	String[] postArray = postDataContent.split("=");
                	Future future = connectionsMap.get(postArray[1]);
                	
                	JSONObject output = new JSONObject();
                	
                	if(future!=null){
                		 future.cancel(true);
	   	            	 output.put("stat", "OK");
	   	                 out.println("HTTP/1.0 200 OK");
                	}else{
                		 output.put("stat", "CONNID NOT FOUND");
	   	                 out.println("HTTP/1.0 400 Bad Request");
                	}
                	
	                 out.println("Content-Type: application/json; charset=utf-8");
	                 out.println("Server: HTTPServer");
	                 out.println("");
	                 out.println(output.toString());
	                 out.println();
                	
	                 out.close();
                	in.close();
                	requestSocket.close();
                } else if (params[0].equals("/server-status")){
                	
                	 out.println("HTTP/1.0 200 OK");
                	 out.println("Content-Type: text/html; charset=utf-8");
	                 out.println("Server: HTTPServer");
	                 out.println("");
	                 out.println("<html>");
	                 out.println("<head>");
	                 out.println("<meta http-equiv=\"refresh\" content=\"1;\">");
	                 out.println("</head>");
	                 out.println("<body>");
	                 out.println("<table border=1>");
	                 out.println("<tr><th>Connections Open</th><th>Time left to sleep</th></tr>");
	                 
                	for(Entry<String, ExecutingThreads> t : connectionsMap2.entrySet()){
                		if(!t.getValue().getFuture().isCancelled() && !t.getValue().getFuture().isDone()){
                			Date start = t.getValue().getStart();
                			Date end = new Date();
                			long seconds = (end.getTime()-start.getTime())/1000;
                			long timeLeftInSecods = t.getValue().getTimeoutInSeconds() - seconds;
                			out.println("<tr><td>"+ t.getKey() +"</td><td>"+ timeLeftInSecods +"</td></tr>");
                		}
                	}
                	
                	out.println("</table>");
                	out.println("</body>");
                	out.println("</html>");
	                out.println();
               	
	            out.close();
               	in.close();
               	requestSocket.close();
                }
            }
            
        } catch (Exception e) {
        }

	}
    
    public HTTPServer() {
    	executor = Executors.newFixedThreadPool(10);
		connectionsMap = new ConcurrentHashMap<String, Future>();
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
	public static void main(String[] args) {
    	
    	HTTPServer httpServer = new HTTPServer();
    	
    	httpServer.bootUp();
        
    }
	
}

class ExecutingThreads {
	
	private Date start;
	private Future future;
	private long timeoutInSeconds;
	
	public ExecutingThreads(Future future, Date date, long timeoutInSeconds) {
		this.start = date;
		this.future = future;
		this.timeoutInSeconds = timeoutInSeconds;
	}

	public Date getStart() {
		return start;
	}

	public void setStart(Date start) {
		this.start = start;
	}

	public Future getFuture() {
		return future;
	}

	public void setFuture(Future future) {
		this.future = future;
	}

	public long getTimeoutInSeconds() {
		return timeoutInSeconds;
	}

	public void setTimeoutInSeconds(long timeoutInSeconds) {
		this.timeoutInSeconds = timeoutInSeconds;
	}

	
}

class MyCallable implements Callable{

	private Socket socket;
	private String timeout;
	private JSONObject output;
	
	public MyCallable(Socket requestSocket, String requestedMethod, String urlRequested, String timeout, String connid) {
		this.socket = requestSocket;
		this.timeout = timeout;
	}

	@Override
	public Boolean call() throws Exception {
		
		PrintWriter out = null;
		InputStream is;
		BufferedReader bReader = null;
		
        try {
        	
        	output = new JSONObject();
        	
        	out = new PrintWriter(socket.getOutputStream());		
        	is = socket.getInputStream();
            bReader = new BufferedReader(new InputStreamReader(is));
            
            Thread.sleep(Integer.parseInt(timeout)*1000);
            
            
            output.put("stat", "OK");
            
            out.println("HTTP/1.0 200 OK");
            out.println("Content-Type: application/json; charset=utf-8");
            out.println("Server: HTTPServer");
            out.println("");
            out.println(output.toString());
            out.println();
            
            return true;
            
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
        	
        	output.put("stat", "KILLED");
        	
        	out.println("HTTP/1.0 200 OK");
            out.println("Content-Type: application/json; charset=utf-8");
            out.println("Server: HTTPServer");
            out.println("");
            out.println(output.toString());
            out.println();
            
        	return false;
        	
		}finally{
		     	out.close();
	            bReader.close();
	            socket.close();
		}
        
        return false;
    }
	
}

