package com.dizvik

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.dizvik.model.OperationDolfilPushNotification
import com.dizvik.prefence.PreferenceProvider
import com.dizvik.util.Constant
import com.dizvik.util.Constant.DUMMY_NOTIFICATION_DEFAULT_CHANNEL_ID
import com.dizvik.util.Constant.FIREBASE_TOKEN
import com.dizvik.util.Constant.FIREBASE_TOKEN_UPDATED
import com.dizvik.util.Constant.MOCK_INTERVIEW_APPEARED
import com.dizvik.util.Constant.MOCK_INTERVIEW_ATTENDED
import com.dizvik.util.Constant.NOTIFICATION_ASSIGNMENT
import com.dizvik.util.Constant.NOTIFICATION_ASSIGNMENT_EVALUATED
import com.dizvik.util.Constant.NOTIFICATION_ASSIGNMENT_REJECTED
import com.dizvik.util.Constant.NOTIFICATION_CATEGORY_TITLE
import com.dizvik.util.Constant.NOTIFICATION_CHAT_MENTION
import com.dizvik.util.Constant.NOTIFICATION_COURSE
import com.dizvik.util.Constant.NOTIFICATION_DOCUMENT
import com.dizvik.util.Constant.NOTIFICATION_LIVE_BATCH_CLASS_APPROVED
import com.dizvik.util.Constant.NOTIFICATION_LIVE_BATCH_CLASS_REJECTED
import com.dizvik.util.Constant.NOTIFICATION_LIVE_BATCH_CLASS_STARTED
import com.dizvik.util.Constant.NOTIFICATION_LIVE_BATCH_ITEM_STARTED
import com.dizvik.util.Constant.NOTIFICATION_LIVE_BATCH_STARTED
import com.dizvik.util.Constant.NOTIFICATION_LIVE_BATCH_TESTSET_STARTED
import com.dizvik.util.Constant.NOTIFICATION_LIVE_CLASS_STARTS_AT
import com.dizvik.util.Constant.NOTIFICATION_LIVE_CLASS_TITLE
import com.dizvik.util.Constant.NOTIFICATION_MCQ
import com.dizvik.util.Constant.NOTIFICATION_PRACTICE
import com.dizvik.util.Constant.NOTIFICATION_TARGET_NAME
import com.dizvik.util.Constant.NOTIFICATION_TEST_SET_TYPE
import com.dizvik.util.Constant.NOTIFICATION_UNAUTHORIZED
import com.dizvik.util.Constant.NOTIFICATION_USER_DEGREE
import com.dizvik.util.Constant.NOTIFICATION_USER_FULL_NAME
import com.dizvik.util.Constant.NOTIFICATION_USER_PROFILE_URL
import com.dizvik.util.Constant.NOTIFICATION_VIDEO
import com.dizvik.util.Constant.SPECIAL_CLASS_STARTED
import com.dizvik.util.clearLogInPrefsAndReDirectToLoginScreen
import com.dizvik.util.deleteCache
import com.dizvik.util.liveClassNotification
import com.dizvik.view.activity.HomeActivity
import com.dizvik.view.activity.OnBoardingActivity
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.json.JSONException


class DolfilFirebaseMessagingService : FirebaseMessagingService() {

