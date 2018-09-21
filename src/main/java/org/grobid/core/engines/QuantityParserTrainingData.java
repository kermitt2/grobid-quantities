package org.grobid.core.engines;

import nu.xom.Attribute;
import nu.xom.Element;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.grobid.core.data.Measurement;
import org.grobid.core.document.Document;
import org.grobid.core.document.xml.XmlBuilderUtils;
import org.grobid.core.engines.config.GrobidAnalysisConfig;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.factory.GrobidFactory;
import org.grobid.core.sax.TextChunkSaxHandler;
import org.grobid.core.utilities.TeiUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.grobid.core.document.xml.XmlBuilderUtils.teiElement;

public class QuantityParserTrainingData {
    private static final Logger LOGGER = LoggerFactory.getLogger(QuantityParserTrainingData.class);

    private QuantityTrainingFormatter quantityTrainingFormatter = null;
    private UnitTrainingFormatter unitTrainingFormatter = null;
    private ValueTrainingFormatter valueTrainingFormatter = null;

    private QuantityParser quantityParser;

    public QuantityParserTrainingData() {
        quantityTrainingFormatter = new QuantityTrainingFormatter();
        unitTrainingFormatter = new UnitTrainingFormatter();
        valueTrainingFormatter = new ValueTrainingFormatter();

        quantityParser = QuantityParser.getInstance();
    }

    public QuantityParserTrainingData(QuantityParser parser) {
        quantityTrainingFormatter = new QuantityTrainingFormatter();
        unitTrainingFormatter = new UnitTrainingFormatter();
        valueTrainingFormatter = new ValueTrainingFormatter();

        quantityParser = parser;
    }

    /**
     * Process the content of the specified input file and format the result as training data.
     * <p>
     * Input file can be
     * (i) xml (.xml or .tei extension) and it is assumed that we have a patent document,
     * (ii) PDF (.pdf) and it is assumed that we have a scientific article which will be processed by GROBID fulltext first,
     * (iii) some text (.txt extension)
     *
     * @param inputFile       input file
     * @param outputDirectory path to TEI with annotated training data
     * @param id              id
     */
    public void createTraining(String inputFile, String outputDirectory, int id) throws Exception {
        File file = new File(inputFile);
        if (!file.exists()) {
            throw new GrobidException("Cannot create training data because input file can not be accessed: " + inputFile);
        }

        if (inputFile.endsWith(".txt") || inputFile.endsWith(".TXT")) {
            createTrainingText(file, outputDirectory, id);
        } else if (inputFile.endsWith(".xml") || inputFile.endsWith(".XML") || inputFile.endsWith(".tei") || inputFile.endsWith(".TEI")) {
            createTrainingXML(file, outputDirectory, id);
        } else if (inputFile.endsWith(".pdf") || inputFile.endsWith(".PDF")) {
            createTrainingPDF(file, outputDirectory, id);
        }
    }

