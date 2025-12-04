package com.example.kaimera.sphereqix

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Mesh
import com.badlogic.gdx.graphics.VertexAttribute
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector3
import java.util.ArrayList

class SphereMesh(val radius: Float, subdivisions: Int) {

    private val mesh: Mesh
    private val vertices = ArrayList<Float>()
    private val indices = ArrayList<Short>()
    
    // Game logic data
    data class Face(val v1: Int, val v2: Int, val v3: Int, val center: Vector3, var isCaptured: Boolean = false)
    val faces = ArrayList<Face>()
    private val adjacency = HashMap<Int, ArrayList<Int>>() // Face index -> List of neighbor face indices

    init {
        generateIcosphere(subdivisions)
        buildAdjacency()
        
        // We use non-indexed rendering for per-face coloring (3 vertices per face)
        val maxVertices = faces.size * 3
        
        mesh = Mesh(true, maxVertices, 0,
            VertexAttribute.Position(),
            VertexAttribute.Normal(),
            VertexAttribute.ColorPacked())
        
        updateMeshVertices()
    }

    private fun generateIcosphere(subdivisions: Int) {
        // ... (Existing generation code) ...
        // Note: We need to regenerate this to populate 'faces' list correctly after subdivision
        // For brevity, let's assume the existing code populates 'indices' and 'vertices'
        // We will parse 'indices' to build 'faces'
        
        val t = (1.0f + Math.sqrt(5.0).toFloat()) / 2.0f
        // ... (Same initial vertices and indices setup) ...
        
        val initialVertices = listOf(
            Vector3(-1f, t, 0f).nor(),
            Vector3(1f, t, 0f).nor(),
            Vector3(-1f, -t, 0f).nor(),
            Vector3(1f, -t, 0f).nor(),

            Vector3(0f, -1f, t).nor(),
            Vector3(0f, 1f, t).nor(),
            Vector3(0f, -1f, -t).nor(),
            Vector3(0f, 1f, -t).nor(),

            Vector3(t, 0f, -1f).nor(),
            Vector3(t, 0f, 1f).nor(),
            Vector3(-t, 0f, -1f).nor(),
            Vector3(-t, 0f, 1f).nor()
        )

        var currentVertices = ArrayList(initialVertices)
        var currentIndices = ArrayList<Short>()

        // ... (Same initial indices setup) ...
        // 5 faces around point 0
        currentIndices.add(0); currentIndices.add(11); currentIndices.add(5)
        currentIndices.add(0); currentIndices.add(5); currentIndices.add(1)
        currentIndices.add(0); currentIndices.add(1); currentIndices.add(7)
        currentIndices.add(0); currentIndices.add(7); currentIndices.add(10)
        currentIndices.add(0); currentIndices.add(10); currentIndices.add(11)

        // 5 adjacent faces
        currentIndices.add(1); currentIndices.add(5); currentIndices.add(9)
        currentIndices.add(5); currentIndices.add(11); currentIndices.add(4)
        currentIndices.add(11); currentIndices.add(10); currentIndices.add(2)
        currentIndices.add(10); currentIndices.add(7); currentIndices.add(6)
        currentIndices.add(7); currentIndices.add(1); currentIndices.add(8)

        // 5 faces around point 3
        currentIndices.add(3); currentIndices.add(9); currentIndices.add(4)
        currentIndices.add(3); currentIndices.add(4); currentIndices.add(2)
        currentIndices.add(3); currentIndices.add(2); currentIndices.add(6)
        currentIndices.add(3); currentIndices.add(6); currentIndices.add(8)
        currentIndices.add(3); currentIndices.add(8); currentIndices.add(9)

        // 5 adjacent faces
        currentIndices.add(4); currentIndices.add(9); currentIndices.add(5)
        currentIndices.add(2); currentIndices.add(4); currentIndices.add(11)
        currentIndices.add(6); currentIndices.add(2); currentIndices.add(10)
        currentIndices.add(8); currentIndices.add(6); currentIndices.add(7)
        currentIndices.add(9); currentIndices.add(8); currentIndices.add(1)

        // Subdivide
        for (i in 0 until subdivisions) {
            val nextIndices = ArrayList<Short>()
            val midPointCache = HashMap<Long, Int>()

            for (j in 0 until currentIndices.size step 3) {
                val v1 = currentIndices[j].toInt()
                val v2 = currentIndices[j + 1].toInt()
                val v3 = currentIndices[j + 2].toInt()

                val a = getMidPoint(v1, v2, currentVertices, midPointCache)
                val b = getMidPoint(v2, v3, currentVertices, midPointCache)
                val c = getMidPoint(v3, v1, currentVertices, midPointCache)

                nextIndices.add(v1.toShort()); nextIndices.add(a.toShort()); nextIndices.add(c.toShort())
                nextIndices.add(v2.toShort()); nextIndices.add(b.toShort()); nextIndices.add(a.toShort())
                nextIndices.add(v3.toShort()); nextIndices.add(c.toShort()); nextIndices.add(b.toShort())
                nextIndices.add(a.toShort()); nextIndices.add(b.toShort()); nextIndices.add(c.toShort())
            }
            currentIndices = nextIndices
        }
        
        indices.addAll(currentIndices)
        
        // Build Faces list
        for (i in 0 until indices.size step 3) {
            val v1 = indices[i].toInt()
            val v2 = indices[i+1].toInt()
            val v3 = indices[i+2].toInt()
            
            val p1 = currentVertices[v1]
            val p2 = currentVertices[v2]
            val p3 = currentVertices[v3]
            
            val center = Vector3(p1).add(p2).add(p3).scl(1f/3f).scl(radius)
            faces.add(Face(v1, v2, v3, center))
        }

        // Build final vertex buffer (Position, Normal, Color)
        // We will do this in updateMeshVertices to allow dynamic updates
        
        // Store raw vertices for updates
        rawVertices = currentVertices
    }
    