    private var mNotificationManager: NotificationManager? = null
    private val GROUP_KEY_DOLFIL = "group_key_dolfil"
    private val channelName = "dizvikNotificationChannel"

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val preferenceProvider = PreferenceProvider(this)
        var oldToken: String? = null
        preferenceProvider.getString(FIREBASE_TOKEN)
            ?.let { oldToken = it }
        preferenceProvider.putBoolean(
            FIREBASE_TOKEN_UPDATED,
            (!oldToken.isNullOrEmpty() && !oldToken.equals(
                token,
                true
            ))
        )
        preferenceProvider.putString(
            FIREBASE_TOKEN,
            token
        )
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        handleMessage(
            remoteMessage,
            this
        )
    }

    private fun handleMessage(
        remoteMessage: RemoteMessage,
        dolfilFirebaseMessagingService: DolfilFirebaseMessagingService
    ) {
        if (!NotificationManagerCompat.from(this)
                .areNotificationsEnabled()
        ) {
            return
        }

        val handler = Handler(Looper.getMainLooper())
        handler.post {
            if (remoteMessage.data.containsValue(NOTIFICATION_LIVE_BATCH_CLASS_STARTED)) {
                liveClassNotification(
                    this,
                    remoteMessage
                )
            } else {
                handleNotification(
                    dolfilFirebaseMessagingService,
                    remoteMessage
                )
            }
        }
    }

    private fun handleNotification(
        context: DolfilFirebaseMessagingService,
        it: RemoteMessage
    ) {
        try {
            if (it.data.containsValue(NOTIFICATION_VIDEO)
                || it.data.containsValue(NOTIFICATION_COURSE)
                || it.data.containsValue(NOTIFICATION_MCQ)
                || it.data.containsValue(NOTIFICATION_UNAUTHORIZED)
                || it.data.containsValue(NOTIFICATION_PRACTICE)
                || it.data.containsValue(NOTIFICATION_ASSIGNMENT)
                || it.data.containsValue(NOTIFICATION_DOCUMENT)
                || it.data.containsValue(NOTIFICATION_CHAT_MENTION)
                || it.data.containsValue(SPECIAL_CLASS_STARTED)
                || isLiveBatchClassStarted(it)
                || it.data.containsValue(MOCK_INTERVIEW_ATTENDED)
            ) {
                createNotification(
                    context,
                    it,
                    notificationNavigation(
                        context,
                        it
                    )
                )
            } else if (it.data.containsValue(MOCK_INTERVIEW_APPEARED)) {
                createNotification(
                    context,
                    it,
                    notificationNavigation(
                        context,
                        it
                    )
                )
                LocalBroadcastManager.getInstance(this)
                    .sendBroadcast(Intent(Constant.MOCK_INTERVIEW_INTENT_FILTER))
            } else if (it.data.containsKey(key = getString(R.string.type))
                && it.data[getString(R.string.type)] == getString(R.string.admin)
            ) {
                performOperation(it.data[getString(R.string.operation)])
            } else {
                createNotification(
                    context,
                    it
                )
            }
        } catch (e: JSONException) {
            e.message?.let { it1 ->
                FirebaseCrashlytics.getInstance()
                    .log(it1)
            }
        }
    }

    private fun performOperation(operation: String?) {
        when (operation) {
            OperationDolfilPushNotification.LOGOUT.javaClass.simpleName -> {
                clearLogInPrefsAndReDirectToLoginScreen(this)
            }
            OperationDolfilPushNotification.CACHEBURST.javaClass.simpleName -> {
                deleteCache(this)
            }
        }
    }

    private fun createNotification(
        mContext: Context,
        remoteMessage: RemoteMessage,
        mIntent: Intent? = null
    ) {
        val title = remoteMessage.notification?.title
        val body = remoteMessage.notification?.body
        var channelId = remoteMessage.notification?.channelId
        // TODO : merge when we have type from backend
        if (channelId == null) channelId = DUMMY_NOTIFICATION_DEFAULT_CHANNEL_ID

        if (remoteMessage.data.containsValue(NOTIFICATION_LIVE_BATCH_CLASS_STARTED)) {
            liveClassNotification(mContext, remoteMessage)
        } else {
            val mBuilder =
                channelId.let {
                    NotificationCompat.Builder(
                        mContext.applicationContext,
                        it
                    )
                }
            val intent: Intent =
                mIntent
                    ?: if (PreferenceProvider(this).getBoolean(Constant.IS_LOGIN)) {
                        Intent(
                            mContext.applicationContext,
                            HomeActivity::class.java
                        )
                    } else {
                        Intent(
                            mContext.applicationContext,
                            OnBoardingActivity::class.java
                        )
                    }
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            val pendingIntent = PendingIntent.getActivity(
                mContext,
                0,
                intent,
                FLAG_UPDATE_CURRENT
            )

            mBuilder.apply {
                setContentIntent(pendingIntent)
                setSmallIcon(R.drawable.ic_notification_small)
                setContentTitle(title)
                color = ContextCompat.getColor(
                    mContext,
                    R.color.blue
                )
                setContentText(body)
                setLargeIcon(
                    ContextCompat.getDrawable(
                        mContext,
                        R.drawable.icon_app_inner_logo
                    )
                        ?.toBitmap()
                )
                setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(body)
                )
                setGroup(GROUP_KEY_DOLFIL)
                priority = NotificationCompat.PRIORITY_MAX
                setAutoCancel(true)
            }

            mNotificationManager =
                mContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    channelName,
                    NotificationManager.IMPORTANCE_HIGH
                )
                mNotificationManager?.createNotificationChannel(channel)
                channelId.let { mBuilder.setChannelId(it) }
            }
            mNotificationManager?.notify(
                title.hashCode(),
                mBuilder.build()
            )
        }
    }

    private fun notificationNavigation(
        context: Context,
        it: RemoteMessage
    ): Intent {
        return Intent(
            context,
            HomeActivity::class.java
        ).apply {
            val targetName = it.data[NOTIFICATION_TARGET_NAME].toString()
            putExtras(Bundle().apply {
                putString(
                    Constant.NOTIFICATION_COURSE_ID,
                    it.data[Constant.NOTIFICATION_COURSE_ID].toString()
                )
                putString(
                    Constant.NOTIFICATION_ITEM_ID,
                    it.data[Constant.NOTIFICATION_ITEM_ID].toString()
                )
                when (targetName) {
                    SPECIAL_CLASS_STARTED -> {
                        specialClassesNotification(
                            it,
                            this
                        )
                    }
                    NOTIFICATION_CHAT_MENTION -> {
                        chatNotification(
                            it,
                            this
                        )
                    }
                    else -> {
                        liveClassNotification(
                            it,
                            this
                        )
                    }
                }
            })
        }
    }

    private fun chatNotification(
        it: RemoteMessage,
        bundle: Bundle
    ) {
        val targetName = it.data[NOTIFICATION_TARGET_NAME].toString()
        if (targetName == NOTIFICATION_CHAT_MENTION) {
            bundle.apply {
                putString(
                    NOTIFICATION_TARGET_NAME,
                    targetName
                )
                putString(
                    Constant.NOTIFICATION_ROOM_ID,
                    it.data[Constant.NOTIFICATION_ROOM_ID].toString()
                )
                val threadId = it.data[Constant.NOTIFICATION_THREAD_ID]
                putString(
                    Constant.NOTIFICATION_THREAD_ID,
                    if (!threadId.isNullOrEmpty()) {
                        threadId.toString()
                    } else {
                        ""
                    }
                )
            }
        }
    }

    private fun specialClassesNotification(
        it: RemoteMessage,
        bundle: Bundle
    ) {
        bundle.apply {
            putString(
                NOTIFICATION_TARGET_NAME,
                it.data[NOTIFICATION_TARGET_NAME].toString()
            )
            putString(
                Constant.NOTIFICATION_YOUTUBE_VIDEO_ID,
                it.data[Constant.NOTIFICATION_YOUTUBE_VIDEO_ID].toString()
            )
            putString(
                Constant.NOTIFICATION_LIVE_CLASS_ID,
                it.data[Constant.NOTIFICATION_LIVE_CLASS_ID].toString()
            )
            putString(
                Constant.NOTIFICATION_SOURCE,
                it.data[Constant.NOTIFICATION_SOURCE].toString()
            )
        }
    }

    private fun liveClassNotification(
        it: RemoteMessage,
        bundle: Bundle
    ) {
        val targetName = it.data[NOTIFICATION_TARGET_NAME].toString()
        if (targetName == NOTIFICATION_LIVE_BATCH_ITEM_STARTED
            || targetName == NOTIFICATION_LIVE_BATCH_TESTSET_STARTED
            || targetName == NOTIFICATION_LIVE_BATCH_CLASS_STARTED
            || targetName == NOTIFICATION_LIVE_BATCH_STARTED
            || targetName == NOTIFICATION_LIVE_BATCH_CLASS_APPROVED
            || targetName == NOTIFICATION_ASSIGNMENT_EVALUATED
            || targetName == NOTIFICATION_ASSIGNMENT_REJECTED
            || targetName == NOTIFICATION_LIVE_BATCH_CLASS_REJECTED
            || targetName == MOCK_INTERVIEW_ATTENDED
            || targetName == MOCK_INTERVIEW_APPEARED
        ) {
            bundle.apply {
                putString(
                    Constant.NOTIFICATION_YOUTUBE_VIDEO_ID,
                    it.data[Constant.NOTIFICATION_YOUTUBE_VIDEO_ID].toString()
                )
                putString(
                    Constant.NOTIFICATION_TESTSET_ID,
                    it.data[Constant.NOTIFICATION_TESTSET_ID].toString()
                )
                putString(
                    NOTIFICATION_TARGET_NAME,
                    it.data[NOTIFICATION_TARGET_NAME].toString()
                )
                putString(
                    Constant.NOTIFICATION_LIVE_BATCH_ID,
                    it.data[Constant.NOTIFICATION_LIVE_BATCH_ID].toString()
                )
                putString(
                    Constant.NOTIFICATION_ENTITY_ID,
                    it.data[Constant.NOTIFICATION_ENTITY_ID].toString()
                )
                putString(
                    Constant.NOTIFICATION_ENTITY_TYPE,
                    it.data[Constant.NOTIFICATION_ENTITY_TYPE].toString()
                )
                putString(
                    NOTIFICATION_TEST_SET_TYPE,
                    it.data[NOTIFICATION_TEST_SET_TYPE].toString()
                )
                putString(
                    NOTIFICATION_LIVE_CLASS_TITLE,
                    it.data[NOTIFICATION_LIVE_CLASS_TITLE].toString()
                )
                putString(
                    NOTIFICATION_LIVE_CLASS_STARTS_AT,
                    it.data[NOTIFICATION_LIVE_CLASS_STARTS_AT].toString()
                )
                putString(
                    NOTIFICATION_USER_FULL_NAME,
                    it.data[NOTIFICATION_USER_FULL_NAME].toString()
                )
                putString(
                    NOTIFICATION_USER_PROFILE_URL,
                    it.data[NOTIFICATION_USER_PROFILE_URL].toString()
                )
                putString(
                    NOTIFICATION_CATEGORY_TITLE,
                    it.data[NOTIFICATION_CATEGORY_TITLE].toString()
                )
                putString(
                    NOTIFICATION_USER_DEGREE,
                    it.data[NOTIFICATION_USER_DEGREE].toString()
                )
                putString(
                    Constant.NOTIFICATION_SOURCE,
                    it.data[Constant.NOTIFICATION_SOURCE].toString()
                )
                putString(
                    Constant.KEY_VIDEO_ID,
                    it.data[Constant.KEY_VIDEO_ID].toString()
                )
            }
        }
    }

    private fun isLiveBatchClassStarted(it: RemoteMessage): Boolean {
        return (it.data.containsValue(NOTIFICATION_LIVE_BATCH_CLASS_STARTED)
                || it.data.containsValue(NOTIFICATION_LIVE_BATCH_TESTSET_STARTED)
                || it.data.containsValue(NOTIFICATION_LIVE_BATCH_STARTED)
                || it.data.containsValue(NOTIFICATION_LIVE_BATCH_ITEM_STARTED)
                || it.data.containsValue(NOTIFICATION_LIVE_BATCH_CLASS_APPROVED)
                || it.data.containsValue(NOTIFICATION_ASSIGNMENT_EVALUATED)
                || it.data.containsValue(NOTIFICATION_ASSIGNMENT_REJECTED)
                || it.data.containsValue(NOTIFICATION_LIVE_BATCH_CLASS_REJECTED))
                || it.data.containsValue(NOTIFICATION_TEST_SET_TYPE)
    }
}
