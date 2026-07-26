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
 * JavaScript injection scripts for YouTube and YouTube Music ad skipping,
 * element hiding, and robust background playback support.
 */
public class AdBlocker {

    private static final Set<String> AD_HOSTS = new HashSet<>();
    private static boolean initialized = false;

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

    public static boolean isAd(String url) {
        if (TextUtils.isEmpty(url)) return false;
        try {
            if (url.contains("googlevideo.com")) return false;
            if (url.contains("/pagead/") ||
                url.contains("/api/stats/ads") ||
                url.contains("doubleclick.net")) {
                return true;
            }
            Uri uri = Uri.parse(url);
            String host = uri.getHost();
            if (host == null) return false;
            host = host.toLowerCase();
            if (AD_HOSTS.contains(host)) return true;
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
        } catch (Exception e) { }
        return false;
    }

    public static WebResourceResponse createEmptyResponse() {
        return new WebResourceResponse("text/plain", "utf-8",
                new ByteArrayInputStream("".getBytes()));
    }

    /**
     * JavaScript to bypass Page Visibility API and keep media playing in background.
     * Uses window.__sbIsBackground (set by Java in onPause/onResume)
     * to distinguish between background pauses (block) and foreground pauses (allow).
     */
    public static String getBackgroundPlaybackScript() {
        return "(function() {" +
            "if (window.__sbBgPlayback) return;" +
            "window.__sbBgPlayback = true;" +
            "try {" +
            "  if (typeof window.__sbUserPaused === 'undefined') window.__sbUserPaused = false;" +
            "  if (typeof window.__sbIsBackground === 'undefined') window.__sbIsBackground = false;" +
            "  window.__sbLastClickTime = 0;" +

            // 1. Override document visibility properties
            "  Object.defineProperty(document, 'hidden', {get: function() { return false; }, configurable: true});" +
            "  Object.defineProperty(document, 'visibilityState', {get: function() { return 'visible'; }, configurable: true});" +
            "  Object.defineProperty(document, 'webkitHidden', {get: function() { return false; }, configurable: true});" +
            "  Object.defineProperty(document, 'webkitVisibilityState', {get: function() { return 'visible'; }, configurable: true});" +
            "  Document.prototype.hasFocus = function() { return true; };" +

            // 2. Block visibilitychange and blur event listeners
            "  Object.defineProperty(document, 'onvisibilitychange', {get: function() { return null; }, set: function() {}, configurable: true});" +
            "  Object.defineProperty(document, 'onwebkitvisibilitychange', {get: function() { return null; }, set: function() {}, configurable: true});" +
            "  var origAEL = EventTarget.prototype.addEventListener;" +
            "  EventTarget.prototype.addEventListener = function(type, fn, opt) {" +
            "    if (type === 'visibilitychange' || type === 'webkitvisibilitychange' || " +
            "        type === 'pagehide' || type === 'freeze') {" +
            "      return;" +
            "    }" +
            "    if (this === window && type === 'blur') { return; }" +
            "    return origAEL.call(this, type, fn, opt);" +
            "  };" +
            "  Object.defineProperty(window, 'onblur', {get: function() { return null; }, set: function() {}, configurable: true});" +

            // 3. Track user click/touch interactions
            "  var recordClick = function() { window.__sbLastClickTime = Date.now(); };" +
            "  document.addEventListener('click', recordClick, true);" +
            "  document.addEventListener('touchstart', recordClick, true);" +

            // 4. Override HTMLMediaElement.prototype.pause
            "  var origPause = HTMLMediaElement.prototype.pause;" +
            "  window.__sbOrigPause = origPause;" +
            "  HTMLMediaElement.prototype.pause = function() {" +
            "    var timeSinceClick = Date.now() - (window.__sbLastClickTime || 0);" +
            "    if (timeSinceClick < 1000) {" +
            "      window.__sbUserPaused = true;" +
            "      return origPause.apply(this, arguments);" +
            "    }" +
            "    if (window.__sbUserPaused) {" +
            "      return origPause.apply(this, arguments);" +
            "    }" +
            "    if (window.__sbIsBackground) {" +
            "      return;" +  // Block background pause
            "    }" +
            "    return origPause.apply(this, arguments);" +  // Allow foreground pause
            "  };" +

            // 5. Override HTMLMediaElement.prototype.play to reset userPaused
            "  var origPlay = HTMLMediaElement.prototype.play;" +
            "  window.__sbOrigPlay = origPlay;" +
            "  HTMLMediaElement.prototype.play = function() {" +
            "    var timeSinceClick = Date.now() - (window.__sbLastClickTime || 0);" +
            "    if (timeSinceClick < 1000 || !window.__sbUserPaused) {" +
            "      window.__sbUserPaused = false;" +
            "    }" +
            "    return origPlay.apply(this, arguments);" +
            "  };" +

            "} catch(e) {}" +
            "})()";
    }

