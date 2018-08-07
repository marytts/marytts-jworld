package marytts.jworld;

import java.util.ArrayList;

import javax.sound.sampled.AudioInputStream;

import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import jworld.JWorldWrapper;
import marytts.MaryException;
import marytts.config.MaryConfiguration;
import marytts.data.Relation;
import marytts.data.Sequence;
import marytts.data.SupportedSequenceType;
import marytts.data.Utterance;
import marytts.data.item.acoustic.AudioItem;
import marytts.data.item.acoustic.F0List;
import marytts.data.item.global.DoubleMatrixItem;
import marytts.data.utils.IntegerPair;
import marytts.exceptions.MaryConfigurationException;
import marytts.jworld.data.JWorldSupportedSequenceType;
import marytts.modules.MaryModule;

/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class JWorldModule extends MaryModule
{
    private String f0_seq_label;

    private double frame_period;
    private int samplerate;
    private boolean as_short;


    public JWorldModule()
    {
        super("acoustic");
        setFramePeriod(5.0);
        setSampleRate(48000);
        setAsShort(false);
        setF0SeqLabel(SupportedSequenceType.F0);
    }

    public void checkStartup() throws MaryConfigurationException
    {
    }

    public void checkInput(Utterance utt) throws MaryException
    {
        if (! utt.hasSequence(JWorldSupportedSequenceType.SPECTRUM)) {
            throw new MaryException("Doesn't have the world dedicated spectrum sequence \"" +
                                    JWorldSupportedSequenceType.SPECTRUM + "\"");
        }

        if (! utt.hasSequence(JWorldSupportedSequenceType.APERIODICITY)) {
            throw new MaryException("Doesn't have the world dedicated aperiodicity sequence \"" +
                                    JWorldSupportedSequenceType.APERIODICITY + "\"");
        }

        // if (! utt.hasSequence(SupportedSequenceType.F0)) {
        //     throw new MaryException("Doesn't have the world dedicated f0 sequence \"" +
        //                             SupportedSequenceType.F0 + "\"");
        // }
    }

    public Utterance process(Utterance utt, MaryConfiguration runtime_configuration) throws MaryException
    {
        runtime_configuration.applyConfiguration(this);

        // FIXME: for now exception !
        if (utt.hasSequence(SupportedSequenceType.AUDIO)) {
            throw new MaryException("Audio sequence already existing");
        }

        // Get sequence
        Sequence<DoubleMatrixItem> sp_chunks = (Sequence<DoubleMatrixItem>) utt.getSequence(JWorldSupportedSequenceType.SPECTRUM);
        Sequence<DoubleMatrixItem> ap_chunks = (Sequence<DoubleMatrixItem>) utt.getSequence(JWorldSupportedSequenceType.APERIODICITY);
        Sequence<F0List> f0_chunks = (Sequence<F0List>) utt.getSequence(getF0SeqLabel());

        // Compute size
        int nb_frames = 0;
        for (F0List f0_chunk: f0_chunks) {
            nb_frames += f0_chunk.getValues().size();
        }


        // Merge F0 (FIXME: should have a more clever way to do that!)
        double[] f0 = new double[nb_frames];
        int t=0;
        for (F0List f0_chunk: f0_chunks) {
            DenseDoubleMatrix1D ch = f0_chunk.getValues();
            for (int i=0; i<ch.size(); i++) {
                f0[t] = ch.getQuick(i);
                t++;
            }
        }

        // Merge Sp (FIXME: should have a more clever way to do that!)
        int size_sp = sp_chunks.get(0).getValues().columns();
        double[][] sp = new double[nb_frames][size_sp];
        t=0;
        for (DoubleMatrixItem sp_chunk: sp_chunks) {
            DenseDoubleMatrix2D ch = sp_chunk.getValues();
            for (int i=0; i<ch.rows(); i++) {
                for (int j=0; j<ch.columns(); j++) {
                    sp[t][j] = ch.getQuick(i, j);
                }
                t++;
            }
        }

        // Merge AP (FIXME: should have a more clever way to do that!)
        int size_ap = ap_chunks.get(0).getValues().columns();
        double[][] ap = new double[nb_frames][size_ap];
        t=0;
        for (DoubleMatrixItem ap_chunk: ap_chunks) {
            DenseDoubleMatrix2D ch = ap_chunk.getValues();
            for (int i=0; i<ch.rows(); i++) {
                for (int j=0; j<ch.columns(); j++) {
                    ap[t][j] = ch.getQuick(i, j);
                }
                t++;
            }
        }

        // Achieve rendering
        JWorldWrapper jww = new JWorldWrapper(getSampleRate(), getFramePeriod());
        AudioInputStream ais = jww.synthesis(f0, sp, ap, getAsShort());

        // Add audio to utterance
        AudioItem au_it = new AudioItem(ais);
        Sequence<AudioItem> seq_audio =new Sequence<AudioItem>();
        seq_audio.add(au_it);
        utt.addSequence(SupportedSequenceType.AUDIO, seq_audio);

        // Add F0 relation
        ArrayList<IntegerPair> al_f0_audio = new ArrayList<IntegerPair>();
        for (int i=0; i<f0_chunks.size(); i++) {
            al_f0_audio.add(new IntegerPair(i, 0));
        }
        Relation rel_f0 = new Relation(f0_chunks, seq_audio, al_f0_audio);
        utt.setRelation(getF0SeqLabel(), SupportedSequenceType.AUDIO, rel_f0);

        // Add Sectrum relation
        ArrayList<IntegerPair> al_sp_audio = new ArrayList<IntegerPair>();
        for (int i=0; i<sp_chunks.size(); i++) {
            al_sp_audio.add(new IntegerPair(i, 0));
        }
        Relation rel_sp = new Relation(sp_chunks, seq_audio, al_sp_audio);
        utt.setRelation(JWorldSupportedSequenceType.SPECTRUM, SupportedSequenceType.AUDIO, rel_sp);


        // Add aperiodicity relation
        ArrayList<IntegerPair> al_ap_audio = new ArrayList<IntegerPair>();
        for (int i=0; i<ap_chunks.size(); i++) {
            al_ap_audio.add(new IntegerPair(i, 0));
        }
        Relation rel_ap = new Relation(ap_chunks, seq_audio, al_ap_audio);
        utt.setRelation(JWorldSupportedSequenceType.APERIODICITY, SupportedSequenceType.AUDIO, rel_ap);

        return utt;
    }

    protected void setDescription()
    {
        this.description = "Module to call the world vocoder";
    }


    public String getF0SeqLabel() {
        return f0_seq_label;
    }

    public void setF0SeqLabel(String f0_seq_label) {
        this.f0_seq_label = f0_seq_label;
    }
    public double getFramePeriod() {
        return frame_period;
    }

    public void setFramePeriod(double frame_period) {
        this.frame_period = frame_period;
    }

    public int getSampleRate() {
        return samplerate;
    }

    public void setSampleRate(int samplerate) {
        this.samplerate = samplerate;
    }


    public boolean getAsShort() {
        return as_short;
    }

    public void setAsShort(boolean as_short) {
        this.as_short = as_short;
    }
}
