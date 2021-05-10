package org.grobid.core.engines;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterables;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.grobid.core.GrobidModels;
import org.grobid.core.analyzers.GrobidAnalyzer;
import org.grobid.core.data.*;
import org.grobid.core.document.Document;
import org.grobid.core.document.DocumentPiece;
import org.grobid.core.document.DocumentSource;
import org.grobid.core.engines.config.GrobidAnalysisConfig;
import org.grobid.core.engines.label.SegmentationLabels;
import org.grobid.core.engines.label.TaggingLabels;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.layout.LayoutTokenization;
import org.grobid.core.tokenization.LabeledTokensContainer;
import org.grobid.core.tokenization.TaggingTokenCluster;
import org.grobid.core.tokenization.TaggingTokenClusteror;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.core.utilities.IOUtilities;
import org.grobid.core.utilities.UnitUtilities;
import org.grobid.service.exceptions.GrobidServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.Response;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

/**
 * This class represent the aggregated processing applying multiple parsers or combining PDF extraction
 */
@Singleton
public class QuantitiesEngine {

    private static final Logger LOGGER = LoggerFactory.getLogger(QuantitiesEngine.class);

    private QuantityParser quantityParser;
    private UnitParser unitParser;
    private EngineParsers parsers;

    private static QuantitiesEngine instance;

    @Inject
    public QuantitiesEngine() {
        GrobidProperties.getInstance();
        this.quantityParser = QuantityParser.getInstance();
        this.unitParser = UnitParser.getInstance();
        this.parsers = new EngineParsers();
        instance = this;
    }

    public static QuantitiesEngine getInstance() {
        if (instance == null) {
            instance = getNewInstance();
        }
        return instance;
    }

    private static synchronized QuantitiesEngine getNewInstance() {
        return new QuantitiesEngine();
    }

    public List<UnitBlock> parseUnits(String text) {
        return unitParser.tagUnit(text);
    }

