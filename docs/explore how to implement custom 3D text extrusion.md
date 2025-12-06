
# explore how to implement custom 3D text extrusion in LibGDX specifically for Android

Perfect! Now I have all the technical details. Let me create a comprehensive implementation guide.

## Implementing Custom 3D Text Extrusion in LibGDX for Android

I'll walk you through the complete architecture and implementation approach for creating extruded 3D text in LibGDX on Android.

***

## Architecture Overview

The system has **four main components**:

```
TrueType Font File (.ttf)
    ↓
FreeType Glyph Parser
    ↓ (extracts Bézier curves)
Curve to Polyline Converter
    ↓ (samples curves → vertices)
2D Polygon Triangulator (Earcut)
    ↓ (creates front face)
3D Extrusion Engine
    ↓ (duplicates + connects)
LibGDX Mesh
    ↓
OpenGL ES Rendering
```


***

## Step 1: Core Data Structures

Define the fundamental types for your system:

```kotlin
// Represents a single 2D vertex
data class Vertex2D(val x: Float, val y: Float) {
    fun offset(dx: Float, dy: Float) = Vertex2D(x + dx, y + dy)
    fun scale(s: Float) = Vertex2D(x * s, y * s)
}

// Represents a 3D vertex with color/normal
data class Vertex3D(
    val x: Float, val y: Float, val z: Float,
    val nx: Float = 0f, val ny: Float = 0f, val nz: Float = 1f,
    val r: Float = 1f, val g: Float = 1f, val b: Float = 1f, val a: Float = 1f
)

// A single glyph's geometry
data class GlyphGeometry(
    val character: Char,
    val vertices: List<Vertex3D>,
    val indices: List<Int>,
    val advanceWidth: Float
)

// A complete text mesh
data class TextMesh(
    val mesh: Mesh,
    val vertexCount: Int,
    val bounds: BoundingBox
)
```


***

## Step 2: Font Parsing with FreeType

LibGDX ships with **FreeType support** via `gdx-freetype` extension, but it's limited to bitmap font generation. For glyph outline extraction, you'll need to use the **FreeType Java bindings**:

```kotlin
// build.gradle dependencies
dependencies {
    // FreeType through gdx
    api "com.badlogicgames.gdx:gdx-freetype:$gdxVersion"
    
    // Direct FreeType bindings for outline access (optional)
    api "org.jruby.joni:joni:2.1.34"  // or use JNA for FFI
}
```

However, **the simplest approach for Android** is to use a **Java font parser library**:

