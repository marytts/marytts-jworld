package marytts.jworld.data;

import marytts.data.SupportedSequenceType;

/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class JWorldSupportedSequenceType
{

    public static final String SPECTRUM = "WORLD_SPECTRUM";
    public static final String APERIODICITY = "WORLD_APERIODICITY";

    static {
        SupportedSequenceType.addSupportedType(SPECTRUM);
        SupportedSequenceType.addSupportedType(APERIODICITY);
    }
}
