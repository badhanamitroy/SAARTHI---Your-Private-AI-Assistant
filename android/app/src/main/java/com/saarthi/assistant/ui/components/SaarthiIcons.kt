package com.saarthi.assistant.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Lightweight, zero-dependency custom vector icons for Saarthi.
 * Keeps APK size tiny and ensures crisp rendering across all screen densities.
 */
object SaarthiIcons {

    val Mic: ImageVector by lazy {
        ImageVector.Builder(
            name = "Mic",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                stroke = null,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(12f, 14f)
                curveToRelative(1.66f, 0f, 3f, -1.34f, 3f, -3f)
                verticalLineTo(5f)
                curveToRelative(0f, -1.66f, -1.34f, -3f, -3f, -3f)
                reflectiveCurveTo(9f, 3.34f, 9f, 5f)
                verticalLineToRelative(6f)
                curveToRelative(0f, 1.66f, 1.34f, 3f, 3f, 3f)
                close()
                moveTo(17f, 11f)
                curveToRelative(0f, 2.76f, -2.24f, 5f, -5f, 5f)
                reflectiveCurveToRelative(-5f, -2.24f, -5f, -5f)
                horizontalLineTo(5f)
                curveToRelative(0f, 3.53f, 2.61f, 6.43f, 6f, 6.92f)
                verticalLineTo(21f)
                horizontalLineToRelative(2f)
                verticalLineToRelative(-3.08f)
                curveToRelative(3.39f, -0.49f, 6f, -3.39f, 6f, -6.92f)
                horizontalLineToRelative(-2f)
                close()
            }
        }.build()
    }

    val Stop: ImageVector by lazy {
        ImageVector.Builder(
            name = "Stop",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                stroke = null
            ) {
                moveTo(6f, 6f)
                horizontalLineToRelative(12f)
                verticalLineToRelative(12f)
                horizontalLineTo(6f)
                close()
            }
        }.build()
    }

    val Send: ImageVector by lazy {
        ImageVector.Builder(
            name = "Send",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                stroke = null
            ) {
                moveTo(2.01f, 21f)
                lineTo(23f, 12f)
                lineTo(2.01f, 3f)
                lineTo(2f, 10f)
                lineToRelative(15f, 2f)
                lineToRelative(-15f, 2f)
                close()
            }
        }.build()
    }

    val Camera: ImageVector by lazy {
        ImageVector.Builder(
            name = "Camera",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                stroke = null
            ) {
                moveTo(9f, 2f)
                lineTo(7.17f, 4f)
                horizontalLineTo(4f)
                curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
                verticalLineToRelative(12f)
                curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
                horizontalLineToRelative(16f)
                curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                verticalLineTo(6f)
                curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
                horizontalLineToRelative(-3.17f)
                lineTo(15f, 2f)
                horizontalLineTo(9f)
                close()
                moveTo(12f, 17f)
                curveToRelative(-2.76f, 0f, -5f, -2.24f, -5f, -5f)
                reflectiveCurveToRelative(2.24f, -5f, 5f, -5f)
                reflectiveCurveToRelative(5f, 2.24f, 5f, 5f)
                reflectiveCurveToRelative(-2.24f, 5f, -5f, 5f)
                close()
                moveTo(12f, 9f)
                curveToRelative(-1.66f, 0f, -3f, 1.34f, -3f, 3f)
                reflectiveCurveToRelative(1.34f, 3f, 3f, 3f)
                reflectiveCurveToRelative(3f, -1.34f, 3f, -3f)
                reflectiveCurveToRelative(-1.34f, -3f, -3f, -3f)
                close()
            }
        }.build()
    }

    val Calculator: ImageVector by lazy {
        ImageVector.Builder(
            name = "Calculator",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                stroke = null
            ) {
                moveTo(19f, 3f)
                horizontalLineTo(5f)
                curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
                verticalLineToRelative(14f)
                curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
                horizontalLineToRelative(14f)
                curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                verticalLineTo(5f)
                curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
                close()
                moveTo(19f, 19f)
                horizontalLineTo(5f)
                verticalLineTo(5f)
                horizontalLineToRelative(14f)
                verticalLineToRelative(14f)
                close()
                moveTo(7f, 7f)
                horizontalLineToRelative(10f)
                verticalLineToRelative(2f)
                horizontalLineTo(7f)
                close()
                moveTo(7f, 11f)
                horizontalLineToRelative(2f)
                verticalLineToRelative(2f)
                horizontalLineTo(7f)
                close()
                moveTo(11f, 11f)
                horizontalLineToRelative(2f)
                verticalLineToRelative(2f)
                horizontalLineToRelative(-2f)
                close()
                moveTo(15f, 11f)
                horizontalLineToRelative(2f)
                verticalLineToRelative(2f)
                horizontalLineToRelative(-2f)
                close()
                moveTo(7f, 15f)
                horizontalLineToRelative(2f)
                verticalLineToRelative(2f)
                horizontalLineTo(7f)
                close()
                moveTo(11f, 15f)
                horizontalLineToRelative(2f)
                verticalLineToRelative(2f)
                horizontalLineToRelative(-2f)
                close()
                moveTo(15f, 15f)
                horizontalLineToRelative(2f)
                verticalLineToRelative(2f)
                horizontalLineToRelative(-2f)
                close()
            }
        }.build()
    }

    val Clock: ImageVector by lazy {
        ImageVector.Builder(
            name = "Clock",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                stroke = null
            ) {
                moveTo(11.99f, 2f)
                curveTo(6.47f, 2f, 2f, 6.48f, 2f, 12f)
                reflectiveCurveToRelative(4.47f, 10f, 9.99f, 10f)
                curveTo(17.52f, 22f, 22f, 17.52f, 22f, 12f)
                reflectiveCurveTo(17.52f, 2f, 11.99f, 2f)
                close()
                moveTo(12f, 20f)
                curveToRelative(-4.42f, 0f, -8f, -3.58f, -8f, -8f)
                reflectiveCurveToRelative(3.58f, -8f, 8f, -8f)
                reflectiveCurveToRelative(8f, 3.58f, 8f, 8f)
                reflectiveCurveToRelative(-3.58f, 8f, -8f, 8f)
                close()
                moveTo(12.5f, 7f)
                horizontalLineTo(11f)
                verticalLineToRelative(6f)
                lineToRelative(5.25f, 3.15f)
                lineToRelative(0.75f, -1.23f)
                lineToRelative(-4.5f, -2.67f)
                close()
            }
        }.build()
    }

    val Calendar: ImageVector by lazy {
        ImageVector.Builder(
            name = "Calendar",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                stroke = null
            ) {
                moveTo(19f, 3f)
                horizontalLineToRelative(-1f)
                verticalLineTo(1f)
                horizontalLineToRelative(-2f)
                verticalLineToRelative(2f)
                horizontalLineTo(8f)
                verticalLineTo(1f)
                horizontalLineTo(6f)
                verticalLineToRelative(2f)
                horizontalLineTo(5f)
                curveToRelative(-1.11f, 0f, -1.99f, 0.9f, -1.99f, 2f)
                lineTo(3f, 19f)
                curveToRelative(0f, 1.1f, 0.89f, 2f, 2f, 2f)
                horizontalLineToRelative(14f)
                curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                verticalLineTo(5f)
                curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
                close()
                moveTo(19f, 19f)
                horizontalLineTo(5f)
                verticalLineTo(8f)
                horizontalLineToRelative(14f)
                verticalLineToRelative(11f)
                close()
            }
        }.build()
    }

    val Settings: ImageVector by lazy {
        ImageVector.Builder(
            name = "Settings",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                stroke = null
            ) {
                moveTo(19.14f, 12.94f)
                curveToRelative(0.04f, -0.3f, 0.06f, -0.61f, 0.06f, -0.94f)
                curveToRelative(0f, -0.32f, -0.02f, -0.64f, -0.07f, -0.94f)
                lineToRelative(2.03f, -1.58f)
                curveToRelative(0.18f, -0.14f, 0.23f, -0.41f, 0.12f, -0.61f)
                lineToRelative(-1.92f, -3.32f)
                curveToRelative(-0.12f, -0.22f, -0.37f, -0.29f, -0.59f, -0.22f)
                lineToRelative(-2.39f, 0.96f)
                curveToRelative(-0.5f, -0.38f, -1.03f, -0.7f, -1.62f, -0.94f)
                lineTo(14.4f, 2.81f)
                curveToRelative(-0.04f, -0.24f, -0.24f, -0.41f, -0.48f, -0.41f)
                horizontalLineToRelative(-3.84f)
                curveToRelative(-0.24f, 0f, -0.43f, 0.17f, -0.47f, 0.41f)
                lineTo(9.25f, 5.35f)
                curveToRelative(-0.59f, 0.24f, -1.13f, 0.56f, -1.62f, 0.94f)
                lineToRelative(-2.39f, -0.96f)
                curveToRelative(-0.22f, -0.08f, -0.47f, 0f, -0.59f, 0.22f)
                lineTo(2.74f, 8.87f)
                curveToRelative(-0.12f, 0.21f, -0.08f, 0.47f, 0.12f, 0.61f)
                lineToRelative(2.03f, 1.58f)
                curveToRelative(-0.05f, 0.3f, -0.09f, 0.63f, -0.09f, 0.94f)
                reflectiveCurveToRelative(0.02f, 0.64f, 0.07f, 0.94f)
                lineToRelative(-2.03f, 1.58f)
                curveToRelative(-0.18f, 0.14f, -0.23f, 0.41f, -0.12f, 0.61f)
                lineToRelative(1.92f, 3.32f)
                curveToRelative(0.12f, 0.22f, 0.37f, 0.29f, 0.59f, 0.22f)
                lineToRelative(2.39f, -0.96f)
                curveToRelative(0.5f, 0.38f, 1.03f, 0.7f, 1.62f, 0.94f)
                lineToRelative(0.36f, 2.54f)
                curveToRelative(0.05f, 0.24f, 0.24f, 0.41f, 0.48f, 0.41f)
                horizontalLineToRelative(3.84f)
                curveToRelative(0.24f, 0f, 0.44f, -0.17f, 0.47f, -0.41f)
                lineToRelative(0.36f, -2.54f)
                curveToRelative(0.59f, -0.24f, 1.13f, -0.56f, 1.62f, -0.94f)
                lineToRelative(2.39f, 0.96f)
                curveToRelative(0.22f, 0.08f, 0.47f, 0f, 0.59f, -0.22f)
                lineToRelative(1.92f, -3.32f)
                curveToRelative(0.12f, -0.22f, 0.07f, -0.47f, -0.12f, -0.61f)
                lineToRelative(-2.01f, -1.58f)
                close()
                moveTo(12f, 15.6f)
                curveToRelative(-1.98f, 0f, -3.6f, -1.62f, -3.6f, -3.6f)
                reflectiveCurveToRelative(1.62f, -3.6f, 3.6f, -3.6f)
                reflectiveCurveToRelative(3.6f, 1.62f, 3.6f, 3.6f)
                reflectiveCurveToRelative(-1.62f, 3.6f, -3.6f, 3.6f)
                close()
            }
        }.build()
    }

    val Home: ImageVector by lazy {
        ImageVector.Builder(
            name = "Home",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                stroke = null
            ) {
                moveTo(10f, 20f)
                verticalLineToRelative(-6f)
                horizontalLineToRelative(4f)
                verticalLineToRelative(6f)
                horizontalLineToRelative(5f)
                verticalLineToRelative(-8f)
                horizontalLineToRelative(3f)
                lineTo(12f, 3f)
                lineTo(2f, 12f)
                horizontalLineToRelative(3f)
                verticalLineToRelative(8f)
                close()
            }
        }.build()
    }

    val Chat: ImageVector by lazy {
        ImageVector.Builder(
            name = "Chat",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                stroke = null
            ) {
                moveTo(20f, 2f)
                horizontalLineTo(4f)
                curveToRelative(-1.1f, 0f, -1.99f, 0.9f, -1.99f, 2f)
                lineTo(2f, 22f)
                lineToRelative(4f, -4f)
                horizontalLineToRelative(14f)
                curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                verticalLineTo(4f)
                curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
                close()
                moveTo(6f, 9f)
                horizontalLineToRelative(12f)
                verticalLineToRelative(2f)
                horizontalLineTo(6f)
                verticalLineTo(9f)
                close()
                moveTo(14f, 14f)
                horizontalLineTo(6f)
                verticalLineToRelative(-2f)
                horizontalLineToRelative(8f)
                verticalLineToRelative(2f)
                close()
                moveTo(18f, 8f)
                horizontalLineTo(6f)
                verticalLineTo(6f)
                horizontalLineToRelative(12f)
                verticalLineToRelative(2f)
                close()
            }
        }.build()
    }

    val Tools: ImageVector by lazy {
        ImageVector.Builder(
            name = "Tools",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                stroke = null
            ) {
                moveTo(22.7f, 19f)
                lineToRelative(-9.1f, -9.1f)
                curveToRelative(0.9f, -2.3f, 0.4f, -5f, -1.5f, -6.9f)
                curveToRelative(-2f, -2f, -5f, -2.4f, -7.4f, -1.3f)
                lineTo(9f, 6f)
                lineTo(6f, 9f)
                lineTo(1.6f, 4.7f)
                curveTo(0.4f, 7.1f, 0.9f, 10.1f, 2.9f, 12.1f)
                curveToRelative(1.9f, 1.9f, 4.6f, 2.4f, 6.9f, 1.5f)
                lineToRelative(9.1f, 9.1f)
                curveToRelative(0.4f, 0.4f, 1f, 0.4f, 1.4f, 0f)
                lineToRelative(2.3f, -2.3f)
                curveToRelative(0.5f, -0.4f, 0.5f, -1.1f, 0.1f, -1.4f)
                close()
            }
        }.build()
    }

    val Trash: ImageVector by lazy {
        ImageVector.Builder(
            name = "Trash",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                stroke = null
            ) {
                moveTo(6f, 19f)
                curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
                horizontalLineToRelative(8f)
                curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                verticalLineTo(7f)
                horizontalLineTo(6f)
                verticalLineToRelative(12f)
                close()
                moveTo(19f, 4f)
                horizontalLineToRelative(-3.5f)
                lineToRelative(-1f, -1f)
                horizontalLineToRelative(-5f)
                lineToRelative(-1f, 1f)
                horizontalLineTo(5f)
                verticalLineToRelative(2f)
                horizontalLineToRelative(14f)
                verticalLineTo(4f)
                close()
            }
        }.build()
    }

    val Shield: ImageVector by lazy {
        ImageVector.Builder(
            name = "Shield",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                stroke = null
            ) {
                moveTo(12f, 1f)
                lineTo(3f, 5f)
                verticalLineToRelative(6f)
                curveToRelative(0f, 5.55f, 3.84f, 10.74f, 9f, 12f)
                curveToRelative(5.16f, -1.26f, 9f, -6.45f, 9f, -12f)
                verticalLineTo(5f)
                lineToRelative(-9f, -4f)
                close()
                moveTo(12f, 11.99f)
                horizontalLineToRelative(7f)
                curveToRelative(-0.53f, 4.12f, -3.28f, 7.79f, -7f, 8.94f)
                verticalLineTo(12f)
                horizontalLineTo(5f)
                verticalLineTo(6.3f)
                lineToRelative(7f, -3.11f)
                verticalLineToRelative(8.8f)
                close()
            }
        }.build()
    }

    val VolumeUp: ImageVector by lazy {
        ImageVector.Builder(
            name = "VolumeUp",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                stroke = null
            ) {
                moveTo(3f, 9f)
                verticalLineToRelative(6f)
                horizontalLineToRelative(4f)
                lineToRelative(5f, 5f)
                verticalLineTo(4f)
                lineTo(7f, 9f)
                horizontalLineTo(3f)
                close()
                moveTo(16.5f, 12f)
                curveToRelative(0f, -1.77f, -1.02f, -3.29f, -2.5f, -4.03f)
                verticalLineToRelative(8.05f)
                curveToRelative(1.48f, -0.73f, 2.5f, -2.25f, 2.5f, -4.02f)
                close()
                moveTo(14f, 3.23f)
                verticalLineToRelative(2.06f)
                curveToRelative(2.89f, 0.86f, 5f, 3.54f, 5f, 6.71f)
                reflectiveCurveToRelative(-2.11f, 5.85f, -5f, 6.71f)
                verticalLineToRelative(2.06f)
                curveToRelative(4.01f, -0.91f, 7f, -4.49f, 7f, -8.77f)
                reflectiveCurveToRelative(-2.99f, -7.86f, -7f, -8.77f)
                close()
            }
        }.build()
    }

    val VolumeOff: ImageVector by lazy {
        ImageVector.Builder(
            name = "VolumeOff",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                stroke = null
            ) {
                moveTo(16.5f, 12f)
                curveToRelative(0f, -1.77f, -1.02f, -3.29f, -2.5f, -4.03f)
                verticalLineToRelative(2.21f)
                lineToRelative(2.45f, 2.45f)
                curveToRelative(0.03f, -0.2f, 0.05f, -0.41f, 0.05f, -0.63f)
                close()
                moveTo(19f, 12f)
                curveToRelative(0f, 0.94f, -0.2f, 1.82f, -0.54f, 2.64f)
                lineToRelative(1.51f, 1.51f)
                curveTo(20.63f, 14.91f, 21f, 13.5f, 21f, 12f)
                curveToRelative(0f, -4.28f, -2.99f, -7.86f, -7f, -8.77f)
                verticalLineToRelative(2.06f)
                curveToRelative(2.89f, 0.86f, 5f, 3.54f, 5f, 6.71f)
                close()
                moveTo(4.27f, 3f)
                lineTo(3f, 4.27f)
                lineToRelative(4.73f, 4.73f)
                horizontalLineTo(3f)
                verticalLineToRelative(6f)
                horizontalLineToRelative(4f)
                lineToRelative(5f, 5f)
                verticalLineToRelative(-6.73f)
                lineToRelative(4.25f, 4.25f)
                curveToRelative(-0.67f, 0.52f, -1.42f, 0.93f, -2.25f, 1.18f)
                verticalLineToRelative(2.06f)
                curveToRelative(1.38f, -0.31f, 2.63f, -0.95f, 3.69f, -1.81f)
                lineTo(19.73f, 21f)
                lineTo(21f, 19.73f)
                lineToRelative(-9f, -9f)
                lineTo(4.27f, 3f)
                close()
                moveTo(12f, 4f)
                lineTo(9.91f, 6.09f)
                lineTo(12f, 8.18f)
                verticalLineTo(4f)
                close()
            }
        }.build()
    }

    val Battery: ImageVector by lazy {
        ImageVector.Builder(
            name = "Battery",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                stroke = null
            ) {
                moveTo(15.67f, 4f)
                horizontalLineTo(14f)
                verticalLineTo(2f)
                horizontalLineToRelative(-4f)
                verticalLineToRelative(2f)
                horizontalLineTo(8.33f)
                curveTo(7.6f, 4f, 7f, 4.6f, 7f, 5.33f)
                verticalLineToRelative(15.33f)
                curveTo(7f, 21.4f, 7.6f, 22f, 8.33f, 22f)
                horizontalLineToRelative(7.33f)
                curveToRelative(0.74f, 0f, 1.34f, -0.6f, 1.34f, -1.33f)
                verticalLineTo(5.33f)
                curveTo(17f, 4.6f, 16.4f, 4f, 15.67f, 4f)
                close()
            }
        }.build()
    }

    val BatteryCharging: ImageVector by lazy {
        ImageVector.Builder(
            name = "BatteryCharging",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                stroke = null
            ) {
                moveTo(15.67f, 4f)
                horizontalLineTo(14f)
                verticalLineTo(2f)
                horizontalLineToRelative(-4f)
                verticalLineToRelative(2f)
                horizontalLineTo(8.33f)
                curveTo(7.6f, 4f, 7f, 4.6f, 7f, 5.33f)
                verticalLineToRelative(15.33f)
                curveTo(7f, 21.4f, 7.6f, 22f, 8.33f, 22f)
                horizontalLineToRelative(7.33f)
                curveToRelative(0.74f, 0f, 1.34f, -0.6f, 1.34f, -1.33f)
                verticalLineTo(5.33f)
                curveTo(17f, 4.6f, 16.4f, 4f, 15.67f, 4f)
                close()
                moveTo(11f, 20f)
                verticalLineToRelative(-5.5f)
                horizontalLineTo(9f)
                lineTo(13f, 7f)
                verticalLineToRelative(5.5f)
                horizontalLineToRelative(2f)
                lineTo(11f, 20f)
                close()
            }
        }.build()
    }

    val FlashlightOn: ImageVector by lazy {
        ImageVector.Builder(
            name = "FlashlightOn",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                stroke = null
            ) {
                moveTo(7f, 2f)
                verticalLineToRelative(11f)
                horizontalLineToRelative(3f)
                verticalLineToRelative(9f)
                lineToRelative(7f, -12f)
                horizontalLineToRelative(-4f)
                lineToRelative(4f, -8f)
                close()
            }
        }.build()
    }

    val FlashlightOff: ImageVector by lazy {
        ImageVector.Builder(
            name = "FlashlightOff",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                stroke = null
            ) {
                moveTo(17f, 10f)
                horizontalLineToRelative(-4f)
                lineToRelative(4f, -8f)
                horizontalLineTo(7f)
                verticalLineToRelative(2.18f)
                lineToRelative(8.46f, 8.46f)
                lineTo(17f, 10f)
                close()
                moveTo(3.27f, 3f)
                lineTo(2f, 4.27f)
                lineToRelative(5f, 5f)
                verticalLineTo(13f)
                horizontalLineToRelative(3f)
                verticalLineToRelative(9f)
                lineToRelative(3.58f, -6.14f)
                lineTo(17.73f, 20f)
                lineTo(19f, 18.73f)
                lineTo(3.27f, 3f)
                close()
            }
        }.build()
    }

    val Wifi: ImageVector by lazy {
        ImageVector.Builder(
            name = "Wifi",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(12f, 4f)
                curveTo(7.31f, 4f, 3.07f, 5.9f, 0f, 8.98f)
                lineTo(12f, 21f)
                lineTo(24f, 8.98f)
                curveTo(20.93f, 5.9f, 16.69f, 4f, 12f, 4f)
                close()
                moveTo(2.92f, 9.07f)
                curveTo(5.51f, 7.08f, 8.67f, 6f, 12f, 6f)
                reflectiveCurveToRelative(6.49f, 1.08f, 9.08f, 3.07f)
                lineTo(12f, 18.25f)
                lineTo(2.92f, 9.07f)
                close()
            }
        }.build()
    }

    val Bluetooth: ImageVector by lazy {
        ImageVector.Builder(
            name = "Bluetooth",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(17.71f, 7.71f)
                lineTo(12f, 2f)
                horizontalLineToRelative(-1f)
                verticalLineToRelative(7.59f)
                lineTo(6.41f, 5f)
                lineTo(5f, 6.41f)
                lineTo(10.59f, 12f)
                lineTo(5f, 17.59f)
                lineTo(6.41f, 19f)
                lineTo(11f, 14.41f)
                verticalLineTo(22f)
                horizontalLineToRelative(1f)
                lineToRelative(5.71f, -5.71f)
                curveToRelative(0.39f, -0.39f, 0.39f, -1.02f, 0f, -1.41f)
                lineTo(13.41f, 12f)
                lineToRelative(4.3f, -4.29f)
                curveToRelative(0.39f, -0.39f, 0.39f, -1.01f, 0f, -1.41f)
                close()
                moveTo(13f, 5.83f)
                lineToRelative(1.88f, 1.88f)
                lineTo(13f, 9.59f)
                verticalLineTo(5.83f)
                close()
                moveTo(14.88f, 16.29f)
                lineTo(13f, 18.17f)
                verticalLineToRelative(-3.76f)
                lineToRelative(1.88f, 1.88f)
                close()
            }
        }.build()
    }

    val MobileData: ImageVector by lazy {
        ImageVector.Builder(
            name = "MobileData",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(16f, 17.01f)
                verticalLineTo(10f)
                horizontalLineToRelative(-2f)
                verticalLineToRelative(7.01f)
                horizontalLineToRelative(-3f)
                lineTo(15f, 21f)
                lineToRelative(4f, -3.99f)
                horizontalLineToRelative(-3f)
                close()
                moveTo(9f, 3f)
                lineTo(5f, 6.99f)
                horizontalLineToRelative(3f)
                verticalLineTo(14f)
                horizontalLineToRelative(2f)
                verticalLineTo(6.99f)
                horizontalLineToRelative(3f)
                lineTo(9f, 3f)
                close()
            }
        }.build()
    }

    val Brightness: ImageVector by lazy {
        ImageVector.Builder(
            name = "Brightness",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(20f, 8.69f)
                verticalLineTo(4f)
                horizontalLineToRelative(-4.69f)
                lineTo(12f, 0.69f)
                lineTo(8.69f, 4f)
                horizontalLineTo(4f)
                verticalLineToRelative(4.69f)
                lineTo(0.69f, 12f)
                lineTo(4f, 15.31f)
                verticalLineTo(20f)
                horizontalLineToRelative(4.69f)
                lineTo(12f, 23.31f)
                lineTo(15.31f, 20f)
                horizontalLineTo(20f)
                verticalLineToRelative(-4.69f)
                lineTo(23.31f, 12f)
                lineTo(20f, 8.69f)
                close()
                moveTo(12f, 18f)
                curveToRelative(-3.31f, 0f, -6f, -2.69f, -6f, -6f)
                reflectiveCurveToRelative(2.69f, -6f, 6f, -6f)
                reflectiveCurveToRelative(6f, 2.69f, 6f, 6f)
                reflectiveCurveToRelative(-2.69f, 6f, -6f, 6f)
                close()
            }
        }.build()
    }

    val Timer: ImageVector by lazy {
        ImageVector.Builder(
            name = "Timer",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(15f, 1f)
                horizontalLineTo(9f)
                verticalLineToRelative(2f)
                horizontalLineToRelative(6f)
                verticalLineTo(1f)
                close()
                moveTo(11f, 14f)
                horizontalLineToRelative(2f)
                verticalLineTo(8f)
                horizontalLineToRelative(-2f)
                verticalLineToRelative(6f)
                close()
                moveTo(19.03f, 7.39f)
                lineToRelative(1.42f, -1.42f)
                curveToRelative(-0.43f, -0.51f, -0.9f, -0.99f, -1.41f, -1.41f)
                lineToRelative(-1.42f, 1.42f)
                curveTo(16.07f, 4.74f, 14.12f, 4f, 12f, 4f)
                curveToRelative(-4.97f, 0f, -9f, 4.03f, -9f, 9f)
                curveToRelative(0f, 4.97f, 4.02f, 9f, 9f, 9f)
                reflectiveCurveToRelative(9f, -4.03f, 9f, -9f)
                curveToRelative(0f, -2.12f, -0.74f, -4.07f, -1.97f, -5.61f)
                close()
                moveTo(12f, 20f)
                curveToRelative(-3.87f, 0f, -7f, -3.13f, -7f, -7f)
                reflectiveCurveToRelative(3.13f, -7f, 7f, -7f)
                reflectiveCurveToRelative(7f, 3.13f, 7f, 7f)
                reflectiveCurveToRelative(-3.13f, 7f, -7f, 7f)
                close()
            }
        }.build()
    }

    val Help: ImageVector by lazy {
        ImageVector.Builder(
            name = "Help",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(12f, 2f)
                curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
                reflectiveCurveToRelative(4.48f, 10f, 10f, 10f)
                reflectiveCurveToRelative(10f, -4.48f, 10f, -10f)
                reflectiveCurveTo(17.52f, 2f, 12f, 2f)
                close()
                moveTo(12f, 19f)
                curveToRelative(-0.55f, 0f, -1f, -0.45f, -1f, -1f)
                reflectiveCurveToRelative(0.45f, -1f, 1f, -1f)
                reflectiveCurveToRelative(1f, 0.45f, 1f, 1f)
                reflectiveCurveToRelative(-0.45f, 1f, -1f, 1f)
                close()
                moveTo(13.07f, 14.25f)
                curveToRelative(-0.46f, 0.28f, -0.7f, 0.61f, -0.7f, 1.05f)
                verticalLineToRelative(0.45f)
                horizontalLineToRelative(-1.5f)
                verticalLineToRelative(-0.51f)
                curveToRelative(0f, -0.88f, 0.46f, -1.53f, 1.25f, -1.95f)
                lineToRelative(0.95f, -0.51f)
                curveToRelative(0.55f, -0.29f, 0.93f, -0.84f, 0.93f, -1.48f)
                curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
                reflectiveCurveToRelative(-2f, 0.9f, -2f, 2f)
                horizontalLineTo(8.5f)
                curveToRelative(0f, -1.93f, 1.57f, -3.5f, 3.5f, -3.5f)
                reflectiveCurveToRelative(3.5f, 1.57f, 3.5f, 3.5f)
                curveToRelative(0f, 0.95f, -0.47f, 1.8f, -1.21f, 2.25f)
                lineToRelative(-1.22f, 0.7f)
                close()
            }
        }.build()
    }

    val Play: ImageVector by lazy {
        ImageVector.Builder(
            name = "Play",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(8f, 5f)
                verticalLineToRelative(14f)
                lineToRelative(11f, -7f)
                close()
            }
        }.build()
    }

    val Pause: ImageVector by lazy {
        ImageVector.Builder(
            name = "Pause",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(6f, 19f)
                horizontalLineToRelative(4f)
                verticalLineTo(5f)
                horizontalLineTo(6f)
                verticalLineToRelative(14f)
                close()
                moveTo(14f, 5f)
                verticalLineToRelative(14f)
                horizontalLineToRelative(4f)
                verticalLineTo(5f)
                horizontalLineToRelative(-4f)
                close()
            }
        }.build()
    }
}