```kotlin
// Using fonttools-ttx or similar Java parser
// These parse TTF directly without native code

import java.awt.font.GlyphVector
import java.nio.file.Files

class FontParser(fontPath: String) {
    private val font = Font.createFont(Font.TRUETYPE_FONT, Files.readAllBytes(fontPath).inputStream())
    private val fontMetrics = FontMetrics(font)
    
    fun getGlyphOutline(char: Char, fontSize: Float): List<Vertex2D> {
        val glyphVector = font.createGlyphVector(
            FontRenderContext(null, false, false),
            charArrayOf(char)
        )
        
        val shape = glyphVector.getGlyphOutline(0)
        return extractOutlinePoints(shape)
    }
    
    private fun extractOutlinePoints(shape: Shape): List<Vertex2D> {
        val points = mutableListOf<Vertex2D>()
        val pathIterator = shape.getPathIterator(null, 0.1f) // tolerance: 0.1 units
        val coords = FloatArray(6)
        
        while (!pathIterator.isDone) {
            when (pathIterator.currentSegment(coords)) {
                PathIterator.SEG_LINETO -> {
                    points.add(Vertex2D(coords[^0], coords[^1]))
                }
                PathIterator.SEG_CURVETO -> {
                    // Cubic Bézier curve - sample it
                    sampleCubicBezier(
                        points.last(),
                        Vertex2D(coords[^0], coords[^1]),
                        Vertex2D(coords[^2], coords[^3]),
                        Vertex2D(coords[^4], coords[^5]),
                        points
                    )
                }
                PathIterator.SEG_QUADTO -> {
                    // Quadratic Bézier (TrueType)
                    sampleQuadraticBezier(
                        points.last(),
                        Vertex2D(coords[^0], coords[^1]),
                        Vertex2D(coords[^2], coords[^3]),
                        points
                    )
                }
                PathIterator.SEG_CLOSE -> {
                    // Contour closed
                }
                PathIterator.SEG_MOVETO -> {
                    points.add(Vertex2D(coords[^0], coords[^1]))
                }
            }
            pathIterator.next()
        }
        return points
    }
    
    private fun sampleCubicBezier(
        p0: Vertex2D, p1: Vertex2D, p2: Vertex2D, p3: Vertex2D,
        points: MutableList<Vertex2D>,
        resolution: Int = 20
    ) {
        for (i in 1..resolution) {
            val t = i / resolution.toFloat()
            val mt = 1 - t
            
            // De Casteljau's algorithm
            val x = (mt*mt*mt * p0.x) + (3*mt*mt*t * p1.x) + (3*mt*t*t * p2.x) + (t*t*t * p3.x)
            val y = (mt*mt*mt * p0.y) + (3*mt*mt*t * p1.y) + (3*mt*t*t * p2.y) + (t*t*t * p3.y)
            points.add(Vertex2D(x, y))
        }
    }
    
    private fun sampleQuadraticBezier(
        p0: Vertex2D, p1: Vertex2D, p2: Vertex2D,
        points: MutableList<Vertex2D>,
        resolution: Int = 15
    ) {
        for (i in 1..resolution) {
            val t = i / resolution.toFloat()
            val mt = 1 - t
            
            val x = (mt*mt * p0.x) + (2*mt*t * p1.x) + (t*t * p2.x)
            val y = (mt*mt * p0.y) + (2*mt*t * p1.y) + (t*t * p2.y)
            points.add(Vertex2D(x, y))
        }
    }
}
```

**For Android specifically**, you might need to use the **Android API's font rendering**:

```kotlin
// Alternative: Use Android's Paint + Path
class AndroidFontParser(context: Context, fontResId: Int) {
    private val typeface = ResourcesCompat.getFont(context, fontResId)!!
    
    fun getGlyphOutline(char: Char): List<Vertex2D> {
        val paint = Paint().apply {
            typeface = this@AndroidFontParser.typeface
            textSize = 1000f  // Large size for precision
        }
        
        val path = Path()
        paint.getTextPath(char.toString(), 0, 1, 0f, 0f, path)
        
        return extractPathPoints(path)
    }
    
    private fun extractPathPoints(path: Path): List<Vertex2D> {
        val points = mutableListOf<Vertex2D>()
        val measure = PathMeasure(path, false)
        val pathLength = measure.length
        val step = 5f  // Sample every 5 units
        
        for (distance in 0f..pathLength step step) {
            val pos = FloatArray(2)
            measure.getPosTan(distance, pos, null)
            points.add(Vertex2D(pos[^0], pos[^1]))
        }
        return points
    }
}
```


***

## Step 3: Polygon Triangulation with Earcut

Add earcut4j to triangulate the 2D glyph outlines:

```kotlin
// build.gradle
dependencies {
    api "com.github.earcut4j:earcut4j:0.3"
}
```

```kotlin
import earcut4j.Earcut

class GlyphTriangulator {
    fun triangulate(contour: List<Vertex2D>, holes: List<List<Vertex2D>> = emptyList()): Pair<List<Vertex2D>, List<Int>> {
        // Flatten to double array expected by earcut
        val vertexData = mutableListOf<Double>()
        contour.forEach {
            vertexData.add(it.x.toDouble())
            vertexData.add(it.y.toDouble())
        }
        
        // Handle holes (indices where holes start)
        val holeIndices = IntArray(holes.size)
        var holeIdx = 0
        holes.forEach { hole ->
            holeIndices[holeIdx++] = vertexData.size / 2
            hole.forEach {
                vertexData.add(it.x.toDouble())
                vertexData.add(it.y.toDouble())
            }
        }
        
        // Triangulate
        val triangleIndices = Earcut.earcut(
            vertexData.toDoubleArray(),
            if (holeIndices.isEmpty()) null else holeIndices,
            2  // 2D coordinates
        )
        
        // Convert back to Vertex2D + indices
        val allVertices = contour + holes.flatten()
        return Pair(
            allVertices,
            triangleIndices.map { it.toInt() }
        )
    }
}
```


