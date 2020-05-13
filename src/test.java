import java.io.*;
import java.net.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.regex.*;
import java.util.stream.Collectors;
import java.util.Date;

public class test {
   final private static String host = "comp3310.ddns.net";
   final private static int port = 7880;
   final private static int WAIT_TIME = 0;

   public static void main(String[] args) throws ParseException, IOException, InterruptedException {
      // String str = "<a href=\"B/20.html\">Here's a page</a>  <a href=\"A/20.html\">Here's a page</a>  <a href=\"C/20.html\">Here's a page</a>";
      // String str1 = "HTTP/1.1 200 OKDate: Fri, 08 May 2020 09:06:34 GMTServer: Apache/2.4.29 (Ubuntu)Last-Modified: Sat, 04 May 2019 10:50:04 GMTETag: \"b26-5880da133a5ce\"Accept-Ranges: bytesContent-Length: 2854; Vary: Accept-Encoding";
      // Pattern p = Pattern.compile("Content-Length: (.+?); ", Pattern.DOTALL);
      // Matcher m = p.matcher(str1);
      // while (m.find()) {
      //    String codeGroup = m.group(1);
      //    // System.out.format("'%s'\n", codeGroup);
      //    System.out.println("code Group: "+Integer.parseInt(codeGroup));
      //    String s = "comp3310.ddns.net/7880";
      //    System.out.println(s.length());
      // }

      // String dt = "Sat, 04 May 2019 08:21:07 GMT";
      // String dt1 = "Sat, 04 May 2019 08:21:07 GMT";
      // System.out.println("Last-modified time found: " + dt);
      // // conver string to datetime e.g. Sat, 04 May 2019 08:21:07
      // String pattern = "E, dd MMM yyyy HH:mm:ss z";
      // SimpleDateFormat df = new SimpleDateFormat(pattern, Locale.ENGLISH);
      // df.setLenient(false);
      // Date date = df.parse(dt);
      
      // SimpleDateFormat df2 = new SimpleDateFormat(pattern, Locale.ENGLISH);
      // df2.setTimeZone(TimeZone.getTimeZone("GMT"));
      // String dates = df2.format(date);
      
      // Date date1 = df.parse(dt1);
      // System.out.println(dates);
      // System.out.println(date.compareTo(date1));

      // try {Socket s = new Socket("www.canberratimes.com.au", 80);
      // } catch (UnknownHostException e) {
      //    System.out.println("Uknown Host!!");
      // }  
      //    System.out.println("yes");
      
      
      // BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
      // bw.write("GET " + "/" + " HTTP/1.0\r\n\r\n");
      // // bw.write("Host: " + host);
      // bw.flush();

      // // start read
      // BufferedReader bf = new BufferedReader(new InputStreamReader(s.getInputStream()));
      // String t;
      // while((t = bf.readLine()) != null) System.out.println(t);

         String url = "comp3310.ddns.net:7880/A/15.html";
         try {
            // Connect to host:port via TCP
            Socket s = new Socket(host, port);
            // send GET request
            url = uTrim(url);
            String path = url.substring(22);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
            bw.write("GET " + "/" + path + " HTTP/1.0\r\n\r\n");
            bw.flush();

            // start read
            BufferedReader bf = new BufferedReader(new InputStreamReader(s.getInputStream()));
            // Convert the buffered reader to string
            String content = bf.lines().collect(Collectors.joining(";\n "));
            bf.close();
            System.out.println("URL contents retrieved: " + url);
            s.close();
            System.out.println(content);
            TimeUnit.SECONDS.sleep(WAIT_TIME);

      } catch (IOException e) {
            e.printStackTrace();
      }
      
   }

   // URL trim
   public static String uTrim(String url){
      // remove "http://", add host and port
      if (url.contains("http://")) url = url.substring(7);
      else if (url.contains("https://")) url = url.substring(7);
      else {
          if (!url.contains(host)){
              if (url.substring(0,1).equals("/")){
                  url = host + ":" + port + url;
              } else url = host + ":" + port + "/" + url;
          }
      }
      if (url.substring(url.length()-1).equals("/")) url = url.substring(0,url.length()-1);
      
      return url;
  } 
}