package history

import com.intellij.openapi.fileEditor.impl.EditorWindow
import com.intellij.openapi.fileEditor.impl.IdeDocumentHistoryImpl
import com.intellij.openapi.project.Project

class XWindowHistory(private val project: Project) {

    private val history = History<IdeDocumentHistoryImpl.PlaceInfo>()

    @Volatile private var navigationInProgress = false

    @Synchronized
    fun addPlace(place: IdeDocumentHistoryImpl.PlaceInfo) {
        if (navigationInProgress) return

        val current = history.current()
        if (current != null && isSameLocation(current, place)) {
            return
        }

        history.push(place)
    }

    @Synchronized fun canBack(): Boolean = history.canGoBack()

    @Synchronized fun canForward(): Boolean = history.canGoForward()

    @Synchronized
    fun back(): Boolean {
        val target = history.back() ?: return false
        gotoPlace(target)
        return true
    }

    @Synchronized
    fun forward(): Boolean {
        val target = history.forward() ?: return false
        gotoPlace(target)
        return true
    }

    private fun gotoPlace(place: IdeDocumentHistoryImpl.PlaceInfo) {
        try {
            navigationInProgress = true
            if (place.file.isValid) {
                IdeDocumentHistoryImpl.getInstance(project).gotoPlaceInfo(place)
            }
        } finally {
            navigationInProgress = false
        }
    }

    private fun isSameLocation(
            p1: IdeDocumentHistoryImpl.PlaceInfo,
            p2: IdeDocumentHistoryImpl.PlaceInfo
    ): Boolean {
        if (p1.file.path != p2.file.path) return false
        val pos1 = p1.caretPosition
        val pos2 = p2.caretPosition
        return pos1 != null && pos2 != null && pos1.startOffset == pos2.startOffset
    }

    @Synchronized
    fun copyForWindow(window: EditorWindow): XWindowHistory {
        return XWindowHistory(project)
    }
}
