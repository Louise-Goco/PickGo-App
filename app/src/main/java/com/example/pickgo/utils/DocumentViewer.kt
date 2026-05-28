package com.example.pickgo.utils


import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.bumptech.glide.Glide
import com.example.pickgo.R

object DocumentViewer {

    fun showDocumentDialog(context: Context, documents: List<Pair<String, String?>>) {
        if (documents.isEmpty()) return

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 20)
        }

        documents.forEach { (title, url) ->
            if (!url.isNullOrBlank()) {
                val itemLayout = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply { bottomMargin = 24 }
                }

                val titleText = TextView(context).apply {
                    text = title
                    textSize = 14f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    setTextColor(context.getColor(R.color.text_primary))
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply { bottomMargin = 8 }
                }
                itemLayout.addView(titleText)

                val imageView = ImageView(context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        400
                    )
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    setBackgroundColor(context.getColor(R.color.input_border))

                    Glide.with(context)
                        .load(url)
                        .placeholder(R.drawable.placeholder_food)
                        .error(R.drawable.placeholder_food)
                        .into(this)
                }
                itemLayout.addView(imageView)

                layout.addView(itemLayout)
            }
        }

        AlertDialog.Builder(context)
            .setTitle("Documents")
            .setView(layout)
            .setPositiveButton("Close", null)
            .show()
    }
}