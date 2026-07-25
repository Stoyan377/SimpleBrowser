package stoyanov.stoyan.simplebrowser;

import android.net.Uri;
import android.text.TextUtils;
import android.webkit.WebResourceResponse;

import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.Set;

/**
 * Simple ad blocker that maintains a set of known ad-serving domains
 * and provides methods to check URLs and return empty responses for blocked requests.
 */
public class AdBlocker {

    private static final Set<String> AD_HOSTS = new HashSet<>();

    static {
        // Google Ads
        AD_HOSTS.add("pagead2.googlesyndication.com");
        AD_HOSTS.add("googleads.g.doubleclick.net");
        AD_HOSTS.add("adservice.google.com");
        AD_HOSTS.add("www.googleadservices.com");
        AD_HOSTS.add("googleadservices.com");
        AD_HOSTS.add("ad.doubleclick.net");
        AD_HOSTS.add("doubleclick.net");
        AD_HOSTS.add("ads.google.com");
        AD_HOSTS.add("tpc.googlesyndication.com");
        AD_HOSTS.add("googlesyndication.com");
        AD_HOSTS.add("fundingchoicesmessages.google.com");

        // Facebook / Meta
        AD_HOSTS.add("an.facebook.com");
        AD_HOSTS.add("pixel.facebook.com");

        // Major ad networks
        AD_HOSTS.add("adnxs.com");
        AD_HOSTS.add("adsrvr.org");
        AD_HOSTS.add("amazon-adsystem.com");
        AD_HOSTS.add("aax.amazon-adsystem.com");
        AD_HOSTS.add("ads.yahoo.com");
        AD_HOSTS.add("advertising.com");
        AD_HOSTS.add("taboola.com");
        AD_HOSTS.add("cdn.taboola.com");
        AD_HOSTS.add("outbrain.com");
        AD_HOSTS.add("widgets.outbrain.com");
        AD_HOSTS.add("criteo.com");
        AD_HOSTS.add("static.criteo.net");
        AD_HOSTS.add("bidswitch.net");
        AD_HOSTS.add("rubiconproject.com");
        AD_HOSTS.add("pubmatic.com");
        AD_HOSTS.add("openx.net");
        AD_HOSTS.add("casalemedia.com");
        AD_HOSTS.add("indexexchange.com");
        AD_HOSTS.add("smartadserver.com");
        AD_HOSTS.add("media.net");

        // Tracking & Analytics used for ad targeting
        AD_HOSTS.add("moatads.com");
        AD_HOSTS.add("serving-sys.com");
        AD_HOSTS.add("adform.net");
        AD_HOSTS.add("everesttech.net");
        AD_HOSTS.add("adsafeprotected.com");
        AD_HOSTS.add("adsymptotic.com");

        // Mobile ad networks
        AD_HOSTS.add("admob.com");
        AD_HOSTS.add("inmobi.com");
        AD_HOSTS.add("appsflyer.com");
        AD_HOSTS.add("mopub.com");
        AD_HOSTS.add("unity3d.com");
        AD_HOSTS.add("unityads.unity3d.com");
        AD_HOSTS.add("vungle.com");
        AD_HOSTS.add("chartboost.com");
        AD_HOSTS.add("ironsrc.com");
        AD_HOSTS.add("applovin.com");

        // Pop-ups and intrusive ads
        AD_HOSTS.add("popads.net");
        AD_HOSTS.add("popcash.net");
        AD_HOSTS.add("propellerads.com");
        AD_HOSTS.add("revcontent.com");
        AD_HOSTS.add("mgid.com");

        // Video ad networks
        AD_HOSTS.add("imasdk.googleapis.com");
        AD_HOSTS.add("pubads.g.doubleclick.net");
        AD_HOSTS.add("securepubads.g.doubleclick.net");
        AD_HOSTS.add("s0.2mdn.net");
        AD_HOSTS.add("static.doubleclick.net");
    }

    /**
     * Checks if the given URL belongs to a known ad-serving domain.
     */
    public static boolean isAd(String url) {
        if (TextUtils.isEmpty(url)) return false;

        try {
            Uri uri = Uri.parse(url);
            String host = uri.getHost();
            if (host == null) return false;

            host = host.toLowerCase();

            // Check exact match and parent domain match
            // e.g. "cdn.taboola.com" should match "taboola.com"
            for (String adHost : AD_HOSTS) {
                if (host.equals(adHost) || host.endsWith("." + adHost)) {
                    return true;
                }
            }
        } catch (Exception e) {
            // Ignore malformed URLs
        }

        return false;
    }

    /**
     * Returns an empty WebResourceResponse to effectively block the request.
     */
    public static WebResourceResponse createEmptyResponse() {
        return new WebResourceResponse("text/plain", "utf-8",
                new ByteArrayInputStream("".getBytes()));
    }
}
