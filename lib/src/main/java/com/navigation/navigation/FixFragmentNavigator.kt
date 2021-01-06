package com.navigation.navigation

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.navigation.NavDestination
import androidx.navigation.NavOptions
import androidx.navigation.Navigator
import androidx.navigation.fragment.FragmentNavigator
import androidx.navigation.fragment.NavHostFragment
import com.rongc.navigation.R
import java.lang.Thread.sleep
import java.util.*

@Navigator.Name("fixFragment")
class FixFragmentNavigator(
    private val context: Context,
    private val manager: FragmentManager,
    private val containerId: Int
) :
    FragmentNavigator(context, manager, containerId) {
    companion object {
        const val TAG = "FixFragmentNavigator"
    }

    private val mBackStack: ArrayDeque<Int> by lazy {
        FragmentNavigator::class.java.getDeclaredField("mBackStack").run {
            isAccessible = true
            @Suppress("UNCHECKED_CAST")
            get(this@FixFragmentNavigator) as ArrayDeque<Int>
        }
    }

    override fun navigate(
        destination: Destination,
        args: Bundle?,
        navOptions: NavOptions?,
        navigatorExtras: Navigator.Extras?
    ): NavDestination? {
        val mFragmentManager = manager
        val mContext = context
        val mContainerId = containerId

        if (mFragmentManager.isStateSaved) {
            Log.i(
                TAG, "Ignoring navigate() call: FragmentManager has already"
                        + " saved its state"
            )
            return null
        }
        var className = destination.className
        if (className[0] == '.') {
            className = mContext.packageName + className
        }

        val ft = mFragmentManager.beginTransaction()

        val options = navOptions ?: destination.getAction(destination.id)?.navOptions
        var enterAnim = options?.enterAnim ?: R.anim.slide_enter
        var exitAnim = options?.exitAnim ?: R.anim.slide_exit
        var popEnterAnim = options?.popEnterAnim ?: R.anim.slide_pop_enter
        var popExitAnim = options?.popExitAnim ?: R.anim.slide_pop_exit

        if (enterAnim != -1 || exitAnim != -1 || popEnterAnim != -1 || popExitAnim != -1) {
            enterAnim = if (enterAnim != -1) enterAnim else 0
            exitAnim = if (exitAnim != -1) exitAnim else 0
            popEnterAnim = if (popEnterAnim != -1) popEnterAnim else 0
            popExitAnim = if (popExitAnim != -1) popExitAnim else 0
            ft.setCustomAnimations(enterAnim, exitAnim, popEnterAnim, popExitAnim)
        }

        val preFrag = mFragmentManager.primaryNavigationFragment
        @IdRes val destId = destination.id

        val tag = destination.id.toString()

        var frag: Fragment? = null
        if ( NavGraphBuilder.isTab(destination.id) || destination.id == NavGraphBuilder.startDestinationId) {
            frag = mFragmentManager.findFragmentByTag(tag)
        }
        @Suppress("DEPRECATION")
         frag = (frag ?: instantiateFragment(
            mContext,
            mFragmentManager,
            className,
            args
        )).apply {
            arguments = args
            if (!isAdded) {
                ft.add(mContainerId, this, tag)
            }
        }

//        val preIsTab = NavGraphBuilder.isTab(preFrag?.tag?.toInt() ?: 0)
//        if (preIsTab && NavGraphBuilder.isTab(destId)) {
//            preFrag?.let {
//                ft.hide(it)
//            }
//            ft.show(frag)
//        } else {
//            ft.replace(mContainerId, frag)
//        }

//        if (isTab) {
//        mFragmentManager.fragments.forEach { if (it !is NavHostFragment) ft.hide(it) }

//        if (preIsTab) {
            preFrag?.let {
                ft.hide(it)
            }
//        }
        ft.show(frag)
//        } else {
//            ft.replace(mContainerId, frag, tag)
//        }

        ft.setPrimaryNavigationFragment(frag)
        val initialNavigation = mBackStack.isEmpty()
        // TODO Build first class singleTop behavior for fragments
        val isSingleTopReplacement = (options != null && !initialNavigation
                && options.shouldLaunchSingleTop()
                && mBackStack.peekLast() == destId)

        val isAdded: Boolean
        isAdded = when {
            initialNavigation -> {
                true
            }
            isSingleTopReplacement -> {
                // Single Top means we only want one instance on the back stack
                if (mBackStack.size > 1) {
                    // If the Fragment to be replaced is on the FragmentManager's
                    // back stack, a simple replace() isn't enough so we
                    // remove it from the back stack and put our replacement
                    // on the back stack in its place
                    mFragmentManager.popBackStack(
                        generateBackStackName(mBackStack.size, mBackStack.peekLast()!!),
                        FragmentManager.POP_BACK_STACK_INCLUSIVE
                    )
                    ft.addToBackStack(generateBackStackName(mBackStack.size, destId))
                }
                false
            }
            else -> {
                ft.addToBackStack(generateBackStackName(mBackStack.size + 1, destId))
                true
            }
        }
        if (navigatorExtras is Extras) {
            for ((key, value) in navigatorExtras.sharedElements) {
                ft.addSharedElement(key!!, value!!)
            }
        }
        ft.setReorderingAllowed(true)
        ft.commit()
        // The commit succeeded, update our view of the world
        return if (isAdded) {
            mBackStack.add(destId)
            destination
        } else {
            null
        }
    }

    private fun generateBackStackName(backStackIndex: Int, destId: Int): String {
        return "$backStackIndex-$destId"
    }
}