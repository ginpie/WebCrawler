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
import java.util.regex.*;
import java.util.stream.Collectors;
import java.util.Date;

public class test {
   public static void main(String[] args) throws ParseException, IOException {
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

      try {Socket s = new Socket("www.canberratimes.com.au", 80);
      } catch (UnknownHostException e) {
         System.out.println("Uknown Host!!");
      }  
         System.out.println("yes");
      
      
      // BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
      // bw.write("GET " + "/" + " HTTP/1.0\r\n\r\n");
      // // bw.write("Host: " + host);
      // bw.flush();

      // // start read
      // BufferedReader bf = new BufferedReader(new InputStreamReader(s.getInputStream()));
      // String t;
      // while((t = bf.readLine()) != null) System.out.println(t);
   }
}