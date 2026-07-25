package stoyanov.stoyan.simplebrowser;

import android.net.Uri;
import android.text.TextUtils;
import android.webkit.WebResourceResponse;

import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.Set;

/**
 * Ad blocker that blocks known ad-serving domains and provides
 * JavaScript injection scripts to hide/skip ads on YouTube and other sites smoothly.
 */
public class AdBlocker {

    private static final Set<String> AD_HOSTS = new HashSet<>();

    static {
        // Google Ads & AdSense
        AD_HOSTS.add("pagead2.googlesyndication.com");
        AD_HOSTS.add("googleads.g.doubleclick.net");
        AD_HOSTS.add("adservice.google.com");
        AD_HOSTS.add("adservice.google.bg");
        AD_HOSTS.add("adservice.google.co.uk");
        AD_HOSTS.add("www.googleadservices.com");
        AD_HOSTS.add("googleadservices.com");
        AD_HOSTS.add("ad.doubleclick.net");
        AD_HOSTS.add("doubleclick.net");
        AD_HOSTS.add("ads.google.com");
        AD_HOSTS.add("tpc.googlesyndication.com");
        AD_HOSTS.add("googlesyndication.com");
        AD_HOSTS.add("fundingchoicesmessages.google.com");
        AD_HOSTS.add("www.google-analytics.com");
        AD_HOSTS.add("ssl.google-analytics.com");
        AD_HOSTS.add("partner.googleadservices.com");

        // Facebook / Meta
        AD_HOSTS.add("an.facebook.com");
        AD_HOSTS.add("pixel.facebook.com");
        AD_HOSTS.add("www.facebook.com/tr");

        // Major ad networks
        AD_HOSTS.add("adnxs.com");
        AD_HOSTS.add("adsrvr.org");
        AD_HOSTS.add("amazon-adsystem.com");
        AD_HOSTS.add("aax.amazon-adsystem.com");
        AD_HOSTS.add("ads.yahoo.com");
        AD_HOSTS.add("advertising.com");
        AD_HOSTS.add("taboola.com");
        AD_HOSTS.add("cdn.taboola.com");
        AD_HOSTS.add("trc.taboola.com");
        AD_HOSTS.add("api.taboola.com");
        AD_HOSTS.add("outbrain.com");
        AD_HOSTS.add("widgets.outbrain.com");
        AD_HOSTS.add("outbrainimg.com");
        AD_HOSTS.add("criteo.com");
        AD_HOSTS.add("static.criteo.net");
        AD_HOSTS.add("cas.criteo.com");
        AD_HOSTS.add("bidswitch.net");
        AD_HOSTS.add("rubiconproject.com");
        AD_HOSTS.add("pubmatic.com");
        AD_HOSTS.add("openx.net");
        AD_HOSTS.add("casalemedia.com");
        AD_HOSTS.add("indexexchange.com");
        AD_HOSTS.add("smartadserver.com");
        AD_HOSTS.add("media.net");
        AD_HOSTS.add("contextweb.com");
        AD_HOSTS.add("sharethrough.com");
        AD_HOSTS.add("33across.com");
        AD_HOSTS.add("sovrn.com");
        AD_HOSTS.add("lijit.com");

        // Tracking & Analytics for ad targeting
        AD_HOSTS.add("moatads.com");
        AD_HOSTS.add("serving-sys.com");
        AD_HOSTS.add("adform.net");
        AD_HOSTS.add("everesttech.net");
        AD_HOSTS.add("adsafeprotected.com");
        AD_HOSTS.add("adsymptotic.com");
        AD_HOSTS.add("scorecardresearch.com");
        AD_HOSTS.add("quantserve.com");
        AD_HOSTS.add("bluekai.com");
        AD_HOSTS.add("exelator.com");
        AD_HOSTS.add("eyeota.net");
        AD_HOSTS.add("krxd.net");
        AD_HOSTS.add("demdex.net");
        AD_HOSTS.add("rlcdn.com");
        AD_HOSTS.add("mathtag.com");

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
        AD_HOSTS.add("startapp.com");

        // Pop-ups and intrusive ads
        AD_HOSTS.add("popads.net");
        AD_HOSTS.add("popcash.net");
        AD_HOSTS.add("propellerads.com");
        AD_HOSTS.add("revcontent.com");
        AD_HOSTS.add("mgid.com");
        AD_HOSTS.add("zergnet.com");
        AD_HOSTS.add("adsterra.com");
        AD_HOSTS.add("exoclick.com");
        AD_HOSTS.add("juicyads.com");
        AD_HOSTS.add("trafficjunky.com");
        AD_HOSTS.add("clickadu.com");
        AD_HOSTS.add("hilltopads.net");
        AD_HOSTS.add("richpush.co");
        AD_HOSTS.add("pushengage.com");
        AD_HOSTS.add("pushwoosh.com");

        // Video ad networks
        AD_HOSTS.add("imasdk.googleapis.com");
        AD_HOSTS.add("pubads.g.doubleclick.net");
        AD_HOSTS.add("securepubads.g.doubleclick.net");
        AD_HOSTS.add("s0.2mdn.net");
        AD_HOSTS.add("static.doubleclick.net");
        AD_HOSTS.add("ad.turn.com");
        AD_HOSTS.add("teads.tv");
        AD_HOSTS.add("spotxchange.com");

        // Regional / EU ad networks
        AD_HOSTS.add("adfox.ru");
        AD_HOSTS.add("adhigh.net");
        AD_HOSTS.add("admixer.net");
        AD_HOSTS.add("adskeeper.com");
        AD_HOSTS.add("adskeeper.co.uk");
        AD_HOSTS.add("cpmstar.com");
        AD_HOSTS.add("directadvert.ru");
        AD_HOSTS.add("marketgid.com");
        AD_HOSTS.add("mixadvert.com");
        AD_HOSTS.add("begun.ru");

        // Gambling / betting ads
        AD_HOSTS.add("betgenius.com");
        AD_HOSTS.add("bettingexpert.com");
        AD_HOSTS.add("oddsshark.com");
    }

