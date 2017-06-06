package sitemap;

import java.io.IOException;
import java.net.URL;
import java.util.Iterator;

import sitemap.Sitemap.SitemapType;

public class Main {
    public static void main(String[] args) {
        // If the user entered too few arguments, output the usage and quit.
        if (args.length < 1) {
            System.out.println("Usage: java SitemapParser <Sitemap URL>");
            System.exit(-1);
        }

        SitemapParser parser = new SitemapParser();

        String sUrl = null;

        // Verbose option?
        if (args[0].equals("-d") && args.length >= 2) {
            parser.VERBOSE = true;
            sUrl = args[1];
        } else {
            sUrl = args[0];
        }

        URL url = null;
        try {
            url = new URL(sUrl);
        } catch (Exception e) {
            System.out.println("Bad URL [" + sUrl + "]: " + e.getMessage());
            System.exit(-1);
        }

        Sitemap sitemap;
        SitemapType type = null;

        try {
            // We don't know if this URL points to a Sitemap or a Sitemap Index,
            // but we will after processing it.
            type = parser.processSitemap(url);

            // If this was a Sitemap Index, we'll need to process
            // each unprocessed Sitemaps found in the index.
            if (type == SitemapType.INDEX) {
                System.out.println("Sitemap Index found with " + parser.sitemapIndex.getSitemapListSize() + " Sitemaps.");
                sitemap = parser.sitemapIndex.getUnprocessedSitemap();
                parser.processSitemap(sitemap);
            }
        } catch (UnknownFormatException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Get the Sitemap that was just processed
        sitemap = parser.getSitemap();

        // Loop through all the Sitemaps found in a Sitemap Index
        for (int i = 1; sitemap != null; i++) {
            System.out.println("Sitemap " + i + ". " + sitemap);
            int j = 1;
            for (Iterator<SitemapUrl> it = sitemap.getUrlList().iterator(); it.hasNext();) {
                System.out.println(j + ". " + it.next());
                j++;
            }

            try {
                // If we originally found a Sitemap Index, parse any remaining
                // unprocessed Sitemaps
                if (type == SitemapType.INDEX) {
                    // Best to free this sitemap's URL list if we're done with
                    // it
                    // because of memory constraints
                    parser.sitemapIndex.freeSitemap(sitemap);

                    sitemap = parser.sitemapIndex.getUnprocessedSitemap();
                    if (sitemap != null) {
                        parser.processSitemap(sitemap);
                        sitemap = parser.getSitemap();
                    }
                } else {
                    // This is the only Sitemap, so set to null to leave the
                    // loop
                    sitemap = null;
                }
            } catch (UnknownFormatException e) {
                System.out.println(e.getMessage());
            } catch (ProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
