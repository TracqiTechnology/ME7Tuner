package data.writer;

import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.subjects.PublishSubject;
import domain.math.map.Map3d;
import data.parser.xdf.TableDefinition;

import javax.script.*;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BinWriter {
    private static final int INVALID_ADDRESS = 0;

    private static volatile BinWriter instance;
    private final ScriptEngine engine = new ScriptEngineManager().getEngineByName("graal.js");


    private final PublishSubject<TableDefinition> publishSubject = PublishSubject.create();

    private BinWriter() {
    }

    public static BinWriter getInstance() {
        if (instance == null) {
            synchronized (BinWriter.class) {
                if (instance == null) {
                    instance = new BinWriter();
                }
            }
        }

        return instance;
    }

    public void register(Observer<TableDefinition> observer) {
        publishSubject.subscribe(observer);
    }

    public void write(@NonNull File file, @NonNull TableDefinition tableDefinition, @NonNull Map3d map) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file, "rws")) {

            if (tableDefinition.getXAxis() != null && tableDefinition.getXAxis().getAddress() != INVALID_ADDRESS) {
                double[] xAxis = new double[Math.max(tableDefinition.getXAxis().getRowCount(), 1) * Math.max(tableDefinition.getXAxis().getIndexCount(), 1)];
                for (int i = 0; i < map.xAxis.length; i++) {
                    xAxis[i] = map.xAxis[i];
                }

                write(raf, tableDefinition.getXAxis().getAddress(), tableDefinition.getXAxis().getSizeBits(), tableDefinition.getXAxis().getEquation(), xAxis);
            }

            if (tableDefinition.getYAxis() != null && tableDefinition.getYAxis().getAddress() != INVALID_ADDRESS) {
                double[] yAxis = new double[Math.max(tableDefinition.getYAxis().getRowCount(), 1) * Math.max(tableDefinition.getYAxis().getIndexCount(), 1)];

                for (int i = 0; i < map.yAxis.length; i++) {
                    yAxis[i] = map.yAxis[i];
                }

                write(raf, tableDefinition.getYAxis().getAddress(), tableDefinition.getYAxis().getSizeBits(), tableDefinition.getYAxis().getEquation(), yAxis);
            }

            if (tableDefinition.getZAxis() != null && tableDefinition.getZAxis().getAddress() != INVALID_ADDRESS) {
                double[] zAxis = new double[Math.max(tableDefinition.getZAxis().getRowCount(), 1) * Math.max(tableDefinition.getZAxis().getColumnCount(), 1)];

                int index = 0;
                for (int i = 0; i < map.zAxis.length; i++) {
                    for (int j = 0; j < map.zAxis[i].length; j++) {
                        zAxis[index++] = map.zAxis[i][j];
                    }
                }

                write(raf, tableDefinition.getZAxis().getAddress(), tableDefinition.getZAxis().getSizeBits(), tableDefinition.getZAxis().getEquation(), zAxis);
            }
        }

        publishSubject.onNext(tableDefinition);
    }

    private void write(RandomAccessFile raf, int address, int size, String equation, double[] values) throws IOException {
        try {
            CompiledScript compiledScript = ((Compilable) engine)
                    .compile("function func(X) { return " + BinWriter.inverse(equation) + "}");
            compiledScript.eval(compiledScript.getEngine()
                    .getBindings(ScriptContext.ENGINE_SCOPE));

            Invocable funcEngine = (Invocable) compiledScript.getEngine();

            raf.seek(address);

            ByteBuffer bb = ByteBuffer.allocate(values.length * (size/8)).order(ByteOrder.LITTLE_ENDIAN);
            for (double value : values) {
                if (size == 8) {
                    bb.put(((Number) funcEngine.invokeFunction("func", value)).byteValue());
                } else if (size == 16) {
                    bb.putShort(((Number) funcEngine.invokeFunction("func", value)).shortValue());
                }
            }

            raf.write(bb.array());
        } catch (ScriptException | NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    private static String inverse(String equation) {
        List<String> operators = extractOperators(equation);
        List<Double> operands = extractOperands(equation);

        if (operators.isEmpty() && operands.isEmpty()) {
            return equation;
        }

        boolean hasMultiply = !operators.isEmpty() && operators.get(0).equals("*");
        Double scale = hasMultiply && !operands.isEmpty() ? operands.get(0) : null;
        Double offset = null;

        if (operators.size() > 1 && operands.size() > 1) {
            // X * a + b
            offset = operands.get(1);
        } else if (hasMultiply && operands.size() > 1) {
            // X * a - b (subtraction captured as negative operand)
            offset = operands.get(1);
        } else if (!hasMultiply && !operands.isEmpty()) {
            // X + b or X - b
            offset = operands.get(0);
        }

        String inverse = "";

        if (scale != null && offset != null) {
            inverse = "(X - " + offset + ") / " + scale;
        } else if (scale != null) {
            inverse = "X / " + scale;
        } else if (offset != null) {
            inverse = "X - " + offset;
        }

        return inverse;
    }

    private static List<String> extractOperators(String equation) {
        Pattern regex = Pattern.compile("[*+]");
        Matcher matcher = regex.matcher(equation);

        List<String> operators = new ArrayList<>();
        while (matcher.find()) {
            operators.add(matcher.group(0));
        }

        return operators;
    }

    private static List<Double> extractOperands(String equation) {
        Pattern regex = Pattern.compile("(\\+|-)?([0-9]*\\.?[0-9]+)");
        Matcher matcher = regex.matcher(equation);

        List<Double> operands = new ArrayList<>();
        while (matcher.find()) {
            operands.add(Double.parseDouble(matcher.group(0)));
        }

        return operands;
    }
}
