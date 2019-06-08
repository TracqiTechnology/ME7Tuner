package parser.mlhfm;

import contract.MlhfmFileContract;
import math.map.Map2d;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MlhfmParser {
    public Map2d parse(File file) {

        List<Double> axis = new ArrayList<>();
        List<Double> data = new ArrayList<>();

        try {
            Reader in = new FileReader(file.getAbsolutePath());
            Iterable<CSVRecord> records = CSVFormat.RFC4180.withHeader(MlhfmFileContract.MAF_VOLTAGE_HEADER, MlhfmFileContract.KILOGRAM_PER_HOUR_HEADER).parse(in);
            int index = 0;
            for (CSVRecord record : records) {
                if(index++ > 0) {
                    axis.add(Double.parseDouble(record.get(MlhfmFileContract.MAF_VOLTAGE_HEADER)));
                    data.add(Double.parseDouble(record.get(MlhfmFileContract.KILOGRAM_PER_HOUR_HEADER)));
                }
            }

            return new Map2d(axis.toArray(new Double[0]), data.toArray(new Double[0]));
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

        return null;
    }
}
