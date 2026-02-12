package domain.model.openloopfueling.correction

import domain.math.map.Map3d

data class OpenLoopMlhfmCorrection(
    val inputMlhfm: Map3d,
    val correctedMlhfm: Map3d,
    val fitMlhfm: Map3d,
    val correctionsAfrMap: Map<Double, List<Double>>,
    val meanAfrMap: Map<Double, Double>,
    val modeAfrMap: Map<Double, DoubleArray>,
    val correctedAfrMap: Map<Double, Double>
)
