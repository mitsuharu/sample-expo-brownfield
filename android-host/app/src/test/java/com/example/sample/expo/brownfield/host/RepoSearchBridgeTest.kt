package com.example.sample.expo.brownfield.host

import com.example.sample.expo.brownfield.reposearchkit.RepoSearchBridge
import com.example.sample.expo.brownfield.reposearchkit.RepoSearchCommand
import com.example.sample.expo.brownfield.reposearchkit.RepoSearchEvent
import com.example.sample.expo.brownfield.reposearchkit.RepoSearchListener
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import android.os.Looper

/**
 * Covers the conversion from the raw messaging payload to typed events —
 * the part of the embedding that is easy to get wrong and hard to see fail.
 */
@RunWith(RobolectricTestRunner::class)
class RepoSearchBridgeTest {
  private fun repository(
    id: Any? = 65750241.0,
    fullName: Any? = "expo/expo",
    stars: Any? = 51842.0,
    language: Any? = "TypeScript",
  ): Map<String, Any?> = mapOf("id" to id, "fullName" to fullName, "stars" to stars, "language" to language)

  private fun succeeded(repositories: List<Any?>): Map<String, Any?> =
    mapOf("type" to "searchSucceeded", "keyword" to "expo", "repositories" to repositories)

  /** Delivers one message and drains the main looper the bridge posts onto. */
  private fun eventFor(message: Map<String, Any?>): RepoSearchEvent? {
    var received: RepoSearchEvent? = null
    val bridge = RepoSearchBridge(onEvent = { received = it })
    bridge.receive(message)
    shadowOf(Looper.getMainLooper()).idle()
    return received
  }

  @Test
  fun `decodes repositories`() {
    val event = eventFor(succeeded(listOf(repository()))) as RepoSearchEvent.Succeeded

    assertEquals("expo", event.keyword)
    assertEquals(1, event.repositories.size)
    assertEquals(65750241, event.repositories[0].id)
    assertEquals("expo/expo", event.repositories[0].fullName)
    assertEquals(51842, event.repositories[0].stars)
    assertEquals("TypeScript", event.repositories[0].language)
  }

  @Test
  fun `accepts integers as well as doubles`() {
    // Numbers cross the bridge as boxed floating point values, but an Int must
    // not break decoding.
    val event = eventFor(succeeded(listOf(repository(id = 7, stars = 3)))) as RepoSearchEvent.Succeeded

    assertEquals(7, event.repositories[0].id)
    assertEquals(3, event.repositories[0].stars)
  }

  @Test
  fun `treats an empty language as missing`() {
    // The JS side sends "" rather than null, which cannot be bridged to Kotlin.
    val event = eventFor(succeeded(listOf(repository(language = "")))) as RepoSearchEvent.Succeeded

    assertNull(event.repositories[0].language)
  }

  @Test
  fun `skips repositories missing required fields`() {
    val event =
      eventFor(
        succeeded(listOf(repository(), repository(id = null), repository(fullName = null), "junk"))
      ) as RepoSearchEvent.Succeeded

    assertEquals(1, event.repositories.size)
  }

  @Test
  fun `defaults missing stars to zero`() {
    val event =
      eventFor(succeeded(listOf(repository().minus("stars")))) as RepoSearchEvent.Succeeded

    assertEquals(0, event.repositories[0].stars)
  }

  @Test
  fun `decodes a failure`() {
    val event =
      eventFor(
        mapOf(
          "type" to "searchFailed",
          "keyword" to "expo",
          "message" to "API rate limit exceeded",
        )
      ) as RepoSearchEvent.Failed

    assertEquals("expo", event.keyword)
    assertEquals("API rate limit exceeded", event.message)
  }

  @Test
  fun `falls back when a failure carries no message`() {
    val event =
      eventFor(mapOf("type" to "searchFailed", "keyword" to "expo")) as RepoSearchEvent.Failed

    assertEquals("unknown error", event.message)
  }

  @Test
  fun `ignores unrelated messages`() {
    assertNull(eventFor(mapOf("type" to "somethingElse", "keyword" to "expo")))
    assertNull(eventFor(mapOf("keyword" to "expo")))
    assertNull(eventFor(mapOf("type" to "searchSucceeded")))
  }

  @Test
  fun `notifies both the listener and the lambda on the main thread`() {
    val seenByListener = mutableListOf<RepoSearchEvent>()
    val seenByLambda = mutableListOf<RepoSearchEvent>()
    val listener = RepoSearchListener { event ->
      assertTrue(Looper.myLooper() == Looper.getMainLooper())
      seenByListener.add(event)
    }

    val bridge = RepoSearchBridge(listener = listener, onEvent = { seenByLambda.add(it) })
    bridge.receive(succeeded(listOf(repository())))
    shadowOf(Looper.getMainLooper()).idle()

    assertEquals(1, seenByListener.size)
    assertEquals(1, seenByLambda.size)
  }

  @Test
  fun `start and stop are idempotent`() {
    val bridge = RepoSearchBridge()
    bridge.stop()
    bridge.start()
    bridge.start()
    bridge.stop()
    bridge.stop()
  }

  @Test
  fun `builds the setKeyword payload`() {
    val payload = RepoSearchBridge.payload(RepoSearchCommand.SetKeyword("swift"))

    assertEquals("setKeyword", payload["type"])
    assertEquals("swift", payload["keyword"])
  }

  @Test
  fun `the setKeyword payload carries no nested null`() {
    // A nested null cannot be converted to a Kotlin type, so the shared wire
    // format never carries one.
    val payload = RepoSearchBridge.payload(RepoSearchCommand.SetKeyword("swift"))

    payload.forEach { (key, value) -> assertNotNull(key, value) }
  }

  @Test
  fun `sending without a react native runtime is harmless`() {
    RepoSearchBridge().send(RepoSearchCommand.SetKeyword("swift"))
  }
}