    private lateinit var rawVertices: ArrayList<Vector3>

    fun updateMeshVertices() {
        vertices.clear()
        val colorCaptured = Color.RED.toFloatBits()

        val colorNeutral = Color.GRAY.toFloatBits()
        
        // We need to duplicate vertices for flat shading/coloring per face?
        // Or just use the face color for all 3 vertices?
        // For flat shading look, we usually duplicate vertices.
        // But our generateIcosphere shares vertices.
        // To support per-face coloring with shared vertices, we'd need a texture or vertex attributes that split.
        // Simplest for v1: Use GL_TRIANGLES with non-shared vertices in the Mesh (unpacking indices).
        
        val unpackedVertices = FloatArray(faces.size * 3 * (3 + 3 + 1)) // Pos(3) + Norm(3) + Col(1)
        var idx = 0
        
        for (face in faces) {
            val color = if (face.isCaptured) colorCaptured else colorNeutral
            
            val vIndices = listOf(face.v1, face.v2, face.v3)
            
            for (vi in vIndices) {
                val v = rawVertices[vi]
                
                // Position
                unpackedVertices[idx++] = v.x * radius
                unpackedVertices[idx++] = v.y * radius
                unpackedVertices[idx++] = v.z * radius
                
                // Normal
                unpackedVertices[idx++] = v.x
                unpackedVertices[idx++] = v.y
                unpackedVertices[idx++] = v.z
                
                // Color
                unpackedVertices[idx++] = color
            }
        }
        
        // Re-create mesh if size changed (shouldn't happen) or just set vertices
        // Note: We are now using non-indexed drawing for simplicity of coloring
        if (mesh.maxVertices < unpackedVertices.size / 7) {
             // Re-allocate if needed (omitted for brevity, assuming fixed size)
        }
        mesh.setVertices(unpackedVertices)
    }
    
    private val shapeRenderer = ShapeRenderer()
    
