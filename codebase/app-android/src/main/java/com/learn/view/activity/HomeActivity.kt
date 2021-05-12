package com.learn.view.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.GravityCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.learn.R
import com.learn.application.LearnApp
import com.learn.manager.LanguageManager
import com.learn.model.NavigationDrawer
import com.learn.network.interceptor.LANGUAGE_ID
import com.learn.prefence.PreferenceProvider
import com.learn.util.*
import com.learn.util.Constant.IS_CLASS_OVER
import com.learn.util.Constant.KEY_CATEGORY_ID
import com.learn.util.Constant.KEY_COURSE_ID
import com.learn.util.Constant.KEY_IS_APPROVED
import com.learn.util.Constant.KEY_SUBJECT_ID
import com.learn.util.Constant.KEY_VIDEO_ID
import com.learn.util.Constant.KEY_YOUTUBE_BATCH_ID
import com.learn.util.Constant.LANGUAGE
import com.learn.util.Constant.LANGUAGE_LIST
import com.learn.util.Constant.LANGUAGE_SHORT_ENGLISH
import com.learn.util.Constant.MOCK_INTERVIEW_APPEARED
import com.learn.util.Constant.MOCK_INTERVIEW_ATTENDED
import com.learn.util.Constant.NOTIFICATION_ASSIGNMENT_EVALUATED
import com.learn.util.Constant.NOTIFICATION_ASSIGNMENT_REJECTED
import com.learn.util.Constant.NOTIFICATION_ENTITY_ID
import com.learn.util.Constant.NOTIFICATION_ENTITY_TYPE
import com.learn.util.Constant.NOTIFICATION_LIVE_BATCH_CLASS_APPROVED
import com.learn.util.Constant.NOTIFICATION_LIVE_BATCH_CLASS_REJECTED
import com.learn.util.Constant.NOTIFICATION_LIVE_BATCH_ID
import com.learn.util.Constant.NOTIFICATION_LIVE_BATCH_IS_PURCHASE
import com.learn.util.Constant.NOTIFICATION_LIVE_BATCH_STARTED
import com.learn.util.Constant.NOTIFICATION_LIVE_BATCH_STARTED_SHOW_POP_UP
import com.learn.util.Constant.NOTIFICATION_LIVE_CLASS_ID
import com.learn.util.Constant.NOTIFICATION_ROOM_ID
import com.learn.util.Constant.NOTIFICATION_TESTSET_ID
import com.learn.util.Constant.NOTIFICATION_TEST_SET_TYPE
import com.learn.util.Constant.NOTIFICATION_THREAD_ID
import com.learn.view.adapter.NavigationDrawerAdapter
import com.learn.view.fragment.*
import com.learn.viewmodel.factory.HomeViewModelFactory
import com.learn.viewmodel.factory.LiveClassTokenModelFactory
import com.learn.viewmodel.home.*
import com.learn.viewmodel.liveClassToken.LiveClassTokenViewModel
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.layout_nav_join.*
import kotlinx.android.synthetic.main.navigation_drawer.*
import kotlinx.coroutines.launch
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance

