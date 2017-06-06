package sitemap;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.zip.GZIPInputStream;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import sitemap.Sitemap.SitemapType;
import sitemap.SitemapParser;
import sitemap.ProtocolException;
import sitemap.Sitemap;
import sitemap.SitemapIndex;
import sitemap.SitemapUrl;
import sitemap.UnknownFormatException;

/** The SitemapParser will parse a given Sitemap or Sitemap Index given a URL */
public class SitemapParser {
    /** The Sitemap we are processing or have processed */
    private Sitemap sitemap = null;

    /** The Sitemap Index we are processing */
    public SitemapIndex sitemapIndex;

    /** According to the specs, 50K URLs per Sitemap is the max */
    private int MAX_URLS = 50000;

    // /** Sitemap docs must be limited to 10MB (10,485,760 bytes) */
    // public static int MAX_BYTES_ALLOWED = 10485760;

    /** Turn on verbose output */
    public boolean VERBOSE = false;

    /** Turn on debug output */
    public boolean DEBUG = false;

    /** Delay between HTTP requests in milliseconds */
    private int delayBetweenRequests = 5000;

    public int getDelayBetweenRequests() {
        return delayBetweenRequests;
    }

    public void setDelayBetweenRequests(int delayBetweenRequests) {
        if (delayBetweenRequests >= 0) {
            this.delayBetweenRequests = delayBetweenRequests;
        }
    }

    public SitemapParser() {

    }

    public SitemapType processSitemap(Sitemap sitemap) throws UnknownFormatException, ProtocolException, IOException, InterruptedException {

        this.sitemap = sitemap;
        URL url = sitemap.getUrl();

        if (VERBOSE)
            System.out.println("Processing Sitemap at " + url);

        // Set so we don't try to re-process it later
        sitemap.setProcessed(true);

        CloseableHttpClient client = HttpClientBuilder.create().build();
        HttpGet request = new HttpGet(url.toString());
        request.setHeader("User-Agent", "SitemapBot");

        HttpResponse response = client.execute(request);

        // Make HTTP request for the URL after a polite delay
        if (VERBOSE)
            System.out.println("Sleeping for " + delayBetweenRequests + " milliseconds before HTTP request...");
        Thread.sleep(delayBetweenRequests);

        if (response.getStatusLine().getStatusCode() != 200) {
            String msg = "Failed to fetch Sitemap at " + url + "   HTTP response code = " + response.getStatusLine().getStatusCode();
            throw new ProtocolException(msg);
        }

        HttpEntity entity = response.getEntity();
        ContentType ct = null;
        if (entity != null) {
            ct = ContentType.get(entity);
        }

        String contentType = ct.getMimeType();

        // Use extension or MIME type to determine how we should try
        // to process the response

        if (url.getPath().endsWith(".xml") || contentType.contains("text/xml") || contentType.contains("application/xml") || contentType.contains("application/x-xml") || contentType.contains("application/atom+xml") || contentType.contains("application/rss+xml")) {

            // Try parsing the XML which could be in a number of formats
            String content = EntityUtils.toString(response.getEntity());
            processXml(url, content);
        } else if (contentType.contains("text/plain")) {

            // plain text
            String content = EntityUtils.toString(response.getEntity());
            processText(content);
        } else if (url.getPath().endsWith(".gz") || contentType.contains("application/gzip") || contentType.contains("application/x-gzip") || contentType.contains("application/x-gunzip") || contentType.contains("application/gzipped") || contentType.contains("application/gzip-compressed") || contentType.contains("application/x-compress") || contentType.contains("gzip/document") || contentType.contains("application/octet-stream")) {

            // gzip
            String content = EntityUtils.toString(response.getEntity());
            processGzip(url, content.getBytes());
        } else {
            throw new UnknownFormatException("Unknown format " + contentType + " at " + url);
        }

        SitemapType type = sitemap.getType();
        if (type == SitemapType.INDEX) {
            // A Sitemap Index contains Sitemaps but is not a Sitemap
            this.sitemap = null;
        }

        return type;
    }

    public SitemapType processSitemap(URL url) throws UnknownFormatException, IOException, ProtocolException, InterruptedException, UnknownHostException {

        sitemap = new Sitemap(url);
        return processSitemap(sitemap);
    }

    private void processXml(URL sitemapUrl, String xmlContent) throws UnknownFormatException {

        InputSource is = new InputSource();
        is.setCharacterStream(new StringReader(xmlContent));
        processXml(sitemapUrl, is);
    }

    private void processXml(URL sitemapUrl, InputSource is) throws UnknownFormatException {

        Document doc = null;

        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            doc = dbf.newDocumentBuilder().parse(is);
        } catch (Exception e) {
            throw new UnknownFormatException("Error parsing XML for " + sitemapUrl);
        }

