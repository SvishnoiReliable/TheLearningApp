package com.learn.view.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.learn.R
import com.learn.databinding.McqColorCodeBinding
import com.learn.model.ColorCodeModel
import com.learn.model.response.mcq.McqResultsDataTotal

class AdapterForRecyclerViewForColorCodes(
    var context: Context,
    var isAnswerCountVisible: Boolean,
    colorCount: McqResultsDataTotal
) :
    RecyclerView.Adapter<AdapterForRecyclerViewForColorCodes.ViewHolderColorCode>() {

    private var colors: ArrayList<ColorCodeModel> = arrayListOf(
        ColorCodeModel(
            R.drawable.shape_color_code_red,
            context.resources.getString(R.string.mcq_not_answered),
            colorCount.notAnswered
        ),
        ColorCodeModel(
            R.drawable.shape_color_code_green,
            context.resources.getString(R.string.answered),
            colorCount.answered
        ),
        ColorCodeModel(
            R.drawable.shape_color_code_marked_review,
            context.resources.getString(R.string.marked_for_review),
            colorCount.markForReview
        ),
        ColorCodeModel(
            R.drawable.shape_color_code_twilight_blue,
            context.resources.getString(R.string.not_visited),
            colorCount.notVisited
        ),
        ColorCodeModel(
            R.drawable.shape_answered_marked_for_review,
            context.resources.getString(R.string.answered_marked_for_review),
            colorCount.answeredAndMarkforReview
        )
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderColorCode {
        val binding =
            McqColorCodeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolderColorCode(binding)
    }

    override fun getItemCount(): Int = colors.size

    override fun onBindViewHolder(holder: ViewHolderColorCode, position: Int) {
        val data = colors[position]
        data.let { holder.bindData(it) }
    }

    inner class ViewHolderColorCode(val binding: McqColorCodeBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bindData(data: ColorCodeModel) {
            binding.colorCodeText.text = data.textColorCode
            binding.colorCodeIV.setImageResource(data.color)
            if (isAnswerCountVisible) {
                binding.colorCodeText.append(" - ${data.count}")
            }
        }
    }
}