    fun renderGrid(camera: Camera) {
        shapeRenderer.projectionMatrix = camera.combined
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
        shapeRenderer.color = Color.BLACK
        
        // Draw Latitudes (Circles around Y axis)
        val numLat = 8
        for (i in 1 until numLat) {
            val y = radius * (2f * i / numLat - 1f)
            val r = Math.sqrt((radius * radius - y * y).toDouble()).toFloat()
            // Draw circle at height y with radius r
            // ShapeRenderer doesn't have 3D circle, so we approximate with segments
            val segments = 32
            for (j in 0 until segments) {
                val angle1 = 2f * Math.PI * j / segments
                val angle2 = 2f * Math.PI * (j + 1) / segments
                
                val x1 = r * Math.cos(angle1).toFloat()
                val z1 = r * Math.sin(angle1).toFloat()
                val x2 = r * Math.cos(angle2).toFloat()
                val z2 = r * Math.sin(angle2).toFloat()
                
                shapeRenderer.line(x1, y, z1, x2, y, z2)
            }
        }
        
        // Draw Longitudes (Circles passing through poles)
        val numLon = 12
        for (i in 0 until numLon) {
            val angle = Math.PI * i / numLon // 0 to PI
            // We draw full circles, so 0 to PI covers all
            
            val segments = 32
            for (j in 0 until segments) {
                val theta1 = 2f * Math.PI * j / segments
                val theta2 = 2f * Math.PI * (j + 1) / segments
                
                // Circle in XY plane rotated by angle around Y? No.
                // Longitude is a circle passing through (0,R,0) and (0,-R,0).
                // It's a circle in a plane containing Y axis.
                
                val x1 = radius * Math.sin(theta1).toFloat() * Math.cos(angle).toFloat()
                val y1 = radius * Math.cos(theta1).toFloat()
                val z1 = radius * Math.sin(theta1).toFloat() * Math.sin(angle).toFloat()
                
                val x2 = radius * Math.sin(theta2).toFloat() * Math.cos(angle).toFloat()
                val y2 = radius * Math.cos(theta2).toFloat()
                val z2 = radius * Math.sin(theta2).toFloat() * Math.sin(angle).toFloat()
                
                shapeRenderer.line(x1, y1, z1, x2, y2, z2)
            }
        }
        shapeRenderer.end()
        
        // Draw Poles as tangent circles
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        
        // North Pole (Red circle)
        shapeRenderer.color = Color.RED
        drawPoleCircle(0f, radius, 0f, 0.2f)
        
        // South Pole (Blue circle)
        shapeRenderer.color = Color.BLUE
        drawPoleCircle(0f, -radius, 0f, 0.2f)
        
        shapeRenderer.end()
    }
    
    private fun drawPoleCircle(x: Float, y: Float, z: Float, circleRadius: Float) {
        // Draw a filled circle tangent to the sphere at the pole
        // Use ShapeRenderer's circle method (draws in XY plane by default)
        // We need to draw it in XZ plane for poles on Y axis
        val segments = 32
        
        // Draw circle by approximating with small boxes arranged in a circle
        for (i in 0 until segments) {
            val angle = 2f * Math.PI * i / segments
            val px = x + circleRadius * Math.cos(angle).toFloat()
            val pz = z + circleRadius * Math.sin(angle).toFloat()
            
            shapeRenderer.translate(px, y, pz)
            shapeRenderer.box(-0.02f, -0.02f, -0.02f, 0.04f, 0.04f, 0.04f)
            shapeRenderer.translate(-px, -y, -pz)
        }
    }
    
    private fun buildAdjacency() {
        // Build graph: Face Index -> List of Neighbor Face Indices
        // Two faces are neighbors if they share 2 vertices
        // This is O(N^2) naive, can be optimized. For N=320 (subdiv 2) or 1280 (subdiv 3), it's fast enough.
        
        for (i in 0 until faces.size) {
            adjacency[i] = ArrayList()
        }
        
        for (i in 0 until faces.size) {
            for (j in i + 1 until faces.size) {
                if (isNeighbor(faces[i], faces[j])) {
                    adjacency[i]?.add(j)
                    adjacency[j]?.add(i)
                }
            }
        }
    }
    
