import com.formdev.flatlaf.intellijthemes.FlatArcDarkOrangeIJTheme;
import presentation.view.MainManager;

import javax.swing.*;
import java.util.Locale;

public class Main {
    public static void main(String[] args) {

        Locale.setDefault(Locale.ENGLISH);

        try {
            UIManager.setLookAndFeel( new FlatArcDarkOrangeIJTheme());
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }

        MainManager mainManager = new MainManager();
        mainManager.start();
    }
}
