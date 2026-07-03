package com.omninote.ui.components

import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus

class CustomTextToolbar(
    private val view: View,
    private val onFormatRequested: ((String, String) -> Unit)? = null
) : TextToolbar {

    private var actionMode: ActionMode? = null
    override val status: TextToolbarStatus
        get() = if (actionMode != null) TextToolbarStatus.Shown else TextToolbarStatus.Hidden

    override fun hide() {
        actionMode?.finish()
        actionMode = null
    }

    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?
    ) {
        actionMode?.finish()
        
        actionMode = view.startActionMode(
            object : ActionMode.Callback2() {
                override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                    // System actions
                    onCopyRequested?.let { copyAction ->
                        menu.add(0, 1, 0, "Copy").setOnMenuItemClickListener { copyAction(); true }
                    }
                    onPasteRequested?.let { pasteAction ->
                        menu.add(0, 2, 1, "Paste").setOnMenuItemClickListener { pasteAction(); true }
                    }
                    onCutRequested?.let { cutAction ->
                        menu.add(0, 3, 2, "Cut").setOnMenuItemClickListener { cutAction(); true }
                    }
                    onSelectAllRequested?.let { selectAllAction ->
                        menu.add(0, 4, 3, "Select All").setOnMenuItemClickListener { selectAllAction(); true }
                    }
                    
                    // Our custom actions
                    if (onFormatRequested != null) {
                        menu.add(0, 10, 4, "Bold").setOnMenuItemClickListener { onFormatRequested.invoke("**", "**"); mode.finish(); true }
                        menu.add(0, 11, 5, "Italic").setOnMenuItemClickListener { onFormatRequested.invoke("*", "*"); mode.finish(); true }
                        menu.add(0, 12, 6, "Code").setOnMenuItemClickListener { onFormatRequested.invoke("`", "`"); mode.finish(); true }
                        menu.add(0, 13, 7, "Quote").setOnMenuItemClickListener { onFormatRequested.invoke("\n> ", ""); mode.finish(); true }
                        menu.add(0, 14, 8, "Highlight").setOnMenuItemClickListener { onFormatRequested.invoke("==", "=="); mode.finish(); true }
                        menu.add(0, 15, 9, "Checkbox").setOnMenuItemClickListener { onFormatRequested.invoke("\n- [ ] ", ""); mode.finish(); true }
                    }
                    return true
                }

                override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean = false

                override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean = false

                override fun onDestroyActionMode(mode: ActionMode) {
                    actionMode = null
                }
            },
            ActionMode.TYPE_FLOATING
        )
    }
}
