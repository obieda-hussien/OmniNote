import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.TextToolbarStatus

class CustomToolbar : TextToolbar {
    override val status = TextToolbarStatus.Hidden
    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?
    ) {}
    override fun hide() {}
}
