package com.learn.core.ui


interface BaseView {
    fun showError(error: String)
    fun showLoader()
    fun hideLoader()
}
