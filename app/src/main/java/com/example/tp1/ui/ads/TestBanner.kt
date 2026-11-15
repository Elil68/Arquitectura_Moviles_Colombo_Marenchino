package com.example.tp1.ui.ads

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.tp1.AcademicTasksApplication
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

/** Only Google's official anchored adaptive demonstration unit is used. */
private const val TEST_BANNER_ID = "ca-app-pub-3940256099942544/9214589741"

@Composable
fun TestBanner() {
    val context = LocalContext.current
    val application = context.applicationContext as AcademicTasksApplication
    val ready by application.isAdsReady.collectAsState()
    if (!ready) return

    BoxWithConstraints(Modifier.fillMaxWidth().windowInsetsPadding(
        WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
    )) {
        val widthDp = maxWidth.value.toInt()
        if (widthDp > 0) {
            // Replace and release the old view only if the available width changes.
            key(widthDp) {
                AdaptiveTestBanner(widthDp)
            }
        }
    }
}

@Composable
private fun AdaptiveTestBanner(widthDp: Int) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var loaded by remember { mutableStateOf(false) }
    val size = remember(context, widthDp) {
        AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp)
    }
    val adView = remember(context, widthDp) {
        AdView(context).apply {
            adUnitId = TEST_BANNER_ID
            setAdSize(size)
        }
    }

    DisposableEffect(adView, lifecycle) {
        var disposed = false
        var requested = false
        fun resumeAndLoad() {
            adView.resume()
            if (!requested) {
                requested = true
                adView.loadAd(AdRequest.Builder().build())
            }
        }
        adView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                if (!disposed) loaded = true
                Log.d("TP4Ads", "Official test banner loaded")
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                if (!disposed) loaded = false
                Log.w("TP4Ads", "Test banner unavailable: ${error.code} ${error.message}")
            }
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> resumeAndLoad()
                Lifecycle.Event.ON_PAUSE -> adView.pause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) resumeAndLoad()
        onDispose {
            disposed = true
            lifecycle.removeObserver(observer)
            adView.adListener = object : AdListener() {}
            adView.destroy()
        }
    }

    // Keep the loading view composed without reserving space or blocking the tasks.
    Box(
        modifier = if (loaded) Modifier.fillMaxWidth()
            .height(size.height.dp) else Modifier.fillMaxWidth().height(0.dp).clipToBounds(),
        contentAlignment = Alignment.TopCenter
    ) {
        AndroidView(
            factory = { adView },
            modifier = Modifier.size(widthDp.dp, size.height.dp)
        )
    }
}
