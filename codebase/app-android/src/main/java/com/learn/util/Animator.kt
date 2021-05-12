package com.learn.util

import android.view.View


interface Animator {
    fun animate(view: View, offset: Int): Boolean
}
