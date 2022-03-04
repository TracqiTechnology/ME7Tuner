package parser.xdf;

import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;
import org.apache.commons.math3.util.Pair;
import org.jdom2.*;
import org.jdom2.input.SAXBuilder;
import preferences.xdf.XdfFilePreferences;

import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class XdfParser {

    private static final String XDF_CONSTANT_TAG = "XDFCONSTANT";
    private static final String XDF_TABLE_TAG = "XDFTABLE";
    private static final String XDF_AXIS_TAG = "XDFAXIS";
    private static final String XDF_LABEL_TAG = "LABEL";
    private static final String XDF_EMBEDDED_TAG = "EMBEDDEDDATA";
    private static final String XDF_MATH_TAG = "MATH";
    private static final String XDF_VAR_TAG = "VAR";

    private static final String XDF_TABLE_TITLE_TAG = "title";
    private static final String XDF_TABLE_DESCRIPTION_TAG = "description";
    private static final String XDF_ID_TAG = "id";
    private static final String XDF_INDEX_TAG = "index";
    private static final String XDF_VALUE_TAG = "value";
    private static final String XDF_INDEX_COUNT_TAG = "indexcount";
    private static final String XDF_UNITS_TAG = "units";

    private static final String XDF_ADDRESS_TAG = "mmedaddress";
    private static final String XDF_SIZE_BITS_TAG = "mmedelementsizebits";
    private static final String XDF_ROW_COUNT_TAG = "mmedrowcount";
    private static final String XDF_COLUMN_COUNT_TAG = "mmedcolcount";
    private static final String XDF_EQUATION_TAG = "equation";

    private static XdfParser instance;

    private final List<TableDefinition> tableDefinitions = new ArrayList<>();

    private final BehaviorSubject<List<TableDefinition>> behaviorSubject = BehaviorSubject.create();

    private XdfParser()  {
        XdfFilePreferences.getInstance().registerObserver(new Observer<File>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {}

            @Override
            public void onNext(@NonNull File file) {
                if(file.exists() && file.isFile()) {
                    try {
                        parse(new BufferedInputStream(new FileInputStream(file)));
                    } catch (JDOMException | IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onError(@NonNull Throwable throwable) {}

            @Override
            public void onComplete() {}
        });
    }

    public static XdfParser getInstance() {
        if(instance == null) {
            synchronized ((XdfParser.class)) {
                if(instance == null) {
                    instance = new XdfParser();
                }
            }
        }

        return instance;
    }

    public void register(Observer<List<TableDefinition>> observer) {
        behaviorSubject.subscribe(observer);
    }

    public List<TableDefinition> getTableDefinitions() {
        return tableDefinitions;
    }

    private void parse(InputStream inputStream) throws JDOMException, IOException {
        tableDefinitions.clear();

        SAXBuilder saxBuilder = new SAXBuilder();
        Document document = saxBuilder.build(inputStream);
        Element classElement = document.getRootElement();
        List<Element> elements = classElement.getChildren();

        for(Element element: elements) {
            if(element.getName().equals(XDF_TABLE_TAG)) {

                String tableName = "";
                String tableDescription = "";

                int xAddress = 0;
                int yAddress = 0;
                int zAddress = 0;

                int xSizeBits = 0;
                int ySizeBits = 0;
                int zSizeBits = 0;

                int xRowCount = 0;
                int yRowCount = 0;
                int zRowCount = 0;

                int xColumnCount = 0;
                int yColumnCount = 0;
                int zColumnCount = 0;

                String xEquation = "";
                String yEquation = "";
                String zEquation = "";

                String xVarId = "";
                String yVarId = "";
                String zVarId = "";

                int xIndexCount = 0;
                int yIndexCount = 0;
                int zIndexCount = 0;

                String xUnits = "-";
                String yUnits = "-";
                String zUnits = "-";

                List<Pair<Integer, Float>> xAxisValues = new ArrayList<>();
                List<Pair<Integer, Float>> yAxisValues = new ArrayList<>();
                List<Pair<Integer, Float>> zAxisValues = new ArrayList<>();

                List<Element> tableChildren = element.getChildren();
                String axis;

                for(Element tableChild:tableChildren) {
                    if(tableChild.getName().equals(XDF_TABLE_TITLE_TAG)) {               // TITLE tag
                        tableName = tableChild.getText();
                    } else if(tableChild.getName().equals(XDF_TABLE_DESCRIPTION_TAG)) {  // DESCRIPTION tag
                        tableDescription = tableChild.getText();
                    } else if(tableChild.getName().equals(XDF_AXIS_TAG)) {               // AXIS tag
                        axis = tableChild.getAttribute(XDF_ID_TAG).getValue();

                        List<Element> axisChildren = tableChild.getChildren();
                        for(Element axisChild:axisChildren) {
                            // Get the axis index count
                            if(axisChild.getName().equals(XDF_INDEX_COUNT_TAG)) {
                                int indexCount = Integer.parseInt(axisChild.getValue());

                                switch (axis) {
                                    case "x":
                                        xIndexCount = indexCount;
                                        break;
                                    case "y":
                                        yIndexCount = indexCount;
                                        break;
                                    case "z":
                                        zIndexCount = indexCount;
                                        break;
                                }
                            }

                            // Get the axis unit
                            if(axisChild.getName().equals(XDF_UNITS_TAG)){
                                String indexUnit = axisChild.getValue();

                                switch (axis) {
                                    case "x":
                                        xUnits = indexUnit;
                                        break;
                                    case "y":
                                        yUnits = indexUnit;
                                        break;
                                    case "z":
                                        zUnits = indexUnit;
                                        break;
                                }
                            }

                            // Get the axis embedded data
                            if(axisChild.getName().equals(XDF_EMBEDDED_TAG)) {
                                int sizeBits = axisChild.getAttribute(XDF_SIZE_BITS_TAG).getIntValue();

                                Attribute addressAttribute = axisChild.getAttribute(XDF_ADDRESS_TAG);
                                int address = 0;
                                if(addressAttribute != null) {
                                    address = Integer.decode(addressAttribute.getValue());
                                }

                                Attribute rowCountAttribute = axisChild.getAttribute(XDF_ROW_COUNT_TAG);
                                int rowCount = 0;
                                if(rowCountAttribute != null) {
                                    rowCount = axisChild.getAttribute(XDF_ROW_COUNT_TAG).getIntValue();
                                }

                                Attribute columnCountAttribute = axisChild.getAttribute(XDF_COLUMN_COUNT_TAG);
                                int columnCount = 0;
                                if(columnCountAttribute != null) {
                                    columnCount = axisChild.getAttribute(XDF_COLUMN_COUNT_TAG).getIntValue();
                                }

                                switch (axis) {
                                    case "x":
                                        xAddress = address;
                                        xSizeBits = sizeBits;
                                        xRowCount = rowCount;
                                        xColumnCount = columnCount;
                                        break;
                                    case "y":
                                        yAddress = address;
                                        ySizeBits = sizeBits;
                                        yRowCount = rowCount;
                                        yColumnCount = columnCount;
                                        break;
                                    case "z":
                                        zAddress = address;
                                        zSizeBits = sizeBits;
                                        zRowCount = rowCount;
                                        zColumnCount = columnCount;
                                        break;
                                }
                            }

                            // Get the axis embedded data
                            if(axisChild.getName().equals(XDF_MATH_TAG)) {
                                String equation = axisChild.getAttribute(XDF_EQUATION_TAG).getValue();
                                String varId = "";
                                for(Element equationChild:axisChild.getChildren()) {
                                    if(equationChild.getName().equals(XDF_VAR_TAG)) {
                                        varId = equationChild.getAttribute(XDF_ID_TAG).getValue();
                                    }
                                }

                                switch (axis) {
                                    case "x":
                                        xEquation = equation;
                                        xVarId = varId;
                                        break;
                                    case "y":
                                        yEquation = equation;
                                        yVarId = varId;
                                        break;
                                    case "z":
                                        zEquation = equation;
                                        zVarId = varId;
                                        break;
                                }
                            }

                            // Get the axis embedded data
                            if(axisChild.getName().equals(XDF_LABEL_TAG)) {
                                int index = axisChild.getAttribute(XDF_INDEX_TAG).getIntValue();

                                float value = 0;

                                try {
                                    value = axisChild.getAttribute(XDF_VALUE_TAG).getFloatValue();
                                } catch (DataConversionException e) {}

                                switch (axis) {
                                    case "x":
                                        xAxisValues.add(new Pair<>(index, value));
                                        break;
                                    case "y":
                                        yAxisValues.add(new Pair<>(index, value));
                                        break;
                                    case "z":
                                        zAxisValues.add(new Pair<>(index, value));
                                        break;
                                }
                            }

                        }
                    }
                }

                AxisDefinition xAxisDefinition = new AxisDefinition("x", xAddress, xIndexCount, xSizeBits, xRowCount, xColumnCount, xUnits, xEquation, xVarId, xAxisValues);
                AxisDefinition yAxisDefinition = new AxisDefinition("y", yAddress, yIndexCount, ySizeBits, yRowCount, yColumnCount, yUnits, yEquation, yVarId, yAxisValues);
                AxisDefinition zAxisDefinition = new AxisDefinition("z", zAddress, zIndexCount, zSizeBits, zRowCount, zColumnCount, zUnits, zEquation, zVarId, zAxisValues);

                tableDefinitions.add(new TableDefinition(tableName, tableDescription, xAxisDefinition, yAxisDefinition, zAxisDefinition));
            } else if(element.getName().equals(XDF_CONSTANT_TAG)) {
                String tableName = "";
                String tableDescription = "";
                String xEquation = "";
                String xVarId = "";
                String xUnits = "-";

                int xAddress = 0;
                int xSizeBits = 0;


                List<Element> tableChildren = element.getChildren();

                for (Element tableChild : tableChildren) {
                    if (tableChild.getName().equals(XDF_TABLE_TITLE_TAG)) {               // TITLE tag
                        tableName = tableChild.getText();
                    } else if (tableChild.getName().equals(XDF_TABLE_DESCRIPTION_TAG)) {  // DESCRIPTION tag
                        tableDescription = tableChild.getText();
                    } else if(tableChild.getName().equals(XDF_UNITS_TAG)) {
                        xUnits = tableChild.getValue();
                    } else if(tableChild.getName().equals(XDF_EMBEDDED_TAG)) {
                        int sizeBits = tableChild.getAttribute(XDF_SIZE_BITS_TAG).getIntValue();

                        Attribute addressAttribute = tableChild.getAttribute(XDF_ADDRESS_TAG);
                        int address = 0;
                        if(addressAttribute != null) {
                            address = Integer.decode(addressAttribute.getValue());
                        }

                        xAddress = address;
                        xSizeBits = sizeBits;

                    } else if(tableChild.getName().equals(XDF_MATH_TAG)) {
                        String equation = tableChild.getAttribute(XDF_EQUATION_TAG).getValue();
                        String varId = "";
                        for(Element equationChild:tableChild.getChildren()) {
                            if(equationChild.getName().equals(XDF_VAR_TAG)) {
                                varId = equationChild.getAttribute(XDF_ID_TAG).getValue();
                            }
                        }
                        xEquation = equation;
                        xVarId = varId;
                    }
                }

                AxisDefinition zAxisDefinition = new AxisDefinition("z", xAddress, 0, xSizeBits, 0, 0, xUnits, xEquation, xVarId, new ArrayList<>());

                tableDefinitions.add(new TableDefinition(tableName, tableDescription, null, null, zAxisDefinition));
            }
        }

        tableDefinitions.sort(Comparator.comparing(TableDefinition::toString));

        behaviorSubject.onNext(tableDefinitions);
    }
}