    private void createTrainingText(File file, String outputDirectory, int id) throws IOException {
        String text = FileUtils.readFileToString(file, UTF_8);

        Element quantityNode = teiElement("text");
        Element unitNode = teiElement("units");
        Element valueNode = teiElement("values");

        // for the moment we suppose we have english only...
        quantityNode.addAttribute(new Attribute("xml:lang", "http://www.w3.org/XML/1998/namespace", "en"));

        // we process the text paragraph by paragraph
        String lines[] = text.split("\n");
        StringBuilder paragraph = new StringBuilder();
        List<Measurement> measurements = null;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.length() != 0) {
                paragraph.append(line).append("\n");
            }
            if (((line.length() == 0) || (i == lines.length - 1)) && (paragraph.length() > 0)) {

                measurements = quantityParser.process(text);
                quantityNode.appendChild(quantityTrainingFormatter.trainingExtraction(measurements, text));

                unitTrainingFormatter.trainingExtraction(measurements)
                        .stream()
                        .filter(a-> a.getChildElements().size() != 0)
                        .forEach(unitNode::appendChild);

                valueTrainingFormatter.trainingExtraction(measurements)
                        .stream()
                        .filter(a-> a.getChildElements().size() != 0)
                        .forEach(valueNode::appendChild);

                paragraph = new StringBuilder();
            }
        }
        writeOutput(file, outputDirectory, id, quantityNode, unitNode, valueNode);
    }

    private void writeOutput(File file, String outputDirectory, int id, Element quantityNode, Element unitNode, Element valueNode) {
        Element quantityDocumentRoot = TeiUtils.getQuantitiesTEIHeader(id);
        quantityDocumentRoot.appendChild(quantityNode);

        //Write the output for quantities model
        String outputFileQuantity = FilenameUtils.concat(outputDirectory, FilenameUtils.removeExtension(file.getName()) + ".quantity.xml");
        try {
            FileUtils.writeStringToFile(new File(outputFileQuantity), XmlBuilderUtils.toXml(quantityDocumentRoot), UTF_8);
        } catch (IOException e) {
            throw new GrobidException("Cannot create training data because output file can not be accessed: " + outputFileQuantity);
        }

        //We don't have TEI header, so we need one less annoying step :-)

        //Write the output for unit model
        String outputFileUnit = FilenameUtils.concat(outputDirectory, FilenameUtils.removeExtension(file.getName()) + ".unit.xml");
        try {
            FileUtils.writeStringToFile(new File(outputFileUnit), XmlBuilderUtils.toXml(unitNode), UTF_8);
        } catch (IOException e) {
            throw new GrobidException("Cannot create training data because output file can not be accessed: " + outputFileUnit);
        }

        //Write the output for unit model
        String outputFileValue = FilenameUtils.concat(outputDirectory, FilenameUtils.removeExtension(file.getName()) + ".value.xml");
        try {
            FileUtils.writeStringToFile(new File(outputFileValue), XmlBuilderUtils.toXml(valueNode), UTF_8);
        } catch (IOException e) {
            throw new GrobidException("Cannot create training data because output file can not be accessed: " + outputFileValue);
        }
    }

    private void createTrainingXML(File input, String outputDirectory, int id) {
        List<Measurement> measurements = null;

        Element quantityNode = teiElement("text");
        Element unitNode = teiElement("units");
        Element valueNode = teiElement("values");

        // for the moment we suppose we have english only...
        quantityNode.addAttribute(new Attribute("xml:lang", "http://www.w3.org/XML/1998/namespace", "en"));

        try {
            // get a factory for SAX parser
            SAXParserFactory spf = SAXParserFactory.newInstance();

            TextChunkSaxHandler handler = new TextChunkSaxHandler();

            //get a new instance of parser
            SAXParser p = spf.newSAXParser();
            p.parse(input, handler);

            List<String> chunks = handler.getChunks();
            for (String text : chunks) {
                measurements = quantityParser.process(text);

                if (measurements != null) {
                    System.out.println("\n");
                    for (Measurement measurement : measurements) {
                        System.out.println(measurement.toString());
                    }
                    System.out.println("\n");
                }

                quantityNode.appendChild(quantityTrainingFormatter.trainingExtraction(measurements, text));

                unitTrainingFormatter.trainingExtraction(measurements)
                        .stream()
                        .filter(a-> a.getChildElements().size() != 0)
                        .forEach(unitNode::appendChild);

                valueTrainingFormatter.trainingExtraction(measurements)
                        .stream()
                        .filter(a-> a.getChildElements().size() != 0)
                        .forEach(valueNode::appendChild);
            }
            Element quantityRoot = TeiUtils.getQuantitiesTEIHeader(id);
            quantityRoot.appendChild(quantityNode);

            writeOutput(input, outputDirectory, id, quantityNode, unitNode, valueNode);
        } catch (Exception e) {
            throw new GrobidException("Cannot create training data because input XML file can not be parsed: " + input.getPath(), e);
        }
    }

    private void createTrainingPDF(File file, String outputDirectory, int id) {
        // first we apply GROBID fulltext model on the PDF to get the full text TEI
        Document teiDoc = null;
        try {
            GrobidAnalysisConfig config =
                    new GrobidAnalysisConfig.GrobidAnalysisConfigBuilder()
                            .build();
            teiDoc = GrobidFactory.getInstance().createEngine().fullTextToTEIDoc(file, config);
        } catch (Exception e) {
            throw new GrobidException("Cannot create training data because GROBIL full text model failed on the PDF: " + file.getPath(), e);
        }
        if (teiDoc == null) {
            return;
        }

        String teiXML = teiDoc.getTei();

        // we parse this TEI string similarly as for createTrainingXML

        List<Measurement> measurements;

        Element quantityNode = teiElement("text");
        Element unitNode = teiElement("units");
        Element valueNode = teiElement("values");
        // for the moment we suppose we have english only...
        quantityNode.addAttribute(new Attribute("xml:lang", "http://www.w3.org/XML/1998/namespace", "en"));

        try {
            // get a factory for SAX parser
            SAXParserFactory spf = SAXParserFactory.newInstance();

            TextChunkSaxHandler handler = new TextChunkSaxHandler();

            //get a new instance of parser
            SAXParser p = spf.newSAXParser();
            p.parse(new InputSource(new StringReader(teiXML)), handler);

            List<String> chunks = handler.getChunks();
            for (String text : chunks) {
                measurements = quantityParser.process(text);

                if (isNotEmpty(measurements)) {
                    quantityNode.appendChild(quantityTrainingFormatter.trainingExtraction(measurements, text));

                    unitTrainingFormatter.trainingExtraction(measurements)
                            .stream()
                            .filter(a-> a.getChildElements().size() != 0)
                            .forEach(unitNode::appendChild);

                    valueTrainingFormatter.trainingExtraction(measurements)
                            .stream()
                            .filter(a-> a.getChildElements().size() != 0)
                            .forEach(valueNode::appendChild);
                }

            }
            writeOutput(file, outputDirectory, id, quantityNode, unitNode, valueNode);
        } catch (Exception e) {
            throw new GrobidException("Cannot create training data because input XML file can not be parsed: " + file.getPath(), e);
        }
    }

    /**
     * Create training data for a list of pdf/text/xml-tei files
     */
    @SuppressWarnings({"UnusedParameters"})
    public int createTrainingBatch(String inputDirectory,
                                   String outputDirectory,
                                   int ind) {
        try {
            File path = new File(inputDirectory);
            if (!path.exists()) {
                throw new GrobidException("Cannot create training data because input directory can not be accessed: " + inputDirectory);
            }

            File pathOut = new File(outputDirectory);
            if (!pathOut.exists()) {
                throw new GrobidException("Cannot create training data because output directory can not be accessed: " + outputDirectory);
            }

            // we process all pdf files in the directory
            List<File> refFiles = Arrays.stream(Objects.requireNonNull(path.listFiles())).filter(
                    file -> file.getName().endsWith(".pdf") || file.getName().endsWith(".PDF") ||
                            file.getName().endsWith(".txt") || file.getName().endsWith(".TXT") ||
                            file.getName().endsWith(".xml") || file.getName().endsWith(".tei") ||
                            file.getName().endsWith(".XML") || file.getName().endsWith(".TEI")
            ).collect(Collectors.toList());

            LOGGER.info(refFiles.size() + " files to be processed.");

            int n = 0;
            if (ind == -1) {
                // for undefined identifier (value at -1), we initialize it to 0
                n = 1;
            }
            for (final File file : refFiles) {
                try {
                    createTraining(file.getAbsolutePath(), outputDirectory, n);
                } catch (final Exception exp) {
                    LOGGER.error("An error occured while processing the following pdf: "
                            + file.getPath(), exp);
                }
                if (ind != -1)
                    n++;
            }

            return refFiles.size();
        } catch (final Exception exp) {
            throw new GrobidException("An exception occured while running Grobid batch.", exp);
        }
    }
}