    /**
     * JavaScript executed periodically from Java Handler while app is in background.
     * Handles visibility re-override, ad skipping, ad fast-forwarding, and force-resume
     * for both YouTube and YouTube Music across all video and audio elements.
     */
    public static String getBgTickScript() {
        return "try {" +
            // Re-override visibility properties
            "  Object.defineProperty(document, 'hidden', {get:function(){return false}, configurable:true});" +
            "  Object.defineProperty(document, 'visibilityState', {get:function(){return 'visible'}, configurable:true});" +

            // Click skip buttons & YouTube Music "Are you still listening?" prompts
            "  var btns = document.querySelectorAll('.ytp-ad-skip-button, .ytp-ad-skip-button-modern, .ytp-skip-ad-button, button.ytp-ad-skip-button-modern, .ytp-ad-skip-button-container button, button[class*=\"ytp-ad-skip\"], .ytmusic-you-there-renderer button, .ytmusic-you-there-renderer .dismiss-button');" +
            "  for (var i=0; i<btns.length; i++) { try { btns[i].click(); } catch(e){} }" +
            "  var closes = document.querySelectorAll('.ytp-ad-overlay-close-button, .ytp-ad-overlay-close-container');" +
            "  for (var j=0; j<closes.length; j++) { try { closes[j].click(); } catch(e){} }" +

            // Check for active ad on YouTube / YouTube Music
            "  var adOn = document.querySelector('.ad-showing, .ad-interrupting, .ytp-ad-player-overlay, .ytp-ad-text-overlay, .ytp-ad-module, ytmusic-ad-rendering-view-model, ytmusic-player-bar[has-ad]');" +

            "  var mediaEls = document.querySelectorAll('video, audio');" +
            "  for (var k=0; k<mediaEls.length; k++) {" +
            "    var v = mediaEls[k];" +
            "    if (adOn || btns.length > 0) {" +
            "      v.muted = true;" +
            "      v.playbackRate = 16;" +
            "      if (isFinite(v.duration) && v.duration > 0 && v.currentTime < v.duration - 0.3) {" +
            "        v.currentTime = v.duration - 0.1;" +
            "      }" +
            "      if (v.paused && window.__sbOrigPlay) window.__sbOrigPlay.call(v).catch(function(){});" +
            "    } else {" +
            "      if (v.playbackRate > 2) { v.playbackRate = 1; v.muted = false; }" +
            "      if (v.paused && !v.ended && !window.__sbUserPaused) {" +
            "        if (window.__sbOrigPlay) window.__sbOrigPlay.call(v).catch(function(){});" +
            "      }" +
            "    }" +
            "  }" +
            "} catch(e) {}";
    }

