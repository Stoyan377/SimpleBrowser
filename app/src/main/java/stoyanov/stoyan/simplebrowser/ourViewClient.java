package stoyanov.stoyan.simplebrowser;

import android.graphics.Bitmap;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Custom WebViewClient with network-level ad-blocking,
 * JavaScript ad/cookie element hiding, YouTube ad skipping,
 * and background playback visibility API bypass.
 */
public class ourViewClient extends WebViewClient {

    private boolean adBlockEnabled = true;

    public void setAdBlockEnabled(boolean enabled) {
        this.adBlockEnabled = enabled;
    }

    public boolean isAdBlockEnabled() {
        return adBlockEnabled;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        // Return false to allow WebView to handle SPA navigations naturally
        return false;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        // Return false to allow WebView to handle SPA navigations naturally
        return false;
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        if (adBlockEnabled && request.getUrl() != null) {
            String url = request.getUrl().toString();
            if (AdBlocker.isAd(url)) {
                return AdBlocker.createEmptyResponse();
            }
        }
        return super.shouldInterceptRequest(view, request);
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        super.onPageStarted(view, url, favicon);
        // Inject background playback visibility bypass early, before page JS runs
        view.evaluateJavascript(AdBlocker.getBackgroundPlaybackScript(), null);
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);

        // Re-inject background playback patch
        view.evaluateJavascript(AdBlocker.getBackgroundPlaybackScript(), null);

        if (adBlockEnabled) {
            // Inject comprehensive ad-block + cookie consent hiding + YouTube ad skipper
            view.evaluateJavascript(AdBlocker.getAdBlockScript(), null);
        }
    }

    @Override
    public void onLoadResource(WebView view, String url) {
        super.onLoadResource(view, url);
        // For SPA pages like YouTube / YouTube Music that don't trigger full page loads,
        // periodically re-ensure background playback script is active
        if (url != null && (url.contains("youtube.com") || url.contains("music.youtube.com"))) {
            view.evaluateJavascript(
                "if(!window.__sbBgPlayback){" + AdBlocker.getBackgroundPlaybackScript() + "}", null);
        }
    }
}
