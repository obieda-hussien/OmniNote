package com.omninote.ui.components

import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus

/** Anchors the native floating menu to the selection instead of the entire screen. */
class CustomTextToolbar(private val view: View, private val onFormatRequested: ((String, String) -> Unit)? = null) : TextToolbar {
    private var actionMode: ActionMode? = null
    internal var selectionRect = Rect.Zero
    private var copy: (() -> Unit)? = null
    private var paste: (() -> Unit)? = null
    private var cut: (() -> Unit)? = null
    private var selectAll: (() -> Unit)? = null
    override val status get() = if (actionMode != null) TextToolbarStatus.Shown else TextToolbarStatus.Hidden
    override fun hide() { actionMode?.finish(); actionMode = null }

    override fun showMenu(rect: Rect, onCopyRequested: (() -> Unit)?, onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?, onSelectAllRequested: (() -> Unit)?) {
        selectionRect = rect
        copy = onCopyRequested; paste = onPasteRequested; cut = onCutRequested; selectAll = onSelectAllRequested
        if (actionMode != null) {
            actionMode?.invalidate()
            actionMode?.invalidateContentRect()
            return
        }
        actionMode = view.startActionMode(callback, ActionMode.TYPE_FLOATING)
    }
    internal val callback = object : ActionMode.Callback2() {
        private fun populate(mode: ActionMode, menu: Menu) {
            menu.clear()
            fun action(id: Int, title: String, primary: Boolean, finish: Boolean = true, block: (() -> Unit)?) {
                if (block == null) return
                menu.add(0, id, id, title).apply {
                    setShowAsAction(if (primary) MenuItem.SHOW_AS_ACTION_IF_ROOM else MenuItem.SHOW_AS_ACTION_NEVER)
                    setOnMenuItemClickListener { block(); if (finish) mode.finish(); true }
                }
            }
            action(1, view.context.getString(android.R.string.copy), true, block = copy)
            action(2, view.context.getString(android.R.string.paste), true, block = paste)
            action(3, view.context.getString(android.R.string.cut), true, block = cut)
            action(4, view.context.getString(android.R.string.selectAll), true, false, selectAll)
            onFormatRequested?.let { format ->
                action(10, "Bold", false) { format("**", "**") }
                action(11, "Italic", false) { format("*", "*") }
                action(12, "Code", false) { format("`", "`") }
                action(13, "Highlight", false) { format("==", "==") }
            }
        }
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean { populate(mode, menu); return menu.size() > 0 }
        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean { populate(mode, menu); return true }
        override fun onActionItemClicked(mode: ActionMode, item: MenuItem) = false
        override fun onDestroyActionMode(mode: ActionMode) { actionMode = null }
        override fun onGetContentRect(mode: ActionMode, view: View?, outRect: android.graphics.Rect) {
            outRect.set(selectionRect.left.toInt(), selectionRect.top.toInt(), selectionRect.right.toInt(), selectionRect.bottom.toInt())
        }
    }
}
