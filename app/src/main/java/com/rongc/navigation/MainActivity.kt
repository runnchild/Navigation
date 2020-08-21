package com.rongc.navigation

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentContainerView
import androidx.navigation.findNavController
import com.navigation.annotation.FragmentDestination
import com.navigation.navigation.NavGraphBuilder

@FragmentDestination(url = "/navigation/main", isStarter = true)
class MainActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val containerView = findViewById<FragmentContainerView>(R.id.containerView)
        NavGraphBuilder.buildTab(
            this,
            containerView.findNavController(),
            containerView.id
        )
    }
}