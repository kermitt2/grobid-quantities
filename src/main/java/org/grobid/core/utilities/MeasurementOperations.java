package org.grobid.core.utilities;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.grobid.core.data.Measurement;
import org.grobid.core.data.Quantity;
import org.grobid.core.data.Unit;
import org.grobid.core.data.UnitDefinition;
import org.grobid.core.data.normalization.UnitNormalizer;
import org.grobid.core.layout.LayoutToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

/**
 * Measurement operations and utilities class
 */
public class MeasurementOperations {
    private static final Logger logger = LoggerFactory.getLogger(MeasurementOperations.class);

    private UnitNormalizer un;

    public MeasurementOperations() {

        un = new UnitNormalizer();
    }

    public MeasurementOperations(UnitNormalizer un) {
        this.un = un;
    }

    /**
     * Check if the given list of measures are well formed:
     * In particular, if intervals are not consistent, they are transformed
     * in atomic value measurements.
     */
    public static List<Measurement> postCorrection(List<Measurement> measurements) {
        List<Measurement> newMeasurements = new ArrayList<>();
        for (Measurement measurement : measurements) {
            if (measurement.getType() == UnitUtilities.Measurement_Type.VALUE) {
                Quantity quantity = measurement.getQuantityAtomic();
                if (quantity == null)
                    continue;
                // if the unit is too far from the value, the measurement needs to be filtered out
                int start = quantity.getOffsetStart();
                int end = quantity.getOffsetEnd();
                Unit rawUnit = quantity.getRawUnit();
                if (rawUnit != null) {
                    int startU = rawUnit.getOffsetStart();
                    int endU = rawUnit.getOffsetEnd();
                    if ((Math.abs(end - startU) < 40) || (Math.abs(endU - start) < 40)) {
                        newMeasurements.add(measurement);
                    }
                } else
                    newMeasurements.add(measurement);

            } else if ((measurement.getType() == UnitUtilities.Measurement_Type.INTERVAL_MIN_MAX) ||
                (measurement.getType() == UnitUtilities.Measurement_Type.INTERVAL_BASE_RANGE)) {
                // values of the interval do not matter if min/max or base/range
                Quantity quantityLeast = measurement.getQuantityLeast();
                if (quantityLeast == null)
                    quantityLeast = measurement.getQuantityBase();
                Quantity quantityMost = measurement.getQuantityMost();
                if (quantityMost == null)
                    quantityMost = measurement.getQuantityRange();

                if ((quantityLeast == null) && (quantityMost == null))
                    continue;
                /*if (((quantityLeast != null) && (quantityMost == null)) || ((quantityLeast == null) && (quantityMost != null))) {
                    Measurement newMeasurement = new Measurement(UnitUtilities.Measurement_Type.VALUES);
                    Quantity quantity = null;
                    if (quantityLeast != null) {
                        quantity = quantityLeast;
                    } else if (quantityMost != null) {
                        quantity = quantityMost;
                    }
                    if (quantity != null) {
                        newMeasurement.setAtomicQuantity(quantity);
                        newMeasurements.add(newMeasurement);
                    }
                } else*/
                if ((quantityLeast != null) && (quantityMost != null)) {
                    // if the interval is expressed over a chunck of text which is too large, it is a recognition error
                    // and we can replace it by two atomic measurements
                    int startL = quantityLeast.getOffsetStart();
                    int endL = quantityLeast.getOffsetEnd();
                    int startM = quantityMost.getOffsetStart();
                    int endM = quantityMost.getOffsetEnd();

                    if ((Math.abs(endL - startM) > 80) && (Math.abs(endM - startL) > 80)) {
                        // we replace the interval measurement by two atomic measurements
                        Measurement newMeasurement = new Measurement(UnitUtilities.Measurement_Type.VALUE);
                        // have to check the position of value and unit for valid atomic measure
                        Unit rawUnit = quantityLeast.getRawUnit();
                        if (rawUnit != null) {
                            int startU = rawUnit.getOffsetStart();
                            int endU = rawUnit.getOffsetEnd();
                            if ((Math.abs(endL - startU) < 40) || (Math.abs(endU - startL) < 40)) {
                                newMeasurement.setAtomicQuantity(quantityLeast);
                                newMeasurements.add(newMeasurement);
                            }
                        } else {
                            newMeasurement.setAtomicQuantity(quantityLeast);
                            newMeasurements.add(newMeasurement);
                        }

                        newMeasurement = new Measurement(UnitUtilities.Measurement_Type.VALUE);
                        rawUnit = quantityMost.getRawUnit();
                        if (rawUnit != null) {
                            int startU = rawUnit.getOffsetStart();
                            int endU = rawUnit.getOffsetEnd();
                            if ((Math.abs(endM - startU) < 40) || (Math.abs(endU - startM) < 40)) {
                                newMeasurement.setAtomicQuantity(quantityMost);
                                newMeasurements.add(newMeasurement);
                            }
                        } else {
                            newMeasurement.setAtomicQuantity(quantityMost);
                            newMeasurements.add(newMeasurement);
                        }

                    } else {
                        newMeasurements.add(measurement);
                    }
                } else {
                    newMeasurements.add(measurement);
                }
            } else if (measurement.getType() == UnitUtilities.Measurement_Type.CONJUNCTION) {
                // list must be consistent in unit type, and avoid too large chunk
                List<Quantity> quantities = measurement.getQuantityList();
                if ((quantities == null) || (quantities.size() == 0))
                    continue;

                /* // case actually not seen
                Unit currentUnit = null;
                Measurement newMeasurement = new Measurement(UnitUtilities.Measurement_Type.CONJUNCTION);
                for(Quantity quantity : quantities) {
                    if (currentUnit == null)
                        currentUnit = quantity.getRawUnit();
                    else {
                        Unit newUnit = quantity.getRawUnit();

                        // is it a new unit?
                        if ((currentUnit != null) && (newUnit != null) && (!currentUnit.getRawName().equals(newUnit.getRawName())) ) {
                             // we have a new unit, so we split the list
                            if ( (newMeasurement != null) && (newMeasurement.getQuantities() != null) && (newMeasurement.getQuantities().size() > 0) )
                                newMeasurements.add(newMeasurement);
                            newMeasurement = new Measurement(UnitUtilities.Measurement_Type.CONJUNCTION);
                            newMeasurement.addQuantity(quantity);
                            currentUnit = newUnit;
                        }
                        else {
                            // same unit, we extend the current list
                            newMeasurement.addQuantity(quantity);
                        }
                    }

                }
                if ( (newMeasurement != null) && (newMeasurement.getQuantities() != null) && (newMeasurement.getQuantities().size() > 0) )
                    newMeasurements.add(newMeasurement);*/

                // the case of atomic values within list should be cover here
                // in this case, we have a list followed by an atomic value, then a following list without unit attachment, with possibly
                // only one quantity - the correction is to extend the starting list with the remaining list after the atomic value, attaching
                // the unit associated to the starting list to the added quantities


                newMeasurements.add(measurement);
            }
        }

        return newMeasurements;
    }


