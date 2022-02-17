package model.msdk_w;

public class Msdk_w {
    public static void calculate() {

        double pspvdkug = 0.95;
        double wdkba_w = 0.5;
        double wdkugdn = 0.5;
        double kfmsnwdk = 1000;
        double msndko_w = 0; // ?

        double iat = 0;
        double ftvdk = 273/(iat+273);
        double klaf = 1.0;
        double fpvdkds_w = 1;

        double oneMinusWdkugdn = 1 - wdkugdn;
        double wdkbaMinusWdkugdn = Math.max(wdkba_w - wdkugdn, 0);
        double oneMinusPspvdkug = 1 - pspvdkug;

        double calc1 = ((oneMinusPspvdkug * wdkbaMinusWdkugdn)/oneMinusWdkugdn) + 1;
        double kfmsnwdkPlusMsndko_w = kfmsnwdk + msndko_w;
        double calc2 = calc1*kfmsnwdkPlusMsndko_w*ftvdk*klaf*fpvdkds_w;

        System.out.println(calc2);
    }
}
