package stoyanov.stoyan.simplebrowser;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.webkit.WebResourceResponse;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

/**
 * Comprehensive ad blocker that loads a large blocklist from assets,
 * blocks network requests to ad/tracking domains, and provides
 * JavaScript injection scripts for YouTube ad skipping,
 * element hiding, and background playback support.
 */
public class AdBlocker {

    private static final Set<String> AD_HOSTS = new HashSet<>();
    private static boolean initialized = false;

    /**
     * Initialize the ad blocker by loading the hosts list from assets.
     * Call this once in Application.onCreate() or MainActivity.onCreate().
     */
    public static void init(Context context) {
        if (initialized) return;
        loadHostsFromAssets(context);
        initialized = true;
    }

    private static void loadHostsFromAssets(Context context) {
        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(context.getAssets().open("ad_hosts.txt")));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    AD_HOSTS.add(line.toLowerCase());
                }
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Checks if the given URL belongs to a known ad-serving domain.
     */
    public static boolean isAd(String url) {
        if (TextUtils.isEmpty(url)) return false;

        try {
            // Never block YouTube video streams
            if (url.contains("googlevideo.com")) {
                return false;
            }

            // Quick path-based ad URL detection
            if (url.contains("/pagead/") ||
                url.contains("/api/stats/ads") ||
                url.contains("doubleclick.net")) {
                return true;
            }

            Uri uri = Uri.parse(url);
            String host = uri.getHost();
            if (host == null) return false;

            host = host.toLowerCase();

            // Direct match
            if (AD_HOSTS.contains(host)) return true;

            // Check parent domains (e.g. "cdn.taboola.com" matches "taboola.com")
            int dotIndex = host.indexOf('.');
            while (dotIndex != -1) {
                String parent = host.substring(dotIndex + 1);
                if (AD_HOSTS.contains(parent)) return true;
                dotIndex = parent.indexOf('.');
                if (dotIndex != -1) {
                    dotIndex += host.length() - parent.length();
                } else {
                    break;
                }
            }
        } catch (Exception e) {
            // Ignore malformed URLs
        }

        return false;
    }

    /**
     * Returns an empty WebResourceResponse to block the request.
     */
    public static WebResourceResponse createEmptyResponse() {
        return new WebResourceResponse("text/plain", "utf-8",
                new ByteArrayInputStream("".getBytes()));
    }

    /**
     * JavaScript to bypass Page Visibility API and keep media playing in background.
     * Overrides all visibility-related properties, event listeners, and handlers.
     * Periodically force-resumes paused video as a safety net.
     */
    public static String getBackgroundPlaybackScript() {
        return "(function() {" +
            "if (window.__sbBgPlayback) return;" +
            "window.__sbBgPlayback = true;" +
            "try {" +
            // Override document.hidden and visibilityState properties
            "  Object.defineProperty(document, 'hidden', {get: function() { return false; }, configurable: true});" +
            "  Object.defineProperty(document, 'visibilityState', {get: function() { return 'visible'; }, configurable: true});" +
            "  Object.defineProperty(document, 'webkitHidden', {get: function() { return false; }, configurable: true});" +
            "  Object.defineProperty(document, 'webkitVisibilityState', {get: function() { return 'visible'; }, configurable: true});" +
            // Override document.hasFocus()
            "  Document.prototype.hasFocus = function() { return true; };" +
            // Block onvisibilitychange property setter
            "  Object.defineProperty(document, 'onvisibilitychange', {get: function() { return null; }, set: function() {}, configurable: true});" +
            "  Object.defineProperty(document, 'onwebkitvisibilitychange', {get: function() { return null; }, set: function() {}, configurable: true});" +
            // Block visibilitychange, blur, pagehide, and freeze event listeners
            "  var origAEL = EventTarget.prototype.addEventListener;" +
            "  EventTarget.prototype.addEventListener = function(type, fn, opt) {" +
            "    if (type === 'visibilitychange' || type === 'webkitvisibilitychange' || " +
            "        type === 'pagehide' || type === 'freeze') {" +
            "      return;" +
            "    }" +
            "    if (this === window && type === 'blur') { return; }" +
            "    return origAEL.call(this, type, fn, opt);" +
            "  };" +
            // Block window.onblur and window.onfocus setters
            "  Object.defineProperty(window, 'onblur', {get: function() { return null; }, set: function() {}, configurable: true});" +
            // Periodically resume paused video (every 1 second)
            "  setInterval(function() {" +
            "    try {" +
            "      Object.defineProperty(document, 'hidden', {get: function() { return false; }, configurable: true});" +
            "      Object.defineProperty(document, 'visibilityState', {get: function() { return 'visible'; }, configurable: true});" +
            "      var v = document.querySelector('video');" +
            "      if (v && v.paused && !v.ended && v.readyState >= 2 && v.currentTime > 0) {" +
            "        v.play().catch(function(){});" +
            "      }" +
            "    } catch(e) {}" +
            "  }, 1000);" +
            "} catch(e) {}" +
            "})()";
    }

    /**
     * JavaScript for YouTube ad skipping and general CSS ad/cookie element hiding.
     */
    public static String getAdBlockScript() {
        return "(function(){" +
            "if (window.__sbAdBlock) return;" +
            "window.__sbAdBlock = true;" +

            // --- YouTube Ad Skipper ---
            "var wasAd = false;" +
            "function skipYTAds(){" +
            // Click skip button variants
            "  var btns = document.querySelectorAll(" +
            "    '.ytp-ad-skip-button, .ytp-ad-skip-button-modern, .ytp-skip-ad-button, " +
            "     button.ytp-ad-skip-button-modern, [class*=\"skip-button\"], .ytp-ad-survey-answer-button'" +
            "  );" +
            "  for(var i=0;i<btns.length;i++){try{btns[i].click();}catch(e){}}" +

            // Close overlay/banner ad close buttons
            "  var closes = document.querySelectorAll(" +
            "    '.ytp-ad-overlay-close-button, .ytp-ad-overlay-close-container'" +
            "  );" +
            "  for(var j=0;j<closes.length;j++){try{closes[j].click();}catch(e){}}" +

            // Handle video ad (ad-showing container)
            "  var adOn = document.querySelector('.ad-showing, .ad-interrupting');" +
            "  var vid = document.querySelector('video');" +

            "  if(adOn && vid){" +
            // Mark that ad was playing
            "    wasAd = true;" +
            // Mute and speed up the ad at 8x (not 16x to avoid breaking player)
            "    vid.muted = true;" +
            "    if(vid.playbackRate < 5) vid.playbackRate = 8;" +
            "  } else if(wasAd && vid){" +
            // Ad just ended — restore and force-play main content
            "    wasAd = false;" +
            "    vid.playbackRate = 1;" +
            "    vid.muted = false;" +
            "    if(vid.paused){" +
            "      vid.play().catch(function(){});" +
            // Retry play after short delay in case player needs time to load
            "      setTimeout(function(){" +
            "        if(vid.paused && !vid.ended) vid.play().catch(function(){});" +
            "      }, 500);" +
            "      setTimeout(function(){" +
            "        if(vid.paused && !vid.ended) vid.play().catch(function(){});" +
            "      }, 1500);" +
            "    }" +
            "  }" +
            "}" +

            // MutationObserver: watch for ad-showing class removal on the player
            "try{" +
            "  var player = document.querySelector('.html5-video-player, #movie_player');" +
            "  if(player && !window.__sbAdObserver){" +
            "    window.__sbAdObserver = new MutationObserver(function(mutations){" +
            "      mutations.forEach(function(m){" +
            "        if(m.attributeName === 'class'){" +
            "          var el = m.target;" +
            "          if(el.className && !el.className.includes('ad-showing') && !el.className.includes('ad-interrupting')){" +
            "            var v = el.querySelector('video');" +
            "            if(v){" +
            "              v.playbackRate = 1;" +
            "              v.muted = false;" +
            "              if(v.paused) v.play().catch(function(){});" +
            "            }" +
            "          }" +
            "        }" +
            "      });" +
            "    });" +
            "    window.__sbAdObserver.observe(player, {attributes: true, attributeFilter: ['class']});" +
            "  }" +
            "}catch(e){}" +

            // --- CSS Element Hiding (Ads + Cookie/Consent Banners) ---
            "function hideEls(){" +
            "  if(document.getElementById('sb-adblock-css')) return;" +
            "  var s = document.createElement('style');" +
            "  s.id = 'sb-adblock-css';" +
            "  s.textContent = '" +
            // YouTube ad elements
            ".ytp-ad-module, .ytp-ad-overlay-container, .ytp-ad-text-overlay, " +
            ".ytp-ad-player-overlay, .ytp-ad-image-overlay, #player-ads, " +
            "ytd-promoted-sparkles-web-renderer, ytd-display-ad-renderer, " +
            "ytd-promoted-video-renderer, ytd-ad-slot-renderer, " +
            "ytd-in-feed-ad-layout-renderer, ytd-banner-promo-renderer, " +
            ".ytd-mealbar-promo-renderer, #masthead-ad, .video-ads, " +
            "ytd-statement-banner-renderer, tp-yt-paper-dialog.ytd-popup-container, " +

            // General ad containers
            "[id*=\"google_ads\"], [id*=\"ad-container\"], [id*=\"ad_container\"], " +
            "[class*=\"ad-banner\"], [class*=\"ad_banner\"], " +
            "ins.adsbygoogle, [class*=\"adsbygoogle\"], " +
            "[data-ad], [data-ads], [data-ad-slot], [data-ad-client], " +
            "iframe[src*=\"doubleclick\"], iframe[src*=\"googlesyndication\"], " +
            "iframe[src*=\"amazon-adsystem\"], iframe[src*=\"taboola\"], " +
            "[class*=\"ad-slot\"], [id*=\"div-gpt-ad\"], " +
            "[class*=\"advertisement\"], [id*=\"advertisement\"], " +
            "[id*=\"taboola\"], [class*=\"taboola\"], " +
            "[id*=\"outbrain\"], [class*=\"outbrain\"], " +
            "[id*=\"mgid\"], [class*=\"mgid\"], " +
            "[class*=\"zergnet\"], [id*=\"zergnet\"], " +
            "[class*=\"popup-ad\"], [class*=\"modal-ad\"], " +
            "[class*=\"interstitial\"], [id*=\"interstitial\"], " +
            "[id*=\"sponsored\"], [class*=\"sponsored-content\"], " +

            // Cookie consent / GDPR banners
            "[class*=\"cookie-banner\"], [class*=\"cookie-consent\"], " +
            "[class*=\"cookie-notice\"], [class*=\"cookie-popup\"], " +
            "[class*=\"cookie-wall\"], [class*=\"cookie-modal\"], " +
            "[id*=\"cookie-banner\"], [id*=\"cookie-consent\"], " +
            "[id*=\"cookie-notice\"], [id*=\"cookie-popup\"], " +
            "[class*=\"consent-banner\"], [class*=\"consent-modal\"], " +
            "[class*=\"consent-popup\"], [class*=\"consent-wall\"], " +
            "[id*=\"consent-banner\"], [id*=\"consent-modal\"], " +
            "[class*=\"gdpr\"], [id*=\"gdpr\"], " +
            "[class*=\"privacy-banner\"], [id*=\"privacy-banner\"], " +
            "[class*=\"CookieConsent\"], [id*=\"CookieConsent\"], " +
            "[id*=\"onetrust-banner\"], [id*=\"onetrust-consent\"], " +
            "[class*=\"onetrust\"], " +
            "[id*=\"sp_message_container\"], " +
            "[id*=\"qc-cmp\"], [class*=\"qc-cmp\"], " +
            ".fc-consent-root, .fc-dialog-overlay, " +
            "[id*=\"didomi\"], [class*=\"didomi\"], " +
            "#usercentrics-root, " +
            "[class*=\"cc-banner\"], [class*=\"cc-window\"], " +
            "[id*=\"cookielaw\"], [class*=\"cookielaw\"], " +
            "[aria-label*=\"cookie\"], [aria-label*=\"Cookie\"], " +
            "[aria-label*=\"consent\"], [aria-label*=\"Consent\"] " +

            "{ display: none !important; visibility: hidden !important; " +
            "  height: 0 !important; max-height: 0 !important; overflow: hidden !important; " +
            "  pointer-events: none !important; }';" +
            "  if(document.head) document.head.appendChild(s);" +
            "}" +

            // Run immediately and on interval
            "hideEls();" +
            "skipYTAds();" +
            "if(!window.__sbAdInterval){" +
            "  window.__sbAdInterval = setInterval(function(){skipYTAds();}, 500);" +
            "}" +
            // Re-inject CSS after dynamic page changes (SPA navigation)
            "new MutationObserver(function(){hideEls();}).observe(document.body||document.documentElement,{childList:true,subtree:true});" +
            "})()";
    }
}