    private fun isNeighbor(f1: Face, f2: Face): Boolean {
        var shared = 0
        if (f1.v1 == f2.v1 || f1.v1 == f2.v2 || f1.v1 == f2.v3) shared++
        if (f1.v2 == f2.v1 || f1.v2 == f2.v2 || f1.v2 == f2.v3) shared++
        if (f1.v3 == f2.v1 || f1.v3 == f2.v2 || f1.v3 == f2.v3) shared++
        return shared == 2
    }

    fun checkCapture(linePoints: List<Vector3>) {
        // 1. Identify faces crossed by the line
        val boundaryFaces = HashSet<Int>()
        for (point in linePoints) {
            // Find closest face to this point
            var closestDist = Float.MAX_VALUE
            var closestIdx = -1
            
            for (i in 0 until faces.size) {
                val dist = faces[i].center.dst(point)
                if (dist < closestDist) {
                    closestDist = dist
                    closestIdx = i
                }
            }
            if (closestIdx != -1) {
                boundaryFaces.add(closestIdx)
            }
        }
        
        // 2. Flood fill from a random uncaptured non-boundary face
        val visited = HashSet<Int>()
        val region = ArrayList<Int>()
        
        // Find a start node
        var startNode = -1
        for (i in 0 until faces.size) {
            if (!faces[i].isCaptured && !boundaryFaces.contains(i)) {
                startNode = i
                break
            }
        }
        
        if (startNode == -1) return // No uncaptured faces left?
        
        val queue = ArrayList<Int>()
        queue.add(startNode)
        visited.add(startNode)
        
        while (queue.isNotEmpty()) {
            val current = queue.removeAt(0)
            region.add(current)
            
            val neighbors = adjacency[current] ?: continue
            for (neighbor in neighbors) {
                if (!visited.contains(neighbor) && !faces[neighbor].isCaptured && !boundaryFaces.contains(neighbor)) {
                    visited.add(neighbor)
                    queue.add(neighbor)
                }
            }
        }
        
        // 3. Determine which region is smaller (The filled one or the rest)
        // Total uncaptured faces
        val totalUncaptured = faces.count { !it.isCaptured }
        val regionSize = region.size
        // The "other" region size (excluding boundary)
        val otherSize = totalUncaptured - regionSize - boundaryFaces.size
        
        val capturedIndices = ArrayList<Int>()
        
        if (regionSize < otherSize) {
            // The flood filled region is smaller, capture it
            capturedIndices.addAll(region)
        } else {
            // The other region is smaller, capture it (everything NOT in region and NOT boundary)
             for (i in 0 until faces.size) {
                if (!faces[i].isCaptured && !boundaryFaces.contains(i) && !visited.contains(i)) {
                    capturedIndices.add(i)
                }
            }
        }
        
        // Also capture the boundary
        capturedIndices.addAll(boundaryFaces)
        
        // Apply capture
        for (idx in capturedIndices) {
            faces[idx].isCaptured = true
        }
        
        updateMeshVertices()
    }

    private fun getMidPoint(p1: Int, p2: Int, vertices: ArrayList<Vector3>, cache: HashMap<Long, Int>): Int {
        val smaller = Math.min(p1, p2).toLong()
        val greater = Math.max(p1, p2).toLong()
        val key = (smaller shl 32) + greater

        if (cache.containsKey(key)) {
            return cache[key]!!
        }

        val v1 = vertices[p1]
        val v2 = vertices[p2]
        val mid = Vector3(
            (v1.x + v2.x) / 2f,
            (v1.y + v2.y) / 2f,
            (v1.z + v2.z) / 2f
        ).nor()

        val index = vertices.size
        vertices.add(mid)
        cache[key] = index
        return index
    }

    fun render(shader: ShaderProgram, primitiveType: Int = GL20.GL_TRIANGLES) {
        mesh.render(shader, primitiveType)
    }

    fun dispose() {
        mesh.dispose()
    }
}
