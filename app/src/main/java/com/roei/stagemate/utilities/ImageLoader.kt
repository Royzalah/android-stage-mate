package com.roei.stagemate.utilities

import android.content.Context
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.roei.stagemate.R

// Glide wrapper per course standard (image-loader.md).
// Accessed globally via MyApp.imageLoader.loadImage(source, imageView).
class ImageLoader(private val context: Context) {

    fun loadImage(source: Any, imageView: ImageView) {
        Glide.with(imageView)
            .load(source)
            .centerCrop()
            .override(800, 600)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .placeholder(R.drawable.placeholder_event)
            .error(R.drawable.placeholder_event)
            .into(imageView)
    }

}
