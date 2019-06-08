package writer;

import math.map.Map2d;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import contract.MlhfmFileContract;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class MlhfmWriter {
    public void write(File file, Map2d mlhfmMap) {
        try {

            List<Double> voltage = Arrays.asList(mlhfmMap.axis);
            List<Double> kgPerHour = Arrays.asList(mlhfmMap.data);

            BufferedWriter writer = Files.newBufferedWriter(Paths.get(file.getAbsolutePath()));

            CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader("voltage", "kg/hr"));

            for(int i = 0; i < voltage.size(); i++) {
                csvPrinter.printRecord(voltage.get(i), kgPerHour.get(i));
            }

            csvPrinter.flush();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}