    /**
     * JavaScript for YouTube / YouTube Music ad skipping and general CSS ad/cookie element hiding.
     */
    public static String getAdBlockScript() {
        return "(function(){" +
            "if (window.__sbAdBlock) return;" +
            "window.__sbAdBlock = true;" +

            // --- YouTube / YouTube Music Ad Skipper ---
            "var wasAd = false;" +
            "function skipYTAds(){" +
            "  var btns = document.querySelectorAll('.ytp-ad-skip-button, .ytp-ad-skip-button-modern, .ytp-skip-ad-button, button.ytp-ad-skip-button-modern, .ytp-ad-skip-button-container button, button[class*=\"ytp-ad-skip\"], .ytmusic-you-there-renderer button, .ytmusic-you-there-renderer .dismiss-button');" +
            "  for (var i=0; i<btns.length; i++) { try { btns[i].click(); } catch(e){} }" +
            "  var closes = document.querySelectorAll('.ytp-ad-overlay-close-button, .ytp-ad-overlay-close-container');" +
            "  for (var j=0; j<closes.length; j++) { try { closes[j].click(); } catch(e){} }" +

            "  var adOn = document.querySelector('.ad-showing, .ad-interrupting, .ytp-ad-player-overlay, .ytp-ad-text-overlay, .ytp-ad-module, ytmusic-ad-rendering-view-model, ytmusic-player-bar[has-ad]');" +
            "  var mediaEls = document.querySelectorAll('video, audio');" +

            "  for (var k=0; k<mediaEls.length; k++) {" +
            "    var vid = mediaEls[k];" +
            "    if (adOn || btns.length > 0) {" +
            "      wasAd = true;" +
            "      vid.muted = true;" +
            "      vid.playbackRate = 16;" +
            "      if (isFinite(vid.duration) && vid.duration > 0 && vid.currentTime < vid.duration - 0.3) {" +
            "        vid.currentTime = vid.duration - 0.1;" +
            "      }" +
            "      if (vid.paused && window.__sbOrigPlay) window.__sbOrigPlay.call(vid).catch(function(){});" +
            "    } else if (wasAd && vid) {" +
            "      wasAd = false;" +
            "      vid.playbackRate = 1;" +
            "      vid.muted = false;" +
            "      if (vid.paused && !window.__sbUserPaused) {" +
            "        vid.play().catch(function(){});" +
            "        setTimeout(function(){ if(vid.paused && !vid.ended) vid.play().catch(function(){}); }, 500);" +
            "        setTimeout(function(){ if(vid.paused && !vid.ended) vid.play().catch(function(){}); }, 1500);" +
            "      }" +
            "    }" +
            "  }" +
            "}" +

            // --- CSS Element Hiding (Ads + Cookie/Consent Banners) ---
            "function hideEls(){" +
            "  if (document.getElementById('sb-adblock-css')) return;" +
            "  var s = document.createElement('style');" +
            "  s.id = 'sb-adblock-css';" +
            "  s.textContent = '" +
            ".ytp-ad-module, .ytp-ad-overlay-container, .ytp-ad-text-overlay, " +
            ".ytp-ad-player-overlay, .ytp-ad-image-overlay, #player-ads, " +
            "ytd-promoted-sparkles-web-renderer, ytd-display-ad-renderer, " +
            "ytd-promoted-video-renderer, ytd-ad-slot-renderer, " +
            "ytd-in-feed-ad-layout-renderer, ytd-banner-promo-renderer, " +
            ".ytd-mealbar-promo-renderer, #masthead-ad, .video-ads, " +
            "ytd-statement-banner-renderer, tp-yt-paper-dialog.ytd-popup-container, " +
            "ytmusic-ad-rendering-view-model, ytmusic-you-there-renderer, " +
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
            "  if (document.head) document.head.appendChild(s);" +
            "}" +

            "hideEls();" +
            "skipYTAds();" +
            "if (!window.__sbAdInterval){" +
            "  window.__sbAdInterval = setInterval(function(){skipYTAds();}, 500);" +
            "}" +
            "new MutationObserver(function(){hideEls();}).observe(document.body||document.documentElement,{childList:true,subtree:true});" +
            "})()";
    }
}
