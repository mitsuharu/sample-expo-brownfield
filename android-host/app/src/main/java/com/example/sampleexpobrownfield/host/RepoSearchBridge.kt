package com.example.sampleexpobrownfield.host

import android.os.Handler
import android.os.Looper
import expo.modules.brownfield.BrownfieldMessaging

/** One repository as it arrives from the React Native screen. */
data class SearchedRepository(
  val id: Int,
  val fullName: String,
  val stars: Int,
  val language: String?,
)

/** What the React Native screen reports back. Mirrors `src/native/bridge.ts`. */
sealed interface RepoSearchEvent {
  data class Succeeded(val keyword: String, val repositories: List<SearchedRepository>) :
    RepoSearchEvent

  data class Failed(val keyword: String, val message: String) : RepoSearchEvent
}

/** The delegate-style callback. */
fun interface RepoSearchListener {
  fun onRepoSearchEvent(event: RepoSearchEvent)
}

/**
 * Wraps `BrownfieldMessaging` — the raw `Map<String, Any?>` channel exposed by the
 * brownfield library — into typed events, delivered on the main thread through
 * either a listener or a lambda.
 *
 * Listeners are global rather than lifecycle-bound, so registering in `onCreate`
 * and releasing in `onDestroy` keeps messages flowing while the React Native
 * activity is in the foreground.
 */
class RepoSearchBridge(
  private val listener: RepoSearchListener? = null,
  private val onEvent: ((RepoSearchEvent) -> Unit)? = null,
) {
  private val mainHandler = Handler(Looper.getMainLooper())
  private var listenerId: String? = null

  fun start() {
    if (listenerId != null) return
    listenerId = BrownfieldMessaging.addListener { message -> handle(message) }
  }

  fun stop() {
    listenerId?.let { id ->
      BrownfieldMessaging.removeListener(id)
      listenerId = null
    }
  }

  private fun handle(message: Map<String, Any?>) {
    val event = makeEvent(message) ?: return
    mainHandler.post {
      listener?.onRepoSearchEvent(event)
      onEvent?.invoke(event)
    }
  }

  private fun makeEvent(message: Map<String, Any?>): RepoSearchEvent? {
    val type = message["type"] as? String ?: return null
    val keyword = message["keyword"] as? String ?: return null

    return when (type) {
      "searchSucceeded" -> {
        val raw = message["repositories"] as? List<*> ?: emptyList<Any?>()
        RepoSearchEvent.Succeeded(keyword, raw.mapNotNull { it.toSearchedRepository() })
      }
      "searchFailed" ->
        RepoSearchEvent.Failed(keyword, message["message"] as? String ?: "unknown error")
      // Messages this screen doesn't care about.
      else -> null
    }
  }
}

/**
 * JS numbers cross the bridge as boxed floating point values, so they are read
 * as `Number` rather than `Int`.
 */
private fun Any?.toSearchedRepository(): SearchedRepository? {
  val json = this as? Map<*, *> ?: return null
  val id = (json["id"] as? Number)?.toInt() ?: return null
  val fullName = json["fullName"] as? String ?: return null

  return SearchedRepository(
    id = id,
    fullName = fullName,
    stars = (json["stars"] as? Number)?.toInt() ?: 0,
    // The JS side sends "" instead of null (see src/native/bridge.ts).
    language = (json["language"] as? String)?.takeIf { it.isNotEmpty() },
  )
}
