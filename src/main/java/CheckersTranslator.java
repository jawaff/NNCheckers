import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.translate.NoBatchifyTranslator;
import ai.djl.translate.TranslatorContext;

public class CheckersTranslator implements NoBatchifyTranslator<int[], double[]> {

    @Override
    public NDList processInput(TranslatorContext ctx, int[] input) {
        return new NDList(ctx.getNDManager().create(input));
        //NDArray array = input.toNDArray(ctx.getNDManager(), Image.Flag.GRAYSCALE);
        //return new NDList(NDImageUtils.toTensor(array));
    }

    @Override
    public double[] processOutput(TranslatorContext ctx, NDList list) {
        // Create a Classifications with the output probabilities
        NDArray probabilities = list.singletonOrThrow().softmax(0);
        return probabilities.toDoubleArray();
    }
}
