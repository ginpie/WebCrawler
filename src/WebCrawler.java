import java.io.*;
import java.net.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.regex.*;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;


public class WebCrawler {
    // Fields to collect URL information
    private static HashSet<String> URLs = new HashSet<>();
    private static HashSet<String> explored = new HashSet<>();
    private static HashSet<String> unexplored = new HashSet<>();
    private static HashMap<String, Date> times = new HashMap<>();
    private static HashMap<String, Integer> sizes = new HashMap<>();
    private static HashSet<String> onSite = new HashSet<>();
    private static HashSet<String> onSiteValid = new HashSet<>();
    private static HashSet<String> invalid = new HashSet<>();
    private static HashMap<String, String> redirect = new HashMap<>();
    private static HashSet<String> offSite_Valid = new HashSet<>();
    private static HashSet<String> offSite_Invalid = new HashSet<>();
    private static HashSet<String> nonHtml = new HashSet<>();

    private static String OLDEST_PAGE = null;
    private static String NEWEST_PAGE = null;
    private static String SMALLEST_PAGE = null;
    private static String LARGEST_PAGE = null;
    
    final private static String host = "comp3310.ddns.net";
    final private static int port = 7880;
    final private static int WAIT_TIME = 0;
    final private static String SCHEME = "http://";

    enum Type {ON_SITE_VALID, INVALID, ON_SITE_REDIRECT, OFF_SITE, OFF_SITE_REDIRECT}

    public static void main(String[] args) throws InterruptedException, ParseException, IOException {
        System.out.println("\n************ Web Crawler start scraping ************");
        goThrough();
        // get results
        System.out.println("\n************ Here is the report ************");
        System.out.println("Number of distinct URLs (both valid and invalid): " + URLs.size());
        System.out.println("Number of html pages (200 OK): " + onSiteValid.size());
        System.out.println("Number of non html objects (img, audio, etc): " + nonHtml.size());

        System.out.println("Smallest page: " + SMALLEST_PAGE + ", size: " + sizes.get(SMALLEST_PAGE));
        System.out.println("Largest page: " + LARGEST_PAGE + ", size: " + sizes.get(LARGEST_PAGE));
        
        String pattern = "E, dd MMM yyyy HH:mm:ss z";
        SimpleDateFormat df = new SimpleDateFormat(pattern, Locale.ENGLISH);
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
        String od = df.format(times.get(OLDEST_PAGE));
        String nd = df.format(times.get(NEWEST_PAGE));
        System.out.println("Oldest page: " + OLDEST_PAGE+ ", modified time: " + od);
        System.out.println("Newest page: " + NEWEST_PAGE+ ", modified time: " + nd);
        
        System.out.println("Number of invalid URLs (404): " + invalid.size());
        System.out.println("List of on site invalid URLs (404): ");
        for (String s : invalid) {
            System.out.println("        " + s);
        }
        System.out.println("Number of redirected URLs (30X): " + redirect.size());
        System.out.println("Redirect map: ");
        for (Map.Entry<String, String> entry : redirect.entrySet()) {
            System.out.println("        source: " + entry.getKey() + ",     redirect to: " + entry.getValue());
        }
        System.out.println("Number of off-site valid URLs: " + offSite_Valid.size());
        for (String s : offSite_Valid) {
            System.out.println("        " + s);
        }
        System.out.println("Number of off-site invalid URLs: " + offSite_Invalid.size());
        for (String s : offSite_Invalid) {
            System.out.println("        " + s);
        }
    }

