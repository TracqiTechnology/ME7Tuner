package parser.bin;

import com.sun.tools.javac.util.Pair;
import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;
import math.map.Map3d;
import parser.xdf.AxisDefinition;
import parser.xdf.TableDefinition;
import parser.xdf.XdfParser;
import preferences.bin.BinFilePreferences;
import writer.BinWriter;

import javax.script.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public class BinParser {

    private static BinParser instance;

    private final ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");

    private final List<Pair<TableDefinition, Map3d>> mapList = new ArrayList<>();

    private final BehaviorSubject<List<Pair<TableDefinition, Map3d>>> behaviorSubject = BehaviorSubject.create();

    private File binaryFile = new File("");

    private BinParser() {
        BinFilePreferences.getInstance().registerObserver(new Observer<File>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {}

            @Override
            public void onNext(@NonNull File file) {
                binaryFile = file;

                if(binaryFile.exists() && binaryFile.isFile()) {
                    try {
                        parse(new BufferedInputStream(new FileInputStream(binaryFile)), XdfParser.getInstance().getTableDefinitions());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onError(@NonNull Throwable throwable) {}

            @Override
            public void onComplete() {}
        });

        XdfParser.getInstance().register(new Observer<List<TableDefinition>>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {}

            @Override
            public void onNext(@NonNull List<TableDefinition> tableDefinitions) {
                if(binaryFile.exists() && binaryFile.isFile()) {
                    try {
                        parse(new BufferedInputStream(new FileInputStream(binaryFile)), tableDefinitions);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onError(@NonNull Throwable throwable) {}

            @Override
            public void onComplete() {}
        });

        BinWriter.getInstance().register(new Observer<TableDefinition>() {
            @Override
            public void onSubscribe(@NonNull Disposable disposable) {}

            @Override
            public void onNext(@NonNull TableDefinition tableDefinition) {
                if(binaryFile.exists() && binaryFile.isFile()) {
                    try {
                        parse(new BufferedInputStream(new FileInputStream(binaryFile)), XdfParser.getInstance().getTableDefinitions());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onError(@NonNull Throwable throwable) {}

            @Override
            public void onComplete() { }
        });
    }

    public static BinParser getInstance() {
        if (instance == null) {
            synchronized ((BinParser.class)) {
                if (instance == null) {
                    instance = new BinParser();
                }
            }
        }

        return instance;
    }

    public void registerMapListObserver(@NonNull Observer<List<Pair<TableDefinition, Map3d>>> observer) {
        behaviorSubject.subscribe(observer);
    }

    public List<Pair<TableDefinition, Map3d>> getMapList() {
        return mapList;
    }

    private void parse(InputStream inputStream, List<TableDefinition> tableDefinitions) throws IOException {
        mapList.clear();

        BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
        byte[] bytes = new byte[bufferedInputStream.available()];
        bufferedInputStream.read(bytes);
        for(TableDefinition tableDefinition:tableDefinitions) {
            AxisDefinition xAxisDefinition = tableDefinition.getXAxis();
            AxisDefinition yAxisDefinition = tableDefinition.getYAxis();
            AxisDefinition zAxisDefinition = tableDefinition.getZAxis();

            Double[] xAxis = parseAxis(bytes, xAxisDefinition);
            Double[] yAxis = parseAxis(bytes, yAxisDefinition);
            Double[][] zAxis = parseData(bytes, zAxisDefinition);

            mapList.add(new Pair<>(tableDefinition, new Map3d(xAxis, yAxis, zAxis)));
        }

        behaviorSubject.onNext(mapList);
    }

    private Double[] parseAxis(byte[] bytes, AxisDefinition axisDefinition) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        int address = axisDefinition.getAddress();

        if(address != 0) { // Parse from the bin
            buffer.position(address);
            int strideBytes = axisDefinition.getSizeBits()/8;
            // Determine how many bytes per row to get the last index of the axis
            buffer.limit(address + (strideBytes*axisDefinition.getColumnCount()));

            ByteBuffer slice = buffer.slice().order(ByteOrder.LITTLE_ENDIAN);
            slice.position(0);

            try {
                CompiledScript compiledScript = ((Compilable)engine)
                        .compile("function func("+ axisDefinition.getVarId() +") { return " + axisDefinition.getEquation() + "}");
                compiledScript.eval(compiledScript.getEngine()
                        .getBindings(ScriptContext.ENGINE_SCOPE));

                Invocable funcEngine = (Invocable) compiledScript.getEngine();

                Double[] axis = new Double[axisDefinition.getColumnCount()];

                for (int i = 0; i < axis.length; i++) {
                    int value;
                    if(strideBytes == 1) {
                        value = Byte.toUnsignedInt(slice.get());
                    } else {
                        value = Short.toUnsignedInt(slice.getShort());
                    }

                    axis[i] = ((Number)funcEngine.invokeFunction("func", value)).doubleValue();
                }

                return axis;
            } catch (ScriptException | NoSuchMethodException e) {
                e.printStackTrace();
            }
        } else if(axisDefinition.getIndexCount() != 0) { // Parse from xdf
            Double[] axis = new Double[axisDefinition.getIndexCount()];
            for(int i = 0; i < axis.length; i++) {
                axis[i] = axisDefinition.getAxisValues().get(i).snd.doubleValue();
            }

            return axis;
        }

        return new Double[0];
    }

    private Double[][] parseData(byte[] bytes, AxisDefinition axisDefinition) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        int address = axisDefinition.getAddress();

        if(address != 0) {
            buffer.position(address);
            int rowCount = axisDefinition.getRowCount();
            int columnCount = Math.max(1, axisDefinition.getColumnCount());

            int stride = axisDefinition.getSizeBits()/8;

            // Determine how many bytes per row to get the last index of the axis
            buffer.limit(address + (stride*(rowCount * columnCount)));
            ByteBuffer slice = buffer.slice().order(ByteOrder.LITTLE_ENDIAN);
            slice.position(0);

            try {
                CompiledScript compiledScript = ((Compilable)engine)
                        .compile("function func("+ axisDefinition.getVarId() +") { return " + axisDefinition.getEquation() + "}");
                compiledScript.eval(compiledScript.getEngine()
                        .getBindings(ScriptContext.ENGINE_SCOPE));

                Invocable funcEngine = (Invocable) compiledScript.getEngine();

                Double[][] axis = new Double[rowCount][columnCount];

                for (int i = 0; i < axis.length; i++) {
                    axis[i] = new Double[columnCount];
                    for (int j = 0; j < axis[i].length; j++) {
                        int value;
                        if(stride == 1) {
                            value = Byte.toUnsignedInt(slice.get());
                        } else {
                            value = Short.toUnsignedInt(slice.getShort());
                        }

                        axis[i][j] = ((Number)funcEngine.invokeFunction("func", value)).doubleValue();
                    }
                }

                return axis;
            } catch (ScriptException | NoSuchMethodException e) {
                e.printStackTrace();
            }
        }

        return new Double[0][0];
    }


}
