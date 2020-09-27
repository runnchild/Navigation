package com.navigation.navigation

import android.os.Bundle
import androidx.navigation.NavOptions
import androidx.navigation.Navigator

class NavigationParam {
    lateinit var url: String
    lateinit var bundle: Bundle
    var options: NavOptions? = null
    var extra: Navigator.Extras? = null
}