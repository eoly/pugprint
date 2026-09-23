package com.example.pugprint.ui.draw

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.pugprint.R
import com.example.pugprint.design.components.BigButton
import com.example.pugprint.design.components.ButtonEmphasis
import com.example.pugprint.design.components.ChoiceRow
import com.example.pugprint.design.components.KidScreen
import com.example.pugprint.design.theme.PugSpacing
import com.example.pugprint.imaging.BrushSize
import com.example.pugprint.imaging.DrawPoint
import com.example.pugprint.imaging.LabelShape
import com.example.pugprint.imaging.Stroke
import com.example.pugprint.imaging.StrokeRasterizer
import com.example.pugprint.ui.imaging.drawRoundStickerGuide
import androidx.compose.ui.graphics.drawscope.Stroke as StrokeStyle

@Composable
fun DrawScreen(
    state: DrawUiState,
    actions: DrawActions = DrawActions(),
    modifier: Modifier = Modifier,
) {
    KidScreen(
        modifier = modifier,
        title = stringResource(R.string.draw_title),
        onBack = actions.onBack,
        onHome = actions.onHome,
    ) {
        DrawingCanvas(state, actions)
        Spacer(Modifier.height(PugSpacing.small))
        Text(
            text = stringResource(if (state.drawing.isEmpty) R.string.draw_hint_empty else R.string.draw_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(PugSpacing.small))
        ChoiceRow(
            options = BrushSize.entries,
            selected = state.brush,
            onSelect = actions.onBrush,
            label = { stringResource(brushLabel(it)) },
        )
        Spacer(Modifier.height(PugSpacing.small))
        ChoiceRow(
            options = DrawTool.entries,
            selected = state.tool,
            onSelect = actions.onTool,
            label = { stringResource(toolLabel(it)) },
        )
        Spacer(Modifier.height(PugSpacing.medium))
        Row(horizontalArrangement = Arrangement.spacedBy(PugSpacing.small)) {
            BigButton(
                text = stringResource(R.string.draw_undo),
                onClick = actions.onUndo,
                enabled = state.canUndo,
                emphasis = ButtonEmphasis.Secondary,
                modifier = Modifier.weight(1f),
            )
            BigButton(
                text = stringResource(R.string.draw_clear),
                onClick = actions.onClear,
                enabled = state.canUndo,
                emphasis = ButtonEmphasis.Secondary,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(PugSpacing.small))
        BigButton(text = stringResource(R.string.draw_next), onClick = actions.onNext, enabled = state.canFinish)
    }
}

/**
 * A square white sheet the size of one sticker; strokes are drawn as they will print. On a round
 * roll the corners outside the circle are faded: they will not print.
 */
@Composable
private fun ColumnScope.DrawingCanvas(
    state: DrawUiState,
    actions: DrawActions,
) {
    val round = state.labelShape == LabelShape.CIRCLE
    val description =
        stringResource(if (round) R.string.draw_canvas_round_description else R.string.draw_canvas_description)
    val guideColour = MaterialTheme.colorScheme.outline
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .weight(1f),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier =
                Modifier
                    .padding(horizontal = CANVAS_INSET)
                    .aspectRatio(1f)
                    .background(Color.White)
                    .border(1.dp, MaterialTheme.colorScheme.outline)
                    .semantics { contentDescription = description }
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            actions.onStrokeStart(offset.toPoint(size.width.toFloat()))
                            actions.onStrokeEnd()
                        }
                    }.pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset -> actions.onStrokeStart(offset.toPoint(size.width.toFloat())) },
                            onDragEnd = { actions.onStrokeEnd() },
                            onDragCancel = { actions.onStrokeEnd() },
                        ) { change, _ ->
                            change.consume()
                            actions.onStrokeMove(change.position.toPoint(size.width.toFloat()))
                        }
                    },
        ) {
            state.drawing.strokes.forEach { drawStroke(it) }
            state.current?.let { drawStroke(it) }
            if (round) drawRoundStickerGuide(guideColour)
        }
    }
}

private fun Offset.toPoint(side: Float): DrawPoint = DrawPoint((x / side).coerceIn(0f, 1f), (y / side).coerceIn(0f, 1f))

/** Mirrors [StrokeRasterizer]: round-capped lines, brush width scaled from print dots to canvas pixels. */
private fun DrawScope.drawStroke(stroke: Stroke) {
    val side = size.width
    val width = stroke.size.dots * side / StrokeRasterizer.SIZE
    val colour = if (stroke.erase) Color.White else Color.Black
    val points = stroke.points.map { Offset(it.x * side, it.y * side) }
    if (points.size == 1) {
        drawCircle(colour, radius = width / 2, center = points.first())
        return
    }
    val path =
        Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
        }
    drawPath(path, colour, style = StrokeStyle(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun brushLabel(brush: BrushSize): Int =
    when (brush) {
        BrushSize.THIN -> R.string.draw_brush_thin
        BrushSize.MEDIUM -> R.string.draw_brush_medium
        BrushSize.FAT -> R.string.draw_brush_fat
    }

private fun toolLabel(tool: DrawTool): Int =
    when (tool) {
        DrawTool.PEN -> R.string.draw_tool_pen
        DrawTool.ERASER -> R.string.draw_tool_eraser
    }

private val CANVAS_INSET = 16.dp
