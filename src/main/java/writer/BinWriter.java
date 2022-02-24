package writer;

import io.reactivex.Observer;
import io.reactivex.subjects.PublishSubject;
import math.map.Map3d;
import parser.bin.BinParser;
import parser.xdf.TableDefinition;

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
    private final ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");
    private static BinWriter instance;

    private final PublishSubject<TableDefinition> publishSubject = PublishSubject.create();

    private BinWriter() {}

    public static BinWriter getInstance() {
        if (instance == null) {
            synchronized ((BinParser.class)) {
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

    public void write(File file, TableDefinition tableDefinition, Map3d map) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(file, "rws");

        if(tableDefinition.getZAxis().getRowCount() > 0 || tableDefinition.getZAxis().getIndexCount() > 0) {
            double[] xAxis = new double[Math.max(tableDefinition.getXAxis().getRowCount(), 1) * Math.max(tableDefinition.getXAxis().getIndexCount(), 1)];
            double[] yAxis = new double[Math.max(tableDefinition.getYAxis().getRowCount(), 1) * Math.max(tableDefinition.getYAxis().getIndexCount(), 1)];
            double[] zAxis = new double[Math.max(tableDefinition.getZAxis().getRowCount(), 1) * Math.max(tableDefinition.getZAxis().getIndexCount(), 1)];

            for(int i = 0; i < map.xAxis.length; i++) {
                xAxis[i] = map.xAxis[i];
            }

            for(int i = 0; i < map.yAxis.length; i++) {
                yAxis[i] = map.yAxis[i];
            }

            int index = 0;

            for(int i = 0; i < map.zAxis.length; i++) {
                for(int j = 0; j < map.zAxis[i].length; j++) {
                    zAxis[index++] = map.zAxis[i][j];
                }
            }

            write(raf, tableDefinition.getXAxis().getAddress(), tableDefinition.getXAxis().getSizeBits(), tableDefinition.getXAxis().getEquation(), xAxis);
            write(raf, tableDefinition.getYAxis().getAddress(), tableDefinition.getYAxis().getSizeBits(), tableDefinition.getYAxis().getEquation(), yAxis);
            write(raf, tableDefinition.getZAxis().getAddress(), tableDefinition.getZAxis().getSizeBits(), tableDefinition.getZAxis().getEquation(), zAxis);

            publishSubject.onNext(tableDefinition);
        }
    }

    private void write(RandomAccessFile raf, int address, int size, String equation, double[] values) throws IOException {
        try {
            CompiledScript compiledScript  = ((Compilable)engine)
                    .compile("function func(X) { return " + BinWriter.inverse(equation) + "}");
            compiledScript.eval(compiledScript.getEngine()
                    .getBindings(ScriptContext.ENGINE_SCOPE));

            Invocable funcEngine = (Invocable) compiledScript.getEngine();

            raf.seek(address);

            ByteBuffer bb = ByteBuffer.allocate(values.length * size).order(ByteOrder.LITTLE_ENDIAN);
            for (double value : values) {
                if (size == 8) {
                    bb.put(((Number)funcEngine.invokeFunction("func", value)).byteValue());
                } else if(size ==16) {
                    bb.putShort(((Number)funcEngine.invokeFunction("func", value)).shortValue());
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

        String inverse = "";

        if(operators.size() > 1) {
            if (operators.get(1).equals("+")) {
                inverse += "(X - " + operands.get(1) + ")";
            }
        }

        if(operators.size() > 0) {
            if (operators.get(0).equals("*")) {
                if(operators.size() > 1) {
                    inverse += " / " + operands.get(0);
                } else {
                    inverse += "X / " + operands.get(0);
                }
            }
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
        Pattern regex = Pattern.compile("(\\+|-)?([0-9]*(\\.[0-9]+))");
        Matcher matcher = regex.matcher(equation);

        List<Double> operands = new ArrayList<>();
        while (matcher.find()) {
            operands.add(Double.parseDouble(matcher.group(0)));
        }

        return operands;
    }
}