    /* 
        Go through all pages in the site, get
        1. all distinct URLs in the pages (all valid, invalid, or objects)
        2. all on-site pages (200)
        3. all page HTML contents
        4. all page size in bytes
        5. all non-html objects
        6. all invalid URLs (404)
        7. all redirect URLs (30X) and URLs they redirect to 
        8. all off-site URLs (30X, ref) and if it is invalid
    */ 
    public static void goThrough() throws InterruptedException, ParseException, IOException {
        String main = host + ":" + port;
        explored.add(main);
        URLs.add(main);
        updateURL(main, getURL(main));
        // iterate over the unexplored URLs
        while (!unexplored.isEmpty()) {
            // make a copy of set to avoid the ConcurrentModificationException
            HashSet<String> tmp = new HashSet<>();
            tmp.addAll(unexplored);
            Iterator<String> it = tmp.iterator();
            while (it.hasNext()){
                String url = it.next();
                if (!explored.contains(url)){
                String content = getURL(url);
                updateURL(url, content);
                explored.add(url);
                }
                unexplored.remove(url);
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
            String u = uTrim(url);
            String path = "";
            if (u.length() > host.length() + 1 + (port+"").length())
                path = u.substring(host.length() + 1 + (port+"").length() + 1);;
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
            bw.write("GET " + "/" + path + " HTTP/1.0\r\n\r\n");
            bw.flush();

            // start read
            BufferedReader bf = new BufferedReader(new InputStreamReader(s.getInputStream()));
            // Convert the buffered reader to string
            String content = bf.lines().collect(Collectors.joining("; "));
            bf.close();
            System.out.println("URL contents retrieved: " + url);
            s.close();
        
            TimeUnit.SECONDS.sleep(WAIT_TIME);
            return content;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    // update the page size, page modified time, non html objects, links in the page, URL types
    public static void updateURL(String url, String content) throws InterruptedException, ParseException, IOException {
        Type type = parseCode(url, content);
        if (type == null) return;
        switch (type) {
            case ON_SITE_VALID: {
                // update type
                onSite.add(url);
                onSiteValid.add(url);
                // update size
                int size = getPageSize(content);
                sizes.put(url, size);
                updateSize(url, size);
                // update links in the page
                for (String l : getLinks(url, content)){
                    if (!explored.contains(l)) unexplored.add(l);
                }
                // update modified time
                Date dt = getTime(content);
                updateTime(url, dt);
                // update non html object
                getNonHtml(url, content);
                break;}
            case INVALID: {
                invalid.add(url); 
                break;}
            case ON_SITE_REDIRECT: {
                String redir = getLocation(content);
                String path = getPath(url);
                redir = getAbsoluteURL(path, redir);
                redirect.put(url, redir); 
                if (!explored.contains(redir)) unexplored.add(redir);
                break;}
            case OFF_SITE: {
                if (isValid(url)) offSite_Valid.add(url);
                else offSite_Invalid.add(url);
                break;}
            case OFF_SITE_REDIRECT: {
                String redir = getLocation(content);
                String path = getPath(url);
                redir = getAbsoluteURL(path, redir);
                redirect.put(url, redir);
                if (isValid(redir)) offSite_Valid.add(redir);
                else offSite_Invalid.add(redir);
                break;}
        }
        return;
    }

    // find the absolute URL for a link
    public static String getAbsoluteURL(String path, String url){
        // find scheme first
        if (url.substring(0,7).equals("http://") || url.substring(0,8).equals("https://")){
            return url;
        } else {
            if (url.substring(0,1).equals("/")){
                url = SCHEME + host + ":" + port + url;
            } else {
                url = path + "/" + url;
            }
        }
        while (url.substring(url.length()-1).equals("/")) url = url.substring(0,url.length()-1);
        return url;
    } 

    // find the absolute path of the current directory
    public static String getPath(String url) {
        for (int i = url.length() - 1; i >= 0; i-- ){
            if (url.charAt(i) == '/') {
                return url.substring(0, i);
            }
        }
        return SCHEME + host + ":" + port;
    }

    public static String uTrim(String url) {
        if (url.substring(0,7).equals("http://")) url = url.substring(7);
        if (url.substring(0,8).equals("https://")) url = url.substring(8);
        while (url.substring(url.length()-1).equals("/")) url = url.substring(0,url.length()-1);
        return url;
    }

    // parse http response code
    public static Type parseCode(String url, String content) {
        if (!isOnSite(url)) return Type.OFF_SITE;
        url = uTrim(url);
        // check if it is valid, invalid, on-site redirect, or off-site
        Pattern p = Pattern.compile("HTTP/1.1 (.+?) ", Pattern.DOTALL);
        Matcher m = p.matcher(content);
        String code = null;
        while (m.find()) {
            code = m.group(1);
        }
        System.out.println("Code: " + code);
        if (isOnSite(url)){
            if (code.equals("200")) return Type.ON_SITE_VALID;
            if (code.equals("404") || code.equals("400")) {
                return Type.INVALID;
            }
        
            if (code.substring(0,2).equals("30")) {
                if (getLocation(content).contains(host+":"+port)){
                    return Type.ON_SITE_REDIRECT;
                } else {
                    return Type.OFF_SITE_REDIRECT;
                }
            }
        } else return Type.OFF_SITE;
        
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
    public static ArrayList<String> getLinks(String url, String content){
        Pattern p = Pattern.compile("href=\"(.+?)\">", Pattern.DOTALL);
        Matcher m = p.matcher(content);
        ArrayList<String> l = new ArrayList<>();
        while (m.find()) {
            String link;
            String path = getPath(url);
            link = getAbsoluteURL(path, m.group(1));
            if (!URLs.contains(link)) System.out.println("New link found: " + link);
            URLs.add(link);
            unexplored.add(link);
        }
        return l;
    }

    // check if a remote web server is valid
    public static boolean isValid(String url) throws IOException {
        explored.add(url);
        url = uTrim(url);
        try {Socket stest = new Socket(url, 80);} 
        catch (UnknownHostException e) {return false;}
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
    public static void getNonHtml(String url, String content) throws InterruptedException {
        Pattern[] Pat = new Pattern[2];
        Pat[0] = Pattern.compile(" src=\"(.+?)\"", Pattern.DOTALL);
        Pat[1] = Pattern.compile(" href=\"(.+?)\"", Pattern.DOTALL);
        
        for (Pattern p : Pat){
            Matcher m = p.matcher(content);
            while (m.find()) {
                String u = m.group(1);
                u = getAbsoluteURL(getPath(url), u);
                URLs.add(u);
                if ( isOnSite(u) ){
                    String c = getURL(u);
                    Pattern pc = Pattern.compile("Content-Type: (.+?); ", Pattern.DOTALL);
                    Matcher mc = pc.matcher(c);
                    while (mc.find()){
                        // System.out.println(mc.group(1));
                        if (!mc.group(1).contains("html")){
                            nonHtml.add(u);
                        } else {
                            unexplored.add(u);
                        }
                    }
                }
            }
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
            df.setTimeZone(TimeZone.getTimeZone("GMT"));
            Date date = df.parse(dt);
            return date;
        }
        return null;
    }

    // update the timestamps
    public static void updateTime(String url, Date date) {
        // put the datetime in the set
        times.put(url, date);
        // update the oldest and newest page if possible
        if (OLDEST_PAGE == null || date.compareTo(times.get(OLDEST_PAGE)) < 0) OLDEST_PAGE = url;
        if (NEWEST_PAGE == null || date.compareTo(times.get(NEWEST_PAGE)) > 0) NEWEST_PAGE = url;
    }

    // check if the url is on site
    public static boolean isOnSite(String url){
        if (url.contains("http://")){
            if (url.substring(7, 7 + host.length()).equals(host)) return true;
            else return false;
        } else if (url.contains("https://")){
            if (url.substring(8, 8 + host.length()).equals(host)) return true;
            else return false;
        } else return true;
    }
    
}