enum class TabsHome { HOME, HELP_CENTER, UNLOCK }
class HomeActivity : BaseActivity(),
    KodeinAware {
    private var mAdapter: NavigationDrawerAdapter? = null
    private var mNavArrayList = ArrayList<NavigationDrawer>()

    private lateinit var mViewModel: HomeViewModel
    private val factory: HomeViewModelFactory by instance()
    private val preferenceProvider: PreferenceProvider by instance()
    lateinit var mTokenViewModel: LiveClassTokenViewModel
    private val tokenFactory: LiveClassTokenModelFactory by instance()

    override val kodein by kodein()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        addReplaceFragment(
            R.id.fl_container,
            HomeFragment(),
            addFragment = true,
            addToBackStack = false
        )
        if (isNavigationBarShow()) {
            fl_container.setPadding(
                0,
                0,
                0,
                getHeightOfNavigationBar()
            ) //Set Padding bottom to your parent layout
        }
        notificationActions(intent)
        init()
    }

    private fun init() {
        mTokenViewModel =
            ViewModelProvider(
                this,
                tokenFactory
            ).get(LiveClassTokenViewModel::class.java)
        mViewModel = ViewModelProvider(
            this,
            factory
        ).get(HomeViewModel::class.java)
        initObserver()
        initRecyclerView()
        mViewModel.navigationDrawerItems()
        setDetailsNavigationText()
        if (isNavigationBarShow()) {
            val constraintSet = ConstraintSet()
            constraintSet.clone(rootHome)
            constraintSet.setMargin(
                R.id.bottom_nav,
                ConstraintSet.BOTTOM,
                getHeightOfNavigationBar()
            )
            constraintSet.applyTo(rootHome)
        }
        txt_bottom_home.isSelected = true
        txt_bottom_home.clickWithDebounce {
            configureTab(TabsHome.HOME)
        }
        txt_bottom_help_center.clickWithDebounce {
            configureTab(TabsHome.HELP_CENTER)
        }
        txt_bottom_unlock.clickWithDebounce {
            configureTab(TabsHome.UNLOCK)
        }
        img_arrow.clickWithDebounce {
            startActivity(
                Intent(
                    this,
                    ProfileActivity::class.java
                )
            )
        }
    }

    private fun setDetailsNavigationText() {
        txt_nav_join_title.text = Constant.DUMMY_JOIN
        txt_nav_join_description.text = Constant.DUMMY_CONNECT
        txt_nav_name.text = preferenceProvider.getString(Constant.USER_NAME, "")

        img_profile_nav.setPicture(
            preferenceProvider.getString(Constant.USER_IMAGE_URL),
            placeholder = R.drawable.profile
        )
        img_profile_nav?.setOnClickListener {
            if (!preferenceProvider.getString(Constant.USER_IMAGE_URL).isNullOrEmpty()) {
                val imageDialog = ImageDialog()
                val bundle = Bundle()
                bundle.putString(
                    Constant.USER_IMAGE_URL,
                    preferenceProvider.getString(Constant.USER_IMAGE_URL)
                )
                imageDialog.arguments = bundle
                imageDialog.show(
                    supportFragmentManager,
                    ImageDialog::class.java.simpleName
                )
            }
        }
        //TODO: fix the correct value fo course
        txt_course_nav.text = Constant.DUMMY_IAS
    }

    private fun initObserver() {
        addProfileObserver()
        mTokenViewModel.liveClassStatusResponse.observe(
            this,
            Observer {
                if (it.data != null) {
                    if (it.data?.statistics?.isAttended == true) {
                        moveToAgoraLiveActivity(
                            intent.getStringExtra(NOTIFICATION_ENTITY_ID),
                            batchId = intent.getStringExtra(NOTIFICATION_LIVE_CLASS_ID)
                        )
                    } else {
                        moveLiveBatchDetailFragment(
                            intent,
                            it.data?.statistics?.isAttended
                        )
                    }
                }
            })

        mTokenViewModel.youtubeLiveClassStatusResponse.observe(
            this,
            Observer {
                if (it.data != null) {
                    if (it.data?.status?.isLive == true) {
                        moveLiveClassesCallActivity(intent)
                    } else if (it.data?.status?.isLive == false) {
                        if ((it.data?.availability?.availableDaysBeforeLeft != null && it.data?.availability?.availableDaysBeforeLeft!! > 0) || (it.data?.availability?.availableMinutesBeforeLeft != null && it.data?.availability?.availableMinutesBeforeLeft!! > 0)) {
                            moveLiveClassesCallActivity(intent)
                        } else if (it.data?.statistics?.isAttended == false) {
                            moveLiveClass(
                                this,
                                targetName = NOTIFICATION_LIVE_BATCH_STARTED,
                                source = SourceEvents.AGORA.name,
                                liveBatchId = intent.getStringExtra(NOTIFICATION_LIVE_BATCH_ID),
                                entityId = intent.getStringExtra(NOTIFICATION_ENTITY_ID),
                                showPopUp = true
                            )
                        } else if (it.data?.statistics?.isAttended == true) {
                            moveLiveClassesCallActivity(
                                intent,
                                isClassOver = true
                            )
                        }
                    }
                }
            })
        mViewModel.mNavigationDrawerArrayList.observe(
            this,
            Observer {
                if (!it.isNullOrEmpty()) {
                    mNavArrayList.clear()
                    mNavArrayList.addAll(it)
                    mAdapter?.notifyDataSetChanged()
                }
            })
    }

    fun setBottomBar(isVisible: Boolean) {
        if (isVisible) {
            bottom_nav.visibility = View.VISIBLE
        } else {
            bottom_nav.visibility = View.GONE
        }
    }

    private fun initRecyclerView() {
        val layoutManager = LinearLayoutManager(this)
        rv_nav.layoutManager = layoutManager
        mAdapter = NavigationDrawerAdapter(
            mNavArrayList,
            object : NavigationDrawerAdapter.ItemClickListener {
                override fun itemClick(mData: NavigationDrawer) {
                    openContentFromDrawer(mData)
                }

                override fun onLanguageChange(langShortName: String) {
                    val languageListJson = preferenceProvider.getString(LANGUAGE_LIST, null)
                    if (languageListJson != null) {
                        preferenceProvider.putString(LANGUAGE, langShortName)
                        preferenceProvider.putString(
                            LANGUAGE_ID,
                            LanguageManager.getLanguageIdByShortName(
                                languageListJson,
                                langShortName
                            )
                        )
                    }
                }
            }, preferenceProvider.getString(LANGUAGE, LANGUAGE_SHORT_ENGLISH)!!
        )
        rv_nav.adapter = mAdapter
    }

    private fun openContentFromDrawer(mData: NavigationDrawer) {
        drawer_layout?.closeDrawer(GravityCompat.START)
        when (mData.contentType) {
            NAV_ORDERS -> {
                startActivity(
                    Intent(
                        this,
                        OrdersActivity::class.java
                    )
                )
            }
            NAV_NOTES -> {
                startActivity(
                    Intent(
                        this,
                        NotesActivity::class.java
                    )
                )
            }
            NAV_BOOKMARK -> {
                startActivity(
                    Intent(
                        this,
                        AllBookmarksActivity::class.java
                    )
                )
            }

            NAV_HOW_TO -> {
                startActivity(
                    Intent(
                        this,
                        HowToActivity::class.java
                    )
                )
            }

            else -> {
                rootHome.snackbar(getString(R.string.coming_soon))
            }
        }
    }

    fun actionDrawer() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.openDrawer(GravityCompat.END)
        } else {
            drawer_layout.openDrawer(GravityCompat.START)
        }
    }

    private fun configureTab(tabsHome: TabsHome) {
        setTabs(tabsHome)
        when (tabsHome) {
            TabsHome.HOME -> {
                if (getCurrentFragment(R.id.fl_container) !is HomeFragment) {
                    addReplaceFragment(
                        R.id.fl_container,
                        HomeFragment(),
                        addFragment = false,
                        addToBackStack = false
                    )
                }
            }
            TabsHome.HELP_CENTER -> {
                addReplaceFragment(
                    R.id.fl_container,
                    HelpCenterCategoriesFragment(),
                    addFragment = true,
                    addToBackStack = true
                )
            }
            TabsHome.UNLOCK -> {
                if (getCurrentFragment(R.id.fl_container) !is MembershipMainFragment) {
                    val membershipMainFragment = MembershipMainFragment()
                    val bundle = Bundle()
                    bundle.putString(
                        Constant.KEY_IS_FROM,
                        HOME_SCREEN
                    )
                    membershipMainFragment.arguments = bundle
                    addReplaceFragment(
                        R.id.fl_container,
                        membershipMainFragment,
                        addFragment = true,
                        addToBackStack = true
                    )
                }
            }
        }
    }

    private fun setTabs(tabsHome: TabsHome) {
        txt_bottom_home?.isSelected = tabsHome == TabsHome.HOME
        txt_bottom_help_center?.isSelected = tabsHome == TabsHome.HELP_CENTER
        txt_bottom_unlock?.isSelected = tabsHome == TabsHome.UNLOCK
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        notificationActions(intent)
    }

    private fun notificationActions(intent: Intent?) {
        val coursesId = intent?.getStringExtra(Constant.NOTIFICATION_COURSE_ID)
        val categoryId = intent?.getStringExtra(Constant.NOTIFICATION_ITEM_ID)
        val targetName = intent?.getStringExtra(Constant.NOTIFICATION_TARGET_NAME)
        if (targetName != null) {
            when (targetName) {
                Constant.NOTIFICATION_UNAUTHORIZED -> {
                    DizvikApp.currentActivity = this
                    MultipleAuthentication().bottomSheetAuthentication()
                }
                Constant.NOTIFICATION_CHAT_MENTION -> {
                    moveChatActivity(intent)
                }
                Constant.SPECIAL_CLASS_STARTED -> {
                    if (intent.getStringExtra(Constant.NOTIFICATION_SOURCE) == SourceEvents.AGORA.name) {
                        moveToAgoraLiveActivity(
                            intent.getStringExtra(NOTIFICATION_LIVE_CLASS_ID),
                            true
                        )
                    } else {
                        moveYoutubePlayerActivity(intent)
                    }
                }
                Constant.NOTIFICATION_COURSE -> {
                    moveCourseFragment(
                        coursesId,
                        categoryId
                    )
                }
                Constant.NOTIFICATION_LIVE_BATCH_CLASS_STARTED -> {
                    if (intent.getStringExtra(Constant.NOTIFICATION_SOURCE) == SourceEvents.AGORA.name) {
                        getLiveClassAttendedStatus(intent.getStringExtra(NOTIFICATION_ENTITY_ID))
                    } else {
                        getYoutubeLiveClassStatus(
                            intent.getStringExtra(NOTIFICATION_LIVE_BATCH_ID),
                            intent.getStringExtra(NOTIFICATION_ENTITY_ID)
                        )
                    }
                }
                Constant.NOTIFICATION_LIVE_BATCH_TESTSET_STARTED, Constant.NOTIFICATION_LIVE_BATCH_ITEM_STARTED -> {
                    checkTestType(
                        intent.getStringExtra(NOTIFICATION_TEST_SET_TYPE),
                        intent
                    )
                }

                NOTIFICATION_LIVE_BATCH_STARTED, NOTIFICATION_LIVE_BATCH_CLASS_REJECTED -> {
                    bottom_nav.hide()
                    moveLiveBatchDetailFragment(
                        intent,
                        if (targetName == NOTIFICATION_LIVE_BATCH_STARTED) null else (targetName == NOTIFICATION_ASSIGNMENT_REJECTED || intent.getBooleanExtra(
                            NOTIFICATION_LIVE_BATCH_STARTED_SHOW_POP_UP,
                            false
                        ))
                    )
                }

                NOTIFICATION_LIVE_BATCH_CLASS_APPROVED -> {
                    bottom_nav.hide()
                    moveLiveBatchDetailFragment(
                        intent,
                        null,
                        isApproved = targetName == NOTIFICATION_LIVE_BATCH_CLASS_APPROVED
                    )
                }

                NOTIFICATION_ASSIGNMENT_EVALUATED, NOTIFICATION_ASSIGNMENT_REJECTED -> {
                    moveAssignment(intent.getStringExtra(NOTIFICATION_TESTSET_ID))
                }

                MOCK_INTERVIEW_APPEARED -> {
                    startActivity(Intent(this, MockInterviewActivity::class.java))
                }

                MOCK_INTERVIEW_ATTENDED -> {
                    val videoId = intent.getStringExtra(KEY_VIDEO_ID)
                    startActivity(Intent(
                        this,
                        VideoPlayActivity::class.java
                    ).apply {
                        putExtra(
                            KEY_VIDEO_ID,
                            videoId
                        )
                    })
                }
                else -> {
                    if (coursesId != null && categoryId != null) {
                        startActivity(
                            moveCourseDetailActivity(
                                coursesId,
                                categoryId
                            )
                        )
                    }
                }
            }
        }
    }

    private fun moveChatActivity(intent: Intent) {
        val roomId = intent.getStringExtra(NOTIFICATION_ROOM_ID)
        val threadId = intent.getStringExtra(NOTIFICATION_THREAD_ID)
        Intent(
            this,
            ChatActivity::class.java
        ).apply {
            putExtras(Bundle().apply {
                putExtra(
                    Constant.ROOM_ID,
                    roomId
                )
                putExtra(
                    Constant.MOVE_IN_PRIVATE_THREAD,
                    !threadId.isNullOrEmpty()
                )
                putExtra(
                    Constant.FROM_NOTIFICATION,
                    true
                )
                putExtra(
                    NOTIFICATION_THREAD_ID,
                    threadId
                )
            })
            startActivity(this)
        }
    }

    private fun moveLiveBatchDetailFragment(
        intent: Intent,
        isAttended: Boolean? = false,
        isApproved: Boolean = false
    ) {
        addReplaceFragment(
            R.id.fl_container,
            LiveBatchesDetailFragment().apply {
                val bundle = getLiveBatchDetailArgument(
                    intent.getStringExtra(NOTIFICATION_LIVE_BATCH_ID),
                    intent.getBooleanExtra(
                        NOTIFICATION_LIVE_BATCH_IS_PURCHASE,
                        false
                    ),
                    isAttended
                )
                val showPopUp = intent.getBooleanExtra(
                    NOTIFICATION_LIVE_BATCH_STARTED_SHOW_POP_UP,
                    false
                )
                bundle.putBoolean(
                    NOTIFICATION_LIVE_BATCH_STARTED_SHOW_POP_UP,
                    showPopUp
                )
                bundle.putString(
                    NOTIFICATION_ENTITY_ID,
                    intent.getStringExtra(NOTIFICATION_ENTITY_ID)
                )
                bundle.putBoolean(
                    KEY_IS_APPROVED,
                    isApproved
                )
                arguments = bundle
            },
            addFragment = true,
            addToBackStack = true
        )
    }

    private fun getLiveBatchDetailArgument(
        id: String?,
        isPurchase: Boolean?,
        isAttended: Boolean?
    ): Bundle {
        return Bundle().apply {
            putString(
                Constant.LIVE_BATCHES_ID,
                id
            )
            putString(
                Constant.LIVE_BATCHES_ACTIVATE_TAB,
                if (isAttended != null) {
                    if (isAttended) {
                        LearnScheduleTabs.ATTENDED.name
                    } else {
                        LearnScheduleTabs.MISSED.name
                    }
                } else {
                    null
                }
            )
            isPurchase?.let {
                putBoolean(
                    Constant.LIVE_BATCHES_IS_PURCHASE,
                    it
                )
            }
        }
    }

    private fun moveCourseFragment(
        coursesId: String?,
        categoryId: String?
    ) {
        if (!coursesId.isNullOrEmpty()) {
            addReplaceFragment(
                R.id.fl_container,
                CoursesFragment().apply {
                    arguments = Bundle().apply {
                        putString(
                            KEY_SUBJECT_ID,
                            coursesId
                        )
                        putString(
                            KEY_CATEGORY_ID,
                            categoryId
                        )
                    }
                },
                addFragment = true,
                addToBackStack = true
            )
        }
    }

    private fun moveYoutubePlayerActivity(intent: Intent?) {
        startActivity(
            Intent(
                this,
                LiveBatchYoutubePlayerActivity::class.java
            ).apply {
                putExtras(Bundle().apply {
                    putString(
                        KEY_YOUTUBE_BATCH_ID,
                        intent?.getStringExtra(NOTIFICATION_LIVE_CLASS_ID)
                    )
                    putExtra(
                        Constant.NOTIFICATION_YOUTUBE_VIDEO_ID,
                        intent?.getStringExtra(Constant.NOTIFICATION_YOUTUBE_VIDEO_ID)
                    )
                    putBoolean(
                        Constant.IS_FORM_SPECIAL_CLASS,
                        true
                    )
                })
            })
    }

    private fun moveLiveClassesCallActivity(
        intent: Intent?,
        isSpecialClass: Boolean = false,
        isClassOver: Boolean = false
    ) {
        if (intent != null) {
            startActivity(
                Intent(
                    this@HomeActivity,
                    LiveClassesCallActivity::class.java
                ).apply {
                    putExtras(Bundle().apply {
                        putString(
                            NOTIFICATION_ENTITY_ID,
                            intent.getStringExtra(NOTIFICATION_ENTITY_ID)
                        )
                        putString(
                            NOTIFICATION_ENTITY_TYPE,
                            intent.getStringExtra(NOTIFICATION_ENTITY_TYPE)
                        )
                        putBoolean(
                            IS_CLASS_OVER,
                            isClassOver
                        )
                        putString(
                            NOTIFICATION_LIVE_BATCH_ID,
                            if (isSpecialClass) {
                                intent.getStringExtra(NOTIFICATION_LIVE_CLASS_ID)
                            } else {
                                intent.getStringExtra(NOTIFICATION_LIVE_BATCH_ID)
                            }
                        )
                        putExtra(
                            Constant.NOTIFICATION_YOUTUBE_VIDEO_ID,
                            intent.getStringExtra(Constant.NOTIFICATION_YOUTUBE_VIDEO_ID)
                        )
                        putString(
                            Constant.NOTIFICATION_LIVE_CLASS_TITLE,
                            intent.getStringExtra(Constant.NOTIFICATION_LIVE_CLASS_TITLE)
                        )
                        putString(
                            Constant.NOTIFICATION_LIVE_CLASS_STARTS_AT,
                            intent.getStringExtra(Constant.NOTIFICATION_LIVE_CLASS_STARTS_AT)
                        )
                        putString(
                            Constant.NOTIFICATION_USER_FULL_NAME,
                            intent.getStringExtra(Constant.NOTIFICATION_USER_FULL_NAME)
                        )
                        putString(
                            Constant.NOTIFICATION_USER_PROFILE_URL,
                            intent.getStringExtra(Constant.NOTIFICATION_USER_PROFILE_URL)
                        )
                        putString(
                            Constant.NOTIFICATION_CATEGORY_TITLE,
                            intent.getStringExtra(Constant.NOTIFICATION_CATEGORY_TITLE)
                        )
                        putString(
                            Constant.NOTIFICATION_USER_DEGREE,
                            intent.getStringExtra(Constant.NOTIFICATION_USER_DEGREE)
                        )
                        putString(
                            Constant.NOTIFICATION_SOURCE,
                            intent.getStringExtra(Constant.NOTIFICATION_SOURCE)
                        )
                    })
                })
        }
    }

    private fun moveCourseDetailActivity(
        courseID: String,
        courseItemID: String
    ): Intent {
        val intent = Intent(
            this,
            CourseDetailsActivity::class.java
        )
        intent.putExtras(
            getArgument(
                courseID,
                courseItemID
            )
        )
        return intent
    }

    private fun getArgument(
        courseID: String,
        courseItemID: String
    ): Bundle {
        return Bundle().apply {
            putString(
                KEY_COURSE_ID,
                courseID
            )
            putString(
                Constant.KEY_COURSE_ITEM_COURSE_ID,
                courseItemID
            )
            putBoolean(
                Constant.IS_FROM_NOTIFICATION,
                true
            )
        }
    }

    private fun updateProfile() {
        setDetailsNavigationText()
    }

    private fun addProfileObserver() {
        val liveData = getProfileChangeLiveData()
        liveData.observe(
            this,
            Observer {
                if (it != null && it) {
                    updateProfile()
                }
            })
    }

    private fun checkTestType(
        type: String?,
        intent: Intent
    ) {
        if (type == LiveBatchesGroupType.MCQ.name) {
            moveMCQ(intent.getStringExtra(NOTIFICATION_ENTITY_ID))
        } else if (type == LiveBatchesGroupType.ASSIGNMENT.name) {
            moveAssignment(intent.getStringExtra(NOTIFICATION_ENTITY_ID))
        }
    }

    private fun moveAssignment(id: String? = null) {
        val intent = Intent(
            this,
            AssignmentActivity::class.java
        )
        intent.putExtra(
            COURSE_ITEM_ID,
            id
        )
        startActivity(intent)
    }

    private fun moveMCQ(id: String? = null) {
        val intent = Intent(
            this,
            McqQuizActivity::class.java
        )
        intent.putExtra(
            COURSE_ITEM_ID,
            id
        )
        startActivity(intent)
    }

    //call LiveBatchesDetailFragment API when user came back from the background
    override fun onRestart() {
        super.onRestart()
        val fragment =
            supportFragmentManager.findFragmentByTag(LiveBatchesDetailFragment::class.java.simpleName)
        if (fragment is LiveBatchesDetailFragment) {
            fragment.callAPI()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        when (getCurrentFragment(R.id.fl_container)) {
            is HomeFragment -> {
                setTabs(TabsHome.HOME)
            }
            is MembershipMainFragment -> {
                setTabs(TabsHome.UNLOCK)
            }
        }
    }

    fun setDefaultTabs() {
        txt_bottom_home.isSelected = true
        txt_bottom_help_center.isSelected = false
        txt_bottom_unlock.isSelected = false
    }

    private fun getLiveClassAttendedStatus(id: String? = null) {
        lifecycleScope.launch {
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
        lifecycleScope.launch {
            mTokenViewModel.youtubeLiveClassStatus(
                liveBatchId,
                entityId
            )
        }
    }
}