    /**
     * Checks if the given URL belongs to a known ad-serving domain.
     * Note: NEVER block googlevideo.com as YouTube relies on it for real video streams.
     */
    public static boolean isAd(String url) {
        if (TextUtils.isEmpty(url)) return false;

        try {
            // Do NOT block googlevideo.com streaming URLs
            if (url.contains("googlevideo.com")) {
                return false;
            }

            // Quick path-based checks for explicit ad URLs
            if (url.contains("/pagead/") || url.contains("/api/stats/ads") || url.contains("doubleclick.net")) {
                return true;
            }

            Uri uri = Uri.parse(url);
            String host = uri.getHost();
            if (host == null) return false;

            host = host.toLowerCase();

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

    /**
     * Returns JavaScript code to bypass Page Visibility API so YouTube / audio continues playing in background.
     */
    public static String getBackgroundPlaybackScript() {
        return "(function() {" +
            "  try {" +
            "    Object.defineProperty(document, 'hidden', {get: function() { return false; }, configurable: true});" +
            "    Object.defineProperty(document, 'visibilityState', {get: function() { return 'visible'; }, configurable: true});" +
            "    Object.defineProperty(document, 'webkitVisibilityState', {get: function() { return 'visible'; }, configurable: true});" +
            "    var origAEL = EventTarget.prototype.addEventListener;" +
            "    EventTarget.prototype.addEventListener = function(type, listener, options) {" +
            "      if (type === 'visibilitychange' || type === 'webkitvisibilitychange' || type === 'visibility-change') {" +
            "        return;" +
            "      }" +
            "      return origAEL.apply(this, arguments);" +
            "    };" +
            "  } catch(e) {}" +
            "})();";
    }

    /**
     * Returns JavaScript code for clean YouTube ad skipping and general CSS ad blocking.
     */
    public static String getAdBlockScript() {
        return "(function(){" +
            // --- YouTube Ad Auto-Skipper ---
            "function skipYouTubeAds(){" +
            // 1. Click skip button if present
            "  var skipButtons = document.querySelectorAll('.ytp-ad-skip-button, .ytp-ad-skip-button-modern, .ytp-skip-ad-button, [class*=\"skip-button\"], button.ytp-ad-skip-button-modern');" +
            "  for(var i = 0; i < skipButtons.length; i++){" +
            "    try { skipButtons[i].click(); } catch(e){}" +
            "  }" +
            // 2. Close overlay/banner ad close buttons
            "  var closeButtons = document.querySelectorAll('.ytp-ad-overlay-close-button, .ytp-ad-overlay-close-container, .ytp-ad-text-overlay .ytp-ad-close-button');" +
            "  for(var j = 0; j < closeButtons.length; j++){" +
            "    try { closeButtons[j].click(); } catch(e){}" +
            "  }" +
            // 3. For video ads (.ad-showing / .ad-interrupting), speed up to 16x and mute without setting currentTime=duration (which breaks playback)
            "  var adContainer = document.querySelector('.ad-showing, .ad-interrupting, .ytp-ad-self-ad-banner');" +
            "  var videoEl = document.querySelector('video');" +
            "  if (adContainer && videoEl) {" +
            "    videoEl.muted = true;" +
            "    videoEl.playbackRate = 16.0;" +
            "  } else if (videoEl && videoEl.playbackRate === 16.0) {" +
            "    videoEl.playbackRate = 1.0;" +
            "    videoEl.muted = false;" +
            "  }" +
            "}" +
            // --- CSS Ad Hiding ---
            "function hideGeneralAds(){" +
            "  if (document.getElementById('sb-adblock-styles')) return;" +
            "  var style = document.createElement('style');" +
            "  style.id = 'sb-adblock-styles';" +
            "  style.textContent = '" +
            // YouTube ad elements
            ".ytp-ad-module, .ytp-ad-overlay-container, .ytp-ad-text-overlay, .ytp-ad-player-overlay, " +
            ".ytp-ad-image-overlay, #player-ads, ytd-promoted-sparkles-web-renderer, ytd-display-ad-renderer, " +
            "ytd-promoted-video-renderer, ytd-ad-slot-renderer, ytd-in-feed-ad-layout-renderer, " +
            "ytd-banner-promo-renderer, .ytd-mealbar-promo-renderer, #masthead-ad, .video-ads, " +
            // General web ad container selectors
            "[id*=\"google_ads\"], [id*=\"ad-container\"], [id*=\"ad_container\"], " +
            "[class*=\"ad-banner\"], [class*=\"ad_banner\"], [class*=\"adsbygoogle\"], " +
            "ins.adsbygoogle, [id*=\"sponsored\"], [class*=\"sponsored-content\"], " +
            "[data-ad], [data-ads], [data-ad-slot], " +
            "iframe[src*=\"doubleclick\"], iframe[src*=\"googlesyndication\"], " +
            "iframe[src*=\"amazon-adsystem\"], iframe[src*=\"taboola\"], " +
            "[class*=\"ad-slot\"], [id*=\"div-gpt-ad\"], " +
            "[class*=\"advertisement\"], [id*=\"advertisement\"], " +
            "[id*=\"taboola\"], [class*=\"taboola\"], " +
            "[id*=\"outbrain\"], [class*=\"outbrain\"], " +
            "[id*=\"mgid\"], [class*=\"mgid\"], " +
            "[class*=\"zergnet\"], [id*=\"zergnet\"], " +
            "[class*=\"popup-ad\"], [class*=\"modal-ad\"], " +
            "[class*=\"interstitial\"], [id*=\"interstitial\"] " +
            "{ display: none !important; visibility: hidden !important; height: 0 !important; overflow: hidden !important; }';" +
            "  if (document.head) { document.head.appendChild(style); }" +
            "}" +
            "hideGeneralAds();" +
            "skipYouTubeAds();" +
            "if (!window.sbAdInterval) { window.sbAdInterval = setInterval(skipYouTubeAds, 500); }" +
            "})()";
    }
}
