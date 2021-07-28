package com.rongc.navigation

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.forEach
import androidx.databinding.DataBindingUtil
import androidx.navigation.*
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.navigation.navigation.NavGraphBuilder
import com.navigation.navigation.destId
import com.navigation.navigation.navigateBy
import com.rongc.navigation.databinding.ActivityMainBinding
import com.rongc.navigator.NavigationAppNavigator

class MainActivity : AppCompatActivity() {
    private lateinit var controller: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
//        val binding =
//            DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main)

        val container =
            supportFragmentManager.findFragmentById(R.id.containerView) as NavHostFragment

        controller = container.findNavController()
        val bottomBar = findViewById<BottomNavigationView>(R.id.bottom_bar)
//        bottomBar.setupWithNavController(controller)
        bottomBar.setOnItemSelectedListener {
            val destination = when (it.itemId) {
                R.id.home -> NavigationAppNavigator.HOME_HOME
                R.id.message -> NavigationAppNavigator.MESSAGE_HOME
                R.id.mine -> NavigationAppNavigator.MINE_HOME
                R.id.navigation -> NavigationAppNavigator.NAVIGATION_ONE
                else -> ""
            }
            onItemSelected(destination.destId())
            true
        }
        NavGraphBuilder.build(this, container.findNavController(), R.id.containerView)
        bottomBar.menu.forEach {

        }
//        binding.bottomBar.menu
    }

    private fun onItemSelected(id: Int) {
        val builder = NavOptions.Builder().setLaunchSingleTop(true).setRestoreState(true)
        builder.setPopUpTo(
            findStartDestination(controller.graph).id,
            inclusive = false,
            saveState = true
        )
        controller.navigate(id, null, builder.build())
    }

    fun findStartDestination(graph: NavGraph): NavDestination =
        generateSequence(graph.findNode(graph.startDestinationId)) {
            if (it is NavGraph) {
                it.findNode(it.startDestinationId)
            } else {
                null
            }
        }.last()

    override fun supportNavigateUpTo(upIntent: Intent) {
        super.supportNavigateUpTo(upIntent)
    }

    override fun onNavigateUp(): Boolean {
        return controller.navigateUp()
    }

    override fun onSupportNavigateUp(): Boolean {
        return controller.navigateUp() && super.onNavigateUp()
    }

    override fun onBackPressed() {
        if (!controller.navigateUp()) {
            super.onBackPressed()
        }
    }
}