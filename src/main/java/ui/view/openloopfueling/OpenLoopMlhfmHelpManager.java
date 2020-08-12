package ui.view.openloopfueling;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;

public class OpenLoopMlhfmHelpManager {
    public JEditorPane getPanel() {
        JEditorPane jep = new JEditorPane();
        jep.setContentType("text/html");//set content as html
        jep.setText("<a href='https://github.com/KalebKE/ME7Tuner#open-loop'>Open Loop MLHFM User Guide</a>.");

        jep.setEditable(false);//so its not editable

        jep.addHyperlinkListener(new HyperlinkListener() {
            @Override
            public void hyperlinkUpdate(HyperlinkEvent hle) {
                if (HyperlinkEvent.EventType.ACTIVATED.equals(hle.getEventType())) {
                    Desktop desktop = Desktop.getDesktop();
                    try {
                        desktop.browse(hle.getURL().toURI());
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });

        return jep;
    }
}