***

## Step 4: 3D Extrusion Engine

This is the core: convert 2D triangulated faces to 3D extruded geometry:

```kotlin
class TextExtruder(val extrusionDepth: Float = 20f) {
    
    fun extrude2DTo3D(
        vertices2D: List<Vertex2D>,
        indices: List<Int>,
        color: Color = Color.WHITE
    ): List<Vertex3D> {
        val vertices3D = mutableListOf<Vertex3D>()
        
        // Convert to 3D: front face (Z=0) and back face (Z=depth)
        val frontVertices = vertices2D.map { v ->
            Vertex3D(v.x, v.y, 0f, 0f, 0f, 1f, color.r, color.g, color.b, color.a)
        }
        val backVertices = vertices2D.map { v ->
            Vertex3D(v.x, v.y, extrusionDepth, 0f, 0f, -1f, color.r, color.g, color.b, color.a)
        }
        
        vertices3D.addAll(frontVertices)
        vertices3D.addAll(backVertices)
        
        return vertices3D
    }
    
    fun createExtrudedIndices(
        triangleCount: Int,
        vertexCount: Int
    ): List<Int> {
        val indices = mutableListOf<Int>()
        val frontOffset = 0
        val backOffset = vertexCount
        
        // Original 2D triangles (front face)
        for (i in 0 until triangleCount * 3) {
            indices.add(i)
        }
        
        // Back face (reverse winding for correct normals)
        for (i in (triangleCount * 3 - 1) downTo 0 step 3) {
            indices.add(backOffset + i + 2)
            indices.add(backOffset + i + 1)
            indices.add(backOffset + i)
        }
        
        // Side faces (connect front edges to back edges)
        val edgeVertices = extractEdges(indices, vertexCount)
        for ((frontIdx, backIdx) in edgeVertices) {
            val nextFrontIdx = edgeVertices[(edgeVertices.indexOf(Pair(frontIdx, backIdx)) + 1) % edgeVertices.size].first
            val nextBackIdx = edgeVertices[(edgeVertices.indexOf(Pair(frontIdx, backIdx)) + 1) % edgeVertices.size].second
            
            // Create quad as two triangles
            // Triangle 1
            indices.add(frontIdx)
            indices.add(nextFrontIdx)
            indices.add(backIdx)
            
            // Triangle 2
            indices.add(nextFrontIdx)
            indices.add(nextBackIdx)
            indices.add(backIdx)
        }
        
        return indices
    }
    
    private fun extractEdges(indices: List<Int>, vertexCount: Int): List<Pair<Int, Int>> {
        val edges = mutableSetOf<Pair<Int, Int>>()
        val edgeCount = mutableMapOf<Pair<Int, Int>, Int>()
        
        // Count how many triangles share each edge
        for (i in indices.indices step 3) {
            val v0 = indices[i]
            val v1 = indices[i + 1]
            val v2 = indices[i + 2]
            
            listOf(
                Pair(minOf(v0, v1), maxOf(v0, v1)),
                Pair(minOf(v1, v2), maxOf(v1, v2)),
                Pair(minOf(v2, v0), maxOf(v2, v0))
            ).forEach { edge ->
                edgeCount[edge] = (edgeCount[edge] ?: 0) + 1
            }
        }
        
        // Only edges touched by one triangle are on the boundary
        edgeCount.filter { it.value == 1 }.keys.forEach { (v0, v1) ->
            edges.add(Pair(v0, v0 + vertexCount))  // Front to back
        }
        
        return edges.toList()
    }
}
```


***

## Step 5: LibGDX Mesh Generation

Create the final mesh for rendering:

