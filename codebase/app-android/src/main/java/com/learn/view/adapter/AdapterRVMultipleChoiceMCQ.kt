package com.learn.view.adapter

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import androidx.appcompat.widget.AppCompatCheckedTextView
import androidx.recyclerview.widget.RecyclerView
import com.learn.databinding.ViewPagerItemMultipleBinding
import com.learn.model.response.mcq.Option
import com.learn.model.response.mcq.Question
import com.learn.util.setHtmlText


class AdapterRVMultipleChoiceMCQ(
    var modelDataTotal: List<Question>,
    val modelPosition: Int
) :
    RecyclerView.Adapter<AdapterRVMultipleChoiceMCQ.RecyclerVH>() {

    val modelListAnswerOptions = modelDataTotal[modelPosition].options
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerVH {
        val binding =
            ViewPagerItemMultipleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RecyclerVH(binding)
    }

    override fun getItemCount(): Int = modelListAnswerOptions?.size!!

    override fun onBindViewHolder(
        holder: RecyclerVH,
        position: Int
    ) {
        val data = modelListAnswerOptions?.get(position)
        data?.let { holder.bindData(it) }
    }

    inner class RecyclerVH(var binding: ViewPagerItemMultipleBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.option.setOnClickListener { checkBox ->
                modelListAnswerOptions?.get(layoutPosition)?.isChecked =
                    !modelListAnswerOptions?.get(layoutPosition)?.isChecked!!

                (checkBox as AppCompatCheckedTextView).isChecked =
                    modelListAnswerOptions[layoutPosition].isChecked

                for (i in modelListAnswerOptions) {
                    if (i.isChecked) {
                        modelDataTotal[modelPosition].isAnswered = true
                        break
                    } else {
                        modelDataTotal[modelPosition].isAnswered = false
                    }
                }

                val timerAnimation = ObjectAnimator.ofPropertyValuesHolder(
                    checkBox, PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 0.95f, 1f),
                    PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 0.95f, 1f)
                ).apply {
                    duration = 300L
                    interpolator = OvershootInterpolator()
                }
                timerAnimation.start()
            }
        }

        fun bindData(answerOptions: Option) = binding.apply {
            binding.option.setHtmlText = answerOptions.answerOptionText.toString()
            binding.option.isChecked = answerOptions.isChecked
        }
    }
}
