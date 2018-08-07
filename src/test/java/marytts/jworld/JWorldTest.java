package marytts.jworld;

import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import com.google.common.io.ByteStreams;

import org.junit.Assert;
import org.testng.annotations.Test;

import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import marytts.data.Sequence;
import marytts.data.SupportedSequenceType;
import marytts.data.Utterance;
import marytts.data.item.acoustic.AudioItem;
import marytts.data.item.acoustic.F0List;
import marytts.data.item.global.DoubleMatrixItem;
import marytts.jworld.data.JWorldSupportedSequenceType;

public class JWorldTest {

    @Test
    public void testJWorldProcess() throws Exception {
        Utterance utt = new Utterance();

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
        jwm.setSampleRate(22050);
        jwm.setFramePeriod(frame_period);
        Utterance utt_enriched = jwm.process(utt);

        // Validate
        AudioItem au_it = (AudioItem) utt_enriched.getSequence(SupportedSequenceType.AUDIO).get(0);
        AudioInputStream ais = au_it.getAudioStream();

        // Load reference
        URL url = JWorldTest.class.getResource("/vaiueo2d_rec.wav");
        AudioInputStream ref_ais = AudioSystem.getAudioInputStream(url);

        // Assert equality
        AudioFormat format = ais.getFormat();
        byte[] rend_bytes = new byte[(int) (ais.getFrameLength() * format.getFrameSize())];
        ais.read(rend_bytes);
        ByteBuffer buf = ByteBuffer.wrap(rend_bytes);
        short[] rend_short = new short[buf.asShortBuffer().remaining()];
        buf.asShortBuffer().get(rend_short);

        format = ref_ais.getFormat();
        byte[] ref_bytes = new byte[(int) (ref_ais.getFrameLength() * format.getFrameSize())];
        ref_ais.read(ref_bytes);
        buf = ByteBuffer.wrap(ref_bytes);
        short[] ref_short = new short[buf.asShortBuffer().remaining()];
        buf.asShortBuffer().get(ref_short);

        Assert.assertEquals(ref_short.length, rend_short.length);
        for (int s=0; s<ref_short.length; s++) {
            Assert.assertEquals(ref_short[s], rend_short[s], 0);
        }
    }
}