        // See if this is a sitemap index
        NodeList nodeList = doc.getElementsByTagName("sitemapindex");
        if (nodeList.getLength() > 0) {
            nodeList = doc.getElementsByTagName("sitemap");
            parseSitemapIndex(sitemapUrl, nodeList);
        } else if (doc.getElementsByTagName("urlset").getLength() > 0) {
            // This is a regular Sitemap
            parseXmlSitemap(doc);
        } else if (doc.getElementsByTagName("link").getLength() > 0) {
            // Could be RSS or Atom
            parseSyndicationFormat(sitemapUrl, doc);
        } else {
            throw new UnknownFormatException("Unknown XML format for " + sitemapUrl);
        }
    }

    private void parseXmlSitemap(Document doc) {

        sitemap.setType(SitemapType.XML);

        NodeList list = doc.getElementsByTagName("url");

        // Loop through the <url>s
        for (int i = 0; i < list.getLength(); i++) {

            Node n = list.item(i);

            if (n.getNodeType() == Node.ELEMENT_NODE) {
                Element elem = (Element) n;

                String loc = getElementValue(elem, "loc");

                URL url = null;
                try {
                    url = new URL(loc);
                    String lastMod = getElementValue(elem, "lastmod");
                    String changeFreq = getElementValue(elem, "changefreq");
                    String priority = getElementValue(elem, "priority");

                    if (urlIsLegal(sitemap.getBaseUrl(), url.toString())) {
                        SitemapUrl sUrl = new SitemapUrl(url.toString(), lastMod, changeFreq, priority);
                        sitemap.addUrl(sUrl);
                        if (VERBOSE)
                            System.out.println("  " + (i + 1) + ". " + sUrl);
                    }
                } catch (MalformedURLException e) {
                    // e.printStackTrace();

                    // Can't create an entry with a bad URL
                    if (DEBUG)
                        System.out.println("Bad url: [" + loc + "]");
                }
            }
        }
    }

    private void parseSitemapIndex(URL url, NodeList nodeList) {

        if (VERBOSE)
            System.out.println("Parsing Sitemap Index");

        // Set the sitemap type which affects parseSitemap's return value
        sitemap.setType(SitemapType.INDEX);

        sitemapIndex = new SitemapIndex(url);

        // Loop through the <sitemap>s
        for (int i = 0; i < nodeList.getLength() && i < MAX_URLS; i++) {

            Node firstNode = nodeList.item(i);

            URL sitemapUrl = null;
            Date lastModified = null;

            if (firstNode.getNodeType() == Node.ELEMENT_NODE) {
                Element elem = (Element) firstNode;
                String loc = getElementValue(elem, "loc");

                try {
                    sitemapUrl = new URL(loc);
                    String lastmod = getElementValue(elem, "lastmod");
                    lastModified = Sitemap.convertToDate(lastmod);

                    // Right now we are not worried about sitemapUrls that point
                    // to different websites.

                    Sitemap s = new Sitemap(sitemapUrl, lastModified);
                    sitemapIndex.addSitemap(s);
                    if (VERBOSE)
                        System.out.println("  " + (i + 1) + ". " + s);
                } catch (MalformedURLException e) {
                    // e.printStackTrace();

                    // Don't create an entry for a bad URL
                    if (DEBUG)
                        System.out.println("Bad url: [" + loc + "]");
                }
            }
        }
    }

    private void parseSyndicationFormat(URL sitemapUrl, Document doc) throws UnknownFormatException {

        // See if this is an Atom feed by looking for "feed" element
        NodeList list = doc.getElementsByTagName("feed");
        if (list.getLength() > 0) {
            parseAtom((Element) list.item(0), doc);
            sitemap.setType(SitemapType.ATOM);
        } else {
            // See if RSS feed by looking for "rss" element
            list = doc.getElementsByTagName("rss");

            if (list.getLength() > 0) {
                parseRSS(sitemap, doc);
                sitemap.setType(SitemapType.RSS);
            } else {
                throw new UnknownFormatException("Unknown syndication format at " + sitemapUrl);
            }
        }
    }

    private void parseAtom(Element elem, Document doc) {

        // Grab items from <feed><entry><link href="URL" /></entry></feed>
        // Use lastmod date from <feed><modified>DATE</modified></feed>

        if (DEBUG)
            System.out.println("Parsing Atom XML");

        String lastMod = getElementValue(elem, "modified");
        if (DEBUG)
            System.out.println("lastMod=" + lastMod);

        NodeList list = doc.getElementsByTagName("entry");

        // Loop through the <entry>s
        for (int i = 0; i < list.getLength() && i < MAX_URLS; i++) {

            Node n = list.item(i);

            if (n.getNodeType() == Node.ELEMENT_NODE) {
                elem = (Element) n;

                String href = getElementAttributeValue(elem, "link", "href");
                if (DEBUG)
                    System.out.println("href=" + href);

                URL url = null;
                try {
                    url = new URL(href);

                    if (urlIsLegal(sitemap.getBaseUrl(), url.toString())) {
                        SitemapUrl sUrl = new SitemapUrl(url.toString(), lastMod, null, null);
                        sitemap.addUrl(sUrl);
                        if (VERBOSE)
                            System.out.println("  " + (i + 1) + ". " + sUrl);
                    }
                } catch (MalformedURLException e) {
                    // Can't create an entry with a bad URL
                    if (DEBUG)
                        System.out.println("Bad url: [" + href + "]");
                }

            }
        }
    }

    private void parseRSS(Sitemap sitemap, Document doc) {

        // Grab items from <item><link>URL</link></item>
        // and last modified date from <pubDate>DATE</pubDate>

        if (DEBUG)
            System.out.println("Parsing RSS doc");

        NodeList list = doc.getElementsByTagName("channel");
        Element elem = (Element) list.item(0);

        // Treat publication date as last mod (Tue, 10 Jun 2003 04:00:00 GMT)
        String lastMod = getElementValue(elem, "pubDate");

        if (DEBUG)
            System.out.println("lastMod=" + lastMod);

        list = doc.getElementsByTagName("item");

        // Loop through the <item>s
        for (int i = 0; i < list.getLength() && i < MAX_URLS; i++) {

            Node n = list.item(i);

            if (n.getNodeType() == Node.ELEMENT_NODE) {
                elem = (Element) n;

                String link = getElementValue(elem, "link");
                if (DEBUG)
                    System.out.println("link=" + link);

                try {
                    URL url = new URL(link);

                    if (urlIsLegal(sitemap.getBaseUrl(), url.toString())) {
                        SitemapUrl sUrl = new SitemapUrl(url.toString(), lastMod, null, null);
                        sitemap.addUrl(sUrl);
                        if (VERBOSE)
                            System.out.println("  " + (i + 1) + ". " + sUrl);
                    }
                } catch (MalformedURLException e) {
                    // Can't create an entry with a bad URL
                    if (DEBUG)
                        System.out.println("Bad url: [" + link + "]");
                }
            }
        }
    }

    private String getElementValue(Element elem, String elementName) {

        NodeList list = elem.getElementsByTagName(elementName);
        Element e = (Element) list.item(0);
        if (e != null) {
            NodeList children = e.getChildNodes();
            if (children.item(0) != null) {
                return ((Node) children.item(0)).getNodeValue().trim();
            }
        }

        return null;
    }

    private String getElementAttributeValue(Element elem, String elementName, String attributeName) {

        NodeList list = elem.getElementsByTagName(elementName);
        Element e = (Element) list.item(0);
        if (e != null) {
            return e.getAttribute(attributeName);
        }

        return null;
    }

    private void processText(String content) throws IOException {

        if (DEBUG)
            System.out.println("Processing textual Sitemap");

        sitemap.setType(SitemapType.TEXT);

        BufferedReader reader = new BufferedReader(new StringReader(content));

        String line;

        int i = 1;
        while ((line = reader.readLine()) != null) {
            if (line.length() > 0 && i <= MAX_URLS) {
                try {
                    URL url = new URL(line);
                    if (urlIsLegal(sitemap.getBaseUrl(), url.toString())) {
                        if (VERBOSE)
                            System.out.println("  " + i + ". " + url);
                        i++;
                        sitemap.addUrl(url);
                    }
                } catch (MalformedURLException e) {
                    if (DEBUG)
                        System.out.println("Bad URL [" + line + "].");
                }
            }
        }
    }

    private void processGzip(URL url, byte[] response) throws MalformedURLException, IOException, UnknownFormatException {

        if (DEBUG)
            System.out.println("Processing gzip");

        InputStream is = new ByteArrayInputStream(response);

        // Remove .gz ending
        String xmlUrl = url.toString().replaceFirst("\\.gz$", "");

        if (DEBUG)
            System.out.println("XML url = " + xmlUrl);

        InputStream decompressed = new GZIPInputStream(is);
        InputSource in = new InputSource(decompressed);
        in.setSystemId(xmlUrl);
        processXml(url, in);
        decompressed.close();
    }

    private boolean urlIsLegal(String sitemapBaseUrl, String testUrl) {

        boolean ret = false;

        // Don't try a comparison if the URL is too short to match
        if (sitemapBaseUrl != null && sitemapBaseUrl.length() <= testUrl.length()) {
            String u = testUrl.substring(0, sitemapBaseUrl.length()).toLowerCase();
            ret = sitemapBaseUrl.equals(u);
        }

        if (DEBUG) {
            System.out.println("urlIsLegal: " + sitemapBaseUrl + " <= " + testUrl + " ? " + ret);
        }

        return ret;
    }

    public Sitemap getSitemap() {
        return sitemap;
    }

    public void freeSitemap() {
        sitemap = null;
    }
}
