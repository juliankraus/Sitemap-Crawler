package sitemap;

import java.net.URL;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Map.Entry;

import sitemap.Sitemap;

/** The SitemapIndex represents a Sitemap Index (collection of Sitemap URLs) */
public class SitemapIndex {
    private URL url;

    private Hashtable<String, Sitemap> sitemaps;

    public SitemapIndex() {
        this.sitemaps = new Hashtable<>();
    }

    public SitemapIndex(URL url) {
        this();
        setUrl(url);
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    public URL getUrl() {
        return url;
    }

    public Collection<Sitemap> getSitemapList() {
        return sitemaps.values();
    }

    public int getSitemapListSize() {
        return sitemaps.size();
    }

    public void addSitemap(Sitemap sitemap) {
        sitemaps.put(sitemap.getUrl().toString(), sitemap);
    }

    public Sitemap getSitemap(URL url) {
        return sitemaps.get(url.toString());
    }

    public void removeSitemap(URL url) {
        sitemaps.remove(url.toString());
    }

    public void removeSitemap(Sitemap sitemap) {
        sitemaps.remove(sitemap.getUrl().toString());
    }

    public boolean unprocessedSitemapsAvailable() {

        // Find an unprocessed Sitemap
        for (Entry<String, Sitemap> sitemap : sitemaps.entrySet()) {
            Sitemap s = sitemap.getValue();
            if (!s.isProcessed()) {
                return true;
            }
        }

        return false;
    }

    public Sitemap getUnprocessedSitemap() {
        for (Entry<String, Sitemap> sitemap : sitemaps.entrySet()) {
            Sitemap s = sitemap.getValue();
            if (!s.isProcessed()) {
                return s;
            }
        }

        return null;
    }

    public void freeSitemap(Sitemap s) {
        if (s == null)
            return;

        Sitemap sitemap = sitemaps.get(s.getUrl().toString());
        if (sitemap != null) {
            sitemap.clearUrlList();
        }
    }

    public String toString() {
        return "url=\"" + url + "\",sitemapListSize=" + sitemaps.size();
    }
}
