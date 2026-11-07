package com.rcmiku.music.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rcmiku.music.R
import com.rcmiku.music.ui.icons.Comment

/**
 * Comment button. Without comments it is a round icon button; with comments it grows into a pill
 * that shows the count next to the icon (see [formatCommentCount]).
 */
@Composable
fun CommentButton(
    count: Int,
    filled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val description = stringResource(R.string.comments)
    if (count <= 0) {
        if (filled) {
            FilledIconButton(onClick = onClick, modifier = modifier) {
                Icon(imageVector = Comment, contentDescription = description)
            }
        } else {
            OutlinedIconButton(onClick = onClick, modifier = modifier) {
                Icon(imageVector = Comment, contentDescription = description)
            }
        }
        return
    }

    val content: @Composable RowScope.() -> Unit = {
        Icon(
            imageVector = Comment,
            contentDescription = description,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = formatCommentCount(count),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
    val buttonModifier = modifier.height(40.dp)
    val padding = PaddingValues(horizontal = 14.dp)
    if (filled) {
        Button(
            onClick = onClick,
            modifier = buttonModifier,
            contentPadding = padding,
            content = content
        )
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = buttonModifier,
            contentPadding = padding,
            content = content
        )
    }
}
