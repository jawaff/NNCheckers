import ai.djl.MalformedModelException;
import ai.djl.Model;
import ai.djl.engine.Engine;
import ai.djl.inference.Predictor;
import ai.djl.metric.Metrics;
import ai.djl.modality.Classifications;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.DataType;
import ai.djl.ndarray.types.Shape;
import ai.djl.nn.Activation;
import ai.djl.nn.Block;
import ai.djl.nn.SequentialBlock;
import ai.djl.nn.core.Linear;
import ai.djl.training.DefaultTrainingConfig;
import ai.djl.training.EasyTrain;
import ai.djl.training.Trainer;
import ai.djl.training.TrainingResult;
import ai.djl.training.dataset.RandomAccessDataset;
import ai.djl.training.evaluator.Accuracy;
import ai.djl.training.listener.SaveModelTrainingListener;
import ai.djl.training.listener.TrainingListener;
import ai.djl.training.loss.Loss;
import ai.djl.basicdataset.tabular.CsvDataset;
import org.apache.commons.csv.CSVFormat;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

public class WaffleNNModel {
    private static NDManager manager = NDManager.newBaseManager();

    public static int BOARD_SIZE = 64;
    // There's two classifications: player 1 wins and player 2 wins.
    public static int NUM_CLASSES = 2;

    private static String MODEL_NAME = "checkers-mlp";
    
    private static String MODEL_DIR = "./model/";
    private static Path CSV_PATH = Paths.get("./training.csv");
    private static Path TMP_PATH = Paths.get("./tmp.csv");

    private static Model model;

    private static Predictor<float[], Classifications> predictor = null;

    public static void main(String[] argv) throws Exception {
        trainNN();
    }
    
    private static Block getModelBlock() {
        SequentialBlock block = new SequentialBlock();
        //block.add(Blocks.batchFlattenBlock(BOARD_SIZE));
        block.add(Linear.builder().setUnits(128).build());
        block.add(Activation::relu);
        block.add(Linear.builder().setUnits(64).build());
        block.add(Activation::relu);
        block.add(Linear.builder().setUnits(NUM_CLASSES).build());

        block.initialize(manager, DataType.FLOAT32, new Shape(1, BOARD_SIZE));
        return block;
    }

    private static void printStackTrace(Throwable e, PrintWriter pw) {
        Throwable next = e;
        while (next != null) {
            next.printStackTrace(pw);
            next = next.getCause();
            if (next != null) {
                pw.println("Cause:");
            }
        }
    }

    public static void loadPredictor() {
        if  (predictor == null) {
            try {
                model = Model.newInstance("checkers-mlp");
                model.setBlock(getModelBlock());
                model.load(Paths.get(MODEL_DIR));

                predictor = model.newPredictor(new BoardStateTranslator());
            } catch (Throwable t) {
                printStackTrace(new IllegalStateException("Failed to load model", t), new PrintWriter(System.err));
            }

            Runtime.getRuntime().addShutdownHook(new Thread(WaffleNNModel::closePredictor));
        }
    }

    public static void closePredictor() {
        predictor.close();
        model.close();
    }

    public static void storeBoardState(WaffleState state, double boardEval) {
        if (!CSV_PATH.toFile().exists()) {
            try (FileWriter writer = new FileWriter(CSV_PATH.toFile(), false)) {
                String[] header = new String[BOARD_SIZE];
                for (int i = 0; i < BOARD_SIZE; i++) {
                    header[i] = String.valueOf(i + 1);
                }
                writer.write(String.join(",", Arrays.asList(header)));

                // These are the classification labels.
                writer.write(",Player1Win");
            } catch (Throwable t) {
                throw new IllegalStateException("Failed to create training csv", t);
            }
        }

        boolean exists = TMP_PATH.toFile().exists();
        try (FileWriter writer = new FileWriter(TMP_PATH.toFile(), exists)) {
            if (exists) {
                writer.write("\n");
            }
            String row = Arrays.toString(state.toNNInputs());
            row = row.substring(1, row.length() - 1);
            row = row.replaceAll(" ", "");
            writer.write(row);
            writer.write("," + boardEval);
        } catch (Throwable t) {
            throw new IllegalStateException("Failed to write to training csv", t);
        }
    }

    public static double predict(WaffleState state) {
        if (predictor != null) {
            try {
                Classifications prediction = predictor.predict(state.toNNInputs());
                if (state.player == 1) {
                    return prediction.get("Player1Win").getProbability();
                } else {
                    return prediction.get("Player2Win").getProbability();
                }
            } catch (Throwable t) {
                throw new IllegalStateException("Failed to predict", t);
            }
        } else {
            return 1.0;
        }
    }

    private static RandomAccessDataset getDataset() {
        CsvDataset.CsvBuilder<?> builder = CsvDataset.builder();
        builder.optCsvFile(CSV_PATH);
        builder.setCsvFormat(
                CSVFormat.DEFAULT
                        .withFirstRecordAsHeader()
                        .withIgnoreHeaderCase()
        );
        builder.setSampling(1, false);
        // Sets the features found in the csv, the column names are just the board positions.
        for (int i = 1; i <= BOARD_SIZE; i++) {
            builder.addNumericFeature(String.valueOf(i));
        }
        builder.addNumericLabel("Player1Win");
        return builder.build();
    }

    private static void trainNN() {
        int epochs = 10;

        try (Model model = Model.newInstance(MODEL_NAME)) {
            model.setBlock(getModelBlock());

            // get training and validation dataset
            RandomAccessDataset trainingSet = getDataset();

            // setup training configuration
            DefaultTrainingConfig config = new DefaultTrainingConfig(Loss.softmaxCrossEntropyLoss())
                    .addEvaluator(new Accuracy())
                    .optDevices(Engine.getInstance().getDevices())
                    .addTrainingListeners(TrainingListener.Defaults.logging(MODEL_DIR));

            try (Trainer trainer = model.newTrainer(config)) {
                trainer.setMetrics(new Metrics());
                Shape inputShape = new Shape(1, BOARD_SIZE);

                // initialize trainer with proper input shape
                trainer.initialize(inputShape);

                EasyTrain.fit(trainer, epochs, trainingSet, null);
                model.save(Paths.get(MODEL_DIR), MODEL_NAME);
            } catch (Throwable t) {
                throw new IllegalStateException("Failed to train", t);
            }
        }
    }
}
