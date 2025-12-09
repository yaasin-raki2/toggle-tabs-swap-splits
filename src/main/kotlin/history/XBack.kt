package history

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware

class XBack : AnAction(), DumbAware {
    override fun actionPerformed(e: AnActionEvent) {
        val history = XManager.getCurrentWindowHistory(e.project)
        history?.back()
    }

    override fun update(e: AnActionEvent) {
        val history = XManager.getCurrentWindowHistory(e.project)
        e.presentation.isEnabled = history != null
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
}
