package com.learn.view.fragment


import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.*
import com.learn.R
import com.learn.application.LearnApp
import com.learn.core.ui.BaseFragment
import com.learn.model.response.homeData.HomeItem
import com.learn.model.response.homeSocketSpecialClassResponse.HomeSpecialClassSocketHelperResponse
import com.learn.model.response.liveClass.LiveClassesData
import com.learn.model.response.updateDeviceInfo.UpdateDeviceInfoRequest
import com.learn.network.KEY_SUB_COURSE_ID
import com.learn.network.interceptor.KEY_PRACTICE_ID
import com.learn.prefence.PreferenceProvider
import com.learn.util.*
import com.learn.util.Constant.BANNER_AUTO_SCROLL_TIME
import com.learn.util.Constant.DEVICE_TYPE
import com.learn.util.Constant.HELP_CENTER_NUMBER
import com.learn.util.Constant.KEY_DAILY_QS
import com.learn.util.Constant.KEY_INTRODUCTION
import com.learn.util.Constant.KEY_IS_FROM
import com.learn.util.Constant.KEY_ONLINE_COURSES
import com.learn.util.Constant.KEY_PRACTICE_TEST
import com.learn.util.Constant.KEY_STUDY_MATERIAL
import com.learn.util.Constant.KEY_STUDY_MATERIAL_ID
import com.learn.util.Constant.KEY_TEST_SERIES
import com.learn.util.Constant.learnPracticesSpanCount
import com.learn.view.activity.*
import com.learn.view.adapter.*
import com.learn.viewmodel.factory.HomeViewModelFactory
import com.learn.viewmodel.home.HomeViewModel
import com.google.android.material.appbar.AppBarLayout
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.layout_home_toolbar.*
import kotlinx.coroutines.launch
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.generic.instance
import java.net.HttpURLConnection
import java.util.concurrent.TimeUnit

const val HOME_SCREEN = "HOME_SCREEN"

enum class SpecialClassEvents {
    SPECIAL_CLASS_STARTED, SPECIAL_CLASS_ENDED, LIVE_CLASS_PUBLISHED, LIVE_CLASS_UNPUBLISHED
}

enum class SourceEvents {
    AGORA, YOUTUBE
}

