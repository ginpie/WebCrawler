import java.io.*;
import java.net.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.regex.*;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;


public class WebCrawler {
    // Fields to collect URL information
    private static HashSet<String> URLs = new HashSet<>();
    private static HashSet<String> explored = new HashSet<>();
    private static HashSet<String> unexplored = new HashSet<>();
    private static HashMap<String, Date> times = new HashMap<>();
    private static HashMap<String, Integer> sizes = new HashMap<>();
    private static HashSet<String> onSiteURLs = new HashSet<>();
    private static HashSet<String> invalidURLs = new HashSet<>();
    private static HashMap<String, String> redirect = new HashMap<>();
    private static HashSet<String> offSiteRedirectURLs = new HashSet<>();
    private static HashSet<String> offSite_Valid = new HashSet<>();
    private static HashSet<String> offSite_Invalid = new HashSet<>();
    private static HashSet<String> nonHtml = new HashSet<>();

    private static String OLDEST_PAGE = null;
    private static String NEWEST_PAGE = null;
    private static String SMALLEST_PAGE = null;
    private static String LARGEST_PAGE = null;
    
    final private static String host = "comp3310.ddns.net";
    final private static int port = 7880;

    enum Type {ON_SITE, INVALID, ON_SITE_REDIRECT, OFF_SITE}

    public static void main(String[] args) throws InterruptedException, ParseException, IOException {
        System.out.println("\n************ Web Crawler start scraping ************");
        goThrough();
        // get results
        System.out.println("\n************ Here is the report ************");
        System.out.println("Number of distinct URLs (200): " + URLs.size());
        System.out.println("Number of html pages: " + onSiteURLs.size());
        System.out.println("Number of non html objects: " + nonHtml.size());
        System.out.println("Smallest page: " + SMALLEST_PAGE + ", size: " + sizes.get(SMALLEST_PAGE));
        System.out.println("Largest page: " + LARGEST_PAGE + ", size: " + sizes.get(LARGEST_PAGE));
        System.out.println("Oldest page: " + OLDEST_PAGE+ ", modified time: " + times.get(OLDEST_PAGE));
        System.out.println("Newest page: " + NEWEST_PAGE+ ", modified time: " + times.get(NEWEST_PAGE));
        System.out.println("Number of invalid URLs (404): " + invalidURLs.size());
        System.out.println("List of invalid URLs (404): ");
        for (String s : invalidURLs) {
            System.out.println("    " + s);
        }
        System.out.println("Number of redirected URLs (30X): " + redirect.size());
        System.out.println("Redirect map: \n");
        for (Map.Entry<String, String> entry : redirect.entrySet()) {
            System.out.println("source: " + entry.getKey() + ", redirect to: " + entry.getValue());
        }
        System.out.println("Number of off-site valid URLs: " + offSite_Valid.size());
        System.out.println("Number of off-site invalid URLs: " + offSite_Invalid.size());
    }

    /* 
        Go through all pages in the site, get
        1. all distinct URLs in the pages
        2. all on-site URLs (200)
        3. all page HTML contents and their size
        4. all page size in bytes
        5. all non-html objects
        6. all invalid URLs (404)
        7. all redirect URLs (30X) and URLs they redirect to 
        8. all off-site URLs (30X, ref) and if it is invalid
    */ 
    public static void goThrough() throws InterruptedException, ParseException, IOException {
        String main = host + ":" + port;
        explored.add(main);
        updateURL(main, getURL(main));
        
        while (!unexplored.isEmpty()) {
            for (String url : unexplored){
                if (!explored.contains(url)){
                String content = getURL(url);
                updateURL(url, content);
                explored.add(url);
                unexplored.remove(url);
                } else {
                    unexplored.remove(url);
                }
            }
        }
        return;
    }

