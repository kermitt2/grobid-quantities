package org.grobid.core.lexicon;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.collections4.CollectionUtils;
import org.grobid.core.analyzers.QuantityAnalyzer;
import org.grobid.core.data.RegexValueHolder;
import org.grobid.core.data.Unit;
import org.grobid.core.data.UnitDefinition;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.*;
import static org.grobid.core.lexicon.LexiconLoader.loadPrefixes;
import static org.grobid.core.lexicon.LexiconLoader.readJsonFile;

/**
 * Class for managing the measurement lexical resources.
 * <p>
 * To be done: generalize to n different languages
 *
 * @author Patrice, Luca
 */
public class QuantityLexicon {
    private static final Logger LOGGER = LoggerFactory.getLogger(QuantityLexicon.class);

    private static volatile QuantityLexicon instance;
    public static final String PREFIX_FILENAME = "prefix.txt";
    public static final String PREFIX_EN_PATH = "lexicon/en/" + PREFIX_FILENAME;

    public static final String UNITS_FILENAME = "units.json";
    public static final String UNITS_EN_PATH = "lexicon/en/" + UNITS_FILENAME;

    private static final String COMPOSED_UNIT_REGEX = "[^/*]";
    private static final String COMPOSED_UNIT_REGEX_WITH_DELIMITER = String.format("((?<=%1$s)|(?=%1$s))", "[/*]{1}");

    private Pattern composedUnitPattern = Pattern.compile(COMPOSED_UNIT_REGEX);
    private Pattern composedUnitPatternWithDelimiter = Pattern.compile(COMPOSED_UNIT_REGEX_WITH_DELIMITER);

    // lexical information - for feature generations
    private FastMatcher unitPattern = null;
    private Set<String> unitTokens = null;
    private Set<String> unitTokensLowerCase = null;

    // the list of tokens involved for expressing numbers with words
    // this set depends on the language
    private Set<String> numberTokens = null;

    private Map<String, String> prefixes = null; // map prefix symbol to prefix string
    private Map<String, List<String>> inflection = null; // map a unit string to its morphological inflections

    // full unit information accessible from the unit names
    // this mapping depends on the language
    private Map<String, UnitDefinition> name2unit = null;

    // full unit information accessible from the unit notation
    // this mapping depends on the language
    private Map<String, UnitDefinition> notation2unit = null;

    // mapping between measurement types and the SI units for this type, the type here is represented with
    // the name() value of the enum
    private Map<String, UnitDefinition> type2SIUnit = null;

    // mapping between inflection (meter, meters) to name (m), considering kilometer a different unit than meter,
    // altough they are two different representation of the same unit
    private Map<String, String> inflection2name = null;

    private QuantityLexicon() {
        init();
    }

    public static QuantityLexicon getInstance() {
        if (instance == null) {
            getNewInstance();
        }
        return instance;
    }

    private static synchronized void getNewInstance() {
        instance = new QuantityLexicon();
    }

    private void init() {
        unitTokens = new HashSet<>();
        unitTokensLowerCase = new HashSet<>();
        unitPattern = new FastMatcher();

        prefixes = loadPrefixes(this.getClass().getClassLoader().getResourceAsStream(PREFIX_EN_PATH));
        readJsonFile(this.getClass().getClassLoader().getResourceAsStream(UNITS_EN_PATH), "units", l -> processJsonNode(l));

        numberTokens = WordsToNumber.getInstance().getTokenSet();
    }


