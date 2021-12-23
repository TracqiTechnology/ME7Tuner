import org.jdom2.JDOMException;
import parser.bin.BinParser;
import parser.xdf.BinDefinitionParser;
import parser.xdf.TableDefinition;
import ui.view.MainManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        MainManager mainManager = new MainManager();
        mainManager.start();

        try {
            InputStream inputStream = Main.class.getClassLoader().getResourceAsStream("8D0907551M-20170411-16bit-kfzw.xdf");
            BinDefinitionParser.getInstance().parse(inputStream);
            List<TableDefinition> tableDefinitions = BinDefinitionParser.getInstance().getTableDefinitions();

            inputStream = Main.class.getClassLoader().getResourceAsStream("MOriginal.bin");
            BinParser.getInstance().parse(inputStream, tableDefinitions);
        } catch (IOException | JDOMException e) {
            e.printStackTrace();
        }
    }
}
