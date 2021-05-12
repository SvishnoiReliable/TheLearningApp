package com.learn.application

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.SharedPreferences
import android.os.Bundle
import com.learn.R
import com.learn.agora.rtc.AgoraEventHandler
import com.learn.agora.rtc.EngineConfig
import com.learn.agora.rtc.EventHandler
import com.learn.agora.stats.StatsManager
import com.learn.agora.utils.Constants.DEFAULT_PROFILE_IDX
import com.learn.agora.utils.Constants.PREF_ENABLE_STATS
import com.learn.agora.utils.Constants.PREF_MIRROR_ENCODE
import com.learn.agora.utils.Constants.PREF_MIRROR_LOCAL
import com.learn.agora.utils.Constants.PREF_MIRROR_REMOTE
import com.learn.agora.utils.Constants.PREF_RESOLUTION_IDX
import com.learn.agora.utils.FileUtil
import com.learn.agora.utils.PrefManager
import com.learn.network.ApiRestService
import com.learn.network.interceptor.HeaderInterceptor
import com.learn.network.interceptor.NetworkInterceptor
import com.learn.prefence.PreferenceProvider
import com.learn.repository.*
import com.learn.util.DeviceUtil
import com.learn.viewmodel.factory.*
import com.learn.viewmodel.liveBatches.InnerLiveBatchesViewModel
import com.learn.viewmodel.liveClassToken.RecordingViewModel
import com.downloader.PRDownloader
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.agora.rtc.RtcEngine
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.androidXModule
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton


@SuppressLint("StaticFieldLeak")
class LearnApp : Application(), KodeinAware, Application.ActivityLifecycleCallbacks {

    private var mRtcEngine: RtcEngine? = null
    private val mGlobalConfig: EngineConfig = EngineConfig()
    private val mHandler: AgoraEventHandler = AgoraEventHandler()
    private val mStatsManager: StatsManager = StatsManager()

    companion object {
        var currentActivity: Activity? = null
    }

    override val kodein = Kodein.lazy {
        import(androidXModule(this@LearnApp))
        bind() from singleton { PreferenceProvider(instance()) }
        bind() from singleton { NetworkInterceptor(instance()) }
        bind() from singleton { DeviceUtil(instance(), instance()) }
        bind() from singleton { HeaderInterceptor(instance(), instance()) }
        bind() from singleton { ApiRestService(instance(), instance(), instance()) }
        bind() from singleton { SignupRepository(instance()) }
        bind() from singleton { SignOutRepository(instance()) }
        bind() from singleton { HomeRepository(instance()) }
        bind() from singleton { OnBoardingRepository(instance()) }
        bind() from singleton { SignupViewModelFactory(instance()) }
        bind() from singleton { SignOutViewModelFactory(instance()) }
        bind() from singleton { HomeViewModelFactory(instance(), instance()) }
        bind() from singleton { OnBoardingViewModelFactory(instance()) }
        bind() from singleton { FirebaseAnalytics.getInstance(instance()) }
        bind() from singleton { FirebaseCrashlytics.getInstance() }
        //Courses
        bind() from singleton { CourseCategoryRepository(instance()) }
        bind() from singleton { CoursesViewModelFactory(instance()) }
        bind() from singleton { CoursesRepository(instance()) }
        bind() from singleton { AllCoursesViewModelFactory(instance(), instance()) }
        // CourseDetails
        bind() from singleton { CourseDetailsRepository(instance()) }
        bind() from singleton { CourseDetailsViewModelFactory(instance(), instance()) }
        // CoursePayment
        bind() from singleton { PaymentRepository(instance()) }
        bind() from singleton { PaymentViewModelFactory(instance(), instance()) }

        bind() from singleton { LanguageRepository(instance()) }
        bind() from singleton { LanguageViewModelFactory(instance()) }
        // Doubts
        bind() from singleton { DoubtsRepository(instance()) }
        bind() from singleton { DoubtsModelFactory(instance(), instance()) }
        // OverAllReport
        bind() from singleton { OverAllReportRepository(instance()) }
        bind() from singleton { OverAllReportModelFactory(instance(), instance()) }
        //splash
        bind() from singleton { SplashRepository(instance()) }
        bind() from singleton { SplashViewModelFactory(instance(), instance()) }
        //MCQ Quiz
        bind() from singleton { McqRepository(instance()) }
        bind() from singleton { McqViewModelFactory(instance()) }
        //MCQ Practice
        bind() from singleton { McqPracticeRepository(instance()) }
        bind() from singleton { McqPracticeViewModelFactory(instance()) }
        // Notes
        bind() from singleton { NotesRepository(instance()) }
        bind() from singleton { NotesModelFactory(instance(), instance()) }
        //Notification
        bind() from singleton { NotificationRepository(instance()) }
        bind() from singleton { NotificationViewModelFactory(instance()) }
        //Announcement
        bind() from singleton { AnnouncementRepository(instance()) }
        bind() from singleton { AnnouncementViewModelFactory(instance()) }
        //Assignment
        bind() from singleton { AssignmentRepository(instance()) }
        bind() from singleton { AssignmentViewModelFactory(instance()) }
        // Bookmark
        bind() from singleton { BookmarkRepository(instance()) }
        bind() from singleton { BookmarkViewModelFactory(instance()) }
        // MainQuestion
        bind() from singleton { MainQuestionRepository(instance()) }
        bind() from singleton { MainQuestionViewModelFactory(instance(), instance()) }
        bind() from singleton { MainQsAssignmentRepository(instance()) }
        bind() from singleton { MainQsAssignmentViewModelFactory(instance()) }
        // Search
        bind() from singleton { SearchRepository(instance()) }
        bind() from singleton { SearchViewModelFactory(instance()) }
        // Mock Practice
        bind() from singleton { PracticeRepository(instance()) }
        bind() from singleton { PracticeViewModelFactory(instance()) }
        // Current Affairs
        bind() from singleton { CurrentAffairsRepository(instance()) }
        bind() from singleton { CurrentAffairsViewModelFactory(instance(), instance()) }
        // Membership
        bind() from singleton { MembershipRepository(instance()) }
        bind() from singleton { MembershipViewModelFactory(instance()) }
        // Study Material
        bind() from singleton { StudyMaterialRepository(instance()) }
        bind() from singleton { StudyMaterialViewModelFactory(instance()) }
        // My Orders
        bind() from singleton { OrdersRepository(instance()) }
        bind() from singleton { MyOrdersViewModelFactory(instance()) }
        // Pdf
        bind() from singleton { PdfViewRepository(instance()) }
        bind() from singleton { PdfViewModelFactory(instance()) }
        // LiveBatches
        bind() from singleton { LiveBatchesRepository(instance()) }
        bind() from singleton { InnerLiveBatchesViewModel(instance()) }
        bind() from singleton { InnerLiveBatchesViewModelFactory(instance()) }
        bind() from singleton { LiveBatchesViewModelFactory(instance()) }
        // LiveBatchesDetail
        bind() from singleton {
            LiveBatchesDetailViewModelFactory(
                instance(),
                instance()
            )
        }
        // TestSeries
        bind() from singleton { TestSeriesRepository(instance()) }
        bind() from singleton { TestSeriesViewModelFactory(instance()) }
        // My Notes
        bind() from singleton { MyNotesRepository(instance()) }
        bind() from singleton { MyNotesViewModelFactory(instance()) }
        // Help Center
        bind() from singleton { ChatRepository(instance()) }
        bind() from singleton { ChatViewModelFactory(instance()) }
        bind() from singleton { ChatThreadViewModelFactory(instance()) }
        bind() from singleton { HelpCenterRepository(instance()) }
        bind() from singleton { HelpCenterViewModelFactory(instance()) }
        //How To
        bind() from singleton { HowToRepository(instance()) }
        bind() from singleton { HowToViewModelFactory(instance(), instance()) }
        //Rank
        bind() from singleton { RankRepository(instance()) }
        bind() from singleton { RankViewModelFactory(instance()) }
        // Open Tests
        bind() from singleton { OpenTestsRepository(instance()) }
        bind() from singleton { OpenTestsViewModelFactory(instance()) }
        //Mock Interview
        bind() from singleton { MockInterviewRepository(instance()) }
        bind() from singleton { MockInterviewViewModelFactory(instance()) }
        // Live Class Token
        bind() from singleton { LiveClassTokenRepository(instance()) }
        bind() from singleton { LiveClassTokenModelFactory(instance()) }
        // Recording ViewModel
        bind() from singleton { RecordingViewModelFactory(instance()) }
        bind() from singleton { RecordingViewModel(instance()) }

        //standAlone Video play
        bind() from singleton { VideoRepository(instance()) }
        bind() from singleton { VideoPlayViewModelFactory(instance()) }
    }

