package domain.model.rlsol

import domain.math.Point

class Rlsol(pu: Double, tans: Double, kfurl: Double, pressure: List<Double>) {
    val points: Array<Point>

    init {
        points = Array(pressure.size) { Point(0.0, 0.0) }
        var ps = pressure[0]
        for (i in points.indices) {
            val x = pressure[i]
            val y = rlsol(pu, ps, tans, 96.0, kfurl, x)
            ps = x
            points[i] = Point(x, y)
        }
    }

    companion object {
        fun rlsol(pu: Double, ps: Double, tans: Double, tmot: Double, kfurl: Double, plsol: Double): Double {
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
            val ftbr = 273.0 / (evtmod + 273) * fwft

            val fupsrl = kfurl * ftbr
            val rfagr = maxOf(pbr - pirg, 0.0) * fupsrl * psagr / ps

            return (plsol * fupsrl * FPBRKDS * VPSSPLS) - rfagr
        }
    }
}