    private void processJsonNode(JsonNode node) {
        UnitUtilities.Unit_Type type = UnitUtilities.Unit_Type.valueOf(node.get("type").asText());
        UnitUtilities.System_Type system = UnitUtilities.System_Type.valueOf(node.get("system").asText());

        UnitDefinition unitDefinition = new UnitDefinition();
        unitDefinition.setSystem(system);
        unitDefinition.setType(type);

        JsonNode supportsPrefixes = node.get("supportsPrefixes");
        if (supportsPrefixes != null) {
            unitDefinition.setSupportsPrefixes(supportsPrefixes.asBoolean());
        } else {
            // Imperial and us customary won't need prefixes
            if (Arrays.asList(UnitUtilities.System_Type.IMPERIAL, UnitUtilities.System_Type.US)
                .contains(unitDefinition.getSystem())) {
                unitDefinition.setSupportsPrefixes(false);
            } else {
                unitDefinition.setSupportsPrefixes(true);
            }
        }

        JsonNode skipNormalisationNode = node.get("skipNormalisation");
        if (skipNormalisationNode != null) {
            unitDefinition.setSkipNormalisation(skipNormalisationNode.asBoolean());
        } else {
            unitDefinition.setSkipNormalisation(false);
        }

        String defaultRawNotation = "";
        if (node.has("notations")) {
            JsonNode notations = node.get("notations");
            final Iterator<JsonNode> elements = notations.elements();
            while (elements.hasNext()) {
                String rawNotation = elements.next().get("raw").asText();

                if (isBlank(defaultRawNotation)) {
                    defaultRawNotation = rawNotation;
                }
                if (unitDefinition.isSupportsPrefixes()) {
                    expandAndAdd(unitDefinition, rawNotation);
                }
            }
        }


        JsonNode namesNode = node.get("names");
        final Iterator<JsonNode> names = namesNode.elements();
        while (names.hasNext()) {
            JsonNode name = names.next();

            String lemma = name.get("lemma").asText();
            if (name.has("inflections")) {
                JsonNode inflections = name.get("inflections");
                List<String> inflectionsList = new ArrayList<>();
                Iterator<JsonNode> inflectionsIterator = inflections.elements();
                inflectionsList.add(lemma);
                while (inflectionsIterator.hasNext()) {
                    inflectionsList.add(inflectionsIterator.next().asText());
                }

                processInflections(defaultRawNotation, unitDefinition, lemma, inflectionsList);

                if (inflection == null) {
                    inflection = new HashMap<>();
                }
                inflection.put(lemma, inflectionsList);
            }
        }

        populateName2Unit(unitDefinition);
        populateNotation2Unit(unitDefinition);
        populateType2SiUnit(unitDefinition);
    }

    private void populateType2SiUnit(UnitDefinition unitDefinition) {
        UnitUtilities.Unit_Type type = unitDefinition.getType();
        UnitUtilities.System_Type system = unitDefinition.getSystem();
        if ((system == UnitUtilities.System_Type.SI_BASE) || (system == UnitUtilities.System_Type.SI_DERIVED)) {
            if (type2SIUnit == null) {
                type2SIUnit = new HashMap<>();
            }
            if ((type == null) || (type.getName() == null)) {
                LOGGER.error("unitDefinition has no type: " + unitDefinition.toString());
            }

            if (type2SIUnit.get(type.getName()) == null) {
                type2SIUnit.put(type.getName(), unitDefinition);
            }
        }
    }

    private void populateNotation2Unit(UnitDefinition unitDefinition) {
        // add unit notation map
        List<String> notations = unitDefinition.getNotations();
        if ((notations != null) && (notations.size() > 0)) {
            for (int j = 0; j < notations.size(); j++) {
                if (notation2unit == null) {
                    notation2unit = new HashMap<>();
                }
                notation2unit.put(notations.get(j).trim(), unitDefinition);
            }
        } else {
            notation2unit.put("no_notation", unitDefinition);
        }
    }

    private void populateName2Unit(UnitDefinition unitDefinition) {
        // add unit names in the first map
        List<String> names = unitDefinition.getNames();
        if (CollectionUtils.isNotEmpty(names)) {
            if (name2unit == null) {
                name2unit = new HashMap<>();
            }

            for (int j = 0; j < names.size(); j++) {
                name2unit.put(names.get(j).trim().toLowerCase(), unitDefinition);
            }
        }
    }

    private void processInflections(String notation, UnitDefinition unitDefinition, String lemma, List<String> inflections) {
        UnitUtilities.System_Type system = unitDefinition.getSystem();
        if (inflection2name == null) {
            inflection2name = new HashMap<>();
        }
        for (String inflectedForm : inflections) {
            String name = notation;
            if (isBlank(name) /*&& !name.equals(subPiece)*/) {
                name = lemma;
            }

            // inflected -> name (e.g. meters -> m)
            inflection2name.put(inflectedForm, name);


            for (Map.Entry<String, String> prefix : prefixes.entrySet()) {
                // complex unit inflected form -> name (kilometers -> km)
                inflection2name.put(prefix.getValue() + inflectedForm, prefix.getKey() + name);

                // (variation) complex unit inflected form -> name (e.g. kmeter -> km)
                inflection2name.put(prefix.getKey() + inflectedForm, prefix.getKey() + name);
            }


            if ((system == UnitUtilities.System_Type.SI_BASE) || (system == UnitUtilities.System_Type.SI_DERIVED)) {
                // expansion with derivational morphology, but only for SI units!
                List<String> derivations = derivationalMorphologyExpansion(inflectedForm, false);
                for (String derivation : derivations) {
                    unitDefinition.addName(derivation);
                    try {
                        unitPattern.loadTerm(derivation, QuantityAnalyzer.getInstance());
                    } catch (Exception e) {
                        LOGGER.error("invalid unit term: " + derivation);
                    }
                    List<String> subSubPieces = null;
                    try {
                        subSubPieces = QuantityAnalyzer.getInstance().tokenize(derivation);

                        for (String word : subSubPieces) {
                            addToUnitTokens(word);
                        }
                    } catch (Exception e) {
                        LOGGER.error("fail to tokenize:, " + derivation, e);
                    }
                }
            } else {
                unitDefinition.addName(inflectedForm);
                try {
                    unitPattern.loadTerm(inflectedForm, QuantityAnalyzer.getInstance());
                } catch (Exception e) {
                    LOGGER.error("invalid unit term: " + inflectedForm);
                }
                List<String> subsubpieces = null;
                try {
                    subsubpieces = QuantityAnalyzer.getInstance().tokenize(inflectedForm);
                    for (String word : subsubpieces) {
                        addToUnitTokens(word);
                    }
                } catch (Exception e) {
                    LOGGER.error("fail to tokenize:, " + inflectedForm, e);
                }
            }
        }
    }

