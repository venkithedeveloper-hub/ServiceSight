package com.ats.servicesight.classes

import android.widget.ImageView
import com.squareup.picasso.Picasso

class CommonClass {
    fun loadImg(img : String, imgView : ImageView) {
        Picasso.get().load(img).into(imgView)
    }
}