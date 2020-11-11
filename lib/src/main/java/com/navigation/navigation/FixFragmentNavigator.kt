package com.navigation.navigation

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.annotation.IdRes
import androidx.fragment.app.FragmentManager
import androidx.navigation.NavDestination
import androidx.navigation.NavOptions
import androidx.navigation.Navigator
import androidx.navigation.fragment.FragmentNavigator
import com.rongc.navigation.R
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

        var enterAnim = navOptions?.enterAnim ?: R.anim.slide_enter
        var exitAnim = navOptions?.exitAnim ?: R.anim.slide_exit
        var popEnterAnim = navOptions?.popEnterAnim ?: R.anim.slide_pop_enter
        var popExitAnim = navOptions?.popExitAnim ?: R.anim.slide_pop_exit

        if (enterAnim != -1 || exitAnim != -1 || popEnterAnim != -1 || popExitAnim != -1) {
            enterAnim = if (enterAnim != -1) enterAnim else 0
            exitAnim = if (exitAnim != -1) exitAnim else 0
            popEnterAnim = if (popEnterAnim != -1) popEnterAnim else 0
            popExitAnim = if (popExitAnim != -1) popExitAnim else 0
            ft.setCustomAnimations(enterAnim, exitAnim, popEnterAnim, popExitAnim)
        }

        var frag = mFragmentManager.primaryNavigationFragment
        frag?.let {
            ft.hide(it)
        }
        mFragmentManager.fragments.forEach {
            ft.hide(it)
        }
        val tag = destination.id.toString()
        @Suppress("DEPRECATION")
        frag = mFragmentManager.findFragmentByTag(tag).apply {
            args?.let {
                this?.arguments = it
            }
        } ?: instantiateFragment(mContext, mFragmentManager, className, args).apply {
            arguments = args
            ft.add(mContainerId, this, tag)
            frag = this
        }
        ft.setPrimaryNavigationFragment(frag)
        ft.show(frag!!)

        @IdRes val destId = destination.id
        val initialNavigation = mBackStack.isEmpty()
        // TODO Build first class singleTop behavior for fragments
        val isSingleTopReplacement = (navOptions != null && !initialNavigation
                && navOptions.shouldLaunchSingleTop()
                && mBackStack.peekLast() == destId)

        val isTab = NavGraphBuilder.isTab(destId)

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
            isTab -> {
                mFragmentManager.findFragmentByTag(tag) == null
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
//            if (isTab && mBackStack.size > 1 && NavGraphBuilder.isTab(mBackStack.peekLast()?:0)) {
//                mBackStack.removeLast()
//            }
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