    /**
     * Right now, only basic matching of units based on lexicon look-up and value validation
     * via regex.
     */
    public List<Measurement> resolveMeasurement(List<Measurement> measurements) {
        for (Measurement measurement : measurements) {
            if (measurement.getType() == null)
                continue;
            else if (measurement.getType() == UnitUtilities.Measurement_Type.VALUE) {
                updateQuantity(measurement.getQuantityAtomic());
            } else if (measurement.getType() == UnitUtilities.Measurement_Type.INTERVAL_MIN_MAX) {
                updateQuantity(measurement.getQuantityLeast());
                updateQuantity(measurement.getQuantityMost());
            } else if (measurement.getType() == UnitUtilities.Measurement_Type.INTERVAL_BASE_RANGE) {
                updateQuantity(measurement.getQuantityBase());
                updateQuantity(measurement.getQuantityRange());
                // the two quantities below are normally not yet set-up
                //updateQuantity(measurement.getQuantityLeast());
                //updateQuantity(measurement.getQuantityMost());
            } else if (measurement.getType() == UnitUtilities.Measurement_Type.CONJUNCTION) {
                if (measurement.getQuantityList() != null) {
                    for (Quantity quantity : measurement.getQuantityList()) {
                        if (quantity == null)
                            continue;
                        updateQuantity(quantity);
                    }
                }
            }
        }
        return measurements;
    }

    private void updateQuantity(Quantity quantity) {
        if ((quantity != null) && (!quantity.isEmpty())) {
            Unit rawUnit = quantity.getRawUnit();
            UnitDefinition foundUnit = un.findDefinition(rawUnit);

            if (foundUnit != null) {
                rawUnit.setUnitDefinition(foundUnit);
            }
        }
    }

    public static int WINDOW_TC = 20;

    /* We work with offsets (so no need to increase by size of the text) and we return indexes in the token list */
    protected Pair<Integer, Integer> getExtremitiesAsIndex(List<LayoutToken> tokens, int centroidOffsetLower, int centroidOffsetHigher) {
        return getExtremitiesAsIndex(tokens, centroidOffsetLower, centroidOffsetHigher, WINDOW_TC);
    }


    protected Pair<Integer, Integer> getExtremitiesAsIndex(List<LayoutToken> tokens, int centroidOffsetLower, int centroidOffsetHigher, int windowlayoutTokensSize) {
        int start = 0;
        int end = tokens.size() - 1;

        List<LayoutToken> centralTokens = tokens.stream().filter(layoutToken -> layoutToken.getOffset() == centroidOffsetLower || (layoutToken.getOffset() > centroidOffsetLower && layoutToken.getOffset() < centroidOffsetHigher)).collect(Collectors.toList());

        if (isNotEmpty(centralTokens)) {
            int centroidLayoutTokenIndexStart = tokens.indexOf(centralTokens.get(0));
            int centroidLayoutTokenIndexEnd = tokens.indexOf(centralTokens.get(centralTokens.size() - 1));

            if (centroidLayoutTokenIndexStart > windowlayoutTokensSize) {
                start = centroidLayoutTokenIndexStart - windowlayoutTokensSize;
            }
            if (end - centroidLayoutTokenIndexEnd > windowlayoutTokensSize) {
                end = centroidLayoutTokenIndexEnd + windowlayoutTokensSize + 1;
            }
        }

        return new ImmutablePair(start, end);
    }

