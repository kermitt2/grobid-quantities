package org.grobid.core.data;

import com.fasterxml.jackson.core.io.JsonStringEncoder;
import com.fasterxml.jackson.core.util.BufferRecyclers;
import org.grobid.core.utilities.OffsetPosition;

import java.math.BigDecimal;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

/**
 * This class represent the parsed value
 */
public class Value {
    private String rawValue = "";
    private OffsetPosition offsets = new OffsetPosition();
    private ValueBlock structure = null;
    private BigDecimal numeric = null;

    public Value(BigDecimal numeric) {
        this.numeric = numeric;
    }

    public Value() {
    }

    public String getRawValue() {
        return rawValue;
    }

    public void setRawValue(String rawValue) {
        this.rawValue = rawValue;
    }

    public void setOffsetStart(int start) {
        if (offsets == null)
            offsets = new OffsetPosition();
        offsets.start = start;
    }

    public int getOffsetStart() {
        if (offsets != null)
            return offsets.start;
        else
            return -1;
    }

    public void setOffsetEnd(int end) {
        if (offsets == null)
            offsets = new OffsetPosition();
        offsets.end = end;
    }

    public int getOffsetEnd() {
        if (offsets != null)
            return offsets.end;
        else
            return -1;
    }

    public OffsetPosition getOffsets() {
        return offsets;
    }

    public void setOffsets(OffsetPosition offsets) {
        this.offsets = offsets;
    }

    public ValueBlock getStructure() {
        return structure;
    }

    public void setStructure(ValueBlock structure) {
        this.structure = structure;
    }

    public BigDecimal getNumeric() {
        return numeric;
    }

    public void setNumeric(BigDecimal numeric) {
        this.numeric = numeric;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Value{");
        sb.append("rawValue='").append(rawValue).append('\'');
        sb.append(", structure=").append(structure);
        sb.append(", numeric=").append(numeric);
        sb.append('}');
        return sb.toString();
    }

    public String toJson() {
        JsonStringEncoder encoder = JsonStringEncoder.getInstance();
        StringBuilder json = new StringBuilder();
        boolean started = false;
        json.append("{ ");
        if (isNotEmpty(rawValue)) {
            byte[] encodedRawName = encoder.quoteAsUTF8(rawValue);
            String outputRawName = new String(encodedRawName);
            if (!started) {
                started = true;
            } else
                json.append(", ");
            json.append("\"name\" : \"" + outputRawName + "\"");
        }

        if (getNumeric() != null) {
            if (!started) {
                started = true;
            } else
                json.append(", ");
            json.append("\"numeric\" : " + getNumeric());
        }

        if (getStructure() != null) {
            if (!started) {
                started = true;
            } else
                json.append(", ");
            json.append("\"structure\" : " + getStructure().toJson() + ", ");
            byte[] encodedParsedName = encoder.quoteAsUTF8(getStructure().toString());
            String outputParsedName = new String(encodedParsedName);
            json.append("\"parsed\" : \"" + outputParsedName + "\"");
        }

        if (offsets != null) {
            if (getOffsetStart() != -1) {
                if (!started) {
                    started = true;
                } else
                    json.append(", ");
                json.append("\"offsetStart\" : " + getOffsetStart());
            }
            if (getOffsetEnd() != -1) {
                if (!started) {
                    started = true;
                } else
                    json.append(", ");
                json.append("\"offsetEnd\" : " + getOffsetEnd());
            }
        }

        json.append(" }");
        return json.toString();
    }


}
