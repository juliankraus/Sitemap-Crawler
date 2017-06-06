package sitemap;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Hashtable;

import sitemap.Sitemap;
import sitemap.SitemapUrl;

/** The Sitemap class represents a Sitemap from the Sitemap protocol */

public class Sitemap {
    /** Sitemap's URL */
    private URL url;

    /** W3C date the Sitemap was last modified */
    private Date lastModified;

    /** Indicates if we have tried to process this Sitemap or not */
    private boolean processed;

    /** Various Sitemap types */
    public enum SitemapType {
        INDEX, XML, ATOM, RSS, TEXT
    };

    /** This Sitemap type */
    private SitemapType type;

    /**
     * If Sitemap was found on https://www.hallhuber.com/sitemap.xml baseURL is
     * https://www.hallhuber.com/
     */
    private String baseUrl;

    /** URL's found in this sitemap */
    private Hashtable<String, SitemapUrl> urlList;

    private static DateFormat dateFormats[] = { new SimpleDateFormat("yyyy-MM-dd"), new SimpleDateFormat("yyyy-MM-dd'T'HH:mm+hh:00"), new SimpleDateFormat("yyyy-MM-dd'T'HH:mm-hh:00"), new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss+hh:00"), new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss-hh:00"),

        // Accept RSS dates
        new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz") };

    public static DateFormat fullDateFormat = dateFormats[1];

    public Sitemap() {
        urlList = new Hashtable<String, SitemapUrl>();
        lastModified = null;
        setProcessed(false);
    }

    public Sitemap(URL url, Date lastModified) {
        this();
        setUrl(url);
        setLastModified(lastModified);
    }

    public Sitemap(URL url) {
        this();
        setUrl(url);
    }

    public Sitemap(String url) {
        this();
        setUrl(url);
    }

    public Sitemap(String url, String lastModified) {
        this();
        setUrl(url);
        setLastModified(lastModified);
    }

    public Collection<SitemapUrl> getUrlList() {
        return urlList.values();
    }

    public void clearUrlList() {
        urlList.clear();
    }

    public void setUrl(URL url) {
        this.url = url;
        setBaseUrl(url);
    }

    public void setUrl(String url) {
        try {
            this.url = new URL(url);

            setBaseUrl(this.url);
        } catch (MalformedURLException e) {
            // e.printStackTrace();
            System.out.println("Bad url: [" + url + "]");
            this.url = null;
        }
    }

    public URL getUrl() {
        return url;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public void setLastModified(String lastModified) {
        this.lastModified = Sitemap.convertToDate(lastModified);
    }

    public Date getLastModified() {
        return lastModified;
    }

    public int getUrlListSize() {
        return urlList.size();
    }

    public String toString() {
        String s = "url=\"" + url + "\",lastMod=";
        s += (lastModified == null) ? "null" : Sitemap.fullDateFormat.format(lastModified);
        s += ",type=" + type + ",processed=" + processed + ",urlListSize=" + urlList.size();
        return s;
    }

    public static Date convertToDate(String date) {

        if (date != null) {
            for (DateFormat df : dateFormats) {
                try {
                    return df.parse(date);
                } catch (ParseException e) {

                }
            }
        }

        return null;
    }

    public void setProcessed(boolean processed) {
        this.processed = processed;
    }

    public boolean isProcessed() {
        return processed;
    }

    private void setBaseUrl(URL sitemapUrl) {
        baseUrl = sitemapUrl.toString().toLowerCase();
        baseUrl = baseUrl.replaceFirst("/[^/]*$", "/");
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void addUrl(SitemapUrl url) {
        urlList.put(url.getUrl().toString(), url);
    }

    public void addUrl(URL url) {
        urlList.put(url.toString(), new SitemapUrl(url));
    }

    public void setType(SitemapType type) {
        this.type = type;
    }

    public SitemapType getType() {
        return type;
    }
}