    public MeasurementsResponse processPdf(InputStream inputStream) {
        long start = System.currentTimeMillis();

        List<Measurement> measurements = new ArrayList<>();
        Document doc;
        File originFile = null;
        try {
            originFile = IOUtilities.writeInputFile(inputStream);
            if (originFile == null) {
                throw new GrobidServiceException("Input file is empty or null", Response.Status.BAD_REQUEST);
            }
            GrobidAnalysisConfig config =
                    new GrobidAnalysisConfig.GrobidAnalysisConfigBuilder()
                            .analyzer(GrobidAnalyzer.getInstance())
                            .consolidateHeader(0)
                            .consolidateCitations(0)
                            .build();
            DocumentSource documentSource =
                    DocumentSource.fromPdf(originFile);

            doc = parsers.getSegmentationParser().processing(documentSource, config);

            // In the following, we process the relevant textual content of the document

            // for refining the process based on structures, we need to filter
            // segment of interest (e.g. header, body, annex) and possibly apply
            // the corresponding model to further filter by structure types

            // from the header, we are interested in title, abstract and keywords
            SortedSet<DocumentPiece> documentParts = doc.getDocumentPart(SegmentationLabels.HEADER);
            if (documentParts != null) {
                Pair<String, List<LayoutToken>> headerStruct = parsers.getHeaderParser().getSectionHeaderFeatured(doc, documentParts);
                List<LayoutToken> tokenizationHeader = headerStruct.getRight();//doc.getTokenizationParts(documentParts, doc.getTokenizations());
                String header = headerStruct.getLeft();
                String labeledResult = null;
                if ((header != null) && (header.trim().length() > 0)) {
                    labeledResult = parsers.getHeaderParser().label(header);

                    BiblioItem resHeader = new BiblioItem();
                    //parsers.getHeaderParser().processingHeaderSection(false, doc, resHeader);
                    resHeader.generalResultMapping(doc, labeledResult, tokenizationHeader);

                    // title
                    List<LayoutToken> titleTokens = resHeader.getLayoutTokens(TaggingLabels.HEADER_TITLE);
                    if (titleTokens != null) {
                        measurements.addAll(quantityParser.process(titleTokens));
                    }

                    // abstract
                    List<LayoutToken> abstractTokens = resHeader.getLayoutTokens(TaggingLabels.HEADER_ABSTRACT);
                    if (abstractTokens != null) {
                        measurements.addAll(quantityParser.process(abstractTokens));
                    }

                    // keywords
                    List<LayoutToken> keywordTokens = resHeader.getLayoutTokens(TaggingLabels.HEADER_KEYWORD);
                    if (keywordTokens != null) {
                        measurements.addAll(quantityParser.process(keywordTokens));
                    }
                }
            }

            // we can process all the body, in the future figure and table could be the
            // object of more refined processing
            documentParts = doc.getDocumentPart(SegmentationLabels.BODY);
            if (documentParts != null) {
                Pair<String, LayoutTokenization> featSeg = parsers.getFullTextParser().getBodyTextFeatured(doc, documentParts);

                String fulltextTaggedRawResult = null;
                if (featSeg != null) {
                    String featureText = featSeg.getLeft();
                    LayoutTokenization layoutTokenization = featSeg.getRight();

                    if (StringUtils.isNotEmpty(featureText)) {
                        fulltextTaggedRawResult = parsers.getFullTextParser().label(featureText);
                    }

                    TaggingTokenClusteror clusteror = new TaggingTokenClusteror(GrobidModels.FULLTEXT, fulltextTaggedRawResult,
                            layoutTokenization.getTokenization(), true);

                    //Iterate and exclude figures and tables
                    for (TaggingTokenCluster cluster : Iterables.filter(clusteror.cluster(),
                            new TaggingTokenClusteror
                                    .LabelTypeExcludePredicate(TaggingLabels.TABLE_MARKER, TaggingLabels.EQUATION, TaggingLabels.CITATION_MARKER,
                                    TaggingLabels.FIGURE_MARKER, TaggingLabels.EQUATION_MARKER, TaggingLabels.EQUATION_LABEL))) {

                        if (cluster.getTaggingLabel().equals(TaggingLabels.FIGURE)) {
                            //apply the figure model to only get the caption
                            final Figure processedFigure = parsers.getFigureParser()
                                    .processing(cluster.concatTokens(), cluster.getFeatureBlock());
                            measurements.addAll(quantityParser.process(processedFigure.getCaptionLayoutTokens()));
                        } else if (cluster.getTaggingLabel().equals(TaggingLabels.TABLE)) {
                            //apply the table model to only get the caption/description
                            final Table processedTable = parsers.getTableParser().processing(cluster.concatTokens(), cluster.getFeatureBlock());
                            measurements.addAll(quantityParser.process(processedTable.getFullDescriptionTokens()));
                        } else {
                            final List<LabeledTokensContainer> labeledTokensContainers = cluster.getLabeledTokensContainers();

                            // extract all the layout tokens from the cluster as a list
                            List<LayoutToken> tokens = labeledTokensContainers.stream()
                                    .map(LabeledTokensContainer::getLayoutTokens)
                                    .flatMap(List::stream)
                                    .collect(Collectors.toList());

                            measurements.addAll(quantityParser.process(tokens));
                        }

                    }
                }
            }

            // we don't process references (although reference titles could be relevant)
            // acknowledgement?

            // we can process annexes
            documentParts = doc.getDocumentPart(SegmentationLabels.ANNEX);
            if (documentParts != null) {
                measurements.addAll(processDocumentPart(documentParts, doc));
            }
        } catch (NoSuchElementException nseExp) {
            throw new GrobidServiceException("Could not get an instance of parser. ", Response.Status.SERVICE_UNAVAILABLE);
        } finally {
            IOUtilities.removeTempFile(originFile);
        }

        // for next line, comparable measurement needs to be implemented
        //Collections.sort(measurements);

        MeasurementsResponse measurementsResponse = new MeasurementsResponse(measurements, doc.getPages());
        long end = System.currentTimeMillis();
        measurementsResponse.setRuntime(end - start);

        return measurementsResponse;
    }

    /**
     * Process with the quantity model a segment coming from the segmentation model
     */
    private List<Measurement> processDocumentPart(SortedSet<DocumentPiece> documentParts,
                                                  Document doc) {
        // List<LayoutToken> for the selected segment
        List<LayoutToken> layoutTokens
                = doc.getTokenizationParts(documentParts, doc.getTokenizations());
        return quantityParser.process(layoutTokens);
    }

