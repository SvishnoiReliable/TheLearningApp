package com.learn.util.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat.IMPORTANCE_MAX
import com.learn.R
import com.learn.repository.LiveClassTokenRepository
import com.learn.util.Constant
import com.learn.util.isMyServiceRunning
import com.learn.util.moveLiveClass
import com.learn.util.moveToAgoraLiveActivity
import com.learn.view.activity.LiveBatchYoutubePlayerActivity
import com.learn.view.activity.LiveClassesCallActivity
import com.learn.view.fragment.SourceEvents
import com.learn.viewmodel.liveClassToken.LiveClassTokenViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance



class LiveBatchCallsService : Service(),
    KodeinAware {

    private var targetName: String? = null
    private var entityId: String? = null
    private var entityType: String? = null
    private var testSetType: String? = null
    private var liveBatchId: String? = null
    private var youtubeVideoId: String? = null

    private var notificationTitle: String? = null
    private var notificationBody: String? = null
    private var liveClassTitle: String? = null
    private var liveClassStartsAt: String? = null
    private var userFullName: String? = null
    private var userProfileUrl: String? = null
    private var categoryTitle: String? = null
    private var userDegree: String? = null
    private var source: String? = null
    private var channelID: String? = null
    private val channelName = "My Push Notification Channel"
    private val GROUP_KEY_DOLFIL = "group_key_dolfil"
    lateinit var mTokenViewModel: LiveClassTokenViewModel
    private val repository: LiveClassTokenRepository by instance()
    override val kodein by kodein()
    private var notificationId: Int = 6541
    var mNotificationBuilder: NotificationCompat.Builder? = null
    private val notificationManager: NotificationManager by lazy {
        getSystemService(
            NOTIFICATION_SERVICE
        ) as NotificationManager
    }

    private var contentViewCollapsed: RemoteViews? = null
    private var contentViewExpanded: RemoteViews? = null
    private var soundPool: SoundPool? = null

    //[Start] Pending Intent for notification Action
    private val notificationIntent: PendingIntent by lazy {
        PendingIntent.getService(
            this,
            0,
            Intent(
                this,
                LiveBatchCallsService::class.java
            ).apply { action = Intent.ACTION_MAIN },
            0
        )
    }

    private val declineIntent: PendingIntent by lazy {
        PendingIntent.getService(
            this,
            0,
            Intent(
                this,
                LiveBatchCallsService::class.java
            ).apply {
                action = Constant.NOTIFICATION_ACTION_CALL_DECLINE
            },
            0
        )
    }

    private val acceptIntent: PendingIntent by lazy {
        PendingIntent.getService(
            this,
            0,
            Intent(
                this,
                LiveBatchCallsService::class.java
            ).apply {
                action = Constant.NOTIFICATION_ACTION_CALL_ACCEPT
            },
            0
        )
    }
    //[End] Pending Intent

    companion object {
        var isCallActivityForeground: Boolean = false
        fun startService(
            context: Context,
            intent: Intent
        ) {
            try {
                if (!isMyServiceRunning(
                        context,
                        LiveBatchCallsService::class.java
                    )
                ) {
                    context.startService(intent)
                }
            } catch (e: IllegalStateException) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    throw IllegalStateException()
                }
            }
        }
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        manageIntent(intent)
        mTokenViewModel = LiveClassTokenViewModel(repository)
        initObserver()
        return super.onStartCommand(
            intent,
            flags,
            startId
        )
    }

    private fun initObserver() {
        mTokenViewModel.liveClassStatusResponse.observeForever{
            if (it.data != null) {
                if (it.data?.statistics?.isAttended == true) {
                    moveToAgoraLiveActivity(
                        entityId,
                        isCallFromService = true
                    )
                    stopService()
                } else {
                    moveLiveClass(
                        this,
                        targetName = Constant.NOTIFICATION_LIVE_BATCH_STARTED,
                        source = source,
                        liveBatchId = liveBatchId,
                        entityId = entityId,
                        showPopUp = true
                    )
                    stopService()
                }
            }
        }

        mTokenViewModel.youtubeLiveClassStatusResponse.observeForever {
            if (it.data != null) {
                if (it.data?.status?.isLive == true) {
                    moveYoutubeActivity()
                } else if (it.data?.status?.isLive == false) {
                    if ((it.data?.availability?.availableDaysBeforeLeft != null && it.data?.availability?.availableDaysBeforeLeft!! > 0) || (it.data?.availability?.availableMinutesBeforeLeft != null && it.data?.availability?.availableMinutesBeforeLeft!! > 0)) {
                        moveYoutubeActivity()
                    } else if (it.data?.statistics?.isAttended == false) {
                        moveLiveClass(
                            this,
                            targetName = Constant.NOTIFICATION_LIVE_BATCH_STARTED,
                            source = SourceEvents.AGORA.name,
                            liveBatchId = liveBatchId,
                            entityId = entityId,
                            showPopUp = true
                        )
                        stopService()
                    } else if (it.data?.statistics?.isAttended == true) {
                        moveYoutubeActivity(true)
                    }
                }
            }
        }
        mTokenViewModel.liveClassStatusResponse.observeForever {
            if (it.data != null) {
                if (it.data?.statistics?.isAttended == true) {
                    moveToAgoraLiveActivity(
                        entityId,
                        isCallFromService = true
                    )
                    stopService()
                } else {
                    moveLiveClass(
                        this,
                        targetName = Constant.NOTIFICATION_LIVE_BATCH_STARTED,
                        source = source,
                        liveBatchId = liveBatchId,
                        entityId = entityId,
                        showPopUp = true
                    )
                    stopService()
                }
            }
        }
    }

    //it's Handle the notification Action
    private fun manageIntent(intent: Intent?) {
        val notificationShow = intent?.getBooleanExtra(Constant.NOTIFICATION_SHOW, false)
        if (notificationShow != null && notificationShow) {
            targetName = intent.getStringExtra(Constant.NOTIFICATION_TARGET_NAME)
            entityId = intent.getStringExtra(Constant.NOTIFICATION_ENTITY_ID)
            entityType = intent.getStringExtra(Constant.NOTIFICATION_ENTITY_TYPE)
            testSetType = intent.getStringExtra(Constant.NOTIFICATION_TEST_SET_TYPE)
            liveBatchId = intent.getStringExtra(Constant.NOTIFICATION_LIVE_BATCH_ID)
            notificationTitle = intent.getStringExtra(Constant.NOTIFICATION_TITLE)
            notificationBody = intent.getStringExtra(Constant.NOTIFICATION_TITLE)
            channelID = intent.getStringExtra(Constant.NOTIFICATION_CHANNEL)
            youtubeVideoId = intent.getStringExtra(Constant.NOTIFICATION_YOUTUBE_VIDEO_ID)
            liveClassTitle = intent.getStringExtra(Constant.NOTIFICATION_LIVE_CLASS_TITLE)
            liveClassStartsAt = intent.getStringExtra(Constant.NOTIFICATION_LIVE_CLASS_STARTS_AT)
            userFullName = intent.getStringExtra(Constant.NOTIFICATION_USER_FULL_NAME)
            userProfileUrl = intent.getStringExtra(Constant.NOTIFICATION_USER_PROFILE_URL)
            categoryTitle = intent.getStringExtra(Constant.NOTIFICATION_CATEGORY_TITLE)
            userDegree = intent.getStringExtra(Constant.NOTIFICATION_USER_DEGREE)
            source = intent.getStringExtra(Constant.NOTIFICATION_SOURCE)
            soundPool = SoundPool(this)

            if (channelID == null) channelID = Constant.DUMMY_NOTIFICATION_DEFAULT_CHANNEL_ID
            val notification = setNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                notification?.setFullScreenIntent(
                    notificationIntent,
                    true
                )
                startForeground(
                    notificationId,
                    notification?.build()
                )
            } else {
                startForeground(
                    notificationId,
                    notification?.build()
                )
                moveLiveClassActivity()
            }
        }

        intent?.let {
            sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))
            when (it.action) {
                Constant.NOTIFICATION_ACTION_CALL_ACCEPT -> {
                    if (source == SourceEvents.AGORA.name) {
                        getLiveClassAttendedStatus(entityId)
                    } else {
                        getYoutubeLiveClassStatus(
                            liveBatchId,
                            entityId
                        )
                    }
                }
                Constant.NOTIFICATION_ACTION_CALL_DECLINE -> {
                    stopService()
                }
                Intent.ACTION_MAIN -> {
                    if (isCallActivityForeground) {
                        if (source == SourceEvents.AGORA.name) {
                            moveToAgoraLiveActivity(entityId,isCallFromService = true)
                            stopService()
                        } else {
                            moveYoutubeActivity()
                        }
                    } else {
                        moveLiveClassActivity()
                    }
                }
                else -> {
                }
            }
        }
    }
    //endregion

    private fun stopService() {
        notificationManager.cancel(notificationId)
        soundPool?.stopRingtoneAndVibration()
        stopForeground(true)
        stopSelf()
    }

    private fun moveYoutubeActivity(isClassOver: Boolean = false) {
        startActivity(
            Intent(
                this,
                LiveBatchYoutubePlayerActivity::class.java
            ).apply {
                putExtras(Bundle().apply {
                    putString(
                        Constant.NOTIFICATION_TARGET_NAME,
                        targetName
                    )
                    putString(
                        Constant.KEY_YOUTUBE_LIVE_CLASS_ITEM_ENTITY_ID,
                        entityId
                    )
                    putBoolean(
                        Constant.IS_CLASS_OVER,
                        isClassOver
                    )
                    putString(
                        Constant.NOTIFICATION_ENTITY_TYPE,
                        entityType
                    )
                    putString(
                        Constant.KEY_YOUTUBE_BATCH_ID,
                        liveBatchId
                    )
                    putExtra(
                        Constant.NOTIFICATION_YOUTUBE_VIDEO_ID,
                        youtubeVideoId
                    )
                    putString(
                        Constant.NOTIFICATION_LIVE_CLASS_TITLE,
                        liveClassTitle
                    )
                    putString(
                        Constant.NOTIFICATION_LIVE_CLASS_STARTS_AT,
                        liveClassStartsAt
                    )
                    putString(
                        Constant.NOTIFICATION_USER_FULL_NAME,
                        userFullName
                    )
                    putString(
                        Constant.NOTIFICATION_USER_PROFILE_URL,
                        userProfileUrl
                    )
                    putString(
                        Constant.NOTIFICATION_CATEGORY_TITLE,
                        categoryTitle
                    )
                    putString(
                        Constant.NOTIFICATION_USER_DEGREE,
                        userDegree
                    )
                })
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            })
        stopService()
    }

    private fun moveLiveClassActivity() {
        startActivity(Intent(
            this,
            LiveClassesCallActivity::class.java
        ).apply {
            putExtras(Bundle().apply {
                putString(
                    Constant.NOTIFICATION_TARGET_NAME,
                    targetName
                )
                putString(
                    Constant.NOTIFICATION_ENTITY_ID,
                    entityId
                )
                putString(
                    Constant.NOTIFICATION_ENTITY_TYPE,
                    entityType
                )
                putString(
                    Constant.NOTIFICATION_LIVE_BATCH_ID,
                    liveBatchId
                )
                putString(
                    Constant.NOTIFICATION_YOUTUBE_VIDEO_ID,
                    youtubeVideoId
                )
                putString(
                    Constant.NOTIFICATION_LIVE_CLASS_TITLE,
                    liveClassTitle
                )
                putString(
                    Constant.NOTIFICATION_LIVE_CLASS_STARTS_AT,
                    liveClassStartsAt
                )
                putString(
                    Constant.NOTIFICATION_USER_FULL_NAME,
                    userFullName
                )
                putString(
                    Constant.NOTIFICATION_USER_PROFILE_URL,
                    userProfileUrl
                )
                putString(
                    Constant.NOTIFICATION_CATEGORY_TITLE,
                    categoryTitle
                )
                putString(
                    Constant.NOTIFICATION_USER_DEGREE,
                    userDegree
                )
                putString(
                    Constant.NOTIFICATION_SOURCE,
                    source
                )
            })
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        })
        stopService()
    }

    private fun setNotification(): NotificationCompat.Builder? {
        contentViewExpanded = RemoteViews(
            packageName,
            R.layout.notification_live_batch_calls
        )
        contentViewCollapsed = RemoteViews(
            packageName,
            R.layout.notification_live_batch_calls_collapsed
        )

        contentViewCollapsed?.also {
            it.setTextViewText(
                R.id.txt_notification_title,
                notificationTitle
            )
            it.setTextViewText(
                R.id.txt_notification_sub_title,
                notificationBody
            )
        }
        contentViewExpanded?.also {
            it.setTextViewText(
                R.id.txt_notification_title,
                notificationTitle
            )
            it.setTextViewText(
                R.id.txt_notification_sub_title,
                notificationBody
            )
            it.setOnClickPendingIntent(
                R.id.txt_notification_accept,
                acceptIntent
            )
            it.setOnClickPendingIntent(
                R.id.txt_notification_reject,
                declineIntent
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val callInviteChannel = NotificationChannel(
                channelID,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            )
            callInviteChannel.description = notificationBody
            callInviteChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            callInviteChannel.enableLights(true)
            callInviteChannel.setSound(null, null)
            callInviteChannel.lightColor = Color.RED
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
                callInviteChannel
            )
        }

        mNotificationBuilder = channelID?.let {
            NotificationCompat.Builder(
                this,
                it
            )
                .setCustomBigContentView(contentViewExpanded)
                .setCustomContentView(contentViewCollapsed)
                .setStyle(NotificationCompat.DecoratedCustomViewStyle())
                .setSmallIcon(R.drawable.icon_app_inner_logo)
                .setContentTitle(notificationTitle)
                .setContentText(notificationBody)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setSound(null)
                .setAutoCancel(true)
                .setGroup(GROUP_KEY_DOLFIL)
                .setOngoing(true)
                .setTicker(getString(R.string.app_name))
                .setContentIntent(notificationIntent)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setPriority(IMPORTANCE_MAX)
        }

        mNotificationBuilder?.apply {
            contentViewExpanded?.setOnClickPendingIntent(
                R.id.txt_notification_accept,
                acceptIntent
            )
            contentViewExpanded?.setOnClickPendingIntent(
                R.id.txt_notification_reject,
                declineIntent
            )
            contentViewCollapsed?.setOnClickPendingIntent(
                R.id.img_notification_accept,
                acceptIntent
            )
            contentViewCollapsed?.setOnClickPendingIntent(
                R.id.img_notification_reject,
                declineIntent
            )
        }
        return mNotificationBuilder?.setOngoing(true)
    }


    private fun getLiveClassAttendedStatus(id: String? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            mTokenViewModel.getLiveClassStatus(
                id,
                true
            )
        }
    }


    private fun getYoutubeLiveClassStatus(
        liveBatchId: String?,
        entityId: String?
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            mTokenViewModel.youtubeLiveClassStatus(
                liveBatchId,
                entityId
            )
        }
    }
}
