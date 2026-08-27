package com.example.sample.expo.brownfield.host

import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.example.sample.expo.brownfield.reposearchkit.RepoSearchBridge
import com.example.sample.expo.brownfield.reposearchkit.RepoSearchEvent
import com.example.sample.expo.brownfield.reposearchkit.SearchedRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/**
 * Covers the host app's own side of the embedding: what it does with the events
 * the brownfield library hands it, and how the React Native screen is launched.
 */
@RunWith(RobolectricTestRunner::class)
class RepoSearchViewModelTest {
  private val viewModel = RepoSearchViewModel()

  private fun repository() = SearchedRepository(1, "expo/expo", 51842, "TypeScript")

  private fun send(event: RepoSearchEvent) {
    viewModel.onRepoSearchEvent(event)
    shadowOf(Looper.getMainLooper()).idle()
  }

  @Test
  fun `starts empty`() {
    assertNull(viewModel.lastKeyword)
    assertNull(viewModel.errorMessage)
    assertTrue(viewModel.repositories.isEmpty())
  }

  @Test
  fun `keeps results from a successful search`() {
    send(RepoSearchEvent.Succeeded("expo", listOf(repository())))

    assertEquals("expo", viewModel.lastKeyword)
    assertEquals(1, viewModel.repositories.size)
    assertEquals("expo/expo", viewModel.repositories[0].fullName)
    assertNull(viewModel.errorMessage)
  }

  @Test
  fun `a failure clears previous results`() {
    send(RepoSearchEvent.Succeeded("expo", listOf(repository())))

    send(RepoSearchEvent.Failed("expo", "API rate limit exceeded"))

    assertEquals("API rate limit exceeded", viewModel.errorMessage)
    assertTrue(viewModel.repositories.isEmpty())
    assertEquals("expo", viewModel.lastKeyword)
  }

  @Test
  fun `a success clears a previous failure`() {
    send(RepoSearchEvent.Failed("expo", "boom"))

    send(RepoSearchEvent.Succeeded("expo", listOf(repository())))

    assertNull(viewModel.errorMessage)
    assertEquals(1, viewModel.repositories.size)
  }

  @Test
  fun `effective keyword falls back when blank`() {
    viewModel.keyword = "   "
    assertEquals("expo", viewModel.effectiveKeyword)

    viewModel.keyword = ""
    assertEquals("expo", viewModel.effectiveKeyword)
  }

  @Test
  fun `effective keyword is trimmed`() {
    viewModel.keyword = "  expo-brownfield \n"
    assertEquals("expo-brownfield", viewModel.effectiveKeyword)
  }

  @Test
  fun `the react native screen is launched with the keyword`() {
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    val intent = RepoSearchActivity.createIntent(context, "expo-brownfield")

    assertEquals(
      RepoSearchActivity::class.java.name,
      intent.component?.className,
    )
    assertEquals("expo-brownfield", intent.getStringExtra(RepoSearchActivity.EXTRA_KEYWORD))
  }
}
