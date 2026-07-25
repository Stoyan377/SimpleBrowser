package stoyanov.stoyan.simplebrowser;

import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Custom WebViewClient with ad-blocking support.
 * When ad-blocking is enabled, intercepts requests to known ad-serving domains
 * and returns empty responses, plus injects ad-hiding JavaScript on page load.
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
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);

        if (adBlockEnabled) {
            // Inject the comprehensive ad-block script
            view.evaluateJavascript(AdBlocker.getAdBlockScript().replace("javascript:", ""), null);
        }
    }
}
