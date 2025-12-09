package history

import com.intellij.openapi.Disposable
import com.intellij.openapi.command.CommandEvent
import com.intellij.openapi.command.CommandListener
import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.fileEditor.*
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.fileEditor.ex.FileEditorWithProvider
import com.intellij.openapi.fileEditor.impl.EditorWindow
import com.intellij.openapi.fileEditor.impl.IdeDocumentHistoryImpl
import com.intellij.openapi.project.Project
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

@Service(Service.Level.PROJECT)
class XManager(private val project: Project) : Disposable {

    private val histories = ConcurrentHashMap<EditorWindow, XWindowHistory>()
    private val finishingCommandName = AtomicReference<String>()

    init {
        val bus = project.messageBus.connect(this)
        bus.subscribe(
                CommandListener.TOPIC,
                object : CommandListener {
                    override fun beforeCommandFinished(event: CommandEvent) {
                        finishingCommandName.set(event.commandName)
                    }
                }
        )

        val excludedCommands =
                setOf(
                        "Find Next / Move to Next Occurrence",
                        "Find Previous / Move to Previous Occurrence"
                )

        bus.subscribe(
                IdeDocumentHistoryImpl.RecentPlacesListener.TOPIC,
                object : IdeDocumentHistoryImpl.RecentPlacesListener {
                    override fun recentPlaceAdded(
                            commandStartPlace: IdeDocumentHistoryImpl.PlaceInfo,
                            isChanged: Boolean
                    ) {
                        val commandName = finishingCommandName.getAndSet(null)
                        if (commandName != null && commandName in excludedCommands) {
                            return
                        }
                        val window = commandStartPlace.window
                        if (!isChanged && window != null) {
                            cleanObsoleteHistories()
                            getHistory(window)?.addPlace(commandStartPlace)
                        }
                        val commandEndPlace = getCurrentPlaceInfo(project)
                        val endWindow = commandEndPlace?.window
                        if (endWindow != null) {
                            getHistory(endWindow)?.addPlace(commandEndPlace)
                        }
                    }

                    override fun recentPlaceRemoved(
                            changePlace: IdeDocumentHistoryImpl.PlaceInfo,
                            isChanged: Boolean
                    ) {}
                }
        )
    }

    fun getHistory(window: EditorWindow?): XWindowHistory? {
        return window?.let { histories.computeIfAbsent(it) { XWindowHistory(project) } }
    }

    fun copyHistory(src: EditorWindow?, dst: EditorWindow?) {
        if (src != null && dst != null) {
            getHistory(src)?.let { history -> histories[dst] = history.copyForWindow(dst) }
        }
    }

    fun addCurrentPlace(window: EditorWindow?) {
        val history = window?.let { getHistory(it) }
        val place = if (history != null) getPlaceInfo(project, window) else null
        place?.let { history?.addPlace(it) }
    }

    private fun cleanObsoleteHistories() {
        val live = FileEditorManagerEx.getInstanceEx(project).windows
        val obsolete = histories.keys.filter { it !in live }
        obsolete.forEach { histories.remove(it) }
    }

    override fun dispose() {
        histories.clear()
    }

    companion object {
        @JvmStatic
        fun getInstance(project: Project): XManager = project.getService(XManager::class.java)

        @JvmStatic
        fun replaceWindow(
                info: IdeDocumentHistoryImpl.PlaceInfo,
                window: EditorWindow
        ): IdeDocumentHistoryImpl.PlaceInfo {
            return IdeDocumentHistoryImpl.PlaceInfo(
                    info.file,
                    info.navigationState,
                    info.editorTypeId,
                    window,
                    info.caretPosition
            )
        }

        @JvmStatic
        fun getPlaceInfo(
                project: Project,
                window: EditorWindow
        ): IdeDocumentHistoryImpl.PlaceInfo? {
            val selectedEditor = window.selectedComposite
            val editor = selectedEditor?.selectedWithProvider
            return editor?.let { createPlaceInfo(project, it.fileEditor, it.provider) }
        }

        @JvmStatic
        fun getCurrentPlaceInfo(project: Project): IdeDocumentHistoryImpl.PlaceInfo? {
            val editor = getSelectedEditor(project)
            return editor?.let { createPlaceInfo(project, it.fileEditor, it.provider) }
        }

        private fun getSelectedEditor(project: Project): FileEditorWithProvider? {
            val editorManager = FileEditorManagerEx.getInstanceEx(project)
            val file = editorManager.currentFile
            return file?.let { editorManager.getSelectedEditorWithProvider(it) }
        }

        private fun createPlaceInfo(
                project: Project,
                fileEditor: FileEditor,
                fileProvider: FileEditorProvider
        ): IdeDocumentHistoryImpl.PlaceInfo? {
            if (!fileEditor.isValid) {
                return null
            }
            val editorManager = FileEditorManagerEx.getInstanceEx(project)
            val file = fileEditor.file ?: return null
            val state = fileEditor.getState(FileEditorStateLevel.NAVIGATION)
            return IdeDocumentHistoryImpl.PlaceInfo(
                    file,
                    state,
                    fileProvider.editorTypeId,
                    editorManager.currentWindow,
                    getCaretPosition(fileEditor)
            )
        }

        private fun getCaretPosition(fileEditor: FileEditor): RangeMarker? {
            if (fileEditor !is TextEditor) {
                return null
            }
            val editor = fileEditor.editor
            val offset = editor.caretModel.offset
            return editor.document.createRangeMarker(offset, offset)
        }

        @JvmStatic
        fun getCurrentWindowHistory(project: Project?): XWindowHistory? {
            if (project == null) return null
            val currentWindow = FileEditorManagerEx.getInstanceEx(project).currentWindow
            return getInstance(project).getHistory(currentWindow)
        }
    }
}
