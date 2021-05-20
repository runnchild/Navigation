# Navigation [![](https://jitpack.io/v/runnchild/Navigation.svg)](https://jitpack.io/#runnchild/Navigation)
基于jetpack navigation改造的路由框架

## 一、功能介绍
1. 支持直接解析标准URL进行跳转
2. 支持多模块，组件化工程使用
3. 支持Activity/Fragment/Dialog路由跳转
4. 支持设置登录拦截

## 二、典型应用
1. 从外部URL映射到内部页面，以及参数传递
2. 跨模块页面跳转，模块间解耦
3. 拦截跳转过程，处理登陆逻辑
4. 跨模块API调用，通过控制反转来做组件解耦

## 三、基础功能
#### 1. 添加依赖和配置

Step 1. Add the JitPack repository to your build file
Add it in your root build.gradle at the end of repositories:
```
allprojects {
  repositories {
    ...
    maven { url 'https://jitpack.io' }
  }
}
```

Step 2. Add the dependency

```
dependencies {
   implementation 'com.github.runnchild.Navigation:navigation:2.0.0'
   kapt 'com.github.runnchild.Navigation:compiler:2.0.0'
   implementation "com.github.runnchild.Navigation:annotation:2.0.0"
}
```

Step 3. Add the kapt arguments
```
android {
  kapt {
    arguments {
      arg("MODULE_NAME", project.module_name) //模块名称，例如：Home，Message，Mine
      arg("PROJECT_NAME", project.name)
    }
  }
}
```
#### 2. 添加注解
```
@FragmentDestination("home/home", "首页-推荐", isStarter = true, isHomeTab = true, needLogin = false)
class HomeFragment : Fragment() {
}

```
#### 3. 发起路由
```
// 打开页面
navController().navigateBy(HomeNavigator.HOME_HOME)
// 携带参数
navController().navigateBy(HomeNavigator.HOME_HOME， bundleOf("key1" to "value1", "key2" to 100))
```

