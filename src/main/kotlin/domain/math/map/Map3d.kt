package domain.math.map

class Map3d {
    var xAxis: Array<Double>
    var yAxis: Array<Double>
    var zAxis: Array<Array<Double>>

    constructor() {
        xAxis = emptyArray()
        yAxis = emptyArray()
        zAxis = emptyArray()
    }

    constructor(xAxis: Array<Double>, yAxis: Array<Double>, zAxis: Array<Array<Double>>) {
        this.xAxis = xAxis.copyOf()
        this.yAxis = yAxis.copyOf()
        this.zAxis = Array(zAxis.size) { zAxis[it].copyOf() }
    }

    constructor(map3d: Map3d) {
        this.xAxis = map3d.xAxis.copyOf()
        this.yAxis = map3d.yAxis.copyOf()
        this.zAxis = Array(map3d.zAxis.size) { map3d.zAxis[it].copyOf() }
    }

    /**
     * Bilinear interpolation lookup.
     * x corresponds to xAxis (columns), y corresponds to yAxis (rows).
     * Inputs are clamped to axis boundaries.
     * Returns 0.0 for empty maps.
     */
    fun lookup(x: Double, y: Double): Double {
        if (xAxis.isEmpty() || yAxis.isEmpty() || zAxis.isEmpty()) return 0.0

        // Clamp inputs to axis bounds
        val xClamped = x.coerceIn(xAxis.first(), xAxis.last())
        val yClamped = y.coerceIn(yAxis.first(), yAxis.last())

        // Find lower bounding indices
        val xIdx = findLowerIndex(xAxis, xClamped)
        val xIdx1 = (xIdx + 1).coerceAtMost(xAxis.size - 1)

        val yIdx = findLowerIndex(yAxis, yClamped)
        val yIdx1 = (yIdx + 1).coerceAtMost(yAxis.size - 1)

        // Compute interpolation fractions
        val xFrac = if (xIdx == xIdx1) 0.0 else (xClamped - xAxis[xIdx]) / (xAxis[xIdx1] - xAxis[xIdx])
        val yFrac = if (yIdx == yIdx1) 0.0 else (yClamped - yAxis[yIdx]) / (yAxis[yIdx1] - yAxis[yIdx])

        // Read four corner values
        val z00 = zAxis[yIdx][xIdx]
        val z01 = zAxis[yIdx][xIdx1]
        val z10 = zAxis[yIdx1][xIdx]
        val z11 = zAxis[yIdx1][xIdx1]

        // Bilinear interpolation
        val z0 = z00 + (z01 - z00) * xFrac
        val z1 = z10 + (z11 - z10) * xFrac

        return z0 + (z1 - z0) * yFrac
    }

    private fun findLowerIndex(axis: Array<Double>, value: Double): Int {
        for (i in 0 until axis.size - 1) {
            if (value >= axis[i] && value <= axis[i + 1]) return i
        }
        return (axis.size - 2).coerceAtLeast(0)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Map3d) return false
        return xAxis.contentEquals(other.xAxis) &&
                yAxis.contentEquals(other.yAxis) &&
                zAxis.contentDeepEquals(other.zAxis)
    }

    override fun hashCode(): Int {
        var result = xAxis.contentHashCode()
        result = 31 * result + yAxis.contentHashCode()
        result = 31 * result + zAxis.contentDeepHashCode()
        return result
    }

    override fun toString(): String {
        val zAxisString = zAxis.joinToString("\n") { it.contentToString() }
        return "Map3d{xAxis=${xAxis.contentToString()}\n, yAxis=${yAxis.contentToString()}\n, zAxis=$zAxisString}"
    }

    companion object {
        fun transpose(map3d: Map3d): Map3d {
            val data = transposeMatrix(map3d.zAxis)
            return Map3d(map3d.yAxis, map3d.xAxis, data)
        }

        private fun transposeMatrix(matrix: Array<Array<Double>>): Array<Array<Double>> {
            val m = matrix.size
            val n = matrix[0].size
            return Array(n) { x -> Array(m) { y -> matrix[y][x] } }
        }
    }
}
