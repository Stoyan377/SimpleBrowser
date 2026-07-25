package stoyanov.stoyan.simplebrowser;

import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Custom WebViewClient with ad-blocking support.
 * When ad-blocking is enabled, intercepts requests to known ad-serving domains
 * and returns empty responses.
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

        // Inject CSS to hide common ad containers when ad-blocking is enabled
        if (adBlockEnabled) {
            String cssHideAds =
                "javascript:(function(){" +
                "var style = document.createElement('style');" +
                "style.textContent = '" +
                // Common ad container selectors
                "[id*=\"google_ads\"], [id*=\"ad-container\"], [id*=\"ad_container\"], " +
                "[class*=\"ad-banner\"], [class*=\"ad_banner\"], [class*=\"adsbygoogle\"], " +
                "ins.adsbygoogle, [id*=\"sponsored\"], [class*=\"sponsored\"], " +
                "[data-ad], [data-ads], [data-ad-slot], " +
                "iframe[src*=\"doubleclick\"], iframe[src*=\"googlesyndication\"], " +
                "[class*=\"ad-slot\"], [id*=\"div-gpt-ad\"], " +
                "[class*=\"advertisement\"], [id*=\"advertisement\"] " +
                "{ display: none !important; }';" +
                "document.head.appendChild(style);" +
                "})()";
            view.loadUrl(cssHideAds);
        }
    }
}
