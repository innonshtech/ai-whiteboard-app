package com.avitrisphare.whiteboard.presentation.canvas

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import java.net.URLDecoder

fun googleImageResultUrl(raw: String): String? {
    val decoded = try {
        URLDecoder.decode(raw, "UTF-8")
    } catch (_: Exception) {
        raw
    }
    val marker = "imgurl="
    val start = decoded.indexOf(marker)
    if (start < 0) return null
    val rest = decoded.substring(start + marker.length)
    val end = rest.indexOf('&').let { if (it < 0) rest.length else it }
    val image = rest.substring(0, end)
    return image.takeIf { it.startsWith("http://") || it.startsWith("https://") }
}

private const val IMAGE_TAP_SCRIPT = """
(function() {
  if (window.__avitriImages) return;
  window.__avitriImages = true;
  document.addEventListener('click', function(e) {
    var node = e.target;
    if (!node) return;
    var link = node.closest ? node.closest('a') : null;
    if (link && link.href && link.href.indexOf('imgurl=') !== -1) {
      e.preventDefault();
      e.stopPropagation();
      Avitri.place(link.href);
      return;
    }
    var img = node.closest ? node.closest('img') : null;
    if (!img && node.tagName === 'IMG') img = node;
    if (!img) return;
    var src = img.currentSrc || img.src || '';
    if (!src || src.indexOf('data:') === 0) return;
    e.preventDefault();
    e.stopPropagation();
    Avitri.place(src);
  }, true);
})();
"""

class ImageBridge(private val onPlace: (String) -> Unit) {
    @JavascriptInterface
    fun place(url: String) {
        if (url.isBlank()) return
        Handler(Looper.getMainLooper()).post { onPlace(url) }
    }
}

@SuppressLint("SetJavaScriptEnabled")
fun WebView.browseInsideApp(
    onImage: ((String) -> Unit)? = null,
    onNavigate: ((String) -> Boolean)? = null
) {
    settings.javaScriptEnabled = true
    settings.domStorageEnabled = true
    settings.javaScriptCanOpenWindowsAutomatically = false
    settings.setSupportMultipleWindows(false)
    settings.loadsImagesAutomatically = true
    settings.userAgentString =
        "Mozilla/5.0 (Linux; Android 13; Tablet) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    if (onImage != null) {
        addJavascriptInterface(ImageBridge(onImage), "Avitri")
    }
    webChromeClient = object : WebChromeClient() {
        override fun onCreateWindow(
            view: WebView,
            isDialog: Boolean,
            isUserGesture: Boolean,
            resultMsg: Message
        ): Boolean {
            val transport = resultMsg.obj as? WebView.WebViewTransport ?: return false
            val popup = WebView(view.context)
            popup.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(popupView: WebView, request: WebResourceRequest): Boolean {
                    val url = request.url.toString()
                    if (onNavigate?.invoke(url) == true) return true
                    if (onImage != null && deliverImage(url, onImage)) return true
                    if (url.startsWith("http://") || url.startsWith("https://")) view.loadUrl(url)
                    return true
                }
            }
            transport.webView = popup
            resultMsg.sendToTarget()
            return true
        }
    }
    webViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            return keepInside(view, request.url.toString(), onImage, onNavigate)
        }

        @Deprecated("Legacy WebView callback")
        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            return keepInside(view, url, onImage, onNavigate)
        }

        override fun onPageFinished(view: WebView, url: String) {
            if (onImage != null) view.evaluateJavascript(IMAGE_TAP_SCRIPT, null)
        }
    }
}

private fun keepInside(
    view: WebView,
    url: String,
    onImage: ((String) -> Unit)?,
    onNavigate: ((String) -> Boolean)?
): Boolean {
    if (onNavigate?.invoke(url) == true) return true
    if (onImage != null && deliverImage(url, onImage)) return true
    if (url.startsWith("http://") || url.startsWith("https://")) return false
    return true
}

private fun deliverImage(url: String, onImage: (String) -> Unit): Boolean {
    val image = googleImageResultUrl(url) ?: return false
    onImage(image)
    return true
}
