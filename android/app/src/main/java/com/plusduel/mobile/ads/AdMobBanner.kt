package com.plusduel.mobile.ads

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.plusduel.mobile.ui.theme.PlusDuelTheme

object AdMobConfig {
    const val BANNER_AD_UNIT_ID = "ca-app-pub-9759510127182695/7697756024"

    fun initialize(context: Context) {
        runCatching {
            MobileAds.initialize(context) {}
        }
    }
}

/**
 * Anchored adaptive bottom banner displayed right above the BottomNavigationBar in menu screens.
 * Reserves a stable 52.dp minimum height so the screen layout never jumps while the banner loads.
 */
@Composable
fun AdMobBannerBar(
    modifier: Modifier = Modifier
) {
    val colors = PlusDuelTheme.colors
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.coerceAtLeast(320)

    var adLoaded by remember { mutableStateOf(false) }

    val adView = remember(context, screenWidthDp) {
        AdView(context).apply {
            adUnitId = AdMobConfig.BANNER_AD_UNIT_ID
            val adSize = runCatching {
                AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, screenWidthDp)
            }.getOrDefault(AdSize.BANNER)
            setAdSize(adSize)
            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    adLoaded = true
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    adLoaded = false
                }
            }
            runCatching {
                loadAd(AdRequest.Builder().build())
            }
        }
    }

    DisposableEffect(adView) {
        onDispose {
            runCatching { adView.destroy() }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .background(colors.surface)
            .border(0.75.dp, colors.line)
            .padding(vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        if (!adLoaded) {
            Text(
                text = "AD · PLUSDUEL 1V1 ARENA",
                color = colors.muted.copy(alpha = 0.45f),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp
            )
        }
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { adView }
        )
    }
}
