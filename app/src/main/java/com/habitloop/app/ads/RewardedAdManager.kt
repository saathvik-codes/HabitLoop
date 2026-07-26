package com.habitloop.app.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class RewardedAdState { Loading, Ready, Showing, Unavailable }

/**
 * Watch-an-ad-to-earn-a-streak-freeze-token. This is the monetization path
 * that's tied directly into the retention loop (grace-day mechanic) rather
 * than a banner nobody looks at — the ad IS the perk-earning moment.
 *
 * Uses Google's official test rewarded ad unit ID. Replace with your real
 * AdMob rewarded ad unit ID before release — using this test ID in a
 * published app generates no real revenue and can violate AdMob policy.
 */
object RewardedAdManager {
    private const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

    private var rewardedAd: RewardedAd? = null
    private val _state = MutableStateFlow(RewardedAdState.Unavailable)
    val state: StateFlow<RewardedAdState> = _state.asStateFlow()

    fun preload(context: Context) {
        if (_state.value == RewardedAdState.Loading || _state.value == RewardedAdState.Ready) return
        _state.value = RewardedAdState.Loading
        RewardedAd.load(
            context,
            TEST_REWARDED_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    _state.value = RewardedAdState.Ready
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    _state.value = RewardedAdState.Unavailable
                }
            }
        )
    }

    fun showForFreezeToken(activity: Activity, onEarned: () -> Unit, onUnavailable: () -> Unit) {
        val ad = rewardedAd
        if (ad == null) {
            onUnavailable()
            _state.value = RewardedAdState.Unavailable
            preload(activity)
            return
        }
        _state.value = RewardedAdState.Showing
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                _state.value = RewardedAdState.Unavailable
                preload(activity)
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                rewardedAd = null
                _state.value = RewardedAdState.Unavailable
                onUnavailable()
                preload(activity)
            }
        }
        ad.show(activity) { onEarned() }
    }
}
