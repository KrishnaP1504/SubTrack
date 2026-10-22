package com.example.subtrack.util

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.material.card.MaterialCardView
import java.util.Locale

object LogoUtils {

    /**
     * Resolves subscription names (including typos and variations) to the company's official domain.
     */
    fun getDomain(subscriptionName: String): String {
        val normalized = subscriptionName.lowercase(Locale.ROOT).trim()

        return when {
            // Netflix (including common typos like Nerflix, Netflx, Netfix)
            normalized.contains("netf") || normalized.contains("nerf") || normalized.contains("netflix") -> "netflix.com"

            // Spotify
            normalized.contains("spot") -> "spotify.com"

            // Amazon Prime Video
            normalized.contains("prime") || normalized.contains("amazon") -> "primevideo.com"

            // Disney+ / Hotstar
            normalized.contains("hotstar") || normalized.contains("disney") -> "hotstar.com"

            // YouTube / Google
            normalized.contains("youtube") || normalized.contains("yt ") -> "youtube.com"
            normalized.contains("google") || normalized.contains("drive") || normalized.contains("gmail") -> "google.com"

            // Apple (Apple Music, Apple TV, iCloud)
            normalized.contains("apple") || normalized.contains("icloud") || normalized.contains("itunes") -> "apple.com"

            // OpenAI / ChatGPT
            normalized.contains("chatgpt") || normalized.contains("openai") || normalized.contains("gpt") -> "openai.com"

            // Microsoft / Office 365 / Xbox
            normalized.contains("microsoft") || normalized.contains("office") || normalized.contains("xbox") || normalized.contains("365") -> "microsoft.com"

            // GitHub
            normalized.contains("github") || normalized.contains("git") -> "github.com"

            // Adobe (Photoshop, Creative Cloud)
            normalized.contains("adobe") || normalized.contains("photoshop") || normalized.contains("illustrator") -> "adobe.com"

            // Productivity / SaaS
            normalized.contains("notion") -> "notion.so"
            normalized.contains("canva") -> "canva.com"
            normalized.contains("figma") -> "figma.com"
            normalized.contains("slack") -> "slack.com"
            normalized.contains("zoom") -> "zoom.us"
            normalized.contains("dropbox") -> "dropbox.com"
            normalized.contains("linkedin") -> "linkedin.com"
            normalized.contains("claude") || normalized.contains("anthropic") -> "anthropic.com"
            normalized.contains("grammarly") -> "grammarly.com"

            // Entertainment & Streaming
            normalized.contains("twitch") -> "twitch.tv"
            normalized.contains("crunchyroll") -> "crunchyroll.com"
            normalized.contains("hulu") -> "hulu.com"
            normalized.contains("hbo") || normalized.contains("max") -> "max.com"
            normalized.contains("paramount") -> "paramountplus.com"
            normalized.contains("peacock") -> "peacocktv.com"

            // Indian Services
            normalized.contains("jio") -> "jiocinema.com"
            normalized.contains("sony") || normalized.contains("liv") -> "sonyliv.com"
            normalized.contains("zee") || normalized.contains("zee5") -> "zee5.com"
            normalized.contains("swiggy") -> "swiggy.com"
            normalized.contains("zomato") -> "zomato.com"
            normalized.contains("blinkit") -> "blinkit.com"
            normalized.contains("cult") || normalized.contains("cure") -> "cure.fit"
            normalized.contains("gaana") -> "gaana.com"
            normalized.contains("wynk") -> "wynk.in"
            normalized.contains("times") -> "timesprime.com"

            // Gaming
            normalized.contains("playstation") -> "playstation.com"
            normalized.contains("nintendo") -> "nintendo.com"
            normalized.contains("steam") -> "steampowered.com"

            // Education & Fitness
            normalized.contains("duolingo") -> "duolingo.com"
            normalized.contains("coursera") -> "coursera.org"
            normalized.contains("udemy") -> "udemy.com"

            // VPNs
            normalized.contains("nord") -> "nordvpn.com"
            normalized.contains("expressvpn") -> "expressvpn.com"

            // User entered a full domain already (e.g. medium.com)
            normalized.contains(".") -> normalized.replace(" ", "")

            // Fallback: strip spaces and try .com
            else -> normalized.replace(" ", "") + ".com"
        }
    }

    /**
     * Returns Google's high-res 128px favicon URL for the service.
     */
    fun getLogoUrl(subscriptionName: String): String {
        val domain = getDomain(subscriptionName)
        return "https://t2.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url=https://$domain&size=128"
    }

    /**
     * Loads the company logo into the ImageView with fallback to DuckDuckGo icon and colored letter.
     */
    fun loadLogo(
        imageView: ImageView,
        subscriptionName: String,
        fallbackLetterView: TextView? = null,
        cardView: MaterialCardView? = null,
        fallbackColor: String? = null
    ) {
        val domain = getDomain(subscriptionName)
        val primaryUrl = "https://t2.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url=https://$domain&size=128"
        val fallbackUrl = "https://icons.duckduckgo.com/ip3/$domain.ico"

        val density = imageView.context.resources.displayMetrics.density
        val paddingPx = (6 * density).toInt()

        // Initially show letter while image loads
        fallbackLetterView?.let {
            it.text = subscriptionName.firstOrNull()?.uppercase() ?: "S"
            it.visibility = View.VISIBLE
        }
        imageView.visibility = View.INVISIBLE

        Glide.with(imageView.context)
            .load(primaryUrl)
            .error(
                Glide.with(imageView.context)
                    .load(fallbackUrl)
                    .centerInside()
            )
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .transition(DrawableTransitionOptions.withCrossFade(250))
            .centerInside()
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    fallbackLetterView?.visibility = View.VISIBLE
                    imageView.visibility = View.GONE
                    if (fallbackColor != null && cardView != null) {
                        try {
                            cardView.setCardBackgroundColor(Color.parseColor(fallbackColor))
                        } catch (_: Exception) {}
                    }
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable,
                    model: Any,
                    target: Target<Drawable>?,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    fallbackLetterView?.visibility = View.GONE
                    imageView.visibility = View.VISIBLE
                    imageView.setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
                    // High-contrast clean white background for the logo
                    cardView?.setCardBackgroundColor(Color.WHITE)
                    return false
                }
            })
            .into(imageView)
    }
}
