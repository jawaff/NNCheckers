import ai.djl.modality.Classifications;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.index.NDIndex;
import ai.djl.translate.Batchifier;
import ai.djl.translate.NoBatchifyTranslator;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorContext;

import java.util.ArrayList;
import java.util.List;

public class BoardStateTranslator implements NoBatchifyTranslator<float[], Classifications> {

    /**
     * @param ctx context of the translator.
     * @param state The state of the checkers board
     * @return NDList of encoded NDArray
     */
    @Override
    public NDList processInput(TranslatorContext ctx, float[] state) {
        return new NDList(ctx.getNDManager().create(state));
    }

    /**
     * Converts the Output NDArray (classification labels) to Classifications object for easy
     * formatting.
     *
     * @param ctx context of the translator.
     * @param list NDlist of prediction output
     * @return returns a Classifications objects
     */
    @Override
    public Classifications processOutput(TranslatorContext ctx, NDList list) {
        NDArray array = list.get(0);
        NDArray pred = array.softmax(-1);
        List<String> labels = new ArrayList<>();
        labels.add("Player1Win");
        labels.add("Player2Win");
        return new Classifications(labels, pred);
    }
}