package com.example.subtrack.util

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.target.Target
import android.graphics.drawable.Drawable
import java.util.Locale

object LogoUtils {

    /**
     * Maps subscription names to their company domain for logo lookup.
     */
    fun getLogoUrl(subscriptionName: String): String {
        val normalized = subscriptionName.lowercase(Locale.ROOT).trim()
        val domain = when {
            normalized.contains("netflix") -> "netflix.com"
            normalized.contains("spotify") -> "spotify.com"
            normalized.contains("amazon") || normalized.contains("prime") -> "primevideo.com"
            normalized.contains("apple") -> "apple.com"
            normalized.contains("disney") -> "disneyplus.com"
            normalized.contains("hulu") -> "hulu.com"
            normalized.contains("youtube") -> "youtube.com"
            normalized.contains("adobe") -> "adobe.com"
            normalized.contains("microsoft") || normalized.contains("xbox") -> "microsoft.com"
            normalized.contains("playstation") || normalized.contains("sony") -> "playstation.com"
            normalized.contains("chatgpt") || normalized.contains("openai") -> "openai.com"
            normalized.contains("github") -> "github.com"
            normalized.contains("notion") -> "notion.so"
            normalized.contains("figma") -> "figma.com"
            normalized.contains("slack") -> "slack.com"
            normalized.contains("zoom") -> "zoom.us"
            normalized.contains("dropbox") -> "dropbox.com"
            normalized.contains("google") -> "google.com"
            normalized.contains("canva") -> "canva.com"
            normalized.contains("twitch") -> "twitch.tv"
            normalized.contains("paramount") -> "paramountplus.com"
            normalized.contains("hbo") || normalized.contains("max") -> "max.com"
            normalized.contains("crunchyroll") -> "crunchyroll.com"
            normalized.contains("linkedin") -> "linkedin.com"
            normalized.contains("claude") || normalized.contains("anthropic") -> "anthropic.com"
            normalized.contains("grammarly") -> "grammarly.com"
            normalized.contains("icloud") -> "icloud.com"
            normalized.contains("onedrive") -> "onedrive.live.com"
            normalized.contains("nordvpn") || normalized.contains("nord") -> "nordvpn.com"
            normalized.contains("expressvpn") -> "expressvpn.com"
            normalized.contains("duolingo") -> "duolingo.com"
            normalized.contains("coursera") -> "coursera.org"
            normalized.contains("udemy") -> "udemy.com"
            else -> {
                // Best-effort: strip spaces and add .com
                normalized.replace(" ", "") + ".com"
            }
        }
        return "${com.example.subtrack.BuildConfig.LOGO_BASE_URL}/$domain"
    }

    /**
     * Loads the company logo into the ImageView. If the logo fails to load,
     * the fallbackLetterView is shown with the first letter of the name.
     */
    fun loadLogo(
        imageView: ImageView,
        subscriptionName: String,
        fallbackLetterView: TextView? = null,
        fallbackColor: String? = null
    ) {
        val url = getLogoUrl(subscriptionName)

        // Setup fallback letter
        fallbackLetterView?.let {
            it.text = subscriptionName.firstOrNull()?.uppercase() ?: "S"
            if (fallbackColor != null) {
                try {
                    it.setTextColor(Color.WHITE)
                } catch (_: Exception) {}
            }
        }

        Glide.with(imageView.context)
            .load(url)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .transition(DrawableTransitionOptions.withCrossFade())
            .centerCrop()
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    // Logo failed — show the fallback letter
                    fallbackLetterView?.visibility = View.VISIBLE
                    imageView.visibility = View.GONE
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable,
                    model: Any,
                    target: Target<Drawable>?,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    // Logo loaded — hide the fallback letter
                    fallbackLetterView?.visibility = View.GONE
                    imageView.visibility = View.VISIBLE
                    return false
                }
            })
            .into(imageView)
    }
}
