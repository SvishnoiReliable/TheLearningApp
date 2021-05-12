package com.learn.view.activity

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.learn.R
import com.learn.prefence.PreferenceProvider
import com.learn.util.*
import com.learn.util.Constant.KEY_PHONE_NUMBER
import com.learn.util.Constant.USER_DATA
import com.learn.viewmodel.SplashViewModel
import com.learn.viewmodel.factory.SplashViewModelFactory
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.iid.FirebaseInstanceId
import kotlinx.android.synthetic.main.activity_splash.*
import kotlinx.coroutines.*
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance

private const val SPLASH_DURATION = 3000L
private const val TAG_FIREBASE_SCREEN_NAME = FirebaseAnalytics.Param.SCREEN_NAME
private const val SCREEN_NAME = "Splash Screen"


class SplashActivity : BaseActivity(), KodeinAware {
    override val kodein by kodein()
    private val preferenceProvider: PreferenceProvider by instance()
    private val mFirebaseAnalytics: FirebaseAnalytics by instance()
    private val mFirebaseCrashlytics: FirebaseCrashlytics by instance()
    private lateinit var splashViewModel: SplashViewModel
    private val factory: SplashViewModelFactory by instance()
    private var mPhoneNumber: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        splashViewModel = ViewModelProvider(this, factory).get(SplashViewModel::class.java)
        if (checkGooglePlayServices(this)) {
            FirebaseInstanceId.getInstance().instanceId
                .addOnCompleteListener(OnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        Log.w("FIREBASE TOKEN Splash", "getInstanceId failed", task.exception)
                        return@OnCompleteListener
                    }
                    val token = task.result?.token
                    //Need to store in Shared prefs or send to server
                    token?.let { PreferenceProvider(this).putString(Constant.FIREBASE_TOKEN, it) }
                })
        } else {
            //You won't be able to send notifications to this device
            Log.w("Google Play Services", "Device doesn't have google play services")
        }

        scheduleSplashScreen()
        val info = packageManager.getPackageInfo(this.packageName, PackageManager.GET_ACTIVITIES)

        val text2020 =
            SpannableString("${getString(R.string.splash_info)}${info.versionName}")
        text2020.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(this, R.color.light_grey)),
            text2020.indexOf("|"), text2020.indexOf("V"),
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        version.text = text2020
        if (isNavigationBarShow()) {
            val constraintSet = ConstraintSet()
            constraintSet.clone(rootSplash)
            constraintSet.setMargin(
                R.id.version,
                ConstraintSet.BOTTOM,
                getHeightOfNavigationBar() + 20
            )
            constraintSet.applyTo(rootSplash)
        }

        val bundle = Bundle()
        bundle.putString(TAG_FIREBASE_SCREEN_NAME, SCREEN_NAME)
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle)

        mFirebaseCrashlytics.setCrashlyticsCollectionEnabled(true)
        mFirebaseCrashlytics.log("Splash Screen Started-> Crashlytics Report")

    }

    private fun scheduleSplashScreen() {
        CoroutineScope(Dispatchers.Main).launch {
            awaitAll(async { getPhoneNumber() }, async { delay(SPLASH_DURATION) })
            routeNextScreen(preferenceProvider.getBoolean(Constant.IS_LOGIN))
            finish()
        }
    }

    private suspend fun getPhoneNumber() {
        if (preferenceProvider.getString(USER_DATA).isNullOrEmpty()) {
            try {
                val phoneNumber = splashViewModel.getPhoneNumber()
                mPhoneNumber = phoneNumber.data?.phone
                if (mPhoneNumber.isNullOrEmpty()) preferenceProvider.putString(KEY_PHONE_NUMBER, "")
                else preferenceProvider.putString(KEY_PHONE_NUMBER, mPhoneNumber!!)
            } catch (e: ApiException) {
                preferenceProvider.putString(KEY_PHONE_NUMBER, "")
                e.printStackTrace()
            }
        }
    }

    private fun routeNextScreen(userLoggedIn: Boolean) {
        intent = when {
            userLoggedIn -> Intent(this, HomeActivity::class.java).apply {
                intent.extras?.let { putExtras(it) }
            }
            else -> {
                Intent(this, OnBoardingActivity::class.java)
            }
        }
        startActivity(intent)
    }
}
