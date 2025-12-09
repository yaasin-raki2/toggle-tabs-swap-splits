package history

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.fileEditor.impl.EditorWindow
import com.intellij.openapi.project.DumbAware
import java.util.WeakHashMap

class XToggle : AnAction(), DumbAware {

    companion object {
        private val lastWasForwardMap = WeakHashMap<EditorWindow, Boolean>()
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val history = XManager.getCurrentWindowHistory(project) ?: return
        val currentWindow = FileEditorManagerEx.getInstanceEx(project).currentWindow ?: return

        val lastWasForward = lastWasForwardMap[currentWindow]
        val canBack = history.canBack()
        val canForward = history.canForward()

        when (lastWasForward) {
            null, true -> {
                if (canBack && history.back()) {
                    lastWasForwardMap[currentWindow] = false
                } else if (canForward && history.forward()) {
                    lastWasForwardMap[currentWindow] = true
                }
            }
            false -> {
                if (canForward && history.forward()) {
                    lastWasForwardMap[currentWindow] = true
                } else if (canBack && history.back()) {
                    lastWasForwardMap[currentWindow] = false
                }
            }
        }
    }

    override fun update(e: AnActionEvent) {
        val history = XManager.getCurrentWindowHistory(e.project)
        e.presentation.isEnabled = history != null && (history.canBack() || history.canForward())
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
}