    private void expandAndAdd(UnitDefinition unitDefinition, String subPiece) {
        List<String> derivations = derivationalMorphologyExpansion(subPiece, true);
        for (String derivation : derivations) {
            try {
                unitPattern.loadTerm(derivation, QuantityAnalyzer.getInstance());
            } catch (Exception e) {
                LOGGER.error("Invalid unit term: " + derivation);
            }
            unitDefinition.addNotation(derivation);

            List<String> subSubPieces = null;
            try {
                subSubPieces = QuantityAnalyzer.getInstance().tokenize(derivation);
                for (String word : subSubPieces) {
                    addToUnitTokens(word);
                }
            } catch (Exception e) {
                LOGGER.error("fail to tokenize:, " + derivation, e);
            }
        }
    }

    private void addToUnitTokens(String word) {
        word = trim(word);
        if ((word.length() > 0) && !unitTokens.contains(word)) {
            // we don't add pure digit sub-token and token delimiters
            if ((TextUtilities.countDigit(word) != word.length()) && (!QuantityAnalyzer.DELIMITERS.contains(word))) {
                unitTokens.add(word);
                unitTokensLowerCase.add(word.toLowerCase());
            }
        }
    }

    /**
     * Expansion of a non-notation unit name into its inflected forms. Note that the
     * input unit name is included in the returned list of forms.
     */
    public List<String> getInflectionsByTerm(String unitTerm) {
        List<String> results = new ArrayList<>();
        results.add(unitTerm);

        List<String> inflections = inflection.get(unitTerm);
        if (CollectionUtils.isNotEmpty(inflections)) {
            results.addAll(inflections);
        }

        return results;
    }

    /**
     * Expansion of a notation and non-notation unit name into derivations
     * based on standard unit prefix.
     * Note that the input unit name is included in the returned list of forms.
     * To be called after the inflectional expansion.
     */
    public List<String> derivationalMorphologyExpansion(String unitTerm, boolean isNotation) {
        List<String> results = new ArrayList<>();
        results.add(unitTerm);

        if (!isComposedUnit(unitTerm)) {
            // we expand based on the prefix list
            for (Map.Entry<String, String> prefix : prefixes.entrySet()) {
                String prefixString = selectPrefix(isNotation, prefix);
                results.add(prefixString + unitTerm);
            }
        } else {
            //A String.split() could have done the same job, since it was not sure which requirements there were
            //we have a more sophisticated - though useless way, that might be used if the expansion became more
            // complex.

            List<RegexValueHolder> decomposition = decomposeComplexUnit(unitTerm);
            RegexValueHolder firstElement = decomposition.get(0);
            RegexValueHolder secondElement = decomposition.get(1);

            for (Map.Entry<String, String> prefix : prefixes.entrySet()) {
                String prefixString = selectPrefix(isNotation, prefix);
                String firstElementExpanded = prefixString + firstElement.getValue();
                results.add(unitTerm.replace(secondElement.getValue(), prefixString + secondElement.getValue()));

                String outputExpanded = unitTerm.replace(firstElement.getValue(), firstElementExpanded);
                results.add(outputExpanded);

                for (Map.Entry<String, String> prefix2 : prefixes.entrySet()) {
                    String prefixString2 = selectPrefix(isNotation, prefix2);

                    results.add(outputExpanded.replace(secondElement.getValue(), prefixString2 + secondElement.getValue()));
                }
            }
        }

        return results;
    }

