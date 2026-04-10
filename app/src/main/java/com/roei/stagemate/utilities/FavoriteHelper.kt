package com.roei.stagemate.utilities

import android.view.View
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.RecyclerView
import com.roei.stagemate.MyApp
import com.roei.stagemate.R
import com.roei.stagemate.data.models.Event
import com.roei.stagemate.data.repository.DataRepository
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

// Optimistic-update favorite toggle with rollback on failure.
// Used by HomeFragment, SearchFragment, and EventDetailActivity.
// Architecture fix: routes through DataRepository, not FirebaseManager directly.
object FavoriteHelper {

    fun toggle(
        event: Event,
        adapter: RecyclerView.Adapter<*>,
        position: Int,
        rootView: View,
        scope: LifecycleCoroutineScope
    ) {
        val newFavoriteState = !event.isFavorite
        event.isFavorite = newFavoriteState
        if (position in 0 until adapter.itemCount) adapter.notifyItemChanged(position)

        val weakView = WeakReference(rootView)

        DataRepository.toggleFavorite(event.id, newFavoriteState) { success ->
            scope.launch {
                val view = weakView.get() ?: return@launch
                if (!view.isAttachedToWindow) return@launch
                if (success) {
                    MyApp.signalManager.vibrate(100)
                    val message = if (newFavoriteState) {
                        view.context.getString(R.string.added_to_favorites)
                    } else {
                        view.context.getString(R.string.removed_from_favorites)
                    }
                    M3Snackbar.success(view, message)
                } else {
                    event.isFavorite = !newFavoriteState
                    if (position in 0 until adapter.itemCount) adapter.notifyItemChanged(position)
                    M3Snackbar.error(view, view.context.getString(R.string.failed_to_update_favorite))
                }
            }
        }
    }
}
