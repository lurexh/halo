package dev.lurexh.halo.ui.render

import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

fun perspective(fovDegrees: Float, aspect: Float, near: Float, far: Float): FloatArray {
    val f = 1f / tan(Math.toRadians(fovDegrees.toDouble()).toFloat() * 0.5f)
    return floatArrayOf(
        f / aspect, 0f, 0f, 0f,
        0f, f, 0f, 0f,
        0f, 0f, (far + near) / (near - far), -1f,
        0f, 0f, (2f * far * near) / (near - far), 0f,
    )
}

fun lookAt(eyeX: Float, eyeY: Float, eyeZ: Float): FloatArray {
    val eyeZLen = sqrt(eyeX * eyeX + eyeY * eyeY + eyeZ * eyeZ)
    val zx = eyeX / eyeZLen
    val zy = eyeY / eyeZLen
    val zz = eyeZ / eyeZLen

    val upX = 0f
    val upY = 1f
    val upZ = 0f

    var xx = upY * zz - upZ * zy
    var xy = upZ * zx - upX * zz
    var xz = upX * zy - upY * zx
    val xLen = sqrt(xx * xx + xy * xy + xz * xz)
    xx /= xLen
    xy /= xLen
    xz /= xLen

    val yx = zy * xz - zz * xy
    val yy = zz * xx - zx * xz
    val yz = zx * xy - zy * xx

    return floatArrayOf(
        xx, yx, zx, 0f,
        xy, yy, zy, 0f,
        xz, yz, zz, 0f,
        -(xx * eyeX + xy * eyeY + xz * eyeZ),
        -(yx * eyeX + yy * eyeY + yz * eyeZ),
        -(zx * eyeX + zy * eyeY + zz * eyeZ),
        1f,
    )
}

fun identity(): FloatArray = floatArrayOf(
    1f, 0f, 0f, 0f,
    0f, 1f, 0f, 0f,
    0f, 0f, 1f, 0f,
    0f, 0f, 0f, 1f,
)

fun scale(uniform: Float): FloatArray = floatArrayOf(
    uniform, 0f, 0f, 0f,
    0f, uniform, 0f, 0f,
    0f, 0f, uniform, 0f,
    0f, 0f, 0f, 1f,
)

fun rotationX(angle: Float): FloatArray {
    val c = cos(angle)
    val s = sin(angle)
    return floatArrayOf(
        1f, 0f, 0f, 0f,
        0f, c, s, 0f,
        0f, -s, c, 0f,
        0f, 0f, 0f, 1f,
    )
}

fun rotationY(angle: Float): FloatArray {
    val c = cos(angle)
    val s = sin(angle)
    return floatArrayOf(
        c, 0f, -s, 0f,
        0f, 1f, 0f, 0f,
        s, 0f, c, 0f,
        0f, 0f, 0f, 1f,
    )
}

fun multiply(a: FloatArray, b: FloatArray): FloatArray {
    val result = FloatArray(16)
    for (column in 0..3) {
        for (row in 0..3) {
            var sum = 0f
            for (k in 0..3) {
                sum += a[k * 4 + row] * b[column * 4 + k]
            }
            result[column * 4 + row] = sum
        }
    }
    return result
}

fun latLonToVec(latDegrees: Float, lonDegrees: Float, radius: Float = 1f): FloatArray {
    val lat = Math.toRadians(latDegrees.toDouble())
    val lon = Math.toRadians(lonDegrees.toDouble())
    val cosLat = cos(lat).toFloat()
    return floatArrayOf(
        cosLat * sin(lon).toFloat() * radius,
        sin(lat).toFloat() * radius,
        cosLat * cos(lon).toFloat() * radius,
    )
}

fun slerp(a: FloatArray, b: FloatArray, t: Float): FloatArray {
    var dot = a[0] * b[0] + a[1] * b[1] + a[2] * b[2]
    dot = dot.coerceIn(-1f, 1f)
    val theta = acos(dot)
    if (theta < 0.0001f) {
        return floatArrayOf(
            a[0] + (b[0] - a[0]) * t,
            a[1] + (b[1] - a[1]) * t,
            a[2] + (b[2] - a[2]) * t,
        ).also { normalizeInPlace(it) }
    }
    val sinTheta = sin(theta)
    val wa = sin((1f - t) * theta) / sinTheta
    val wb = sin(t * theta) / sinTheta
    return floatArrayOf(
        a[0] * wa + b[0] * wb,
        a[1] * wa + b[1] * wb,
        a[2] * wa + b[2] * wb,
    ).also { normalizeInPlace(it) }
}

fun normalizeInPlace(vector: FloatArray) {
    val length = sqrt(vector[0] * vector[0] + vector[1] * vector[1] + vector[2] * vector[2])
    if (length <= 0.0001f) return
    vector[0] /= length
    vector[1] /= length
    vector[2] /= length
}

fun transformPoint(matrix: FloatArray, point: FloatArray): FloatArray {
    val x = point[0]
    val y = point[1]
    val z = point[2]
    return floatArrayOf(
        matrix[0] * x + matrix[4] * y + matrix[8] * z + matrix[12],
        matrix[1] * x + matrix[5] * y + matrix[9] * z + matrix[13],
        matrix[2] * x + matrix[6] * y + matrix[10] * z + matrix[14],
        matrix[3] * x + matrix[7] * y + matrix[11] * z + matrix[15],
    )
}
