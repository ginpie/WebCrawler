import java.io.*;
import java.net.*;

public class Client {
   public static void main(String[] args) throws IOException{
      
      Socket socket = new Socket("localhost", 6331);

      System.out.println("server connected");

      OutputStreamWriter out = new OutputStreamWriter(socket.getOutputStream());
      out.write("are you there?");
      out.flush();

      InputStreamReader in = new InputStreamReader(socket.getInputStream());
      BufferedReader buf = new BufferedReader(in);

      String str = buf.readLine();
      System.out.println("Server: " + str);
      
   }
}