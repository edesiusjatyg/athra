package id.ac.ub.athra.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Simple, consistent outlines matching the ATHRA reference icon set. */
object AthraIcons {
    val Home = ImageVector.Builder("AthraHome", 24.dp, 24.dp, 24f, 24f).apply {
        path(stroke = SolidColor(Color.White), strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            moveTo(3f, 10f); lineTo(12f, 3f); lineTo(21f, 10f); verticalLineTo(21f)
            horizontalLineTo(15f); verticalLineTo(14f); horizontalLineTo(9f); verticalLineTo(21f)
            horizontalLineTo(3f); close()
        }
    }.build()

    val Workout = ImageVector.Builder("AthraWorkout", 24.dp, 24.dp, 24f, 24f).apply {
        path(stroke = SolidColor(Color.White), strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            moveTo(3f, 9f); verticalLineTo(15f)
            moveTo(6f, 6f); verticalLineTo(18f)
            moveTo(18f, 6f); verticalLineTo(18f)
            moveTo(21f, 9f); verticalLineTo(15f)
            moveTo(6f, 12f); horizontalLineTo(18f)
            moveTo(3f, 12f); horizontalLineTo(6f)
            moveTo(18f, 12f); horizontalLineTo(21f)
        }
    }.build()

    val Progress = ImageVector.Builder("AthraProgress", 24.dp, 24.dp, 24f, 24f).apply {
        path(stroke = SolidColor(Color.White), strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            moveTo(3f, 3f); verticalLineTo(21f); horizontalLineTo(21f)
            moveTo(7f, 15f); lineTo(11f, 10f); lineTo(15f, 13f); lineTo(21f, 5f)
        }
    }.build()

    val Profile = ImageVector.Builder("AthraProfile", 24.dp, 24.dp, 24f, 24f).apply {
        path(stroke = SolidColor(Color.White), strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            moveTo(16f, 6f)
            curveTo(16f, 8.2f, 14.2f, 10f, 12f, 10f)
            curveTo(9.8f, 10f, 8f, 8.2f, 8f, 6f)
            curveTo(8f, 3.8f, 9.8f, 2f, 12f, 2f)
            curveTo(14.2f, 2f, 16f, 3.8f, 16f, 6f); close()
            moveTo(4f, 22f); verticalLineTo(20f)
            curveTo(4f, 15.6f, 7.6f, 12f, 12f, 12f)
            curveTo(16.4f, 12f, 20f, 15.6f, 20f, 20f); verticalLineTo(22f)
        }
    }.build()
}
