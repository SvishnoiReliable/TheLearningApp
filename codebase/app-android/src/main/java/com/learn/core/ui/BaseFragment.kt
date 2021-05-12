package com.learn.core.ui

import android.content.Context
import androidx.fragment.app.Fragment
import com.learn.util.hideKeyboard
import com.learn.view.activity.BaseActivity
import com.learn.view.activity.HomeActivity



abstract class BaseFragment : Fragment(), BaseView {

    private var mBaseActivity: BaseActivity? = null

    override fun showError(error: String) {

    }

    override fun showLoader() {
        mBaseActivity?.showLoader()
    }


    override fun hideLoader() {
        mBaseActivity?.hideLoader()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is BaseActivity)
            mBaseActivity = context
    }

    fun showBottomBar(show: Boolean) {
        if (requireActivity() is HomeActivity) {
            (activity as HomeActivity).setBottomBar(show)
        }
    }

    override fun onDestroyView() {
        activity?.hideKeyboard()
        super.onDestroyView()
    }
}
