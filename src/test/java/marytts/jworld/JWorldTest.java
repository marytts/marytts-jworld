package marytts.jworld;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;

import com.google.common.io.ByteStreams;

import org.testng.annotations.Test;

import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import marytts.data.Sequence;
import marytts.data.SupportedSequenceType;
import marytts.data.Utterance;
import marytts.data.item.acoustic.F0List;
import marytts.data.item.global.DoubleMatrixItem;
import marytts.jworld.data.JWorldSupportedSequenceType;

public class JWorldTest {

    @Test
    public void testJWorldProcess() throws Exception {
        Utterance utt = new Utterance();
        // FIXME: Load some resources (not added to the repo for space, they should be produced by analysis first!!)

        //  - F0

        // Load reference F0
        byte[] bytes = ByteStreams.toByteArray(JWorldTest.class.getResourceAsStream("/test.f0"));
        ByteBuffer byteBuffer = ByteBuffer.allocate(bytes.length);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        byteBuffer.put(bytes);
        byteBuffer.rewind();

        double[] f0 = new double[byteBuffer.asDoubleBuffer().remaining()];
        byteBuffer.asDoubleBuffer().get(f0);
        F0List f0_item = new F0List(new DenseDoubleMatrix1D(f0));
        Sequence<F0List> f0_seq = new Sequence<F0List>();
        f0_seq.add(f0_item);
        utt.addSequence(SupportedSequenceType.F0, f0_seq);

        //  - SP

        // Load spectrum bytes
        bytes = ByteStreams.toByteArray(JWorldTest.class.getResourceAsStream("/test.sp"));
        byteBuffer = ByteBuffer.allocate(bytes.length);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        byteBuffer.put(bytes);
        byteBuffer.rewind();

        // Load spectrum header
        int sample_rate = byteBuffer.getInt();
        double frame_period = byteBuffer.getDouble();

        // Load actual reference spectrum
        DoubleBuffer doubleBuffer = byteBuffer.asDoubleBuffer();
        double[][] sp = new double[f0.length][doubleBuffer.remaining()/f0.length];
        for(int t=0; t<f0.length; t++){
            doubleBuffer.get(sp[t]);
        }
        DoubleMatrixItem sp_item = new DoubleMatrixItem(new DenseDoubleMatrix2D(sp));
        Sequence<DoubleMatrixItem> sp_seq = new Sequence<DoubleMatrixItem>();
        sp_seq.add(sp_item);
        utt.addSequence(JWorldSupportedSequenceType.SPECTRUM, sp_seq);

        //  - AP
        // Load aperiodicity bytes
        bytes = ByteStreams.toByteArray(JWorldTest.class.getResourceAsStream("/test.ap"));
        byteBuffer = ByteBuffer.allocate(bytes.length);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        byteBuffer.put(bytes);
        byteBuffer.rewind();

        // Load actual reference aperiodicity
        doubleBuffer = byteBuffer.asDoubleBuffer();
        double[][] ap = new double[f0.length][doubleBuffer.remaining()/f0.length];
        for(int t=0; t<f0.length; t++){
            doubleBuffer.get(ap[t]);
        }
        DoubleMatrixItem ap_item = new DoubleMatrixItem(new DenseDoubleMatrix2D(ap));
        Sequence<DoubleMatrixItem> ap_seq = new Sequence<DoubleMatrixItem>();
        ap_seq.add(ap_item);
        utt.addSequence(JWorldSupportedSequenceType.APERIODICITY, ap_seq);

        // Synthesize
        JWorldModule jwm = new JWorldModule();
        jwm.setSampleRate(sample_rate);
        jwm.setFramePeriod(frame_period);
        Utterance utt_enriched = jwm.process(utt);
    }
}