    /**
     * Return measurement extremities as the index of the layout token. It's useful if we want to combine
     * measurement and layoutToken content.
     */
    public Pair<Integer, Integer> calculateQuantityExtremities(List<LayoutToken> tokens, Measurement measurement) {
        Pair<Integer, Integer> extremities = null;
        switch (measurement.getType()) {
            case VALUE:
                Quantity quantity = measurement.getQuantityAtomic();
                List<LayoutToken> layoutTokens = quantity.getLayoutTokens();

                extremities = getExtremitiesAsIndex(tokens, layoutTokens.get(0).getOffset(), layoutTokens.get(layoutTokens.size() - 1).getOffset());

                break;
            case INTERVAL_BASE_RANGE:
                if (measurement.getQuantityBase() != null && measurement.getQuantityRange() != null) {
                    Quantity quantityBase = measurement.getQuantityBase();
                    Quantity quantityRange = measurement.getQuantityRange();

                    extremities = getExtremitiesAsIndex(tokens, quantityBase.getLayoutTokens().get(0).getOffset(), quantityRange.getLayoutTokens().get(quantityRange.getLayoutTokens().size() - 1).getOffset());
                } else {
                    Quantity quantityTmp;
                    if (measurement.getQuantityBase() == null) {
                        quantityTmp = measurement.getQuantityRange();
                    } else {
                        quantityTmp = measurement.getQuantityBase();
                    }

                    extremities = getExtremitiesAsIndex(tokens, quantityTmp.getLayoutTokens().get(0).getOffset(), quantityTmp.getLayoutTokens().get(0).getOffset());
                }

                break;

            case INTERVAL_MIN_MAX:
                if (measurement.getQuantityLeast() != null && measurement.getQuantityMost() != null) {
                    Quantity quantityLeast = measurement.getQuantityLeast();
                    Quantity quantityMost = measurement.getQuantityMost();

                    extremities = getExtremitiesAsIndex(tokens, quantityLeast.getLayoutTokens().get(0).getOffset(), quantityMost.getLayoutTokens().get(quantityMost.getLayoutTokens().size() - 1).getOffset());
                } else {
                    Quantity quantityTmp;
                    if (measurement.getQuantityLeast() == null) {
                        quantityTmp = measurement.getQuantityMost();
                    } else {
                        quantityTmp = measurement.getQuantityLeast();
                    }

                    extremities = getExtremitiesAsIndex(tokens, quantityTmp.getLayoutTokens().get(0).getOffset(), quantityTmp.getLayoutTokens().get(quantityTmp.getLayoutTokens().size() - 1).getOffset());
                }
                break;

            case CONJUNCTION:
                List<Quantity> quantityList = measurement.getQuantityList();
                if (quantityList.size() > 1) {
                    extremities = getExtremitiesAsIndex(tokens, quantityList.get(0).getLayoutTokens().get(0).getOffset(), quantityList.get(quantityList.size() - 1).getLayoutTokens().get(0).getOffset());
                } else {
                    extremities = getExtremitiesAsIndex(tokens, quantityList.get(0).getLayoutTokens().get(0).getOffset(), quantityList.get(0).getLayoutTokens().get(0).getOffset());
                }

                break;
        }
        return extremities;
    }

    /**
     * Return the offset of the measurement - useful for matching into sentences or lexicons
     */
    public OffsetPosition calculateExtremitiesOffsets(Measurement measurement) {
        List<OffsetPosition> offsets = new ArrayList<>();

        switch (measurement.getType()) {
            case VALUE:
                CollectionUtils.addAll(offsets, QuantityOperations.getOffsets(measurement.getQuantityAtomic()));
                break;
            case INTERVAL_BASE_RANGE:
                CollectionUtils.addAll(offsets, QuantityOperations.getOffsets(measurement.getQuantityBase()));
                CollectionUtils.addAll(offsets, QuantityOperations.getOffsets(measurement.getQuantityRange()));

                break;
            case INTERVAL_MIN_MAX:
                CollectionUtils.addAll(offsets, QuantityOperations.getOffsets(measurement.getQuantityLeast()));
                CollectionUtils.addAll(offsets, QuantityOperations.getOffsets(measurement.getQuantityMost()));
                break;

            case CONJUNCTION:
                CollectionUtils.addAll(offsets, QuantityOperations.getOffsets(measurement.getQuantityList()));

                break;
        }
        return QuantityOperations.getContainingOffset(offsets);
    }

}