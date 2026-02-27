package domain.math.map

class Map2d {
    var axis: Array<Double>
    var data: Array<Double>

    constructor() {
        axis = emptyArray()
        data = emptyArray()
    }

    constructor(axis: Array<Double>, data: Array<Double>) {
        this.axis = axis
        this.data = data
    }

    constructor(map2d: Map2d) {
        this.axis = map2d.axis.copyOf()
        this.data = map2d.data.copyOf()
    }
}