## 四、基本用法
使用前最好对[JitPack Navigation](https://developer.android.com/guide/navigation)有基本认识。
本库出发点是在使用Navigation实现单个Activity多Fragment的开发模式下，同样支持组件化或多模块间路由通讯。
也就是将定义在模块下res/navigation/navigation.xml的路由配置文件转换成字符串（url）以便与其他模块或跨平台通讯。
但也意味着有些初衷和功能与原库不一样，不过同样可以使用原库的所有方法。

以最简单的一个MainActivity多个Fragment构成的项目为例：

1. 创建MainActivity
```
class MainActivity : Activity() {
  private lateinit var navController: NavController
    
  override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
        navController = navHostFragment.navController

        // 如果在注解中设置了isStarter=true
        // NavGraphBuilder.build(this, navController, navHostFragment.id) 
        
        // 如果需要指定其他Fragment作为首个页面
        NavGraphBuilder.build(this, navController, navHostFragment.id) { n, d ->
            n.startDestination = AppAppNavigator.APP_MAIN_FRAGMENT.destId()
        }
    }
}
```
activity_main.xml
```
<androidx.fragment.app.FragmentContainerView
        android:id="@+id/nav_host"
        android:name="androidx.navigation.fragment.NavHostFragment"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />
```

2. 创建Fragment页面：
```
@FragmentDestination("home/home", "首页", isStarter = true, needLogin = false)
class HomeFragment : Fragment() {
    
    fun navigate(v: View) {
      // 打开列表页面
      v.findNavController().navigateBy(HomeNavigator.HOME_LIST)
    }
}

@FragmentDestination("home/list", "列表页", needLogin = false)
class ListFragment : Fragment() {
    
    // 打开详情页面
    fun navigate(v: View) {
      // Bundle 传参
      v.findNavController().navigateBy(HomeNavigator.HOME_DETAIL, bundleOf("id" to 123, "type" to 1))
      // Url 传参
      v.findNavController().navigateByl("http://home/detail?id=123&type=1")
      // or
      v.findNavController().navigateBy(HomeNavigator.HOME_DETAIL.deepLink("123", 1))
      // Uri
      v.findNavController().navigateBy(uri)
    }
}

@FragmentDestination("home/detail?id={id}&type={type}", title="详情页", doc="这是商品详情页", needLogin = true)
class ListFragment : Fragment() {

    private val id by lazy { arguments?.getString("id") }
    
    private val type by lazy { arguments?.getInt("type") }
    
    fun backToList() {
      navController().navigateUp()
    }
    
    fun backToHome() {
      navController().popBackTo(HomeNavigator.HOME_HOME)
    }
}
```

## 五、Api详细说明

> Destination注解：

```
@FragmentDestination(url="home/home", title="页面标题"， doc="url的注释"，isStarter=true, needLogin=true, animStyle = ANIMATE_POP)

url: 页面路由地址，由模块名称/页面名称组成，后面可以拼接参数，例如（/home/home?id=${id}&type=${type}）
title: 页面标题，可作为ToolBar的标题。
doc： 页面注释
isStarter:是否是首页，整个项目应只有一个为true，或可以在后期指定其他的页面作为首页，参考前面基本用法。
needLogin： 打开此页面是否需要先登录。可设置路由拦截器实现相关业务。
animStyle： 页面打开动画，内置3种跳转动画：ANIMATE_POP 弹出式; ANIMATE_DEFAULT 从右到左; ANIMATE_NON 无动画; 如需要自定义动画可在跳转时配置，后面会详解。
@ActivityDestination/@DialogDestination同理。
```
配置好后 make一下就会在对应模块下生成HomeNavigator路由定义接口：
```
public interface HomeNavigator {
  /**
   * url的注释
   */
  String HOME_HOME = "http://home/home";
}
```
> 页面跳转

```
// 页面跳转
navController.navigateBy(HomeNavigator.HOME_HOME)
// 页面带参跳转
navController.navigateBy(HomeNavigator.HOME_HOME， bundleOf("key" to "value"))
// 标准url跳转
navController.navigateBy("http://home/detail")
// 标准url带参跳转
navController.navigateBy("http://home/detail?id=123&type=1")
// deepLink跳转(前提是Fragment的注解的url地址为 "home/detail?id={id}&type={type}" )
navController.navigateBy(HomeNavigator.HOME_DETAIL.deepLink("123", 1))
// Uri跳转
val uri = intent.data
navController.navigateBy(uri)
```

> 更多操作

```
navController.navigateBy(HomeNavigator.HOME_HOME, options = navOptions {
  // Whether this navigation action should launch as single-top
  launchSingleTop = false
  // 跳转前弹出到指定页面
  popUpTo(HomeNavigator.HOME_LIST.destId()) {
    // 是否包含此页面，如果为true，则会把HOME_LIST也一并弹出。
    inclusive = true
  }
  
  // 跳转动画
  anim {
    // 详细参数注释参考 androidx.navigation.AnimBuilder 
    enter = R.anim.enter
    exit = ...
    popEnter = ...
    popExit = ...
  }
})
```

> 转场动画
```
navController.navigateBy(HomeNavigator.HOME_HOME, navExtra = FragmentNavigatorExtras { elements: Pair<View, String> ->
  transitionView1 to "transitionName1",
  transitionView2 to "transitionName2",
  transitionView3 to "transitionName3"
})
```

> 参数接收
```
@FragmentDestination("home/detail", title="详情页", doc="这是商品详情页", needLogin = true)
class ListFragment : Fragment() {

    // 跳转时传递的参数都会设置在arguments里面，正常从里拿就是。
    private val id by lazy { arguments?.getString("id") }
    // 后期考虑自动解析...
    private val type by lazy { arguments?.getInt("type") }
}
```

> 页面返回按下监听
```
requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
    navController().navigateUp()
}
```
> 页面返回时参数传递（onFragmentResult）
Fragment并不像Activity有onActivityResult方法，不过可以配合ViewModel实现，效果更完美：
```
class HomeFragment : Fragment() {
   override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // 在onViewCreated（）设置结果监听
        navController().observeCurrent<String>("onFragmentResult") { result->
            "result from NewFragment: $result".logd()
        }
  }
}
```

```
class NewFragment : Fragment() {
  fun back() {
    // 页面关闭前通知
    navController.notifyPreBack("onFragmentResult", "This is NewFragment result")
    navController.navgateUp()
    
    // navController.previousBackStackEntry是上一个页面的相关信息，这在做埋点需求时很实用。
  }
}
```

## 六、写在最后

目前商业项目应该很少人使用单个MainActivity搭配多个Fragment的方式写吧，刚看到JetPack的Navigation时还是很震撼的，遗憾的是无法在组件化的项目中使用。
本着爱瞎折腾精神改造了下，也在我自己的项目中使用，当然也碰到许多坑：

- 1. 默认的Fragment添加方式为replace，这就有点不理解了，每次打开新页面后返回上个页面都会被移除和重新添加？页面控件和数据都要重新创建和请求？显然是不合理的。
    此项目我改成了hide/show方式。
- 2. 当Fragment添加方式为hide/show时，旧页面和新打开的页面生命周期方法并不是成对出现，（旧页面并不会调用 onPause()方法，不过可通过onHiddenChanged()判断），
    顺序为打开新页面时A -> B： A.onHiddenChanged(true)->B.onResume()。
    返回B->A： A.onHiddenChanged(false)->B.onPause()->B.onHiddenChanged(true)->B.onDestroy().
    也就是返回时并不是B的onPause或者onHiddenChanged(true)先调用，这在做友盟页面统计时会提示生命周期方法并没有成对调用。
- 3. 当Fragment添加方式为hide/show时，如果图片加载框架为Fresco，在打开新页面时旧页面的图片会先隐藏起来，观感上就是页面图片部分白一下再打开页面。真让人头秃。。。
- 4. 如果应用首次安装时打开的第一个页面是引导或者欢迎页，之后再也不会打开这个页面，那么这个页面就不适合设置为startDestination。可把登录页面设为startDestination，
    并在onViewCreate中判断是否是首次打开决定要不要跳转到引导。
    其实并不一定项目只能一个Activity和多个Fragment，也可以根据业务模块分成多个Activity，每个Activity再对应多个Fragment，只不过每个Activity都要配置基本用法的第一步。
- 5. 其他暂没遇到大问题，不过不代表没有哈，如果要在商业项目中使用请考虑好利弊。如果各位大佬有解决方案或者其他建议还请不吝赐教。欢迎star和fork;

另可关注另外两个项目 

[Feature](https://github.com/runnchild/Feature)：超简洁，超实用MVVM开发框架。

[BuildConfig](https://github.com/runnchild/BuildConfig)：项目build脚本配置插件

