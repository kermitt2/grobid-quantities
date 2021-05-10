package org.grobid.trainer;

import org.apache.commons.io.IOUtils;
import org.grobid.core.engines.QuantitiesModels;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.features.FeaturesVectorUnits;
import org.grobid.core.lexicon.QuantityLexicon;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.core.utilities.OffsetPosition;
import org.grobid.core.utilities.Pair;
import org.grobid.core.utilities.UnicodeUtil;
import org.grobid.trainer.sax.UnitAnnotationSaxHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.StringUtils.lowerCase;

/**
 * Created by lfoppiano on 21.02.16.
 */
public class UnitTrainer extends AbstractTrainer {
    private static final Logger LOGGER = LoggerFactory.getLogger(UnitTrainer.class);

    private final QuantityLexicon quantityLexicon;

    public UnitTrainer() {
        super(QuantitiesModels.UNITS);
        epsilon = 0.0000001;
        window = 20;

        quantityLexicon = QuantityLexicon.getInstance();
    }

    @Override
    public int createCRFPPData(File sourcePathLabel,
                               File outputPath) {
        return createCRFPPData(sourcePathLabel, outputPath, null, 1.0);
    }

    @SuppressWarnings({"UnusedParameters"})
    private void addFeatures(List<Pair<String, String>> texts,
                             Writer writer,
                             List<OffsetPosition> unitTokenPositions, boolean isUnitLeft) {

        int posit = 0;
        List<OffsetPosition> localPositions = unitTokenPositions;

        // PL: to be reviewed, unitTokenPositions not used !
        try {
            for (Pair<String, String> text : texts) {
                String token = text.getA();

                //Unicode normalisation
                token = UnicodeUtil.normaliseTextAndRemoveSpaces(token);

                if (token.trim().equals("@newline")) {
                    writer.write("\n");
                    writer.flush();
                }

                String label = text.getB();

                FeaturesVectorUnits featuresVector =
                        FeaturesVectorUnits.addFeaturesUnit(token, label,
                                quantityLexicon.inUnitDictionary(token), quantityLexicon.inPrefixDictionary(token),
                                isUnitLeft);

                if (featuresVector.label == null) {
                    continue;
                }

                writer.write(featuresVector.printVector());
                writer.write("\n");
                writer.flush();

                posit++;
            }
        } catch (Exception e) {
            throw new GrobidException("An exception occurred while running Grobid.", e);
        }
    }

    @Override
    public int createCRFPPData(File corpusDir, File trainingOutputPath, File evalOutputPath, double splitRatio) {
        int totalExamples = 0;
        Writer trainingOutputWriter = null;
        Writer evaluationOutputWriter = null;

        try {
            LOGGER.info("sourcePathLabel: " + corpusDir);
            if (trainingOutputPath != null)
                LOGGER.info("outputPath for training data: " + trainingOutputPath);
            if (evalOutputPath != null)
                LOGGER.info("outputPath for evaluation data: " + evalOutputPath);

            // the file for writing the training data
            OutputStream os2 = null;

            if (trainingOutputPath != null) {
                os2 = new FileOutputStream(trainingOutputPath);
                trainingOutputWriter = new OutputStreamWriter(os2, UTF_8);
            }

            // the file for writing the evaluation data
            OutputStream os3 = null;

            if (evalOutputPath != null) {
                os3 = new FileOutputStream(evalOutputPath);
                evaluationOutputWriter = new OutputStreamWriter(os3, UTF_8);
            }

            // then we convert the tei files into the usual CRF label format
            // we process all tei files in the output directory
            File[] refFiles = corpusDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".tei") || name.toLowerCase().endsWith(".tei.xml"));

            if (refFiles == null) {
                return 0;
            }

            LOGGER.info(refFiles.length + " files");

            // get a factory for SAX parser
            SAXParserFactory spf = SAXParserFactory.newInstance();

            String name;

            for (int n = 0; n < refFiles.length; n++) {
                Writer writer = dispatchExample(trainingOutputWriter, evaluationOutputWriter, splitRatio);

                File thefile = refFiles[n];
                name = thefile.getName();
                LOGGER.info(name);

                UnitAnnotationSaxHandler handler = new UnitAnnotationSaxHandler();

                SAXParser p = spf.newSAXParser();
                p.parse(thefile, handler);

                List<UnitLabeled> labeledUnits = handler.getLabeledResult();

                // we need to add now the features to the labeled tokens
                int pos = 0;

                for (UnitLabeled labeledUnit : labeledUnits) {
                    List<Pair<String, String>> labels = labeledUnit.getLabels();
                    List<OffsetPosition> unitTokenPositions = new ArrayList<>();
                    OffsetPosition offsetPosition = new OffsetPosition();
                    offsetPosition.start = pos;
                    offsetPosition.end = pos + 1;
                    unitTokenPositions.add(offsetPosition);

                    addFeatures(labels, writer, unitTokenPositions, labeledUnit.hasRightAttachment());
                    writer.write("\n");
                    writer = dispatchExample(trainingOutputWriter, evaluationOutputWriter, splitRatio);
                    pos++;
                }
            }

        } catch (Exception e) {
            throw new GrobidException("An exception occured while running Grobid.", e);
        } finally {
            IOUtils.closeQuietly(evaluationOutputWriter, trainingOutputWriter);
        }
        return totalExamples;
    }

    /**
     * Command line execution. Assuming grobid-home is in ../grobid-home
     *
     * @param args Command line arguments.
     */
    public static void main(String[] args) {
        GrobidProperties.getInstance();

        Trainer trainer = new UnitTrainer();
        AbstractTrainer.runTraining(trainer);
    }
}