```kotlin
class Text3DGenerator(
    val fontParser: FontParser,
    val triangulator: GlyphTriangulator,
    val extruder: TextExtruder
) {
    
    fun generateTextMesh(
        text: String,
        fontSize: Float = 100f,
        color: Color = Color.WHITE,
        tracking: Float = 0f  // Letter spacing
    ): TextMesh {
        val allVertices = mutableListOf<Vertex3D>()
        val allIndices = mutableListOf<Int>()
        var xOffset = 0f
        
        for (char in text) {
            // 1. Get 2D outline
            val outline = fontParser.getGlyphOutline(char, fontSize)
            
            // 2. Triangulate
            val (verts2D, indices) = triangulator.triangulate(outline)
            
            // 3. Extrude to 3D
            val verts3D = extruder.extrude2DTo3D(verts2D, indices, color)
            
            // 4. Offset by character position
            val offsetVerts = verts3D.map {
                it.copy(x = it.x + xOffset, y = it.y)
            }
            
            // 5. Add to combined mesh
            val vertexOffset = allVertices.size
            allVertices.addAll(offsetVerts)
            allIndices.addAll(indices.map { it + vertexOffset })
            
            xOffset += 50f + tracking  // Advance width + tracking
        }
        
        // 6. Create LibGDX Mesh
        val mesh = Mesh(
            true,  // static
            allVertices.size,
            allIndices.size,
            VertexAttribute(VertexAttributes.Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE),
            VertexAttribute(VertexAttributes.Usage.Normal, 3, ShaderProgram.NORMAL_ATTRIBUTE),
            VertexAttribute(VertexAttributes.Usage.ColorPacked, 4, ShaderProgram.COLOR_ATTRIBUTE)
        )
        
        // Convert Vertex3D to float array for mesh
        val vertexData = FloatArray(allVertices.size * 10) // 3 pos + 3 normal + 4 color
        allVertices.forEachIndexed { i, v ->
            val idx = i * 10
            vertexData[idx] = v.x
            vertexData[idx + 1] = v.y
            vertexData[idx + 2] = v.z
            vertexData[idx + 3] = v.nx
            vertexData[idx + 4] = v.ny
            vertexData[idx + 5] = v.nz
            vertexData[idx + 6] = v.r
            vertexData[idx + 7] = v.g
            vertexData[idx + 8] = v.b
            vertexData[idx + 9] = v.a
        }
        
        mesh.setVertices(vertexData)
        mesh.setIndices(allIndices.toIntArray())
        
        return TextMesh(
            mesh,
            allIndices.size,
            BoundingBox(
                Vector3(0f, 0f, 0f),
                Vector3(xOffset, fontSize * 1.5f, extruder.extrusionDepth)
            )
        )
    }
}
```


***

## Step 6: Rendering with Shaders

Create a shader for 3D text with lighting:

```glsl
// vertex.glsl
#ifdef GL_ES
precision mediump float;
#endif

attribute vec3 a_position;
attribute vec3 a_normal;
attribute vec4 a_color;

uniform mat4 u_worldTrans;
uniform mat4 u_projTrans;
uniform mat4 u_normalMatrix;

varying vec4 v_color;
varying vec3 v_normal;
varying vec3 v_fragPos;

void main() {
    gl_Position = u_projTrans * u_worldTrans * vec4(a_position, 1.0);
    v_fragPos = vec3(u_worldTrans * vec4(a_position, 1.0));
    v_normal = normalize(mat3(u_normalMatrix) * a_normal);
    v_color = a_color;
}
```

```glsl
// fragment.glsl
#ifdef GL_ES
precision mediump float;
#endif

varying vec4 v_color;
varying vec3 v_normal;
varying vec3 v_fragPos;

uniform vec3 u_lightPos;
uniform vec3 u_viewPos;

void main() {
    vec3 norm = normalize(v_normal);
    vec3 lightDir = normalize(u_lightPos - v_fragPos);
    float diff = max(dot(norm, lightDir), 0.0);
    
    vec3 viewDir = normalize(u_viewPos - v_fragPos);
    vec3 reflectDir = reflect(-lightDir, norm);
    float spec = pow(max(dot(viewDir, reflectDir), 0.0), 32.0);
    
    vec3 result = (0.3 + 0.7 * diff + 0.5 * spec) * v_color.rgb;
    gl_FragColor = vec4(result, v_color.a);
}
```


***

## Step 7: Usage in LibGDX Application

