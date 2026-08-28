package com.example.sample.expo.brownfield.host

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import com.example.sample.expo.brownfield.reposearchkit.BrownfieldActivity
import com.example.sample.expo.brownfield.reposearchkit.RepoSearchBridge
import com.example.sample.expo.brownfield.reposearchkit.RepoSearchCommand
import com.example.sample.expo.brownfield.reposearchkit.RepoSearchEvent
import com.example.sample.expo.brownfield.reposearchkit.ReactNativeHostManager
import com.example.sample.expo.brownfield.reposearchkit.ReactNativeViewFactory
import com.example.sample.expo.brownfield.reposearchkit.setUpNativeBackHandling

/**
 * Hosts the React Native screen.
 *
 * `showReactNativeFragment()` would be the one-liner version, but it takes no
 * launch options — so the three steps it performs are spelled out here in order
 * to hand `keyword` to React Native as an initial prop.
 */
class RepoSearchActivity : BrownfieldActivity() {
  /** Receives the results through the lambda form of the bridge. */
  private val bridge =
    RepoSearchBridge(
      onEvent = { event ->
        val text =
          when (event) {
            is RepoSearchEvent.Succeeded ->
              "${event.keyword}: ${event.repositories.size} 件受信"
            is RepoSearchEvent.Failed -> "${event.keyword}: 取得失敗"
          }
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
      }
    )

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val keyword = intent.getStringExtra(EXTRA_KEYWORD) ?: DEFAULT_KEYWORD

    ReactNativeHostManager.shared.initialize(application)
    val reactNativeView =
      ReactNativeViewFactory.createFrameLayout(
        this,
        this,
        ROOT_COMPONENT,
        bundleOf(EXTRA_KEYWORD to keyword),
      )
    setContentView(withKeywordBar(reactNativeView))
    setUpNativeBackHandling()

    bridge.start()
  }

  override fun onDestroy() {
    bridge.stop()
    super.onDestroy()
  }

  /**
   * Puts a row of keywords above the React Native view.
   *
   * `initialProps` is fixed once the screen exists, so changing the keyword
   * from here goes over the message channel instead.
   */
  private fun withKeywordBar(reactNativeView: android.view.View): ViewGroup {
    val caption =
      TextView(this).apply {
        text = getString(R.string.send_to_react_native)
        setPadding(32, 24, 32, 0)
      }

    val bar =
      LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        PRESET_KEYWORDS.forEach { preset ->
          addView(
            Button(this@RepoSearchActivity).apply {
              text = preset
              setOnClickListener { bridge.send(RepoSearchCommand.SetKeyword(preset)) }
            }
          )
        }
      }

    return LinearLayout(this).apply {
      orientation = LinearLayout.VERTICAL
      // The activity is edge to edge, so the bar would sit under the status bar.
      fitsSystemWindows = true
      addView(
        caption,
        LinearLayout.LayoutParams(
          LinearLayout.LayoutParams.MATCH_PARENT,
          LinearLayout.LayoutParams.WRAP_CONTENT,
        ),
      )
      addView(
        bar,
        LinearLayout.LayoutParams(
          LinearLayout.LayoutParams.MATCH_PARENT,
          LinearLayout.LayoutParams.WRAP_CONTENT,
        ),
      )
      addView(
        reactNativeView,
        LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f),
      )
    }
  }

  companion object {
    /** Keywords the host app can push into the screen while it is open. */
    private val PRESET_KEYWORDS = listOf("expo", "swift", "kotlin")

    const val EXTRA_KEYWORD = "keyword"
    const val DEFAULT_KEYWORD = "expo"
    private const val ROOT_COMPONENT = "main"

    fun createIntent(context: Context, keyword: String): Intent =
      Intent(context, RepoSearchActivity::class.java).putExtra(EXTRA_KEYWORD, keyword)
  }
}
