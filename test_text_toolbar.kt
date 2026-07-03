import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.geometry.Rect

class MyTextToolbar : TextToolbar {
    override val status = TextToolbarStatus.Hidden
    override fun showMenu(rect: Rect, onCopy: (() -> Unit)?, onPaste: (() -> Unit)?, onCut: (() -> Unit)?, onSelectAll: (() -> Unit)?) {}
    override fun hide() {}
}
