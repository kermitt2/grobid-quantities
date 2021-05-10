package org.grobid.core.engines;

import org.apache.commons.lang3.StringUtils;
import org.grobid.core.GrobidModel;
import org.grobid.core.analyzers.QuantityAnalyzer;
import org.grobid.core.data.Measurement;
import org.grobid.core.data.QuantifiedObject;
import org.grobid.core.engines.label.TaggingLabel;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.features.FeaturesVectorQuantifiedObjects;
import org.grobid.core.layout.BoundingBox;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.tokenization.TaggingTokenCluster;
import org.grobid.core.tokenization.TaggingTokenClusteror;
import org.grobid.core.utilities.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.grobid.core.engines.label.QuantitiesTaggingLabels.*;
import static org.grobid.core.utilities.QuantityOperations.synchroniseLayoutTokensWithOffsets;

/**
 * Parser for identifying and attaching the quantified "substance".
 */
public class QuantifiedObjectParser extends AbstractParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(QuantifiedObjectParser.class);

    private static volatile QuantifiedObjectParser instance;

    public static QuantifiedObjectParser getInstance() {
        if (instance == null) {
            getNewInstance();
        }
        return instance;
    }

    private static synchronized void getNewInstance() {
        instance = new DefaultQuantifiedObjectParser();
    }

    protected QuantifiedObjectParser() {
        super(QuantitiesModels.QUANTIFIED_OBJECT);
    }

    /** Workaround to maintain the compatibility for the time being **/
    protected QuantifiedObjectParser(GrobidModel model) {
        super(model);
    }

    public List<Measurement> process(List<LayoutToken> layoutTokens, List<Measurement> measurements) {

        List<Measurement> newMeasurements = new ArrayList<>();
        Collections.copy(measurements, newMeasurements);

        // List<LayoutToken> for the selected segment
        List<LayoutToken> retokenizeLayoutTokens = QuantityAnalyzer.getInstance().retokenizeLayoutTokens(layoutTokens);

        //Normalisation
        List<LayoutToken> layoutTokenNormalised = retokenizeLayoutTokens
                .stream()
                .map(layoutToken -> {
                            layoutToken.setText(UnicodeUtil.normaliseText(layoutToken.getText()));

                            return layoutToken;
                        }
                ).collect(Collectors.toList());

        if (isEmpty(layoutTokenNormalised))
            return measurements;

        try {
            List<OffsetPosition> offsetList = QuantityOperations.getOffset(measurements);

            List<Boolean> measurementFlags = synchroniseLayoutTokensWithOffsets(layoutTokenNormalised, offsetList);

            String ress = addFeatures(layoutTokenNormalised, measurementFlags);

            if (StringUtils.isEmpty(ress))
                return newMeasurements;

            String res = null;
            try {
                res = label(ress);
            } catch (Exception e) {
                throw new GrobidException("CRF labeling for quantified object parsing failed.", e);
            }

            List<QuantifiedObject> quantifiedObjects = extractOutput(res, layoutTokenNormalised);

            LOGGER.info("Found " + quantifiedObjects.size() + " quantified objects!");

            measurements = attachQuantifiedObjects(quantifiedObjects, measurements);

            return measurements;

        } catch (Exception e) {
            throw new GrobidException("An exception occured while running Grobid.", e);
        }
    }

    public List<Measurement> attachQuantifiedObjects(List<QuantifiedObject> quantifiedObjects, List<Measurement> measurements) {
        if (isEmpty(quantifiedObjects))
            return measurements;

        int indexQuantifiedObject = 0;
        QuantifiedObject currentQuantifiedObject = quantifiedObjects.get(indexQuantifiedObject);

        for (Measurement measurement : measurements) {
            List<OffsetPosition> offsetList1 = QuantityOperations.getOffset(measurement);
            OffsetPosition offsetMeasurement = QuantityOperations.getContainingOffset(offsetList1);

            if (currentQuantifiedObject.getAttachment() == null) {
                indexQuantifiedObject++;
                currentQuantifiedObject = quantifiedObjects.get(indexQuantifiedObject);
            }
            if (currentQuantifiedObject.getAttachment().equals(QuantifiedObject.Attachment.LEFT)) {
                if (offsetMeasurement.start <= currentQuantifiedObject.getOffsetStart()) {
                    measurement.setQuantifiedObject(currentQuantifiedObject);
                } else {
                    continue;
                }

            } else if (currentQuantifiedObject.getAttachment().equals(QuantifiedObject.Attachment.RIGHT)) {
                if (offsetMeasurement.end < currentQuantifiedObject.getOffsetStart()) {
                    continue;
                } else {
                    measurement.setQuantifiedObject(currentQuantifiedObject);
                }
            }

            indexQuantifiedObject++;
            if (indexQuantifiedObject < quantifiedObjects.size()) {
                currentQuantifiedObject = quantifiedObjects.get(indexQuantifiedObject);
            } else {
                break;
            }

        }

        return measurements;
    }

    protected List<QuantifiedObject> extractOutput(String result, List<LayoutToken> tokens) {
        List<QuantifiedObject> quantifiedObjects = new ArrayList<>();

        TaggingTokenClusteror clusteror = new TaggingTokenClusteror(QuantitiesModels.QUANTIFIED_OBJECT, result, tokens);
        List<TaggingTokenCluster> clusters = clusteror.cluster();

        for (TaggingTokenCluster cluster : clusters) {
            if (cluster == null) {
                continue;
            }

            TaggingLabel clusterLabel = cluster.getTaggingLabel();
            List<LayoutToken> theTokens = cluster.concatTokens();
            String clusterContent = LayoutTokensUtil.toText(theTokens).trim();
            List<BoundingBox> boundingBoxes = null;

            if (!clusterLabel.equals(QUANTIFIED_OBJECT_OTHER))
                boundingBoxes = BoundingBoxCalculator.calculate(theTokens);

            int offsetStart = theTokens.get(0).getOffset();
            int offsetEnd = offsetStart + clusterContent.length();

            if (clusterLabel.equals(QUANTIFIED_OBJECT_LEFT)) {
                QuantifiedObject quantifiedObject = new QuantifiedObject(clusterContent);
                quantifiedObject.setOffsetStart(offsetStart);
                quantifiedObject.setOffsetEnd(offsetEnd);
                quantifiedObject.setLayoutTokens(theTokens);
                quantifiedObject.setBoundingBoxes(boundingBoxes);
                quantifiedObject.setAttachment(QuantifiedObject.Attachment.LEFT);

                quantifiedObjects.add(quantifiedObject);

            } else if (clusterLabel.equals(QUANTIFIED_OBJECT_RIGHT)) {
                QuantifiedObject quantifiedObject = new QuantifiedObject(clusterContent);
                quantifiedObject.setOffsetStart(offsetStart);
                quantifiedObject.setOffsetEnd(offsetEnd);
                quantifiedObject.setLayoutTokens(theTokens);
                quantifiedObject.setBoundingBoxes(boundingBoxes);
                quantifiedObject.setAttachment(QuantifiedObject.Attachment.RIGHT);

                quantifiedObjects.add(quantifiedObject);

            } else if (clusterLabel.equals(QUANTIFIED_OBJECT_OTHER)) {

            } else {
                LOGGER.error("Unexpected label in quantified object parser: " + clusterLabel.getLabel() + " for " + clusterContent);
            }
        }

        return quantifiedObjects;
    }

    private String addFeatures(List<LayoutToken> tokens, List<Boolean> isMeasurement) {
        StringBuilder result = new StringBuilder();
        try {

            ListIterator<LayoutToken> it = tokens.listIterator();
            while (it.hasNext()) {
                int index = it.nextIndex();
                LayoutToken token = it.next();

                if (token.getText().trim().equals("@newline")) {
                    result.append("\n");
                    continue;
                }

                String text = token.getText();
                if (text.equals(" ") || text.equals("\n")) {
                    continue;
                }

                FeaturesVectorQuantifiedObjects featuresVector =
                        FeaturesVectorQuantifiedObjects.addFeatures(text, null, isMeasurement.get(index));
                result.append(featuresVector.printVector());
                result.append("\n");
            }
        } catch (Exception e) {
            throw new GrobidException("An exception occurred while running Grobid.", e);
        }
        return result.toString();
    }

}