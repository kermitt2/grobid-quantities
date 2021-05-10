package org.grobid.core.data.normalization;

import javax.measure.format.MeasurementParseException;

/**
 * Created by lfoppiano on 15.02.16.
 */
public class NormalizationException extends Exception {
    public NormalizationException(String s) {
        super(s);
    }

    public NormalizationException(String s, MeasurementParseException pe) {
        super(s, pe);
    }

    public NormalizationException(String s, Exception e) {
        super(s, e);
    }
}
