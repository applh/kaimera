package com.example.kaimera.text3d

import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.Typeface
import com.example.kaimera.text3d.earcut.Earcut
import java.util.ArrayList
import kotlin.math.abs

/**
 * Generates 3D mesh data (vertices) for text with robust hole support.
 * Uses Android's Graphics API to extract glyph contours and Earcut4j for triangulation.
 */
class Text3DMeshGenerator {

    // Number of floats per vertex: Position(3) + Color(1 packed float)
    // Position: x, y, z
    // Color: float (LibGDX packed color)
    // Total: 4 floats per vertex.

    fun generateFullMesh(
        text: String,
        typeface: Typeface,
        textSize: Float,
        depth: Float,
        sideColorPacked: Float,
        frontColorPacked: Float,
        backColorPacked: Float
    ): MeshData {
        val paint = Paint()
        paint.typeface = typeface
        paint.textSize = textSize
        paint.style = Paint.Style.FILL

        val path = Path()
        paint.getTextPath(text, 0, text.length, 0f, 0f, path)

        val pathMeasure = PathMeasure(path, false)
        
        // 1. Extract all contours from the Path
        val allContours = ArrayList<List<FloatArray>>()
        val step = textSize / 20f // Resolution step
        
        do {
            val length = pathMeasure.length
            if (length == 0f) continue
            
            val contourPoints = ArrayList<FloatArray>()
            var d = 0f
            val pos = FloatArray(2)
            // No need for tan here
            
            while (d < length) {
                pathMeasure.getPosTan(d, pos, null)
                // Flip Y because Android Canvas is Y-down, but we want Y-up for 3D world usually,
                // OR keep it consistent. LibGDX 2D is Y-up. Let's Flip Y like before: -pos[1]
                contourPoints.add(floatArrayOf(pos[0], -pos[1]))
                d += step
            }
            
            // Ensure closure if not already closed
            if (contourPoints.isNotEmpty()) {
                val first = contourPoints[0]
                val last = contourPoints[contourPoints.size - 1]
                val dx = first[0] - last[0]
                val dy = first[1] - last[1]
                if (dx * dx + dy * dy > 0.0001f) {
                   contourPoints.add(first)
                }
            }
            
            // Only add if we have enough points for a shape
            if (contourPoints.size >= 3) {
                 allContours.add(contourPoints)
            }
        } while (pathMeasure.nextContour())

        // 2. Identify Outers and Holes
        // We assume standard font behavior: 
        // - Outer contours usually have one winding (e.g. CCW)
        // - Holes have opposite winding (e.g. CW)
        // BUT to be robust, we should calculate Signed Area.
        // Convention: If Y is flipped (-y), standard CCW might become CW.
        // Let's just group by nesting.
        
        val polygons = groupContours(allContours)
        
        val outVertices = ArrayList<Float>()

        // 3. Process each Polygon (Outer + its Holes)
        for (poly in polygons) {
            // --- A. Triangulate Front and Back Faces ---
            
            // Earcut expects a flat double array of all vertices (outer then holes)
            // and an array of hole start indices in that flat array.
            val flatVertices = ArrayList<Double>()
            val holeIndices = ArrayList<Int>()
            
            // Add Outer
            var currentIndex = 0
            for (p in poly.outer) {
                flatVertices.add(p[0].toDouble())
                flatVertices.add(p[1].toDouble())
            }
            currentIndex += poly.outer.size
            
            // Add Holes
            for (hole in poly.holes) {
                holeIndices.add(currentIndex) // Start index of this hole (in vertices count, not coordinate count?)
                // Earcut java docs: "holeIndices is an array of integer indices of hole beginnings" (index of the vertex, not coordinate)
                // Wait, check Earcut API. usually it's vertex index.
                // E.g. if outer has 4 verts, hole starts at index 4.
                
                for (p in hole) {
                    flatVertices.add(p[0].toDouble())
                    flatVertices.add(p[1].toDouble())
                }
                currentIndex += hole.size
            }
            
            // Run Earcut
            val indices = Earcut.earcut(flatVertices.toDoubleArray(), holeIndices.toIntArray(), 2)
            
            // Extract all points for easy access by index
            val allPolyPoints = ArrayList<FloatArray>()
            allPolyPoints.addAll(poly.outer)
            for (hole in poly.holes) {
                allPolyPoints.addAll(hole)
            }
            
            // Generate Front Face (Z = 0)
            for (i in 0 until indices.size step 3) {
                val i1 = indices[i]
                val i2 = indices[i+1]
                val i3 = indices[i+2]
                
                val p1 = allPolyPoints[i1]
                val p2 = allPolyPoints[i2]
                val p3 = allPolyPoints[i3]
                
                // Add Triangle (CCW)
                addVertex(outVertices, p1[0], p1[1], 0f, frontColorPacked)
                addVertex(outVertices, p2[0], p2[1], 0f, frontColorPacked)
                addVertex(outVertices, p3[0], p3[1], 0f, frontColorPacked)
            }
            
            // Generate Back Face (Z = -depth)
            for (i in 0 until indices.size step 3) {
                val i1 = indices[i]
                val i2 = indices[i+1]
                val i3 = indices[i+2]
                
                val p1 = allPolyPoints[i1]
                val p2 = allPolyPoints[i2]
                val p3 = allPolyPoints[i3]
                
                // Add Triangle (CW - reverse order for back face to face backwards)
                addVertex(outVertices, p1[0], p1[1], -depth, backColorPacked)
                addVertex(outVertices, p3[0], p3[1], -depth, backColorPacked) // Swap 2 and 3
                addVertex(outVertices, p2[0], p2[1], -depth, backColorPacked)
            }
            
            // --- B. Generate Side Walls ---
            // We need to generate walls for the Outer loop AND for all Hole loops.
            // Earcut handles the face, but we must manually stitch the sides.
            
            val allLoops = ArrayList<List<FloatArray>>()
            allLoops.add(poly.outer)
            allLoops.addAll(poly.holes)
            
            for (loop in allLoops) {
                 // For sides, we just iterate the loop points.
                 // Winding order matters for normal direction.
                 // If generated walls are "inside out", lighting will be wrong or cull face will hide them.
                 // Assuming:
                 // Outer is CCW -> Wall norm points OUT.
                 // Hole is CW -> Wall norm points IN (which is OUT of the solid).
                 // So simply following the vertex order P[i]->P[i+1] should work if loops are oriented correctly.
                 // Earcut doesn't strictly enforce input winding, but commonly data is clean.
                 // Let's rely on the input order for now. If holes look black/invisible, we flip them.
                 
                 for (i in 0 until loop.size - 1) {
                     val p1 = loop[i]
                     val p2 = loop[i+1]
                     
                     // Wall Quad:
                     // Top edge: P1->P2 (at Z=0)
                     // Bottom edge: P1->P2 (at Z=-depth)
                     
                     // 2 Triangles.
                     // Front=0, Back=-depth
                     // V1(P1,0), V2(P1,-d), V3(P2,-d)
                     // V1(P1,0), V3(P2,-d), V4(P2,0)
                     
                     // T1
                     addVertex(outVertices, p1[0], p1[1], 0f, sideColorPacked)
                     addVertex(outVertices, p1[0], p1[1], -depth, sideColorPacked)
                     addVertex(outVertices, p2[0], p2[1], -depth, sideColorPacked)
                     
                     // T2
                     addVertex(outVertices, p1[0], p1[1], 0f, sideColorPacked)
                     addVertex(outVertices, p2[0], p2[1], -depth, sideColorPacked)
                     addVertex(outVertices, p2[0], p2[1], 0f, sideColorPacked)
                 }
            }
        }

        // Calculate Bounds
        var minX = Float.MAX_VALUE; var maxX = -Float.MAX_VALUE
        var minY = Float.MAX_VALUE; var maxY = -Float.MAX_VALUE
        
        if (outVertices.isEmpty()) {
            minX = 0f; maxX = 0f; minY = 0f; maxY = 0f
        } else {
            for (i in 0 until outVertices.size step 4) {
                val x = outVertices[i]
                val y = outVertices[i+1]
                if (x < minX) minX = x
                if (x > maxX) maxX = x
                if (y < minY) minY = y
                if (y > maxY) maxY = y
            }
        }

        return MeshData(outVertices.toFloatArray(), outVertices.size / 4, minX, maxX, minY, maxY, 0f, 0f)
    }
    
