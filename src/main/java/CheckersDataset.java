
import ai.djl.ndarray.NDManager;
import ai.djl.training.dataset.RandomAccessDataset;
import ai.djl.training.dataset.Record;
import ai.djl.translate.TranslateException;
import ai.djl.translate.TranslatorOptions;
import ai.djl.util.Progress;

import java.io.IOException;

class CheckersDataset extends RandomAccessDataset {

    public CheckersDataset(BaseBuilder<?> builder) {
        super(builder);
    }

    @Override
    public Record get(NDManager ndManager, long l) throws IOException {
        return null;
    }

    @Override
    protected long availableSize() {
        return 0;
    }

    @Override
    public void prepare() throws IOException, TranslateException {
        super.prepare();
    }

    @Override
    public void prepare(Progress progress) throws IOException, TranslateException {
    }
}