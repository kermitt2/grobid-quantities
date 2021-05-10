package org.grobid.core.utilities;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.grobid.core.data.normalization.NormalizationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.*;

/**
 * Convert a number expressed with alphabetical characters into a normalized numerical value.
 * 
 * @author Patrice Lopez
 */
public class WordsToNumber {

    private static final Logger logger = LoggerFactory.getLogger(WordsToNumber.class);

    private static volatile WordsToNumber instance;

    private final String VALUES_PATH = "lexicon/en/values.json";

    private static List<String> bases = null; 
    private static List<String> tens = null; 
    private static List<String> scales = null; 
    private static String decimalMark = null; 

    private Set<String> numberTokens = null;
 
    // the lexicon
    private Map<String, ScaleIncrementPair> numWord = null; 

    public static WordsToNumber getInstance() {
        if (instance == null) {
            getNewInstance();
        }
        return instance;
    }

    private static synchronized void getNewInstance() {
        instance = new WordsToNumber();
    }

    private WordsToNumber() {
        // init the lexicon with the numerical operations
        numWord = new HashMap<>();
        numWord.put("and", new ScaleIncrementPair(1, 0));

        init();
    }

    private void init() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode rootNode = mapper.readTree(this.getClass().getClassLoader().getResourceAsStream(VALUES_PATH));

            bases = new ArrayList<>();
            tens = new ArrayList<>();
            scales = new ArrayList<>();
            numberTokens = new HashSet<>();
            List<String> decimalMarks = new ArrayList<>();

            JsonNode basesNode = rootNode.findPath("bases");
            if ((basesNode != null) && (!basesNode.isMissingNode())) {
                Iterator<JsonNode> iter = basesNode.elements();
                while (iter.hasNext()) {
                    String text = ((JsonNode) iter.next()).textValue();
                    bases.add(text);
                    numberTokens.add(text);
                }
            }
            for (int i = 0; i < bases.size(); i++) {
                numWord.put(bases.get(i), new ScaleIncrementPair(1, i));
            }

            JsonNode tensNode = rootNode.findPath("tens");
            if ((tensNode != null) && (!tensNode.isMissingNode())) {
                Iterator<JsonNode> iter = tensNode.elements();
                while (iter.hasNext()) {
                    String text = ((JsonNode) iter.next()).textValue();
                    tens.add(text);
                    numberTokens.add(text);
                }
            }
            for (int i = 1; i < tens.size(); i++) {
                numWord.put(tens.get(i), new ScaleIncrementPair(1, i * 10));                
            }

            JsonNode scalesNode = rootNode.findPath("scales");
            if ((scalesNode != null) && (!scalesNode.isMissingNode())) {
                Iterator<JsonNode> iter = scalesNode.elements();
                while (iter.hasNext()) {
                    String text = ((JsonNode) iter.next()).textValue();
                    scales.add(text);
                    numberTokens.add(text);
                }
            }
            for (int i = 0; i < scales.size(); i++) {
                if(i == 0)
                    numWord.put(scales.get(i), new ScaleIncrementPair(100, 0));
                else
                    numWord.put(scales.get(i), new ScaleIncrementPair(Math.pow(10, (i*3)), 0));
            }

            JsonNode decimalNode = rootNode.findPath("decimalMark");
            if ((decimalNode != null) && (!decimalNode.isMissingNode())) {
                Iterator<JsonNode> iter = decimalNode.elements();
                while (iter.hasNext()) {
                    String text = ((JsonNode) iter.next()).textValue();
                    decimalMarks.add(text);
                    numberTokens.add(text);
                }
            }
            decimalMark = decimalMarks.get(0);
        } catch(IOException e) {
            logger.error("Error when reading the values.json file");
        }

    }

    public Set<String> getTokenSet() {
        return numberTokens;
    }

    public BigDecimal normalize(String text, Locale local) throws NormalizationException {
        double current = 0; 
        double result = 0;
        text = StringUtils.lowerCase(text);

        // split integer and possible decimal part
        String[] pieces = text.split(decimalMark);
        if(ArrayUtils.isEmpty(pieces)) {
            throw new NormalizationException("Cannot convert the alphabetic value '" + text + "' to digits");
        }
        String integerPart = pieces[0].trim();
        String decimalPart = null;
        if (pieces.length > 1)
            decimalPart = pieces[1].trim();

        if ((integerPart != null) && (integerPart.length() > 0)) {
            pieces = integerPart.split("\\W"); // or limit to split(" |-|—");
            for(int i=0; i< pieces.length; i++) {
                String word = pieces[i];
                ScaleIncrementPair scaleIncrement = numWord.get(word);
                if (scaleIncrement == null) {
                    logger.warn("Invalid token to be converted into number: " + word);
                    continue;
                }
                current = current * scaleIncrement.scale + scaleIncrement.increment;
                if (scaleIncrement.scale > 100) {
                    result += current;
                    current = 0;
                }
            }
            result += current;
        }

        // decimal part 
        BigDecimal decimalResult = new BigDecimal(0);
        if ((decimalPart != null) && (decimalPart.length() > 0)) {
            pieces = decimalPart.split("\\W"); // or limit to split(" |-|—");
            StringBuilder res = new StringBuilder().append("0.");
            for(int i=0; i< pieces.length; i++) {
                String word = pieces[i].trim();
                if (word.length() == 0) 
                    continue;
                ScaleIncrementPair scaleIncrement = numWord.get(word);
                if (scaleIncrement == null) {
                    logger.warn("Invalid decimal token to be converted into number: " + word);
                    continue;
                }
                res.append(scaleIncrement.increment); 
            }
            NumberFormat format = NumberFormat.getInstance(local);
            try {
                Number number = format.parse(res.toString());
                decimalResult = new BigDecimal(number.toString());
            } catch (ParseException pe) {
                logger.error("Invalid value expression: " + res.toString() + " , for: " + decimalPart);
            }
        }

        return new BigDecimal(result).add(decimalResult);
    }

    public class ScaleIncrementPair {

        public double scale;
        public int increment;

        public ScaleIncrementPair(double s, int i) {
            scale = s;
            increment = i;
        }
    }
}
