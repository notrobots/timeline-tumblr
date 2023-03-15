package dev.notrobots.timeline.jumblrdemo

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isGone
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val webView = findViewById<WebView>(R.id.webview)
        val state = UUID.randomUUID().toString()
        val requestBuilder = App.tumblr.requestBuilder

        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()

        webView.clearCache(true)
        webView.clearHistory()
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)

                if (requestBuilder.isFinalRequestUrl(url)) {
                    view.stopLoading()
                    view.isGone = true

                    lifecycleScope.launch(Dispatchers.Default) {
                        try {
                            requestBuilder.onUserChallenge(url, state)

                            val name = App.tumblr.user().name

                            lifecycleScope.launch (Dispatchers.Main) {
                                Toast.makeText(this@LoginActivity, "Welcome $name", Toast.LENGTH_LONG).show()
                            }

                            finish()
                            startActivity(Intent(this@LoginActivity, ProfileActivity::class.java))
                        }catch (e: Exception) {
                            Log.e("JUMBLR", "Access token error", e)
                        }
                    }
                }
            }
        }
        webView.loadUrl(App.oauthService.getAuthorizationUrl(state))
    }
}