class HomeFragment : BaseFragment(),
    KodeinAware {

    private var mAdapter: LearnPracticesAdapter? = null
    private var mLiveAdapter: LiveClassesAdapter? = null
    private var mAdvanceListAdapter: AdvanceListAdapter? = null
    private var mBannerAdapter: BannerAdapter? = null
    private var mDolfilCornerAdapter: DolfilCornerAdapter? = null
    private var mOutperformAdapter: OutperformAdapter? = null
    private var mLearnPracticeList = ArrayList<HomeItem>()
    private var mLiveClassesArrayList = ArrayList<LiveClassesData>()
    private var mLiveClassesTimerArrayList = ArrayList<LiveClassesData>()
    private var mAdvanceArrayList = ArrayList<HomeItem>()
    private var mBannerArrayList = ArrayList<HomeItem>()
    private var dolfilCornerList = ArrayList<HomeItem>()
    private var mOutperformList = ArrayList<HomeItem>()
    private var bannerLayoutManager: LinearLayoutManager? = null

    private var mBannerPosition: Int = 0
    private var handler: Handler? = null
    private var mBannerRunnable: Runnable? = null

    //ViewModel
    private lateinit var mViewModel: HomeViewModel
    private val preferenceProvider: PreferenceProvider by instance()
    private val deviceUtil: DeviceUtil by instance()
    private val factory: HomeViewModelFactory by instance()
    override val kodein: Kodein by lazy { (activity as KodeinAware).kodein }
    private val mFirebaseCrashlytics: FirebaseCrashlytics by instance()
    private var examId: String? = null
    private var millisInFuture: Long = 60 * 60 * 1000 // 1 hour
    private var countDownInterval: Long = 1000
    private var countDownTimer: CountDownTimer? = null
    private var youtube: LiveBatchYoutubePlayerActivity? = null

    //Socket Helper
    private var mSocketHelper: SocketHelper? = null
    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(
            R.layout.fragment_home,
            container,
            false
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mFirebaseCrashlytics.log("Home Fragment Started-> Crashlytics Report ")
        mViewModel = ViewModelProvider(this, factory).get(HomeViewModel::class.java)
        initObserver()
    }

    private fun initObserver() {
        mViewModel.mLearnAndPracticesArrayLists.observe(this, Observer {
            if (!it.isNullOrEmpty()) {
                mLearnPracticeList.clear()
                mLearnPracticeList.addAll(it)
                mAdapter?.notifyDataSetChanged()
            }
        })
        mViewModel.mLiveClassesLists.observe(
            this,
            Observer { it ->
                if (!it.isNullOrEmpty()) {
                    if (!mLiveClassesArrayList.isNullOrEmpty()) {
                        mLiveClassesArrayList.remove(mLiveClassesArrayList[mLiveClassesArrayList.size - 2])
                        mLiveClassesArrayList.remove(mLiveClassesArrayList[mLiveClassesArrayList.size - 1])
                    }
                    val oldSize = mLiveClassesArrayList.size
                    mLiveClassesArrayList.addAll(it)
                    if (mViewModel.specialClassPage + 1 <= mViewModel.specialClassMaxPage) {
                        mLiveClassesArrayList.add(LiveClassesData(viewType = 1))
                        mLiveClassesArrayList.add(LiveClassesData(viewType = 1))
                    }
                    mLiveAdapter?.notifyItemRangeChanged(
                        oldSize,
                        mLiveClassesArrayList.size
                    )
                    mLiveClassesTimerArrayList.clear()
                    val currentTime = System.currentTimeMillis()
                    for (data in mLiveClassesArrayList) {
                        if (data.liveClass?.startsAt != null && data.liveClass?.beforeStartSec != null && data.enrolling != null && (data.liveClass?.isLive == null || data.liveClass?.isLive == false)) {
                            val serverTime = convertDateIntoMillis(
                                data.liveClass?.startsAt.toString(),
                                mCommonServerDateFormat
                            )
                            val differenceTime = serverTime?.minus(currentTime)
                            val hours =
                                differenceTime?.let { hour -> TimeUnit.MILLISECONDS.toHours(hour) }

                            if (hours != null && hours > 24) {
                                data.timer = resources.getString(
                                    R.string.days,
                                    differenceTime.let { TimeUnit.MILLISECONDS.toDays(it) }
                                        .toString()
                                )
                            } else {
                                mLiveClassesTimerArrayList.add(data)
                            }
                        }
                    }
                    if (!mLiveClassesTimerArrayList.isNullOrEmpty()) {
                        startTimerForLive()
                    }
                }
                mViewModel.isLoading.value = false
            })

        addNotificationCountObserver()
        mViewModel.mUnreadNotificationCountResponse.observe(this,
            Observer {
                if (it.data != null && it.data!!.unreadCount!!.toInt() > 0) {
                    img_badge_notification.show()
                } else {
                    img_badge_notification.hide()
                }
            })
        mViewModel.mHelpCenterPhoneNumberResponse.observe(this,
            Observer {
                if (it?.data != null && !it.data?.APP_HELP_PHONE_NUMBER.isNullOrEmpty()) {
                    preferenceProvider.putString(
                        HELP_CENTER_NUMBER,
                        it.data?.APP_HELP_PHONE_NUMBER
                    )
                }
            })

        mViewModel.mAdvanceArrayLists.observe(this,
            Observer {
                if (!it.isNullOrEmpty()) {
                    mAdvanceArrayList.clear()
                    mAdvanceArrayList.addAll(it)
                    mAdvanceListAdapter?.notifyDataSetChanged()
                }
            })
        mViewModel.mBannerLists.observe(this,
            Observer {
                if (!it.isNullOrEmpty()) {
                    mBannerArrayList.clear()
                    mBannerArrayList.addAll(it)
                    if (it.size > 1) rv_banner?.addItemDecoration(
                        CirclePagerIndicatorDecoration(
                            requireContext()
                        )
                    )
                    mBannerAdapter?.notifyDataSetChanged()
                }
            })
        mViewModel.mCornerLists.observe(
            this,
            Observer {
                if (!it.isNullOrEmpty()) {
                    val oldSize = dolfilCornerList.size
                    dolfilCornerList.addAll(it)
                    mDolfilCornerAdapter?.notifyItemRangeChanged(
                        oldSize,
                        dolfilCornerList.size
                    )
                }
                mViewModel.isLoading.value = false
            })
        mViewModel.mOutperformLists.observe(
            this,
            Observer {
                if (!it.isNullOrEmpty()) {
                    mOutperformList.clear()
                    mOutperformList.addAll(it)
                    mOutperformAdapter?.notifyDataSetChanged()
                }
            })
        mViewModel.mError.observe(
            this,
            androidx.lifecycle.Observer {
                it?.let {
                    if (it.message.isNotEmpty()) {
                        if (it.code == HttpURLConnection.HTTP_GATEWAY_TIMEOUT) {
                            val intent = Intent(
                                requireActivity(),
                                NoInternetActivity::class.java
                            )
                            startActivityForResult(
                                intent,
                                RequestCodeUtil.REQUEST_CODE_NO_INTERNET
                            )
                        } else {
                            home_container.snackbarWithAnchor(
                                it.message,
                                (activity as HomeActivity).bottom_nav
                            )
                        }
                    }
                }
            })
        //set title
        mViewModel.learnPracticesTitle.observe(
            this,
            Observer {
                if (!it.isNullOrEmpty()) {
                    text_title_learn.text = it
                }
            })
        mViewModel.freeLiveClassesTitle.observe(
            this,
            Observer {
                if (!it.isNullOrEmpty()) {
                    txt_title_free_live_classes.text = it
                }
            })
        mViewModel.cornerTitle.observe(
            this,
            Observer {
                if (!it.isNullOrEmpty()) {
                    txt_title_dolfil_corner.text = it
                }
            })
        mViewModel.outperformTitle.observe(
            this,
            Observer {
                if (!it.isNullOrEmpty()) {
                    txt_title_outperform.text = it
                }
            })
        mViewModel.outperformSubTitle.observe(
            this,
            Observer {
                if (!it.isNullOrEmpty()) {
                    txt_full_access_outperform.text = it
                }
            })

        mViewModel.showLoader.observe(
            this,
            Observer {
                if (it != null && it.showLoader) {
                    lifecycleScope.launch {
                        setUI(true)
                    }
                }
            })
        mViewModel.mEnrollResponse.observe(
            this,
            Observer {
                if (it != null) {
                    if (mViewModel.selectedSpecialClassPosition != null) {
                        if (it.data == true) {
                            mLiveClassesArrayList[mViewModel.selectedSpecialClassPosition!!].enrolling?.isEnrolled =
                                true
                            mLiveAdapter?.notifyItemChanged(mViewModel.selectedSpecialClassPosition!!)
                        }
                    }
                }
            })
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        init()
    }

    private fun init() {
        handler = Handler(Looper.myLooper()!!)
        examId = preferenceProvider.getString(KEY_SUB_COURSE_ID)
        setSpinner()
        initLearnPracticesRecyclerView()
        initLiveClassesRecyclerView()
        initAdvanceListRecyclerView()
        initBannerRecyclerView()
        initDolfilCornerRecyclerView()
        initOutperformRecyclerView()
        getData()
        card_scroll_to_top.setOnClickListener {
            ns_main.smoothScrollTo(0, 0)
            app_bar.setExpanded(true)
        }
        cl_collapsing.setOnClickListener {
            activity?.addReplaceFragment(
                R.id.fl_container, LiveBatchesFragment(),
                addFragment = true,
                addToBackStack = true
            )
        }
        ns_main?.fullScroll(View.FOCUS_UP)
        setCollapsing()

        img_menu.setOnClickListener {
            (activity as HomeActivity).actionDrawer()
        }
        spinner_text.setOnClickListener {
            activity?.addReplaceFragmentWithAnimation(
                R.id.fl_container,
                SelectExamFragment(),
                addFragment = true,
                addToBackStack = true,
                enterAnimation = R.anim.slide_out_top,
                exitAnimation = R.anim.slide_out_up
            )
        }
        swipe_refresh_home.setOnRefreshListener {
            getData()
        }
        img_notification.setOnClickListener {
            startActivity(Intent(context, NotificationActivity::class.java))
        }
        img_search.setOnClickListener {
            startActivity(Intent(context, SearchActivity::class.java))
        }

        txt_upgrade_outperform?.setOnClickListener {
            val membershipMainFragment = MembershipMainFragment()
            val bundle = Bundle()
            bundle.putString(
                KEY_IS_FROM,
                HOME_SCREEN
            )
            membershipMainFragment.arguments = bundle
            requireActivity().addReplaceFragment(
                R.id.fl_container,
                membershipMainFragment,
                addFragment = true,
                addToBackStack = true
            )
        }
        updateDeviceInfo()
        initSocketHelper()
    }

    private val helperListener = object : SocketHelper.SocketHelperListener {
        override fun onMessageReceive(arrayOfAny: Array<Any>) {
            handleSocketData(arrayOfAny)
        }

        override fun onConnect(arrayOfAny: Array<Any>) {}

        override fun onDisconnect(arrayOfAny: Array<Any>) {}

        override fun onConnectError(arrayOfAny: Array<Any>) {}
    }

    private fun initSocketHelper() {
        if (mSocketHelper == null) {
            mSocketHelper = SocketHelper()
            examId = preferenceProvider.getString(KEY_SUB_COURSE_ID)
            mSocketHelper?.let {
                it.connect(
                    resources.getString(R.string.socket_url),
                    it.socketOptions(
                        getChatQuery(examId),
                        Constant.HOME_CHANNELS_PATH
                    ),
                    Constant.HOME_EVENT_NAME,
                    helperListener,
                    "/",
                    SocketHelper.SocketType.HOME.name
                )
            }
        }
    }

    private fun handleSocketData(arrayOfAny: Array<Any>) {
        val adapter: JsonAdapter<HomeSpecialClassSocketHelperResponse> =
            moshi.adapter(HomeSpecialClassSocketHelperResponse::class.java)
        val helperResponse = adapter.fromJson(arrayOfAny[0].toString())
        try {
            activity?.runOnUiThread {
                when (helperResponse?.eventName) {
                    SpecialClassEvents.SPECIAL_CLASS_STARTED.name -> {
                        for ((position, data) in mLiveClassesArrayList.withIndex()) {
                            if (data.liveClass?.id == helperResponse.event?.liveClassId) {
                                data.liveClass?.isLive = true
                                data.enrolling?.isEnrolled = false
                                mLiveAdapter?.notifyItemChanged(position)
                                break
                            }
                        }
                    }
                    SpecialClassEvents.SPECIAL_CLASS_ENDED.name, SpecialClassEvents.LIVE_CLASS_PUBLISHED.name, SpecialClassEvents.LIVE_CLASS_UNPUBLISHED.name -> {
                        if (helperResponse.eventName == SpecialClassEvents.SPECIAL_CLASS_ENDED.name && DizvikApp.currentActivity is LiveBatchYoutubePlayerActivity) {
                           (DizvikApp.currentActivity as LiveBatchYoutubePlayerActivity).specialClassEnded(helperResponse.event?.liveClassId)
                        }
                        lifecycleScope.launch {
                            mViewModel.specialClassPage = 1
                            countDownTimer?.cancel()
                            countDownTimer = null
                            rv_live_classes.recycledViewPool.clear()
                            mViewModel.mLiveClassesLists.value?.clear()
                            mLiveClassesArrayList.clear()
                            examId?.let { mViewModel.getLiveClasses(it) }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getChatQuery(roomID: String?): String {
        return "roomId=$roomID${"&token="}${preferenceProvider.getString(Constant.USER_TOKEN)}"
    }

    private fun updateDeviceInfo() {
        val tokenUpdate = preferenceProvider.getBoolean(Constant.FIREBASE_TOKEN_UPDATED)
        if (tokenUpdate) {
            preferenceProvider.putBoolean(
                Constant.FIREBASE_TOKEN_UPDATED,
                false
            )
            val token = preferenceProvider.getString(Constant.FIREBASE_TOKEN)
            lifecycleScope.launch {
                mViewModel.updateDeviceInfo(
                    UpdateDeviceInfoRequest(
                        deviceUtil.getDeviceId(),
                        deviceUtil.getDeviceName(),
                        DEVICE_TYPE,
                        token,
                        deviceUtil.getUserAgent()
                    )
                )
            }
        }
    }

    private fun getData() {
        lifecycleScope.launch {
            setUI(false)
            preferenceProvider.getString(KEY_SUB_COURSE_ID)?.let {
                onReset()
                mViewModel.resetValues()
                mViewModel.courseID = it
                mViewModel.getAllSections(
                    it
                )
            }
            swipe_refresh_home.isRefreshing = false
        }
    }

    private fun setUI(isVisible: Boolean) {
        if (isVisible) {
            shimmerLayout.startStopShimmer(false)
            img_banner?.show()
            app_bar?.show()
            ns_main?.show()
        } else {
            img_banner?.invisible()
            app_bar?.invisible()
            ns_main?.invisible()
            shimmerLayout?.startStopShimmer(true)
        }
    }

    private fun setCollapsing() {
        app_bar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
            val percentage: Float =
                kotlin.math.abs(verticalOffset).toFloat() / app_bar.totalScrollRange
            cl_collapsing.alpha = 1 - percentage
            img_banner.alpha = 1 - percentage
        })
    }

    private fun initLearnPracticesRecyclerView() {
        val layoutManager = GridLayoutManager(requireContext(), learnPracticesSpanCount)
        rv_learn.layoutManager = layoutManager
        mAdapter = LearnPracticesAdapter(
            mLearnPracticeList,
            object : LearnPracticesAdapter.ItemClickListener {
                override fun itemClick(homeItem: HomeItem) {
                    moveOnScreen(homeItem)
                }
            })
        rv_learn.adapter = mAdapter
        val spacing = resources.getDimension(R.dimen.dp_14).toInt() // 50px
        val includeEdge = true
        rv_learn.addItemDecoration(
            GridSpacingDecoration(
                learnPracticesSpanCount,
                spacing,
                includeEdge,
                0
            )
        )
    }

    private fun initLiveClassesRecyclerView() {
        val layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        rv_live_classes.layoutManager = layoutManager
        mLiveAdapter = LiveClassesAdapter(
            requireContext(),
            mLiveClassesArrayList,
            object : LiveClassesAdapter.ItemClickListener {
                override fun itemClick(position: Int) {
                    val liveClassesData = mLiveClassesArrayList[position]
                    if ((liveClassesData.liveClass?.isLive == null || liveClassesData.liveClass?.isLive == false) && liveClassesData.enrolling?.isEnrolled == false) {
                        mViewModel.selectedSpecialClassPosition = position
                        mLiveClassesArrayList[position].enrolling?.isEnrolled = true
                        mLiveAdapter?.notifyItemChanged(position)
                        callEnrolledAPI(position)
                    } else if (liveClassesData.liveClass?.isLive == true) {
                        if (liveClassesData.liveClass?.source == SourceEvents.AGORA.name) {
                            context?.moveToAgoraLiveActivity(
                                mLiveClassesArrayList[position].liveClass?.id,
                                true
                            )
                        } else {
                            moveYoutubePlayerActivity(position)
                        }
                    }
                }
            })

        rv_live_classes.adapter = mLiveAdapter
        rv_live_classes.addOnScrollListener(
            object :
                PaginationScrollListener(layoutManager) {
                override fun isLastPage(): Boolean {
                    return false
                }

                override fun isLoading(): Boolean {
                    return mViewModel.isLoading()
                }

                override fun loadMoreItems() {
                    mViewModel.specialClassPage += 1
                    lifecycleScope.launch {
                        if (mViewModel.specialClassPage <= mViewModel.specialClassMaxPage) mViewModel.getLiveClasses(
                            mViewModel.courseID,
                            mViewModel.specialClassLimit,
                            mViewModel.specialClassPage
                        )
                    }
                }
            })
    }

    private fun moveYoutubePlayerActivity(position: Int) {
        activity?.startActivity(
            Intent(
                activity,
                LiveBatchYoutubePlayerActivity::class.java
            ).apply {
                putExtras(Bundle().apply {
                    putString(
                        Constant.KEY_YOUTUBE_BATCH_ID,
                        mLiveClassesArrayList[position].liveClass?.id
                    )
                    putExtra(
                        Constant.NOTIFICATION_YOUTUBE_VIDEO_ID,
                        mLiveClassesArrayList[position].liveClass?.youtubeVideoId
                    )
                    putString(
                        Constant.NOTIFICATION_LIVE_CLASS_TITLE,
                        mLiveClassesArrayList[position].liveClass?.title
                    )
                    putString(
                        Constant.NOTIFICATION_LIVE_CLASS_STARTS_AT,
                        mLiveClassesArrayList[position].liveClass?.startsAt
                    )
                    putString(
                        Constant.NOTIFICATION_USER_FULL_NAME,
                        mLiveClassesArrayList[position].faculties?.get(0)?.fullName
                    )
                    putString(
                        Constant.NOTIFICATION_USER_PROFILE_URL,
                        mLiveClassesArrayList[position].faculties?.get(0)?.pictureUrl
                    )
                    putString(
                        Constant.NOTIFICATION_CATEGORY_TITLE,
                        mLiveClassesArrayList[position].categories?.get(0)?.categoryName
                    )
                    putString(
                        Constant.NOTIFICATION_USER_DEGREE,
                        mLiveClassesArrayList[position].categories?.get(0)?.categoryName
                    )
                    putBoolean(
                        Constant.IS_FORM_SPECIAL_CLASS,
                        true
                    )
                })
            }
        )
    }

    private fun callEnrolledAPI(position: Int) {
        lifecycleScope.launch {
            mViewModel.enrollLiveClass(mLiveClassesArrayList[position].liveClass?.id)
        }
    }

    private fun initAdvanceListRecyclerView() {
        val layoutManager =
            LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.VERTICAL,
                false
            )
        rv_advance_list.layoutManager = layoutManager
        mAdvanceListAdapter =
            AdvanceListAdapter(
                mAdvanceArrayList,
                object : AdvanceListAdapter.ItemClickListener {
                    override fun itemClick(data: HomeItem) {
                        initializeAdvanceListClicks(data)
                    }
                })
        rv_advance_list.adapter = mAdvanceListAdapter
    }

    private fun initializeAdvanceListClicks(data: HomeItem) {
        when (data.name) {
            Constant.KEY_OPEN_TESTS -> {
                startActivity(Intent(requireContext(), OpenTestsActivity::class.java))
            }
            Constant.KEY_MOCK_INTERVIEW -> {
                startActivity(Intent(requireContext(), MockInterviewActivity::class.java))
            }
            else -> {
                home_container?.snackbar(getString(R.string.coming_soon))
            }
        }
    }

    private fun initBannerRecyclerView() {
        bannerLayoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        rv_banner.layoutManager = bannerLayoutManager
        mBannerAdapter = BannerAdapter(mBannerArrayList, object : BannerAdapter.ItemClickListener {
            override fun itemClick(position: Int) {
            }
        })
        rv_banner.adapter = mBannerAdapter
        val snapHelper: SnapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(rv_banner)
        setScrollListener()
    }

    private fun setScrollListener() {
        rv_banner.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                bannerLayoutManager?.findFirstVisibleItemPosition()?.let {
                    mBannerPosition = it
                }
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                bannerLayoutManager?.findFirstVisibleItemPosition()?.let {
                    mBannerPosition = it
                }
            }
        })
        autoScroll()
    }

    private fun autoScroll() {
        mBannerRunnable = Runnable {
            if (mBannerPosition != mBannerArrayList.size - 1) {
                mBannerPosition += 1
                rv_banner.smoothScrollToPosition(mBannerPosition)
            } else {
                rv_banner.smoothScrollToPosition(0)
            }
            mBannerRunnable?.let { handler!!.postDelayed(it, BANNER_AUTO_SCROLL_TIME) }
        }
        mBannerRunnable?.let { handler!!.postDelayed(it, BANNER_AUTO_SCROLL_TIME) }
    }

    private fun initDolfilCornerRecyclerView() {
        val layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.HORIZONTAL,
            false
        )
        rv_dolfil_corner.layoutManager = layoutManager
        mDolfilCornerAdapter = DolfilCornerAdapter(
            requireContext(),
            dolfilCornerList,
            object : DolfilCornerAdapter.ItemClickListener {
                override fun itemClick(position: Int) {
                    val intent = Intent(
                        activity!!,
                        DolfilCornerPlayerActivity::class.java
                    ).apply {
                        putExtras(Bundle().apply {
                            putParcelableArrayList(
                                Constant.KEY_CORNER_LIST,
                                dolfilCornerList
                            )
                            putInt(
                                Constant.KEY_CORNER_POSITION,
                                position
                            )
                        })
                    }
                    startActivity(intent)
                }
            })
        rv_dolfil_corner.adapter = mDolfilCornerAdapter

        rv_dolfil_corner.addOnScrollListener(
            object :
                PaginationScrollListener(layoutManager) {
                override fun isLastPage(): Boolean {
                    return false
                }

                override fun isLoading(): Boolean {
                    return mViewModel.isLoading()
                }

                override fun loadMoreItems() {
                    mViewModel.dolfilCornerPage += 1
                    lifecycleScope.launch {
                        if (mViewModel.dolfilCornerPage <= mViewModel.dolfilCornerMaxPage) mViewModel.getCornerList(
                            mViewModel.cornerID,
                            mViewModel.dolfilCornerLimit,
                            mViewModel.dolfilCornerPage
                        )
                    }
                }
            })
    }

    private fun initOutperformRecyclerView() {
        val layoutManager = GridLayoutManager(requireContext(), 2)
        rv_outperform.layoutManager = layoutManager
        mOutperformAdapter =
            OutperformAdapter(mOutperformList, object : OutperformAdapter.ItemClickListener {
                override fun itemClick(position: Int) {
                }
            })
        rv_outperform.adapter = mOutperformAdapter
    }

    private fun moveOnScreen(homeItem: HomeItem) {
        when (homeItem.name) {
            KEY_INTRODUCTION -> {
                val currentAffairsMainFragment = CurrentAffairsMainFragment()
                currentAffairsMainFragment.arguments = Bundle().apply {
                    putString(Constant.KEY_CURRENT_AFFAIRS_ID, homeItem.id)
                }
                activity?.addReplaceFragment(
                    R.id.fl_container,
                    currentAffairsMainFragment,
                    addFragment = true,
                    addToBackStack = true
                )
            }
            KEY_ONLINE_COURSES -> {
                val mCourseFragment = CourseFragment()
                mCourseFragment.arguments = Bundle().apply {
                    putString(Constant.KEY_CATEGORY_ID, homeItem.id)
                }
                activity?.addReplaceFragment(
                    R.id.fl_container,
                    mCourseFragment,
                    addFragment = true,
                    addToBackStack = true
                )
            }
            KEY_TEST_SERIES -> {
                val testSeriesMainFragment = TestSeriesMainFragment()
                testSeriesMainFragment.arguments = Bundle().apply {
                    putString(Constant.KEY_CATEGORY_ID, homeItem.id)
                }
                activity?.addReplaceFragment(
                    R.id.fl_container,
                    testSeriesMainFragment,
                    addFragment = true,
                    addToBackStack = true
                )
            }
            KEY_PRACTICE_TEST -> {
                val mockPracticeSubjectFragment = PracticeSubjectFragment()
                mockPracticeSubjectFragment.arguments = Bundle().apply {
                    putString(KEY_PRACTICE_ID, homeItem.id)
                }
                activity?.addReplaceFragment(
                    R.id.fl_container,
                    mockPracticeSubjectFragment,
                    addFragment = true,
                    addToBackStack = true
                )
            }
            KEY_STUDY_MATERIAL -> {
                val studyMaterialSubjectFragment = StudyMaterialSubjectFragment()
                studyMaterialSubjectFragment.arguments = Bundle().apply {
                    putString(KEY_STUDY_MATERIAL_ID, homeItem.id)
                }
                activity?.addReplaceFragment(
                    R.id.fl_container,
                    studyMaterialSubjectFragment,
                    addFragment = true,
                    addToBackStack = true
                )
            }
            KEY_DAILY_QS -> {
                startActivity(Intent(requireContext(), MainQuestionActivity::class.java))
            }
        }
    }

    private fun setSpinner() {
        val arrayList =
            ArrayList<String>(resources.getStringArray(R.array.home_spinner).toMutableList())
        val spinnerCountShoesArrayAdapter: ArrayAdapter<String> = ArrayAdapter(
            requireContext(),
            R.layout.item_home_spinner,
            arrayList
        )
        home_spinner.adapter = spinnerCountShoesArrayAdapter
        home_spinner.isEnabled = false
        home_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, id: Long) {
                spinner_text.text = arrayList[position]
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {

            }
        }
    }

    override fun onDestroyView() {
        mBannerRunnable?.let { handler?.removeCallbacks(it) }
        countDownTimer?.cancel()
        stopSocket()
        super.onDestroyView()
    }

    private fun stopSocket() {
        mSocketHelper?.disconnect()
        mSocketHelper = null
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        if (requestCode == RequestCodeUtil.REQUEST_CODE_NO_INTERNET) {
            when (resultCode) {
                Activity.RESULT_OK -> {
                    getData()
                }
                Activity.RESULT_CANCELED -> {
                    activity?.finish()
                }
            }
        }
    }

    private fun addNotificationCountObserver() {
        val liveData = getNotificationCountLiveData()
        liveData.observe(
            this,
            Observer {
                if (it != null && it) {
                    lifecycleScope.launch {
                        mViewModel.getNotificationUnreadCount()
                    }
                }
            })
    }

    private fun startTimerForLive() {
        if (countDownTimer == null) {
            countDownTimer = object : CountDownTimer(
                millisInFuture,
                countDownInterval
            ) {
                override fun onTick(millisUntilFinished: Long) {
                    val currentTime = System.currentTimeMillis()
                    if (mLiveClassesArrayList.isNullOrEmpty()) {
                        countDownTimer?.cancel()
                    } else {
                        mLiveClassesTimerArrayList.forEachIndexed { _, childItems ->
                            val serverTime =
                                convertSecondsToMillis(childItems.liveClass?.beforeStartSec?.toLong())
                            val differenceTime = serverTime.minus(currentTime)
                            val seconds = differenceTime.div(1000)

                            if (seconds > 0) {
                                val time = if (seconds <= 60) {
                                    context?.resources?.getString(
                                        R.string.sec,
                                        seconds.toString()
                                    )
                                } else {
                                    convertMillisecondIntoDateFormat(
                                        differenceTime,
                                        HH_MM_SS_TIME_FORMAT
                                    ).toString()

                                }
                                childItems.timer = time
                            }
                            mLiveAdapter?.notifyDataSetChanged()
                        }
                    }
                }

                override fun onFinish() {}
            }.start()
        }
    }

    private fun onReset() {
        countDownTimer?.cancel()
        countDownTimer = null
        mLearnPracticeList.clear()
        mLiveClassesArrayList.clear()
        mAdvanceArrayList.clear()
        mBannerArrayList.clear()
        dolfilCornerList.clear()
        mOutperformList.clear()
    }
}
