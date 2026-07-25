package stoyanov.stoyan.simplebrowser;

import android.graphics.Bitmap;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Custom WebViewClient with ad-blocking and background playback visibility patch.
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
        view.loadUrl(url);
        return true;
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
        // Inject background playback visibility API bypass script early
        view.evaluateJavascript(AdBlocker.getBackgroundPlaybackScript(), null);
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);

        // Always inject background playback visibility patch
        view.evaluateJavascript(AdBlocker.getBackgroundPlaybackScript(), null);

        if (adBlockEnabled) {
            // Inject ad-block script
            view.evaluateJavascript(AdBlocker.getAdBlockScript(), null);
        }
    }
}
