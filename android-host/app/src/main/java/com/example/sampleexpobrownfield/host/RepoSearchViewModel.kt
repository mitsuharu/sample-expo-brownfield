package com.example.sampleexpobrownfield.host

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.sampleexpobrownfield.reposearchkit.RepoSearchBridge
import com.example.sampleexpobrownfield.reposearchkit.RepoSearchEvent
import com.example.sampleexpobrownfield.reposearchkit.RepoSearchListener
import com.example.sampleexpobrownfield.reposearchkit.SearchedRepository

/**
 * Receives search results through the listener form of the bridge.
 *
 * The bridge lives in the ViewModel rather than in the composable so it stays
 * registered while `RepoSearchActivity` is in the foreground — that is when the
 * results actually arrive.
 */
class RepoSearchViewModel : ViewModel(), RepoSearchListener {
  var keyword by mutableStateOf(RepoSearchActivity.DEFAULT_KEYWORD)
  var lastKeyword by mutableStateOf<String?>(null)
    private set

  var repositories by mutableStateOf<List<SearchedRepository>>(emptyList())
    private set

  var errorMessage by mutableStateOf<String?>(null)
    private set

  private val bridge = RepoSearchBridge(listener = this).also { it.start() }

  /** The keyword handed to React Native as an initial prop. */
  val effectiveKeyword: String
    get() = keyword.trim().ifEmpty { RepoSearchActivity.DEFAULT_KEYWORD }

  override fun onRepoSearchEvent(event: RepoSearchEvent) {
    when (event) {
      is RepoSearchEvent.Succeeded -> {
        lastKeyword = event.keyword
        repositories = event.repositories
        errorMessage = null
      }
      is RepoSearchEvent.Failed -> {
        lastKeyword = event.keyword
        repositories = emptyList()
        errorMessage = event.message
      }
    }
  }

  override fun onCleared() {
    bridge.stop()
    super.onCleared()
  }
}
