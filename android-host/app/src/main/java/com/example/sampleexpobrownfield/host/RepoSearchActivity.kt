package com.example.sampleexpobrownfield.host

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.core.os.bundleOf
import com.example.sampleexpobrownfield.reposearchkit.BrownfieldActivity
import com.example.sampleexpobrownfield.reposearchkit.ReactNativeHostManager
import com.example.sampleexpobrownfield.reposearchkit.ReactNativeViewFactory
import com.example.sampleexpobrownfield.reposearchkit.setUpNativeBackHandling

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
    setContentView(
      ReactNativeViewFactory.createFrameLayout(
        this,
        this,
        ROOT_COMPONENT,
        bundleOf(EXTRA_KEYWORD to keyword),
      )
    )
    setUpNativeBackHandling()

    bridge.start()
  }

  override fun onDestroy() {
    bridge.stop()
    super.onDestroy()
  }

  companion object {
    const val EXTRA_KEYWORD = "keyword"
    const val DEFAULT_KEYWORD = "expo"
    private const val ROOT_COMPONENT = "main"

    fun createIntent(context: Context, keyword: String): Intent =
      Intent(context, RepoSearchActivity::class.java).putExtra(EXTRA_KEYWORD, keyword)
  }
}
