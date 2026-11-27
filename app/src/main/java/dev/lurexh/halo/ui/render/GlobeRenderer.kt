package dev.lurexh.halo.ui.render

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import dev.lurexh.halo.vpn.VpnStatus
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class GlobeRenderer(private val onFailure: () -> Unit) : GLSurfaceView.Renderer {
    @Volatile
    private var scene = GlobeScene()

    private var surface = 0
    private var atmosphere = 0
    private var points = 0
    private var lines = 0
    private var surfaceLocations = SurfaceLocations()
    private var atmosphereLocations = AtmosphereLocations()
    private var pointLocations = PointLocations()
    private var lineLocations = LineLocations()

    private var sphereVbo = 0
    private var sphereIbo = 0
    private var sphereIndexCount = 0
    private var starVbo = 0
    private var starCount = 0
    private var nodeVbo = 0
    private var lineVbo = 0

    private var width = 1
    private var height = 1
    private var projection = FloatArray(16)
    private var lastFrame = 0L
    private var elapsed = 0f
    private var rotationY = 0f
    private var rotationX = -0.12f
    private var velocityY = 0f
    private var velocityX = 0f
    private var zoom = 1f
    private var failed = false
    @Volatile
    private var projected = emptyList<ProjectedNode>()

    fun setScene(value: GlobeScene) {
        scene = value
    }

    fun drag(dx: Float, dy: Float) {
        rotationY += dx * 0.0065f
        rotationX = (rotationX + dy * 0.0045f).coerceIn(-0.85f, 0.85f)
        velocityY = dx * 0.010f
        velocityX = dy * 0.005f
    }

    fun pinch(factor: Float) {
        zoom = (zoom * factor).coerceIn(0.72f, 1.65f)
    }

    fun hitTest(x: Float, y: Float): String? {
        val threshold = 28f * (width / 360f).coerceIn(0.8f, 2.2f)
        return projected
            .asSequence()
            .filter { it.code != "WARP" && it.depth > -0.15f }
            .map { node ->
                val dx = node.x - x
                val dy = node.y - y
                node to (dx * dx + dy * dy)
            }
            .filter { (_, distance) -> distance <= threshold * threshold }
            .minByOrNull { (_, distance) -> distance }
            ?.first
            ?.code
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        try {
            surface = createProgram(GlobeShaders.SURFACE_VERTEX, GlobeShaders.SURFACE_FRAGMENT)
            atmosphere = createProgram(GlobeShaders.SURFACE_VERTEX, GlobeShaders.ATMOSPHERE_FRAGMENT)
            points = createProgram(GlobeShaders.POINT_VERTEX, GlobeShaders.POINT_FRAGMENT)
            lines = createProgram(GlobeShaders.LINE_VERTEX, GlobeShaders.LINE_FRAGMENT)
            surfaceLocations = SurfaceLocations(surface)
            atmosphereLocations = AtmosphereLocations(atmosphere)
            pointLocations = PointLocations(points)
            lineLocations = LineLocations(lines)

            createSphere()
            createStars()
            nodeVbo = createBuffer()
            lineVbo = createBuffer()

            GLES30.glClearColor(0f, 0f, 0f, 0f)
            GLES30.glEnable(GLES30.GL_DEPTH_TEST)
            GLES30.glEnable(GLES30.GL_BLEND)
            GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE)
        } catch (error: Exception) {
            failed = true
            onFailure()
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        this.width = width.coerceAtLeast(1)
        this.height = height.coerceAtLeast(1)
        GLES30.glViewport(0, 0, this.width, this.height)
        projection = perspective(42f, this.width.toFloat() / this.height, 0.1f, 60f)
    }

    override fun onDrawFrame(gl: GL10?) {
        if (failed) return

        val now = System.nanoTime()
        val delta = if (lastFrame == 0L) 0.016f else ((now - lastFrame) / 1_000_000_000f).coerceIn(0f, 0.05f)
        lastFrame = now
        elapsed += delta

        val current = scene
        val speed = when (current.status) {
            VpnStatus.CONNECTING -> 0.72f
            VpnStatus.CONNECTED -> 0.22f
            VpnStatus.ERROR -> 0.05f
            VpnStatus.DISCONNECTED -> 0.11f
        }
        if (!current.reducedMotion || velocityY != 0f || velocityX != 0f) {
            rotationY += speed * delta
            rotationY += velocityY * delta * 60f
            rotationX = (rotationX + velocityX * delta * 60f).coerceIn(-0.85f, 0.85f)
        }
        velocityY *= 0.94f
        velocityX *= 0.90f
        if (kotlin.math.abs(velocityY) < 0.0002f) velocityY = 0f
        if (kotlin.math.abs(velocityX) < 0.0002f) velocityX = 0f

        val model = multiply(rotationX(rotationX), rotationY(rotationY))
        val view = lookAt(0f, 0f, 3.35f / zoom)
        val viewProjection = multiply(projection, view)
        val mvp = multiply(viewProjection, model)
        val scale = (width / 360f).coerceIn(0.8f, 2.2f)

        val accent = when (current.status) {
            VpnStatus.CONNECTED -> floatArrayOf(0.24f, 0.90f, 0.72f)
            VpnStatus.CONNECTING -> floatArrayOf(1f, 0.78f, 0.34f)
            VpnStatus.ERROR -> floatArrayOf(1f, 0.28f, 0.36f)
            VpnStatus.DISCONNECTED -> floatArrayOf(0.22f, 0.90f, 1f)
        }
        val pulse = when (current.status) {
            VpnStatus.CONNECTED -> floatArrayOf(0.24f, 0.86f, 0.59f)
            VpnStatus.CONNECTING -> floatArrayOf(1f, 0.78f, 0.34f)
            VpnStatus.ERROR -> floatArrayOf(1f, 0.28f, 0.36f)
            else -> floatArrayOf(0.43f, 0.29f, 1f)
        }
        val scan = when (current.status) {
            VpnStatus.CONNECTING -> 1f
            VpnStatus.CONNECTED -> 0.22f
            else -> 0f
        }
        val alert = if (current.status == VpnStatus.ERROR) 1f else 0f

        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        drawStars(viewProjection)
        drawSurface(viewProjection, model, accent, pulse, scan, alert)
        drawAtmosphere(viewProjection, model, accent, alert)
        updateProjectedNodes(current, model, mvp)

        val anchor = GlobeNode("WARP", -22f, 24f, true, false)
        val allNodes = current.nodes + anchor
        val arc = current.activeNode?.let { active ->
            val from = latLonToVec(active.lat, active.lon, 1.035f)
            val to = latLonToVec(anchor.lat, anchor.lon, 1.035f)
            (0..48).map { index -> slerp(from, to, index / 48f) }
        }
        drawNodes(viewProjection, model, allNodes, arc, accent, scale)
        if (!arc.isNullOrEmpty()) {
            drawArc(viewProjection, model, arc, pulse)
        }
    }

    fun dispose() {
        if (surface != 0) GLES30.glDeleteProgram(surface)
        if (atmosphere != 0) GLES30.glDeleteProgram(atmosphere)
        if (points != 0) GLES30.glDeleteProgram(points)
        if (lines != 0) GLES30.glDeleteProgram(lines)
        if (sphereVbo != 0) GLES30.glDeleteBuffers(1, intArrayOf(sphereVbo), 0)
        if (sphereIbo != 0) GLES30.glDeleteBuffers(1, intArrayOf(sphereIbo), 0)
        if (starVbo != 0) GLES30.glDeleteBuffers(1, intArrayOf(starVbo), 0)
        if (nodeVbo != 0) GLES30.glDeleteBuffers(1, intArrayOf(nodeVbo), 0)
        if (lineVbo != 0) GLES30.glDeleteBuffers(1, intArrayOf(lineVbo), 0)
    }

    private fun drawSurface(
        viewProjection: FloatArray,
        model: FloatArray,
        accent: FloatArray,
        pulse: FloatArray,
        scan: Float,
        alert: Float,
    ) {
        GLES30.glUseProgram(surface)
        GLES30.glUniformMatrix4fv(surfaceLocations.projection, 1, false, viewProjection, 0)
        GLES30.glUniformMatrix4fv(surfaceLocations.view, 1, false, identity(), 0)
        GLES30.glUniformMatrix4fv(surfaceLocations.model, 1, false, model, 0)
        GLES30.glUniform3f(surfaceLocations.accent, accent[0], accent[1], accent[2])
        GLES30.glUniform3f(surfaceLocations.pulse, pulse[0], pulse[1], pulse[2])
        GLES30.glUniform1f(surfaceLocations.time, elapsed)
        GLES30.glUniform1f(surfaceLocations.scan, scan)
        GLES30.glUniform1f(surfaceLocations.alert, alert)

        bindSphere(surfaceLocations.position, surfaceLocations.uv)
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, sphereIbo)
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, sphereIndexCount, GLES30.GL_UNSIGNED_SHORT, 0)
    }

    private fun drawAtmosphere(
        viewProjection: FloatArray,
        model: FloatArray,
        accent: FloatArray,
        alert: Float,
    ) {
        val atmosphereModel = multiply(model, scale(1.085f))
        GLES30.glUseProgram(atmosphere)
        GLES30.glUniformMatrix4fv(atmosphereLocations.projection, 1, false, viewProjection, 0)
        GLES30.glUniformMatrix4fv(atmosphereLocations.view, 1, false, identity(), 0)
        GLES30.glUniformMatrix4fv(atmosphereLocations.model, 1, false, atmosphereModel, 0)
        GLES30.glUniform3f(atmosphereLocations.accent, accent[0], accent[1], accent[2])
        GLES30.glUniform1f(atmosphereLocations.alert, alert)

        GLES30.glDepthMask(false)
        bindSphere(atmosphereLocations.position, atmosphereLocations.uv)
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, sphereIbo)
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, sphereIndexCount, GLES30.GL_UNSIGNED_SHORT, 0)
        GLES30.glDepthMask(true)
    }

    private fun drawStars(viewProjection: FloatArray) {
        GLES30.glDisable(GLES30.GL_DEPTH_TEST)
        GLES30.glUseProgram(points)
        GLES30.glUniformMatrix4fv(pointLocations.projection, 1, false, viewProjection, 0)
        GLES30.glUniformMatrix4fv(pointLocations.view, 1, false, identity(), 0)
        GLES30.glUniformMatrix4fv(pointLocations.model, 1, false, identity(), 0)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, starVbo)
        enableAttrib(pointLocations.position, 3, 32, 0)
        enableAttrib(pointLocations.color, 4, 32, 12)
        enableAttrib(pointLocations.size, 1, 32, 28)
        GLES30.glDrawArrays(GLES30.GL_POINTS, 0, starCount)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
    }

    private fun drawNodes(
        viewProjection: FloatArray,
        model: FloatArray,
        nodes: List<GlobeNode>,
        arc: List<FloatArray>?,
        accent: FloatArray,
        scale: Float,
    ) {
        val packetCount = if (arc.isNullOrEmpty()) 0 else 4
        val total = nodes.size + packetCount
        val buffer = ensureBuffer(nodeVbo, total * 8)

        nodes.forEach { node ->
            val vector = latLonToVec(node.lat, node.lon, 1.045f)
            val color = when {
                node.code == "WARP" -> floatArrayOf(0.22f, 0.90f, 1f, 0.78f)
                node.active -> floatArrayOf(0.24f, 0.94f, 0.62f, 1f)
                node.available -> floatArrayOf(accent[0], accent[1], accent[2], 0.86f)
                else -> floatArrayOf(0.45f, 0.50f, 0.68f, 0.22f)
            }
            val size = when {
                node.code == "WARP" -> 9f * scale
                node.active -> 13f * scale
                node.available -> 6.5f * scale
                else -> 3.2f * scale
            }
            buffer.put(vector)
            buffer.put(color)
            buffer.put(size)
        }

        if (arc != null) {
            val from = arc.first()
            val to = arc.last()
            repeat(packetCount) { index ->
                val t = frac(elapsed * 0.24f + index / packetCount.toFloat())
                val point = slerp(from, to, t)
                val radius = 1.045f + 0.02f * sin(t * Math.PI.toFloat())
                normalizeInPlace(point)
                buffer.put(point[0] * radius, point[1] * radius, point[2] * radius)
                buffer.put(accent[0], accent[1], accent[2], (1f - t) * 0.9f + 0.1f)
                buffer.put(7f * scale)
            }
        }

        buffer.flip()
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, nodeVbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, total * 8 * 4, buffer.asReadOnlyBuffer(), GLES30.GL_DYNAMIC_DRAW)

        GLES30.glUseProgram(points)
        GLES30.glUniformMatrix4fv(pointLocations.projection, 1, false, viewProjection, 0)
        GLES30.glUniformMatrix4fv(pointLocations.view, 1, false, identity(), 0)
        GLES30.glUniformMatrix4fv(pointLocations.model, 1, false, model, 0)
        GLES30.glDepthMask(false)
        enableAttrib(pointLocations.position, 3, 32, 0)
        enableAttrib(pointLocations.color, 4, 32, 12)
        enableAttrib(pointLocations.size, 1, 32, 28)
        GLES30.glDrawArrays(GLES30.GL_POINTS, 0, total)
        GLES30.glDepthMask(true)
    }

    private fun drawArc(
        viewProjection: FloatArray,
        model: FloatArray,
        arc: List<FloatArray>,
        pulse: FloatArray,
    ) {
        val buffer = ensureBuffer(lineVbo, arc.size * 7)
        arc.forEach { point ->
            buffer.put(point)
            buffer.put(pulse[0], pulse[1], pulse[2], 0.72f)
        }
        buffer.flip()
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, lineVbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, arc.size * 7 * 4, buffer.asReadOnlyBuffer(), GLES30.GL_DYNAMIC_DRAW)

        GLES30.glUseProgram(lines)
        GLES30.glUniformMatrix4fv(lineLocations.projection, 1, false, viewProjection, 0)
        GLES30.glUniformMatrix4fv(lineLocations.view, 1, false, identity(), 0)
        GLES30.glUniformMatrix4fv(lineLocations.model, 1, false, model, 0)
        GLES30.glDepthMask(false)
        enableAttrib(lineLocations.position, 3, 28, 0)
        enableAttrib(lineLocations.color, 4, 28, 12)
        GLES30.glDrawArrays(GLES30.GL_LINE_STRIP, 0, arc.size)
        GLES30.glDepthMask(true)
    }

    private fun updateProjectedNodes(current: GlobeScene, model: FloatArray, mvp: FloatArray) {
        projected = current.nodes.mapNotNull { node ->
            val vector = latLonToVec(node.lat, node.lon, 1.045f)
            val modelPoint = transformPoint(model, vector)
            if (modelPoint[2] < -0.18f) return@mapNotNull null
            val clip = transformPoint(mvp, vector)
            if (clip[3] <= 0f) return@mapNotNull null
            val ndcX = clip[0] / clip[3]
            val ndcY = clip[1] / clip[3]
            if (ndcX < -1.2f || ndcX > 1.2f || ndcY < -1.2f || ndcY > 1.2f) return@mapNotNull null
            ProjectedNode(
                code = node.code,
                x = (ndcX * 0.5f + 0.5f) * width,
                y = (0.5f - ndcY * 0.5f) * height,
                depth = modelPoint[2],
            )
        }
    }

    private fun bindSphere(positionLocation: Int, uvLocation: Int) {
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, sphereVbo)
        enableAttrib(positionLocation, 3, 20, 0)
        enableAttrib(uvLocation, 2, 20, 12)
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, sphereIbo)
    }

    private fun enableAttrib(location: Int, size: Int, stride: Int, offset: Int) {
        if (location < 0) return
        GLES30.glEnableVertexAttribArray(location)
        GLES30.glVertexAttribPointer(location, size, GLES30.GL_FLOAT, false, stride, offset)
    }

    private fun ensureBuffer(bufferId: Int, floats: Int): FloatBuffer {
        val capacity = if (bufferId == nodeVbo) nodeCapacity else lineCapacity
        if (capacity >= floats) {
            cachedBuffer(bufferId).clear()
            return cachedBuffer(bufferId)
        }
        val fresh = ByteBuffer.allocateDirect(floats * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        if (bufferId == nodeVbo) {
            nodeBuffer = fresh
            nodeCapacity = floats
        } else {
            lineBuffer = fresh
            lineCapacity = floats
        }
        return fresh
    }

    private fun cachedBuffer(bufferId: Int): FloatBuffer = if (bufferId == nodeVbo) nodeBuffer else lineBuffer

    private var nodeBuffer: FloatBuffer = ByteBuffer.allocateDirect(64).order(ByteOrder.nativeOrder()).asFloatBuffer()
    private var lineBuffer: FloatBuffer = ByteBuffer.allocateDirect(64).order(ByteOrder.nativeOrder()).asFloatBuffer()
    private var nodeCapacity = 16
    private var lineCapacity = 16

    private fun createSphere() {
        val stacks = 48
        val slices = 96
        val vertices = FloatArray((stacks + 1) * (slices + 1) * 5)
        var index = 0
        for (stack in 0..stacks) {
            val v = stack / stacks.toFloat()
            val theta = v * Math.PI.toFloat()
            val sinTheta = sin(theta)
            val cosTheta = cos(theta)
            for (slice in 0..slices) {
                val u = slice / slices.toFloat()
                val phi = u * 2f * Math.PI.toFloat()
                vertices[index++] = sinTheta * cos(phi)
                vertices[index++] = cosTheta
                vertices[index++] = sinTheta * sin(phi)
                vertices[index++] = u
                vertices[index++] = v
            }
        }

        val indices = ShortArray(stacks * slices * 6)
        var cursor = 0
        for (stack in 0 until stacks) {
            for (slice in 0 until slices) {
                val a = (stack * (slices + 1) + slice).toShort()
                val b = (a + slices + 1).toShort()
                indices[cursor++] = a
                indices[cursor++] = b
                indices[cursor++] = (a + 1).toShort()
                indices[cursor++] = b
                indices[cursor++] = (b + 1).toShort()
                indices[cursor++] = (a + 1).toShort()
            }
        }

        sphereVbo = createBuffer()
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, sphereVbo)
        GLES30.glBufferData(
            GLES30.GL_ARRAY_BUFFER,
            vertices.size * 4,
            ByteBuffer.allocateDirect(vertices.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().put(vertices).also { it.flip() },
            GLES30.GL_STATIC_DRAW,
        )
        sphereIbo = createBuffer()
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, sphereIbo)
        GLES30.glBufferData(
            GLES30.GL_ELEMENT_ARRAY_BUFFER,
            indices.size * 2,
            ByteBuffer.allocateDirect(indices.size * 2).order(ByteOrder.nativeOrder()).asShortBuffer().put(indices).also { it.flip() },
            GLES30.GL_STATIC_DRAW,
        )
        sphereIndexCount = indices.size
    }

    private fun createStars() {
        val count = 520
        val data = FloatArray(count * 8)
        val random = Random(90210L)
        var offset = 0
        repeat(count) {
            val z = random.nextFloat() * 2f - 1f
            val angle = random.nextFloat() * 2f * Math.PI.toFloat()
            val radius = 6f + random.nextFloat() * 9f
            val planar = kotlin.math.sqrt(1f - z * z)
            data[offset++] = kotlin.math.cos(angle) * planar * radius
            data[offset++] = z * radius
            data[offset++] = kotlin.math.sin(angle) * planar * radius
            val alpha = 0.15f + random.nextFloat() * 0.5f
            if (random.nextFloat() > 0.86f) {
                data[offset++] = 1f
                data[offset++] = 0.36f
                data[offset++] = 0.55f
                data[offset++] = alpha
            } else {
                data[offset++] = 0.72f
                data[offset++] = 0.88f
                data[offset++] = 1f
                data[offset++] = alpha
            }
            data[offset++] = 1.5f + random.nextFloat() * 2.5f
        }
        starCount = count
        starVbo = createBuffer()
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, starVbo)
        GLES30.glBufferData(
            GLES30.GL_ARRAY_BUFFER,
            data.size * 4,
            ByteBuffer.allocateDirect(data.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().put(data).also { it.flip() },
            GLES30.GL_STATIC_DRAW,
        )
    }

    private fun createBuffer(): Int {
        val buffers = IntArray(1)
        GLES30.glGenBuffers(1, buffers, 0)
        return buffers[0]
    }

    private fun createProgram(vertexSource: String, fragmentSource: String): Int {
        val vertex = compile(GLES30.GL_VERTEX_SHADER, vertexSource)
        val fragment = compile(GLES30.GL_FRAGMENT_SHADER, fragmentSource)
        val program = GLES30.glCreateProgram()
        GLES30.glAttachShader(program, vertex)
        GLES30.glAttachShader(program, fragment)
        GLES30.glLinkProgram(program)
        val linked = IntArray(1)
        GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, linked, 0)
        GLES30.glDeleteShader(vertex)
        GLES30.glDeleteShader(fragment)
        if (linked[0] == 0) {
            val log = GLES30.glGetProgramInfoLog(program)
            GLES30.glDeleteProgram(program)
            throw IllegalStateException("Program link failed: $log")
        }
        return program
    }

    private fun compile(type: Int, source: String): Int {
        val shader = GLES30.glCreateShader(type)
        GLES30.glShaderSource(shader, source)
        GLES30.glCompileShader(shader)
        val compiled = IntArray(1)
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == 0) {
            val log = GLES30.glGetShaderInfoLog(shader)
            GLES30.glDeleteShader(shader)
            throw IllegalStateException("Shader compile failed: $log")
        }
        return shader
    }

    private fun frac(value: Float): Float = value - kotlin.math.floor(value)

    private class SurfaceLocations(program: Int) {
        val projection = GLES30.glGetUniformLocation(program, "uProjection")
        val view = GLES30.glGetUniformLocation(program, "uView")
        val model = GLES30.glGetUniformLocation(program, "uModel")
        val accent = GLES30.glGetUniformLocation(program, "uAccent")
        val pulse = GLES30.glGetUniformLocation(program, "uPulse")
        val time = GLES30.glGetUniformLocation(program, "uTime")
        val scan = GLES30.glGetUniformLocation(program, "uScan")
        val alert = GLES30.glGetUniformLocation(program, "uAlert")
        val position = GLES30.glGetAttribLocation(program, "aPosition")
        val uv = GLES30.glGetAttribLocation(program, "aUv")
    }

    private class AtmosphereLocations(program: Int) {
        val projection = GLES30.glGetUniformLocation(program, "uProjection")
        val view = GLES30.glGetUniformLocation(program, "uView")
        val model = GLES30.glGetUniformLocation(program, "uModel")
        val accent = GLES30.glGetUniformLocation(program, "uAccent")
        val alert = GLES30.glGetUniformLocation(program, "uAlert")
        val position = GLES30.glGetAttribLocation(program, "aPosition")
        val uv = GLES30.glGetAttribLocation(program, "aUv")
    }

    private class PointLocations(program: Int) {
        val projection = GLES30.glGetUniformLocation(program, "uProjection")
        val view = GLES30.glGetUniformLocation(program, "uView")
        val model = GLES30.glGetUniformLocation(program, "uModel")
        val position = GLES30.glGetAttribLocation(program, "aPosition")
        val color = GLES30.glGetAttribLocation(program, "aColor")
        val size = GLES30.glGetAttribLocation(program, "aSize")
    }

    private class LineLocations(program: Int) {
        val projection = GLES30.glGetUniformLocation(program, "uProjection")
        val view = GLES30.glGetUniformLocation(program, "uView")
        val model = GLES30.glGetUniformLocation(program, "uModel")
        val position = GLES30.glGetAttribLocation(program, "aPosition")
        val color = GLES30.glGetAttribLocation(program, "aColor")
    }
}
