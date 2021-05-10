package org.grobid.core.data.normalization;

import org.grobid.core.data.Unit;
import org.grobid.core.data.UnitBlock;
import org.grobid.core.data.UnitDefinition;
import org.grobid.core.engines.UnitParser;
import org.grobid.core.lexicon.QuantityLexicon;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

/**
 * Created by lfoppiano on 23/03/16.
 */
public class UnitNormalizer {

    private UnitParser unitParser;
    private QuantityLexicon quantityLexicon;

    public UnitNormalizer(UnitParser unitParser, QuantityLexicon quantityLexicon) {
        this.unitParser = unitParser;
        this.quantityLexicon = quantityLexicon;
    }

    public UnitNormalizer() {
        unitParser = UnitParser.getInstance();
        quantityLexicon = QuantityLexicon.getInstance();
    }


    public List<UnitBlock> parseToProduct(String rawUnit, boolean isUnitLeft) {
        String unitName = quantityLexicon.getNameByInflection(rawUnit);

        List<UnitBlock> unitBlockList = new ArrayList<>();
        List<UnitBlock> unitBlocks = unitParser.tagUnit(rawUnit, isUnitLeft);
        if(unitName != null) {
            unitBlocks = unitParser.tagUnit(unitName, isUnitLeft);
        }

        // If the product is just one, and the unitName is found, then I use directly the unitName
        // If the unitName is found, it might still be a complex unit, so we don't want to put that as base (as it was done before).
        //
        // Here if the unit parser is not precise there might be many problems...
        if (unitName != null && unitBlocks.size() == 1) {
            unitBlockList.add(new UnitBlock(null, unitName, null));
        } else {
            unitBlockList.addAll(unitBlocks);
        }

        return unitBlockList;
    }

    /**
     * Unit parsing:
     * - infer the name (name + decomposition) from the written
     * form (including inflections), e.g. m <- meters, A/V <- volt per meter
     * - then we still parse the unit using the Unit CRF model
     * - if the parse give only one Block, and the inflection was taken, we keep the inflection result.
     * - else we keep whatever the unit parser understood
     */
    public Unit parseUnit(Unit rawUnit) {
        List<UnitBlock> blocks = parseToProduct(rawUnit.getRawName(), rawUnit.hasUnitRightAttachment());

        Unit parsedUnit = new Unit();
        parsedUnit.setOffsetStart(rawUnit.getOffsetStart());
        parsedUnit.setOffsetEnd(rawUnit.getOffsetEnd());
        parsedUnit.setProductBlocks(blocks);

        // transforming inflection within the blocks in their notation
        final List<UnitBlock> decomposedBlocks = transformInflectionsToNotationInBlocks(blocks);
        final String reformatted = UnitBlock.asString(decomposedBlocks);
        parsedUnit.setRawName(reformatted);
        parsedUnit.setUnitRightAttachment(rawUnit.hasUnitRightAttachment());

        UnitDefinition def = quantityLexicon.getUnitByNotation(reformatted);
        if (def == null) {
            def = quantityLexicon.getUnitbyName(reformatted);
        }
        parsedUnit.setUnitDefinition(def);
        return parsedUnit;
    }

    /**
     * Try to find the notation of each blocks
     */
    public List<UnitBlock> transformInflectionsToNotationInBlocks(List<UnitBlock> blocks) {
//        List<UnitBlock> unitsByNotation = blocks.stream().map(b -> {
//        });

        // Try to transform inflections to notation for each element with no prefix
        // WHy NO PREFIX?
        List<UnitBlock> unitsByInflection = blocks.stream().map(b -> {
            if (isNotEmpty(b.getBase()) && isEmpty(b.getPrefix())) {
                String newName = quantityLexicon.getNameByInflection(b.getBase());

                if (isNotEmpty(newName)) {
                    return new UnitBlock(b.getPrefix(), newName, b.getPow());
                }
            }
            return b;
        }).collect(Collectors.toList());


        return unitsByInflection;
    }

    public UnitDefinition findDefinition(Unit unit) {
        return quantityLexicon.lookup(unit);
    }

    public Unit findDefinitionAndUpdate(Unit unit) {
        UnitDefinition definition = findDefinition(unit);

        if (definition != null) {
            unit.setUnitDefinition(definition);
        }
        return unit;
    }


    public void setUnitParser(UnitParser unitParser) {
        this.unitParser = unitParser;
    }

    public void setQuantityLexicon(QuantityLexicon quantityLexicon) {
        this.quantityLexicon = quantityLexicon;
    }
}