    override fun onCreate() {
        super.onCreate()
        PRDownloader.initialize(applicationContext)
        registerActivityLifecycleCallbacks(this)
        initAgora()
    }

    override fun onActivityCreated(activity: Activity, bundle: Bundle?) {
        currentActivity = activity
    }

    override fun onActivityStarted(activity: Activity) {
        currentActivity = activity
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
    }

    override fun onActivityPaused(activity: Activity) {
        currentActivity = null
    }

    override fun onActivityStopped(activity: Activity) {
    }

    override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) {
    }

    override fun onActivityDestroyed(activity: Activity) {
    }

    private fun initAgora() {
        try {
            mRtcEngine =
                RtcEngine.create(applicationContext, getString(R.string.agora_app_id), mHandler)
            mRtcEngine?.setLogFile(FileUtil.initializeLogFile(this))
        } catch (e: Exception) {
            e.printStackTrace()
        }

        initConfig()
    }

    private fun initConfig() {
        val pref: SharedPreferences = PrefManager.getPreferences(applicationContext)
        mGlobalConfig.videoDimenIndex =
            pref.getInt(PREF_RESOLUTION_IDX, DEFAULT_PROFILE_IDX)
        val showStats = pref.getBoolean(PREF_ENABLE_STATS, false)
        mGlobalConfig.setIfShowVideoStats(showStats)
        mStatsManager.enableStats(showStats)
        mGlobalConfig.mirrorLocalIndex = pref.getInt(PREF_MIRROR_LOCAL, 0)
        mGlobalConfig.mirrorRemoteIndex = pref.getInt(PREF_MIRROR_REMOTE, 0)
        mGlobalConfig.mirrorEncodeIndex = pref.getInt(PREF_MIRROR_ENCODE, 0)
    }

    fun engineConfig(): EngineConfig? {
        return mGlobalConfig
    }

    fun rtcEngine(): RtcEngine? {
        return mRtcEngine
    }

    fun statsManager(): StatsManager? {
        return mStatsManager
    }

    fun registerEventHandler(handler: EventHandler?) {
        handler?.let { mHandler.addHandler(it) }
    }

    fun removeEventHandler(handler: EventHandler?) {
        mHandler.removeHandler(handler)
    }

    override fun onTerminate() {
        super.onTerminate()
        RtcEngine.destroy()
    }
}