    // get the html contents of an URL
    public static String getURL(String url) throws InterruptedException {
        
        try {
            // Connect to host:port via TCP
            Socket s = new Socket(host, port);
            // send GET request
            String path = url.substring(22);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
            bw.write("GET " + "/" + path + " HTTP/1.0\r\n\r\n");
            bw.flush();

            // start read
            BufferedReader bf = new BufferedReader(new InputStreamReader(s.getInputStream()));
            // Convert the buffered reader to string
            String content = bf.lines().collect(Collectors.joining("; "));
            bf.close();
            // System.out.println(content);
            System.out.println("URL contents retrieved: " + url);
            s.close();
        
            TimeUnit.SECONDS.sleep(2);
            return content;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    // update the page size, page modified time, non html objects, links in the page, URL types
    public static void updateURL(String url, String content) throws InterruptedException, ParseException, IOException {
        Type type = parseCode(url, content);
        System.out.println(type);
        if (type == null) return;
        switch (type) {
            case ON_SITE: {
                // update type
                onSiteURLs.add(url);
                // update size
                int size = getPageSize(content);
                sizes.put(url, size);
                updateSize(url, size);
                // update links in the page
                for (String l : getLinks(content)){
                    if (!explored.contains(l)) unexplored.add(l);
                }
                // update modified time
                Date dt = getTime(content);
                updateTime(url, dt);
                // update non html object
                getNonHtml(content);
                break;}
            case INVALID: {
                invalidURLs.add(url); 
                break;}
            case ON_SITE_REDIRECT: {
                String redir = getLocation(content);
                redirect.put(url, redir); 
                if (!explored.contains(redir)) unexplored.add(redir);
                break;}
            case OFF_SITE: {
                offSiteRedirectURLs.add(url);
                String redir = getLocation(url);
                redirect.put(url, redir); 
                if (isValid(url)) offSite_Valid.add(redir);
                else offSite_Invalid.add(redir);
                break;}
        }
        return;
    }

    // parse http response code
    public static Type parseCode(String url, String content) {
        // check if it is valid, invalid, on-site redirect, or off-site
        Pattern p = Pattern.compile("HTTP/1.1 (.+?) ", Pattern.DOTALL);
        Matcher m = p.matcher(content);
        String code = null;
        while (m.find()) {
            code = m.group(1);
        }
        System.out.println("Code: " + code);
        if (code.equals("200")) return Type.ON_SITE;
        if (code.equals("404")) return Type.INVALID;
        if (code.equals("400")) return Type.INVALID;
        if (code.substring(0,2).equals("30")) {
            if (getLocation(content).contains(host)){
                return Type.ON_SITE_REDIRECT;
            } else {
                return Type.OFF_SITE;
            }
        }
        if (url.contains("http")) {
            return Type.OFF_SITE;
        }
        
        return null;
    }

    // get redirect location
    public static String getLocation(String content) {
        Pattern p = Pattern.compile("Location: (.+?); ", Pattern.DOTALL);
        Matcher m = p.matcher(content);
        while (m.find()) {
            String loc = m.group(1);
            return loc;
        }
        return null;
    }

    // add all links exist in a page to the links set
    public static ArrayList<String> getLinks(String content){
        
        Pattern p = Pattern.compile("href=\"(.+?)\">", Pattern.DOTALL);
        Matcher m = p.matcher(content);

        ArrayList<String> l = new ArrayList<>();
        while (m.find()) {
            String link;
            if (m.group(1).contains("http")){
                link = m.group(1);
            } else {
                link = host + ":" + port + m.group(1);
            }
            
            System.out.println("link found: "+link);
            URLs.add(link);
            unexplored.add(link);
        }
        return l;
    }

    // check if a remote web server is valid
    public static boolean isValid(String url) throws IOException {
        try {
            Socket stest = new Socket(url, 80);
        } catch (UnknownHostException e) {
            return false;
        }
        return true;
    }


    // get page size
    public static int getPageSize(String content){
        Pattern p = Pattern.compile("Content-Length: (.+?); ", Pattern.DOTALL);
        Matcher m = p.matcher(content);
        while (m.find()) {
            int size = Integer.parseInt(m.group(1));
            return size;
        }
        return 0;
    }

    // update max and min size
    public static void updateSize(String url, int size){
        if (SMALLEST_PAGE == null || size < sizes.get(SMALLEST_PAGE)) SMALLEST_PAGE = url;
        if (LARGEST_PAGE == null || size > sizes.get(LARGEST_PAGE)) LARGEST_PAGE = url;
    }

    // get non html objects in a page
    public static void getNonHtml(String content){
        Pattern p = Pattern.compile("<img src=\"(.+?)>; ", Pattern.DOTALL);
        Matcher m = p.matcher(content);
        while (m.find()) {
            String obj = m.group(1);
            nonHtml.add(obj);
        }
    }

    // get time stamp of a page
    public static Date getTime(String content) throws ParseException{
            
        Pattern p = Pattern.compile("Last-Modified: (.+?); ", Pattern.DOTALL);
        Matcher m = p.matcher(content);

        while (m.find()) {
            String dt = m.group(1).trim();
            // conver string to datetime e.g. Sat, 04 May 2019 08:21:07
            String pattern = "E, dd MMM yyyy HH:mm:ss z";
            SimpleDateFormat df = new SimpleDateFormat(pattern, Locale.ENGLISH);
            df.setLenient(false);
            Date date = df.parse(dt);
            return date;
        }
        return null;
    }

    //update the timestamps
    public static void updateTime(String url, Date date) {
        // put the datetime in the set
        times.put(url, date);
        // update the oldest and newest page if possible
        if (OLDEST_PAGE == null || date.compareTo(times.get(OLDEST_PAGE)) == -1) OLDEST_PAGE = url;
        if (NEWEST_PAGE == null || date.compareTo(times.get(NEWEST_PAGE)) == 1) NEWEST_PAGE = url;
    }


    
}