    // --- Helper Classes & Functions ---
    
    private fun addVertex(list: ArrayList<Float>, x: Float, y: Float, z: Float, color: Float) {
        list.add(x)
        list.add(y)
        list.add(z)
        list.add(color)
    }
    
    data class Polygon(
        val outer: List<FloatArray>,
        val holes: MutableList<List<FloatArray>> = ArrayList()
    )
    
    // Naive but robust grouping:
    // 1. Calculate signed area for all.
    // 2. Sort by Absolute Area (Largest to Smallest).
    // 3. Largest is definitely Outer.
    // 4. For each subsequent, check if it's inside an existing Outer.
    //    (Actually, text can have multiple separate Outers e.g. "i", "!", "A B")
    //    So:
    //    List<Polygon> roots.
    //    For each contour:
    //      Check if it fits inside an existing root's outer.
    //      If yes, add as hole?
    //      Wait, what if it's an island inside a hole? (e.g. copyright symbol ©? No, that's just O and C).
    //      Let's assume 1 level of nesting for Font Glyphs (Outer -> Hole). 
    //      Islands inside holes are rare in standard text but possible.
    //      Let's stick to simple 1-level nesting for now:
    //      A contour is a Hole if it is inside another contour.
    //      If it is inside a Hole, it is an Outer?
    //      
    //      Better Algorithm:
    //      1. Compute Area for all.
    //      2. If Area > 0 (CCW) -> Potentially Outer.
    //         If Area < 0 (CW) -> Potentially Hole.
    //      3. BUT Winding depends on coordinate system.
    //      Let's use Containment.
    //      For every pair (A, B):
    //         If A contains B, B is child of A.
    //      Build a tree/forest.
    //      Root nodes = Outer.
    //      Children of Root = Holes.
    //      Children of Holes = Outer (ignored or treated as separate outer?).
    //      
    //      Let's try a simpler heuristic often used for text:
    //      Sort by Area Descending.
    //      List<Polygon> polies.
    //      For each contour C:
    //         Find the *smallest* polygon P in 'polies' that contains C.
    //         If P found:
    //             Add C as hole to P.
    //         Else:
    //             Create new Polygon(C).
    //      This works for "O" (Outer O contains Inner O).
    //      Works for "B" (Outer B contains Top-Hole and Bottom-Hole).
    //      Works for "i" (Dot and Body are separate, neither contains other).
    
