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
    if (start >= 0) {
        val rest = decoded.substring(start + marker.length)
        val end = rest.indexOf('&').let { if (it < 0) rest.length else it }
        val image = rest.substring(0, end)
        if (image.startsWith("http://") || image.startsWith("https://")) {
            return image
        }
    }
    if (raw.startsWith("data:image/")) {
        return raw
    }
    val clean = raw.lowercase().substringBefore('?').substringBefore('#')
    if (clean.endsWith(".png") || clean.endsWith(".jpg") || clean.endsWith(".jpeg") ||
        clean.endsWith(".webp") || clean.endsWith(".gif") || clean.endsWith(".svg")) {
        return raw
    }
    return null
}

private const val IMAGE_TAP_SCRIPT = """
(function() {
  if (window.__avitriImagesInstalled) return;
  window.__avitriImagesInstalled = true;

  function isIgnored(el) {
    if (!el) return true;
    if (el.closest && (
      el.closest('header') ||
      el.closest('nav') ||
      el.closest('form') ||
      el.closest('#header') ||
      el.closest('#searchform') ||
      el.closest('#fbar') ||
      el.closest('.sfbg') ||
      el.closest('.sfcnt') ||
      el.closest('[role="search"]') ||
      el.closest('[role="navigation"]')
    )) {
      return true;
    }
    return false;
  }

  function extractUrl(target) {
    if (!target || isIgnored(target)) return null;

    var link = target.closest ? target.closest('a') : null;
    if (link && link.href) {
      var m = link.href.match(/[?&]imgurl=([^&]+)/);
      if (m && m[1]) {
        try { return decodeURIComponent(m[1]); } catch(e) { return m[1]; }
      }
    }

    var img = target.tagName === 'IMG' ? target : (target.querySelector ? target.querySelector('img') : null);
    if (!img && target.closest) img = target.closest('img');
    if (img && !isIgnored(img)) {
      var rect = img.getBoundingClientRect();
      if (rect.width >= 40 && rect.height >= 40) {
        var src = img.getAttribute('data-src') || img.currentSrc || img.src || img.getAttribute('data-iurl') || '';
        if (src && src.length > 25 && !src.includes('googlelogo_') && !src.includes('cleardot.gif') && !src.includes('nav_logo')) {
          return src;
        }
      }
    }

    var bgEl = target.closest ? target.closest('[style*="background"]') : null;
    if (bgEl && !isIgnored(bgEl)) {
      var rect = bgEl.getBoundingClientRect();
      if (rect.width >= 40 && rect.height >= 40) {
        var style = window.getComputedStyle(bgEl);
        var bg = style.backgroundImage;
        if (bg && bg.indexOf('url(') !== -1) {
          var clean = bg.replace(/^url\(["']?/, '').replace(/["']?\)$/, '');
          if (clean.length > 25 && !clean.includes('googlelogo_') && !clean.includes('cleardot.gif')) {
            return clean;
          }
        }
      }
    }
    return null;
  }

  document.addEventListener('click', function(e) {
    var url = extractUrl(e.target);
    if (url && (url.indexOf('http://') === 0 || url.indexOf('https://') === 0 || url.indexOf('data:image/') === 0)) {
      e.preventDefault();
      e.stopPropagation();
      Avitri.place(url);
    }
  }, true);
})();
"""

private const val YOUTUBE_TAP_SCRIPT = """
(function() {
  if (window.__avitriYouTubeInstalled) return;
  window.__avitriYouTubeInstalled = true;

  function findVideoUrl(target) {
    if (!target) return null;
    if (target.closest && (
      target.closest('header') ||
      target.closest('#header-bar') ||
      target.closest('form') ||
      target.closest('.mobile-topbar-header') ||
      target.closest('input')
    )) {
      return null;
    }

    var a = target.closest ? target.closest('a') : null;
    if (a && a.href) {
      if (a.href.indexOf('/watch?v=') !== -1 || a.href.indexOf('/shorts/') !== -1 || a.href.indexOf('youtu.be/') !== -1) {
        return a.href;
      }
    }
    
    var renderer = target.closest ? target.closest('ytm-compact-video-renderer, ytm-video-with-context-renderer, ytm-compact-radio-renderer, ytm-video-renderer, ytd-video-renderer') : null;
    if (renderer) {
      var link = renderer.querySelector('a[href*="/watch?v="], a[href*="/shorts/"]');
      if (link && link.href) return link.href;
    }
    return null;
  }

  document.addEventListener('click', function(e) {
    var url = findVideoUrl(e.target);
    if (url) {
      e.preventDefault();
      e.stopPropagation();
      Avitri.place(url);
    }
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

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
fun WebView.browseInsideApp(
    onImage: ((String) -> Unit)? = null,
    onVideo: ((String) -> Unit)? = null,
    onNavigate: ((String) -> Boolean)? = null
) {
    settings.javaScriptEnabled = true
    settings.domStorageEnabled = true
    settings.javaScriptCanOpenWindowsAutomatically = false
    settings.setSupportMultipleWindows(false)
    settings.loadsImagesAutomatically = true
    settings.userAgentString =
        "Mozilla/5.0 (Linux; Android 13; Tablet) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    val callback = onImage ?: onVideo
    if (callback != null) {
        addJavascriptInterface(ImageBridge(callback), "Avitri")
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
                    if (onVideo != null && youtubeVideoId(url) != null) {
                        onVideo(url)
                        return true
                    }
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
            val url = request.url.toString()
            if (onVideo != null && youtubeVideoId(url) != null) {
                onVideo(url)
                return true
            }
            return keepInside(view, url, onImage, onNavigate)
        }

        @Deprecated("Legacy WebView callback")
        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            if (onVideo != null && youtubeVideoId(url) != null) {
                onVideo(url)
                return true
            }
            return keepInside(view, url, onImage, onNavigate)
        }

        override fun onPageFinished(view: WebView, url: String) {
            if (onImage != null) view.evaluateJavascript(IMAGE_TAP_SCRIPT, null)
            if (onVideo != null) view.evaluateJavascript(YOUTUBE_TAP_SCRIPT, null)
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
