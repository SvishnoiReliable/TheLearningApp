package com.learn.util

import android.content.ContextWrapper
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.graphics.drawable.DrawableCompat
import androidx.databinding.BindingAdapter
import androidx.lifecycle.MutableLiveData
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.learn.R
import com.learn.model.response.SubscriptionInfo
import com.learn.model.response.agora.AgoraStudent


fun <T> mutableLiveData(defaultValue: T? = null): MutableLiveData<T> {
    val data = MutableLiveData<T>()
    if (defaultValue != null) {
        data.value = defaultValue
    }
    return data
}

fun View.getParentActivity(): AppCompatActivity? {
    var context = this.context
    while (context is ContextWrapper) {
        if (context is AppCompatActivity) {
            return context
        }
        context = context.baseContext
    }
    return null
}

fun <T> ImageView.setPicture(
    obj: T,
    options: RequestOptions? = null,
    listener: RequestListener<Drawable>? = null,
    placeholder: Int? = null
) {
    Glide.with(this).load(obj).also {
        placeholder?.let { it1 -> it.placeholder(it1) }
        it.apply(
            options ?: RequestOptions.centerCropTransform().diskCacheStrategy(DiskCacheStrategy.ALL)
        )
        if (listener != null)
            it.listener(listener)
        it.into(this)
    }
}

fun <T> ImageView.setIcon(obj: T) {
    Glide.with(this).load(obj).also {
        it.apply(RequestOptions().diskCacheStrategy(DiskCacheStrategy.NONE))
        it.into(this)
    }
}

fun <T> ImageView.setBitmap(obj: T) {
    Glide.with(this).load(obj).also {
        it.apply(RequestOptions().diskCacheStrategy(DiskCacheStrategy.AUTOMATIC))
        it.into(this)
    }
}

@BindingAdapter("setVisible")
fun setVisible(layout: View?, show: Boolean?) {
    if (layout != null && show != null && show) {
        layout.show()
    } else {
        layout?.hide()
    }
}

@BindingAdapter("setItemImage")
fun setItemImage(image: AppCompatImageView, url: String?) {
    if (!url.isNullOrEmpty()) {
        image.setPicture(url, placeholder = R.drawable.icon_courses_bag)
    } else {
        image.setPicture(R.drawable.icon_courses_bag)
    }
}

@BindingAdapter("setDateView")
fun setDateView(
    view: AppCompatTextView,
    date: String?
) {
    view.text = date?.let {
        getDate(it)
    }
}

@BindingAdapter("setHtmlText")
fun setHtmlText(
    view: AppCompatTextView,
    text: String?
) {
    view.setHtmlText = text.toString()
}


@BindingAdapter("setProfileUrl")
fun setProfileUrl(image: AppCompatImageView, url: String?) {
    if (!url.isNullOrBlank()) {
        image.setPicture(url)
    }
}

@BindingAdapter("setVisibleLock")
fun setVisibleLock(imageView: View, subscriptionInfo: SubscriptionInfo?) {
    if (subscriptionInfo != null) {
        if (subscriptionInfo.isActive) {
            imageView.hide()
        } else {
            imageView.show()
        }
    } else {
        imageView.hide()
    }
}

@BindingAdapter("setInitialsShapeColorChange")
fun setInitialsShapeColorChange(tvInitials: AppCompatTextView, student: AgoraStudent?) {
    if (student != null) {
        student.color?.let { DrawableCompat.setTint(tvInitials.background, it) }
        student.fullName?.let{tvInitials.text= getInitials(it)}
    }
}

@BindingAdapter("setActivate")
fun setActivate(layout: View?, show: Boolean?) {
    if (layout != null && show != null) {
        layout.isActivated = show
    }
}
