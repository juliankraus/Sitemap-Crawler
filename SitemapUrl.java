package sitemap;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

import sitemap.SitemapUrl;
import sitemap.Sitemap;

/** The SitemapUrl class represents the URL's found in a Sitemap */

public class SitemapUrl {

    /** Allowed change frequencies */
    public enum ChangeFrequency {
        ALWAYS, HOURLY, DAILY, WEEKLY, MONTHLY, YEARLY, NEVER
    };

    /** URL found in Sitemap (required) */
    private URL url;

    /** When URL was last modified (optional) */
    private Date lastModified;

    /** How often the URL changes (optional) */
    private ChangeFrequency changeFreq;

    /** Value between [0.0 - 1.0] (optional) */
    private double priority;

    public SitemapUrl(String url) {
        setUrl(url);
    }

    public SitemapUrl(URL url) {
        setUrl(url);
    }

    public SitemapUrl(String url, String lastModified, String changeFreq, String priority) {

        setUrl(url);
        setLastModified(lastModified);
        setChangeFrequency(changeFreq);
        setPriority(priority);
    }

    public SitemapUrl(URL url, Date lastModified, ChangeFrequency changeFreq, double priority) {

        setUrl(url);
        setLastModified(lastModified);
        setChangeFrequency(changeFreq);
        setPriority(priority);
    }

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    public void setUrl(String url) {
        try {
            this.url = new URL(url);
        } catch (MalformedURLException e) {
            // e.printStackTrace();
            System.out.println("Bad url: [" + url + "]");
            this.url = null;
        }
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(String lastModified) {
        this.lastModified = Sitemap.convertToDate(lastModified);
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public double getPriority() {
        return priority;
    }

    public void setPriority(double priority) {

        if (priority < 0.0 || priority > 1.0) {
            this.priority = 0.0;
        } else {
            this.priority = priority;
        }
    }

    public void setPriority(String priority) {

        if (priority != null && priority.length() > 0) {
            try {
                setPriority(Double.parseDouble(priority));
            } catch (NumberFormatException e) {
                setPriority(0.0);
            }
        } else {
            setPriority(0.0);
        }
    }

    public ChangeFrequency getChangeFrequency() {
        return changeFreq;
    }

    public void setChangeFrequency(ChangeFrequency changeFreq) {
        this.changeFreq = changeFreq;
    }

    public void setChangeFrequency(String changeFreq) {

        if (changeFreq != null) {
            changeFreq = changeFreq.toUpperCase();

            if (changeFreq.contains("ALWAYS")) {
                this.changeFreq = ChangeFrequency.ALWAYS;
            } else if (changeFreq.contains("HOURLY")) {
                this.changeFreq = ChangeFrequency.HOURLY;
            } else if (changeFreq.contains("DAILY")) {
                this.changeFreq = ChangeFrequency.DAILY;
            } else if (changeFreq.contains("WEEKLY")) {
                this.changeFreq = ChangeFrequency.WEEKLY;
            } else if (changeFreq.contains("MONTHLY")) {
                this.changeFreq = ChangeFrequency.MONTHLY;
            } else if (changeFreq.contains("YEARLY")) {
                this.changeFreq = ChangeFrequency.YEARLY;
            } else if (changeFreq.contains("NEVER")) {
                this.changeFreq = ChangeFrequency.NEVER;
            } else {
                this.changeFreq = null;
            }
        }
    }

    public String toString() {
        String s = "url=\"" + url + "\",";
        s += "lastMod=";
        s += (lastModified == null) ? "null" : Sitemap.fullDateFormat.format(lastModified);
        s += ",changeFreq=" + changeFreq;
        s += ",priority=" + priority;
        return s;
    }

    /** For testing */
    public static void main(String[] argv) {
        SitemapUrl sitemap = new SitemapUrl("http://www.google.com/", "2008-05-04T11:34:56-10:00", "ALWAYS", "0.9");

        System.out.println(sitemap.toString());
    }
}
