package com.navigation.navigation

import androidx.annotation.Keep

@Keep
class Destination(val url: String) {
    var id: Int = 0
    var needLogin = false
    var isStarter = false
    var isFragment = false
    var className: String= ""
    var isHomeTab = false
    var title: String = ""
    var popAnim = false
    var animStyle = ANIM_DEFAULT
}