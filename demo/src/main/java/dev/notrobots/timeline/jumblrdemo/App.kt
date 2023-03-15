package dev.notrobots.timeline.jumblrdemo

import android.app.Application
import com.tumblr.jumblr.JumblrClient
import dev.notrobots.timeline.oauth.OAuth2TokenStore

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        val userAgent = "jumblr/0.0.13"
        val tokenStore = OAuth2TokenStore(this)
        tokenStore.load()
        tokenStore.autoPersist = true

        tumblr = JumblrClient(
            BuildConfig.TUMBLR_CONSUMER_KEY,
            BuildConfig.TUMBLR_CONSUMER_SECRET,
            userAgent,
            "https://localhost/",
            tokenStore
        )
    }

    companion object {
        lateinit var tumblr: JumblrClient
    }
}