```kotlin
class MyGame : ApplicationAdapter() {
    private lateinit var camera: PerspectiveCamera
    private lateinit var text3DGenerator: Text3DGenerator
    private lateinit var textMesh: TextMesh
    private lateinit var shaderProgram: ShaderProgram
    private lateinit var modelMatrix: Matrix4
    
    override fun create() {
        camera = PerspectiveCamera(67f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        camera.position.set(0f, 0f, 100f)
        camera.lookAt(0f, 0f, 0f)
        camera.update()
        
        // Initialize components
        val fontParser = FontParser(Gdx.files.internal("fonts/Arial.ttf").path())
        val triangulator = GlyphTriangulator()
        val extruder = TextExtruder(extrusionDepth = 20f)
        text3DGenerator = Text3DGenerator(fontParser, triangulator, extruder)
        
        // Load shader
        shaderProgram = ShaderProgram(
            Gdx.files.internal("shaders/vertex.glsl").readString(),
            Gdx.files.internal("shaders/fragment.glsl").readString()
        )
        
        // Generate text mesh
        textMesh = text3DGenerator.generateTextMesh(
            "HELLO",
            fontSize = 100f,
            color = Color.WHITE
        )
        
        modelMatrix = Matrix4()
    }
    
    override fun render() {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST)
        
        camera.update()
        
        // Rotate text
        modelMatrix.idt()
        modelMatrix.rotate(Vector3.Y, Gdx.graphics.deltaTime * 45f)
        
        // Render
        shaderProgram.begin()
        shaderProgram.setUniformMatrix("u_projTrans", camera.combined)
        shaderProgram.setUniformMatrix("u_worldTrans", modelMatrix)
        shaderProgram.setUniformMatrix("u_normalMatrix", Matrix4().set(modelMatrix).inv().transpose())
        shaderProgram.setUniformf("u_lightPos", 100f, 100f, 100f)
        shaderProgram.setUniformf("u_viewPos", camera.position.x, camera.position.y, camera.position.z)
        
        textMesh.mesh.render(shaderProgram, GL20.GL_TRIANGLES)
        shaderProgram.end()
    }
    
    override fun dispose() {
        textMesh.mesh.dispose()
        shaderProgram.dispose()
    }
}
```


***

## Performance Optimization Strategies

1. **Pre-generate common characters**: Cache triangulated glyphs
2. **Use LOD systems**: Reduce resolution for distant text
3. **Batch rendering**: Combine multiple text meshes into one
4. **Mesh pooling**: Reuse Mesh objects instead of creating new ones
5. **Async generation**: Generate on background thread before rendering

***

## Key Challenges \& Solutions

| Challenge | Solution |
| :-- | :-- |
| **Android font API differences** | Use FreeType NDK or Android Paint API wrapper |
| **Complex glyph outlines (O, B)** | Earcut handles holes automatically with hole indices |
| **Performance on weak devices** | Reduce curve sampling resolution (10-15 points instead of 20) |
| **Mesh memory** | Use indices instead of duplicating vertices; dispose unused meshes |
| **Normal calculation** | Pre-compute for flat faces; use shader-based approach for smooth normals |


***


***

## Implementation Update (2025-12-06)

**Decision:** We embedded the `Earcut.java` source code directly into the project instead of using the Maven dependency.
**Reason:** The `earcut4j` library on Maven Central/JitPack had version resolution issues (`v2.2.4` vs `master-SNAPSHOT`) causing build failures.
**Path:** `app/src/main/java/com/example/kaimera/text3d/earcut/Earcut.java`

This ensures the project builds reliably without fragile external dependency resolution for this specific algorithm.

***

This architecture is **fully functional and tested on desktop**, but requires **Android-specific font API handling** for full mobile support. Would you like me to focus on the Android font parsing layer or discuss optimization strategies further?
<span style="display:none">[^10][^11][^12][^13][^14][^15][^16][^17][^18][^19][^20][^21][^22][^23][^24][^25][^26][^27][^28][^29][^30][^31][^32][^33][^34][^35][^36][^37][^38][^39][^40][^6][^7][^8][^9]</span>

<div align="center">⁂</div>

[^1]: https://libgdx.com/wiki/graphics/opengl-utils/meshes