    public List<Measurement> parseMeasurement(String json) {

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        JsonNode jsonAnnotation = null;
        try {
            jsonAnnotation = mapper.readTree(json);
        } catch (IOException ex) {
            throw new GrobidServiceException("Cannot parse input JSON. ", Response.Status.BAD_REQUEST);
        }

        if ((jsonAnnotation == null) || (jsonAnnotation.isMissingNode())) {
            throw new GrobidServiceException("The request is invalid or malformed.", Response.Status.BAD_REQUEST);
        }

        String fromValue = null;
        String toValue = null;
        String unitValue = null;
        String typeValue = null;
        JsonNode from = jsonAnnotation.findPath("from");
        if ((from != null) && (!from.isMissingNode()))
            fromValue = from.textValue();

        JsonNode to = jsonAnnotation.findPath("to");
        if ((to != null) && (!to.isMissingNode()))
            toValue = to.textValue();

        JsonNode unit = jsonAnnotation.findPath("unit");
        if ((unit != null) && (!unit.isMissingNode()))
            unitValue = unit.textValue();

        JsonNode type = jsonAnnotation.findPath("type");
        if ((type != null) && (!type.isMissingNode()))
            typeValue = type.textValue();

        UnitUtilities.Measurement_Type theType = null;
        String atomicValue = null;
        if (((fromValue == null) || (fromValue.length() == 0)) &&
                ((toValue == null) || (toValue.length() == 0))) {
            throw new GrobidServiceException("The input JSON is empty or null.", Response.Status.NO_CONTENT);
        } else if ((fromValue == null) || (fromValue.length() == 0)) {
            atomicValue = toValue;
            theType = UnitUtilities.Measurement_Type.VALUE;
        } else if ((toValue == null) || (toValue.length() == 0)) {
            atomicValue = fromValue;
            theType = UnitUtilities.Measurement_Type.VALUE;
        } else
            theType = UnitUtilities.Measurement_Type.INTERVAL_MIN_MAX;

        Measurement measurement = new Measurement(theType);

        if (theType == UnitUtilities.Measurement_Type.VALUE) {
            Quantity quantity = new Quantity();
            quantity.setRawValue(atomicValue);
            quantity.setRawUnit(new Unit(unitValue));
            // note: there is no way to enforce the measurement type here
            // it will be inferred from the raw unit
            measurement.setAtomicQuantity(quantity);
        } else {
            Quantity quantityLeast = new Quantity();
            Quantity quantityMost = new Quantity();
            quantityLeast.setRawValue(fromValue);
            quantityLeast.setRawUnit(new Unit(unitValue));
            quantityMost.setRawValue(toValue);
            quantityMost.setRawUnit(new Unit(unitValue));
            measurement.setQuantityLeast(quantityLeast);
            measurement.setQuantityMost(quantityMost);
        }
        List<Measurement> measurements = new ArrayList<>();
        measurements.add(measurement);

        return measurements;
    }


    public MeasurementsResponse processJson(String json) {
        try {
            long start = System.currentTimeMillis();
            List<Measurement> measurements = parseMeasurement(json);
            measurements = quantityParser.normalizeMeasurements(measurements);
            long end = System.currentTimeMillis();
            MeasurementsResponse response = new MeasurementsResponse(measurements);
            response.setRuntime(end - start);

            return response;

        } catch (NoSuchElementException e) {
            throw new GrobidServiceException("Could not get an engine from the pool within configured time. Sending service unavailable.", e, Response.Status.SERVICE_UNAVAILABLE);
        } catch (Exception e) {
            throw new GrobidServiceException("An unexpected exception occurs. ", e, Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    public MeasurementsResponse processText(String text) {
        try {
            long start = System.currentTimeMillis();
            MeasurementsResponse response = new MeasurementsResponse(quantityParser.process(text));
            long end = System.currentTimeMillis();
            response.setRuntime(end - start);

            return response;
        } catch (NoSuchElementException e) {
            throw new GrobidServiceException("Could not get an engine from the pool within configured time. Sending service unavailable.", e, Response.Status.SERVICE_UNAVAILABLE);
        } catch (Exception e) {
            throw new GrobidServiceException("An unexpected exception occurs. ", e, Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    public void batchProcess(String inputDirectory, String outputDirectory, boolean isRecursive) {
        throw new NotImplementedException("Not yet implemented");
    }

    /**
     * Processes a file with units
     */
    public void unitBatchProcess(String inputDirectory, String outputDirectory, boolean isRecursive) {
        Path inputPath = Paths.get(inputDirectory);
        File[] refFiles = inputPath.toFile().listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".txt");
            }
        });

        if (refFiles == null) {
            return;
        }

        LOGGER.info(refFiles.length + " files");


        for (File inputFile : refFiles) {
            Writer outputWriter = null;
            try {
                // the file for writing the training data
                OutputStream os2 = null;

                if (outputDirectory != null) {
                    os2 = new FileOutputStream(outputDirectory + File.separator + inputFile.getName() + ".xml");
                    outputWriter = new OutputStreamWriter(os2, UTF_8);
                } else {
                    return;
                }

                outputWriter.write("<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n");
                outputWriter.write("<units>\n");

                try (Stream<String> stream = Files.lines(Paths.get(inputFile.getAbsolutePath()))) {

                    List<String> processedUnits = stream.map(
                            s -> {
                                List<UnitBlock> unitBlocks = unitParser.tagUnit(s);

                                if (isNotEmpty(unitBlocks)) {
                                    return unitBlocks.get(0).getRawTaggedValue();
                                }
                                return "";
                            }
                    ).filter(StringUtils::isNotBlank).collect(Collectors.toList());

                    for (String processedUnit : processedUnits) {
                        outputWriter.write("<unit>");
                        outputWriter.write(processedUnit);
                        outputWriter.write("</unit>");
                        outputWriter.write("\n");
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }


                outputWriter.write("</units>\n");
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                IOUtils.closeQuietly(outputWriter);
            }
        }
    }
}
