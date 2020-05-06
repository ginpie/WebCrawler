import java.io.*;
import java.net.*;

public class Server {
   public static void main(String[] args) throws IOException{
      ServerSocket sSocket = new ServerSocket(6331);
      Socket socket = sSocket.accept();

      System.out.println("client connected");

      InputStreamReader in = new InputStreamReader(socket.getInputStream());
      BufferedReader buf = new BufferedReader(in);

      String str = buf.readLine();
      System.out.println("Client: " + str);

      OutputStreamWriter out = new OutputStreamWriter(socket.getOutputStream());
      out.write("Yes");
      out.flush();
   }
}