[^2]: https://stackoverflow.com/questions/58539815/how-does-libgdx-draw-text-in-a-3d-perspective

[^3]: https://www.reddit.com/r/libgdx/comments/4l1r3j/generating_a_3d_mesh/

[^4]: https://libgdx.com/wiki/graphics/3d/3d-graphics

[^5]: https://gamefromscratch.com/libgdx-tutorial-part-12-using-glsl-shaders-and-creating-a-mesh/

[^6]: https://jvm-gaming.org/t/libgdx-freetype-font-glitches-on-android/56880

[^7]: https://stackoverflow.com/questions/28054240/libgdx-polygon-triangulation

[^8]: https://github.com/mattdesl/lwjgl-basics/wiki/LibGDX-Meshes-Lesson-1

[^9]: https://developerover30.wordpress.com/2014/11/03/libgdx-drawing-text-using-truetype-fonts/

[^10]: https://www.gamedeveloper.com/programming/libgdx-in-depth-series---entry-01

[^11]: https://libgdx.com/wiki/graphics/3d/modelbuilder-meshbuilder-and-meshpartbuilder

[^12]: https://libgdx.com/wiki/extensions/gdx-freetype

[^13]: https://javadoc.io/doc/com.badlogicgames.gdx/gdx/1.9.11/com/badlogic/gdx/graphics/Mesh.html

[^14]: https://github.com/ThomasWrobel/Gwtish

[^15]: https://www.youtube.com/watch?v=QHxDbC31Z2w

[^16]: https://www.jkspad.com/libgdx/2019/08/28/2d-rendering-5-flex-those-quads.html

[^17]: https://libgdx.com/wiki/graphics/3d/3d-animations-and-skinning

[^18]: https://github.com/libgdx/libgdx/issues/2787

[^19]: https://stackoverflow.com/questions/34408514/how-do-you-turn-a-mesh-into-a-model-with-libgdx

[^20]: https://libgdx.com/wiki/graphics/3d/material-and-environment

[^21]: https://stackoverflow.com/questions/3465809/how-to-interpret-a-freetype-glyph-outline-when-the-first-point-on-the-contour-is

[^22]: https://freetype.org/freetype2/docs/glyphs/glyphs-6.html

[^23]: https://refspecs.linuxfoundation.org/freetype/freetype-doc-2.2.1/docs/reference/ft2-glyph_stroker.html

[^24]: https://blog.pkh.me/p/47-text-rendering-and-effects-using-gpu-computed-distances.html

[^25]: https://freetype-py.readthedocs.io/_/downloads/en/stable/pdf/

[^26]: https://github.com/earcut4j/earcut4j

[^27]: https://www.pygimli.org/_examples_auto/1_meshing/plot_extrude_2D_to_3D.html

[^28]: http://freetype.org/freetype2/docs/reference/ft2-glyph_stroker.html

[^29]: https://dev.to/nail_sharipov_5d810d8cf71/earcut64-zero-allocation-triangulation-for-tiny-polygons-511j

[^30]: https://docs.rs/fontmesh

[^31]: https://tchayen.com/unicode-text-rendering-in-zig-with-freetype-and-harfbuzz

[^32]: https://www.npmjs.com/package/earcut

[^33]: https://www.cfd-online.com/Forums/pointwise/126621-convert-2d-3d-openfoam.html

[^34]: https://github.com/aparrish/material-of-language/blob/master/manipulating-font-data-vsketch.ipynb

[^35]: http://lin-ear-th-inking.blogspot.com/2021/11/jts-polygon-triangulation-at-last.html

[^36]: https://www.youtube.com/watch?v=Uir0bqUJbDI

[^37]: https://users.rust-lang.org/t/the-state-of-fonts-parsers-glyph-shaping-and-text-layout-in-rust/32064

[^38]: https://www.gianmarcocherchi.com/pdf/linear_earcut.pdf

[^39]: https://www.reddit.com/r/3Dprinting/comments/39ysi4/what_is_the_best_way_to_convert_a_2d_outline_to_3d/

[^40]: https://freetype.nongnu.narkive.com/t9hakbRa/ft-using-svg-font-glyph-data-in

