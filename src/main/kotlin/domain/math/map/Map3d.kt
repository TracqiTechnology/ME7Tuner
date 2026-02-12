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
