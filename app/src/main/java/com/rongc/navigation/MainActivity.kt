package com.rongc.navigation

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.navigation.findNavController
import com.navigation.annotation.FragmentDestination
import com.navigation.navigation.NavGraphBuilder
import com.rongc.navigation.databinding.ActivityMainBinding

@FragmentDestination(url = "/navigation/main", isStarter = true)
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding =
            DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main)

        val containerView = binding.containerView
        NavGraphBuilder.buildTab(
            this,
            containerView.findNavController(),
            containerView.id
        )
    }
}