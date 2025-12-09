package history

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.project.DumbAware

class XSwap : AnAction(), DumbAware {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editorManager = FileEditorManagerEx.getInstanceEx(project)
        val currentWindow = editorManager.currentWindow ?: return

        val windows = editorManager.windows
        if (windows.size < 2) return

        val nextWindow = editorManager.getNextWindow(currentWindow)
        if (nextWindow == null || nextWindow == currentWindow) return

        val currentFile = currentWindow.selectedFile ?: return
        val nextFile = nextWindow.selectedFile ?: return

        if (currentFile == nextFile) return

        val xManager = XManager.getInstance(project)

        xManager.addCurrentPlace(currentWindow)
        xManager.addCurrentPlace(nextWindow)

        editorManager.currentWindow = nextWindow
        OpenFileDescriptor(project, currentFile).navigate(true)

        editorManager.currentWindow = currentWindow
        OpenFileDescriptor(project, nextFile).navigate(true)

        currentWindow.closeFile(currentFile)
        nextWindow.closeFile(nextFile)

        xManager.addCurrentPlace(currentWindow)
        xManager.addCurrentPlace(nextWindow)
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        if (project == null) {
            e.presentation.isEnabled = false
            return
        }
        val editorManager = FileEditorManagerEx.getInstanceEx(project)
        val windows = editorManager.windows
        e.presentation.isEnabled = windows != null && windows.size >= 2
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
}
