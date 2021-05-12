package com.learn.core.ui

import androidx.lifecycle.ViewModel
import com.learn.model.ErrorModel
import com.learn.util.mutableLiveData


open class BaseViewModel : ViewModel() {
    var mError = mutableLiveData(ErrorModel())
}
