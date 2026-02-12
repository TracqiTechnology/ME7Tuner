package domain.model.plsol

import domain.math.Point

class Plsol {
    val points: Array<Point>

    constructor(pu: Double, tans: Double, kfurl: Double) {
        points = Array(400) { i ->
            Point(0.0, 0.0) // placeholder
        }
        var ps = pu
        for (i in points.indices) {
            val x = i.toDouble()
            val y = plsol(pu, ps, tans, 96.0, kfurl, x)
            ps = y
            points[i] = Point(x, y)
        }
    }

    constructor(pu: Double, tans: Double, kfurl: Double, load: List<Double>) {
        points = Array(load.size) { Point(0.0, 0.0) }
        var ps = pu
        for (i in points.indices) {
            val x = load[i]
            val y = plsol(pu, ps, tans, 96.0, kfurl, x)
            ps = y
            points[i] = Point(x, y)
        }
    }

    companion object {
        fun plsol(pu: Double, ps: Double, tans: Double, tmot: Double, kfurl: Double, rlsol: Double): Double {
            val KFPRG = 70.0
            val FPBRKDS = 1.016
            val KFFWTBR = 0.02
            val VPSSPLS = 1.016

            val fho = pu / 1013.0
            val pirg = fho * KFPRG
            val pbr = ps * FPBRKDS
            val psagr = 250.0

            val evtmod = tans + (tmot - tans) * KFFWTBR
            val fwft = (tans + 673.425) / 731.334
            val ftbr = 273.0 / (evtmod + 273.0) * fwft

            val fupsrl = kfurl * ftbr
            val rfagr = maxOf(pbr - pirg, 0.0) * fupsrl * psagr / ps
            val pssol = (rlsol + rfagr) / fupsrl / FPBRKDS

            return pssol / VPSSPLS
        }
    }
}
