package dev.notrobots.timeline.jumblrdemo

import android.app.Application
import com.github.scribejava.core.builder.ServiceBuilder
import com.github.scribejava.core.oauth.OAuth20Service
import com.tumblr.jumblr.JumblrClient
import dev.notrobots.timeline.oauth.OAuth2TokenStore
import dev.notrobots.timeline.oauth.apis.TumblrApi20

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        val userAgent = "jumblr/0.0.13"

        oauthService = ServiceBuilder(BuildConfig.TUMBLR_CONSUMER_KEY)
            .callback("https://localhost/")
            .userAgent(userAgent)
            .defaultScope("basic offline_access")
            .apiSecret(BuildConfig.TUMBLR_CONSUMER_SECRET)
            .build(TumblrApi20.instance())

        tokenStore = OAuth2TokenStore(this)
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
        lateinit var tokenStore: OAuth2TokenStore
        lateinit var oauthService: OAuth20Service
    }
}