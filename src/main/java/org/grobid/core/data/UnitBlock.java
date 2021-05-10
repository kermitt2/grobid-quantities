package org.grobid.core.data;

import org.apache.commons.lang3.StringUtils;

import java.util.List;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

/**
 * Created by lfoppiano on 08.03.16.
 */
public class UnitBlock {
    private String prefix = "";
    private String base = "";
    private String pow = "";

    /** this value represent the raw tagging of the whole sequence, not only of this unit block
     * Yes it's a workaround but there is no other way to carry this information further without creating a new object
     * in replacement of the List<UnitBlock>
     * **/
    private String rawTaggedValue;


    public UnitBlock(String prefix, String base, String pow) {
        setPrefix(prefix);
        setBase(base);
        setPow(pow);
    }

    public UnitBlock(String base) {
        setBase(base);
    }

    public UnitBlock() {

    }


    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        if (prefix != null) {
            this.prefix = prefix;
        }
    }

    public String getBase() {
        return base;
    }

    public void setBase(String base) {
        if (base != null) {
            this.base = base;
        }
    }

    public String getPow() {
        return pow;
    }

    public void setPow(String pow) {
        if (pow != null) {
            this.pow = pow;
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getPrefix());
        sb.append(getBase());
        if (!getPow().equals("")) {
            sb.append("^");
            sb.append(getPow());
        }

        return sb.toString();
    }

    public String toXml() {
        StringBuilder sb = new StringBuilder();

        if (isNotEmpty(getPrefix())) {
            sb.append("<prefix>" + getPrefix() + "</prefix>");
        }
        if (isNotEmpty(getBase())) {
            sb.append("<base>" + getBase() + "</base>");
        }
        if (isNotEmpty(getPow())) {
            sb.append("<pow>" + getPow() + "</pow>");
        }

        return sb.toString();
    }

    public static String asProduct(List<UnitBlock> unitBlockList) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (UnitBlock ub : unitBlockList) {
            //It should not happens but if some multiplication sign are slipping out we should replace them 
            ub.setBase(ub.getBase().replace("•", ""));
            ub.setPow(ub.getPow().replace("•", ""));
            ub.setPrefix(ub.getPrefix().replace("•", ""));
            if (!first) {
                sb.append("·");
            } else {
                first = false;
            }
            sb.append(ub.toString());
        }

        return sb.toString();
    }

    public static String asString(List<UnitBlock> unitBlockList) {
        StringBuilder numerator = new StringBuilder();
        StringBuilder denominator = new StringBuilder();
        boolean firstNumerator = true;
        boolean firstDenominator = true;
        boolean fraction = false;
        for (UnitBlock ub : unitBlockList) {
            if (!ub.getPow().equals("0")) {
                if (ub.getPow().contains("-")) {
                    fraction = true;
                    String den = ub.toString().replace("-", "");
                    if (den.endsWith("^1")) {
                        den = den.replace("^1", "");
                    }

                    if (!firstDenominator) {
                        denominator.append("·");
                    } else {
                        firstDenominator = false;
                    }
                    denominator.append(den);
                } else {
                    if (!firstNumerator) {
                        numerator.append("·");
                    } else {
                        firstNumerator = false;
                    }

                    numerator.append(ub.toString());
                }
            }
        }

        if (fraction) {
            return numerator.append("/").append(denominator.toString()).toString();
        } else {
            return numerator.toString();
        }
    }


    public static String asString(UnitBlock ub) {
        StringBuilder output = new StringBuilder();

        if (StringUtils.isNotEmpty(ub.getPow())) {
            if (ub.getPow().contains("−")) {
                output.append("1/");
            }
        }

        if (StringUtils.isNotEmpty(ub.getPrefix())) {
            output.append(ub.getPrefix());
        }
        if (StringUtils.isNotEmpty(ub.getBase())) {
            output.append(ub.getBase());
        }

        return output.toString();
    }

    public void setRawTaggedValue(String rawTaggedValue) {
        this.rawTaggedValue = rawTaggedValue;
    }

    public String getRawTaggedValue() {
        return rawTaggedValue;
    }
}