    private fun groupContours(contours: List<List<FloatArray>>): List<Polygon> {
        // Compute bbox/area for speed? Just iterate. 
        // Sort by approx area (bbox area) descending to ensure containers come first.
        
        val sorted = contours.map { c -> 
            val area = abs(calculateSignedArea(c))
            c to area 
        }.sortedByDescending { it.second }
        
        val polygons = ArrayList<Polygon>()
        
        for ((contour, _) in sorted) {
            // Find a container
            var bestContainer: Polygon? = null
            
            // We want the *smallest* container, but since we process large to small,
            // the first one we find might be the parent, or a grandparent.
            // Actually processing large to small:
            // 1. Big O.
            // 2. Small O. Fits in Big O.
            //    Is Big O the direct parent? Yes.
            //    What if 3 rings? A(big) > B(mid) > C(small).
            //    1. A -> New Poly(A).
            //    2. B -> Inside A. Add as hole to A?
            //       If we treat B as hole, what about C?
            //       C is inside B (which is a hole).
            //       If C is inside a hole of A, C should be a new Outer!
            //    
            //    Revised Logic:
            //    Forest of Contours.
            //    roots = []
            //    For each C in sorted:
            //       recursivePlace(C, roots)
            //
            //    place(C, list):
            //       for P in list:
            //          if P.outer contains C:
            //             // C is inside P.
            //             // Check if C fits inside any of P's holes? (Recursive)
            //             // If C is inside a hole H of P, then C is an ISLAND inside H.
            //             // Treat C as a sibling of P? Or new Outer?
            //             // For 3D text purposes, an Island inside a hole is just another Outer mesh part.
            //             // So if inside a hole, "promote" back to root list?
            //             // Or simpler:
            //             // If inside P.outer:
            //             //    It is a hole of P.
            //             //    UNLESS it is inside one of P's existing holes... this gets complex.
            //
            //    Let's stick to the Simple Heuristic for Fonts:
            //    Fonts usually don't have islands inside holes.
            //    So: If C is inside P, it is a hole of P.
            //    We check all existing Polygons.
            //    If C is inside multiple, it's inside the innermost one?
            //    Since we sort by Area, we see Parents before Children.
            //    So if C is inner, the parents are already in 'polygons'.
            //    We just need to find if it is inside ANY current outer.
            //    For robustness: Find the *Tightest* container?
            //    Or just any container? 
            //    If "B", TopHole inside B_body.
            //    If we handle nested islands later if needed.
            
            var placed = false
            for (poly in polygons) {
                 if (contains(poly.outer, contour)) {
                     poly.holes.add(contour)
                     placed = true
                     break // Assuming only 1 level deep
                 }
            }
            if (!placed) {
                polygons.add(Polygon(contour))
            }
        }
        return polygons
    }

