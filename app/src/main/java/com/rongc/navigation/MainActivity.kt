package com.rongc.navigation

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.navigation.navigation.NavGraphBuilder
import com.rongc.navigation.databinding.ActivityMainBinding
import com.rongc.navigator.NavigationNavigator

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding =
            DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main)

        val container = supportFragmentManager.findFragmentById(R.id.containerView) as NavHostFragment

        val containerView = binding.containerView
        NavGraphBuilder.buildOther(
            this,
            container.findNavController(),
            containerView.id
        )

//        NavigationNavigator
    }
}