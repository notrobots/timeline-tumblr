package dev.notrobots.timeline.jumblrdemo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat

class ProfileActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        lifecycleScope.launch(Dispatchers.Default) {
            if (!App.tumblr.requestBuilder.lastToken.isExpired()) {
                try {
                    val name = App.tumblr.user().name
                    val expiry = App.tumblr.requestBuilder.lastToken.expirationDate

                    lifecycleScope.launch(Dispatchers.Main) {
                        findViewById<TextView>(R.id.name).text = name
                        findViewById<TextView>(R.id.expiry).text = SimpleDateFormat("HH:mm").format(expiry)
                    }
                } catch (e: Exception) {
                    Log.e("JUMBLRDEMO", "Cannot fetch user data", e)
                }
            }
        }

        val refreshButton = findViewById<Button>(R.id.refresh_token)

        refreshButton.isVisible = App.tumblr.requestBuilder.lastToken.isExpired()
        refreshButton.setOnClickListener {
            lifecycleScope.launch(Dispatchers.Default) {
                App.tumblr.requestBuilder.refreshAccessToken()
            }
        }

        findViewById<Button>(R.id.logout).setOnClickListener {
            App.tumblr.logout()
            finish()
            startActivity(Intent(this, MainActivity::class.java))
        }
    }
}