    private String selectPrefix(boolean isNotation, Map.Entry<String, String> prefix) {
        String prefixString;
        if (isNotation) {
            // if we have a notation, we use notation prefix (e.g. g -> kg)
            prefixString = prefix.getKey();
        } else {
            // otherwise we have a full form and we use the derivational prefix (e.g. gram -> kilogram)
            prefixString = prefix.getValue();
        }
        return prefixString;
    }

    public static boolean isComposedUnit(String unitTerm) {
        return unitTerm.contains("/")
            || unitTerm.contains("*")
            || unitTerm.contains("·");
    }

    public List<RegexValueHolder> decomposeComplexUnit(String unitTerm) {
        List<RegexValueHolder> decomposition = new ArrayList<>();
        Matcher m = composedUnitPattern.matcher(unitTerm);

        while (m.find()) {
            decomposition.add(new RegexValueHolder(m.group(), m.start(), m.end()));
        }

        return decomposition;
    }

    @SuppressWarnings("deprecated")
    @Deprecated
    public static List<RegexValueHolder> decomposeComplexUnitWithDelimiter(String unitTerm) {
        List<RegexValueHolder> decomposition = new ArrayList<>();
        String[] splits = unitTerm.split(COMPOSED_UNIT_REGEX_WITH_DELIMITER);

        int i = 0;
        for (String split : splits) {
            decomposition.add(new RegexValueHolder(split, i, i = i + split.length()));
//            i += split.length();
        }

        return decomposition;
    }

    /**
     * Soft look-up in unit dictionary
     */
    public List<OffsetPosition> inUnitNames(String s) {
        if (unitPattern == null) {
            init();
        }
        List<OffsetPosition> results = unitPattern.matchToken(s);
        return results;
    }

    public List<OffsetPosition> inUnitNames(List<LayoutToken> s) {
        if (unitPattern == null) {
            init();
        }
        List<OffsetPosition> results = unitPattern.matchLayoutToken(s);
        return results;
    }

    public List<OffsetPosition> inUnitNamesPairs(List<Pair<String, String>> s) {
        if (unitPattern == null) {
            init();
        }
        List<OffsetPosition> results = unitPattern.matcherPairs(s);
        return results;
    }

    /**
     * Lookup the unit name by inflection, for example
     * meter -> m
     * kilogram -> kg
     * picohenry -> pH
     * mile per hours -> mile/h
     */
    public String getNameByInflection(String inflection) {
        return inflection2name.get(inflection);
    }

    public boolean inPrefixDictionary(String s) {
        return prefixes.containsKey(s);
    }

    public boolean inPrefixDictionaryCaseInsensitive(String s) {
        boolean inPrefix = inPrefixDictionary(s);
        if (inPrefix == false) {
            return inPrefixDictionary(upperCase(s));
        }
        return inPrefix;
    }

    public boolean inUnitDictionary(String s) {
        return unitTokens.contains(s);
    }

    public boolean inUnitDictionaryCaseInsensitive(String s) {
        return unitTokensLowerCase.contains(lowerCase(s));
    }

    /**
     * Return a unit object based on an unit name.
     */
    public UnitDefinition getUnitbyName(String name) {
        if (name == null) {
            return null;
        }
        return name2unit.get(name.toLowerCase());
    }

    /**
     * Return a unit object based on an unit notation.
     * for example:
     * getUnitByNotation("cm") would return meter (LENGTH, SI)
     * getUnitByNotation("cm3") would return m3 (VOLUME, SI)
     */
    public UnitDefinition getUnitByNotation(String notation) {
        if (notation == null) {
            return null;
        }
        return notation2unit.get(notation);
    }

    /**
     * Return the SI unit object from a measure type name
     */
    public UnitDefinition getSIUnitByType(String type) {
        if (type == null)
            return null;
        return type2SIUnit.get(type);
    }

    public UnitDefinition lookup(Unit rawUnit) {
        if ((rawUnit != null) && rawUnit.getRawName() != null) {
            UnitDefinition foundUnit = getUnitbyName(rawUnit.getRawName().trim());
            if (foundUnit == null) {
                foundUnit = getUnitByNotation(rawUnit.getRawName().trim());
            }

            return foundUnit;
        }

        return null;
    }

    public UnitDefinition find(Unit rawUnit) {
        if ((rawUnit != null) && rawUnit.getRawName() != null) {
            UnitDefinition foundUnit = getUnitByNotation(rawUnit.getRawName().trim());

            return foundUnit;
        }

        return null;
    }

    public boolean isNumberToken(String token) {
        if (token == null) {
            return false;
        }
        if (numberTokens == null) {
            init();
        }
        return numberTokens.contains(token.toLowerCase());
    }
}