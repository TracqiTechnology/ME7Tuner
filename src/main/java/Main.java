import com.formdev.flatlaf.intellijthemes.FlatArcDarkOrangeIJTheme;
import presentation.view.MainManager;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {

        try {
            UIManager.setLookAndFeel( new FlatArcDarkOrangeIJTheme());
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }

        MainManager mainManager = new MainManager();
        mainManager.start();
    }
}