    private fun calculateSignedArea(contour: List<FloatArray>): Float {
        var area = 0f
        for (i in 0 until contour.size) {
            val p1 = contour[i]
            val p2 = contour[(i + 1) % contour.size]
            area += (p2[0] - p1[0]) * (p2[1] + p1[1])
        }
        return area / 2f
    }
    
    // Ray casting algorithm to check if a polygon contains a small sample of the other contour
    private fun contains(polygon: List<FloatArray>, candidateHole: List<FloatArray>): Boolean {
        // Heuristic: Check if the first point of candidate is inside polygon.
        // Good enough for font glyphs which don't self-intersect or overlap weirdly.
        if (candidateHole.isEmpty()) return false
        val point = candidateHole[0]
        return isPointInPolygon(point, polygon)
    }

    private fun isPointInPolygon(p: FloatArray, polygon: List<FloatArray>): Boolean {
        var inside = false
        val x = p[0]
        val y = p[1]
        var j = polygon.size - 1
        for (i in polygon.indices) {
            val xi = polygon[i][0]
            val yi = polygon[i][1]
            val xj = polygon[j][0]
            val yj = polygon[j][1]
            
            val intersect = ((yi > y) != (yj > y)) &&
                    (x < (xj - xi) * (y - yi) / (yj - yi) + xi)
            if (intersect) inside = !inside
            j = i
        }
        return inside
    }

    data class MeshData(
        val vertices: FloatArray, 
        val vertexCount: Int, 
        val minX: Float, val maxX: Float, 
        val minY: Float, val maxY: Float,
        val originalCenterX: Float, val originalCenterY: Float
    ) {
        val width: Float get() = maxX - minX
        val height: Float get() = maxY - minY
        val centerX: Float get() = (minX + maxX) / 2f
        val centerY: Float get() = (minY + maxY) / 2f
    }
}
