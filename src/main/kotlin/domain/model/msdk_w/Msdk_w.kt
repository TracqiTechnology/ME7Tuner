package domain.model.msdk_w

object Msdk_w {
    fun calculate() {
        val pspvdkug = 0.95
        val wdkba_w = 0.5
        val wdkugdn = 0.5
        val kfmsnwdk = 1000.0
        val msndko_w = 0.0

        val iat = 0.0
        val ftvdk = 273 / (iat + 273)
        val klaf = 1.0
        val fpvdkds_w = 1.0

        val oneMinusWdkugdn = 1 - wdkugdn
        val wdkbaMinusWdkugdn = maxOf(wdkba_w - wdkugdn, 0.0)
        val oneMinusPspvdkug = 1 - pspvdkug

        val calc1 = ((oneMinusPspvdkug * wdkbaMinusWdkugdn) / oneMinusWdkugdn) + 1
        val kfmsnwdkPlusMsndko_w = kfmsnwdk + msndko_w
        val calc2 = calc1 * kfmsnwdkPlusMsndko_w * ftvdk * klaf * fpvdkds_w
    }
}
