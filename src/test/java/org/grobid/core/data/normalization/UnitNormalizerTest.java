package org.grobid.core.data.normalization;

import org.grobid.core.data.UnitBlock;
import org.grobid.core.engines.UnitParser;
import org.grobid.core.lexicon.QuantityLexicon;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.easymock.EasyMock.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

public class UnitNormalizerTest {

    private UnitNormalizer target;
    private UnitParser mockUnitParser;
    private QuantityLexicon mockQuantityLexicon;

    @Before
    public void setUp() {
        mockUnitParser = createMock(UnitParser.class);

        mockQuantityLexicon = createMock(QuantityLexicon.class);
        target = new UnitNormalizer(mockUnitParser, mockQuantityLexicon);
    }

    @Test
    public void testParseToProduct_simpleUnit_notation_meter_noPow() throws Exception {
        String unitSymbol = "m";

        expect(mockQuantityLexicon.getNameByInflection("m")).andReturn(null);
        final UnitBlock block = new UnitBlock("m");
        expect(mockUnitParser.tagUnit("m", false)).andReturn(Arrays.asList(block));

        replay(mockQuantityLexicon, mockUnitParser);
        List<UnitBlock> reformatted = target.parseToProduct(unitSymbol, false);
        verify(mockQuantityLexicon, mockUnitParser);

        assertThat(reformatted, hasSize(1));
        assertThat(reformatted.get(0).toString(), is("m"));
    }

    @Test
    public void testParseToProduct_simpleUnit_notation_seconds_noPow() throws Exception {
        String unitSymbol = "s";

        expect(mockQuantityLexicon.getNameByInflection(unitSymbol)).andReturn(null);
        final UnitBlock block = new UnitBlock("s");
        expect(mockUnitParser.tagUnit(unitSymbol, false)).andReturn(Arrays.asList(block));

        replay(mockQuantityLexicon, mockUnitParser);
        List<UnitBlock> reformatted = target.parseToProduct(unitSymbol, false);
        verify(mockQuantityLexicon, mockUnitParser);

        assertThat(reformatted, hasSize(1));
        assertThat(reformatted.get(0).toString(), is("s"));
    }

    @Test
    public void testParseToProduct_simpleUnit_notation_ml_noPow() throws Exception {
        String unitSymbol = "ml";

        expect(mockQuantityLexicon.getNameByInflection("ml")).andReturn(null);
        final UnitBlock block = new UnitBlock("m", "l", null);
        expect(mockUnitParser.tagUnit("ml", false)).andReturn(Arrays.asList(block));

        replay(mockQuantityLexicon, mockUnitParser);
        List<UnitBlock> reformatted = target.parseToProduct(unitSymbol, false);
        verify(mockQuantityLexicon, mockUnitParser);

        assertThat(reformatted, hasSize(1));
        assertThat(reformatted.get(0).toString(), is("ml"));
    }

    @Test
    public void testParseToProduct_baseUnit_fullName_noPow() throws Exception {
        String unitSymbol = "meter";
        String unitSymbolNotation = "m";

        expect(mockQuantityLexicon.getNameByInflection(unitSymbol)).andReturn(unitSymbolNotation);
        final UnitBlock block = new UnitBlock(unitSymbolNotation);
        expect(mockUnitParser.tagUnit(unitSymbol, false)).andReturn(Arrays.asList(block));
        expect(mockUnitParser.tagUnit(unitSymbolNotation, false)).andReturn(Arrays.asList(block));

        replay(mockQuantityLexicon, mockUnitParser);
        List<UnitBlock> reformatted = target.parseToProduct(unitSymbol, false);
        verify(mockQuantityLexicon, mockUnitParser);

        assertThat(reformatted, hasSize(1));
        assertThat(reformatted.get(0).toString(), is(unitSymbolNotation));
    }

    @Test
    public void testParseToProduct_simpleUnit_notation_pow() throws Exception {
        String unitSymbol = "m2";
        expect(mockQuantityLexicon.getNameByInflection(unitSymbol)).andReturn(null);
        final UnitBlock block = new UnitBlock(null, "m", "2");
        expect(mockUnitParser.tagUnit(unitSymbol, false)).andReturn(Arrays.asList(block));

        replay(mockQuantityLexicon, mockUnitParser);
        List<UnitBlock> reformatted = target.parseToProduct(unitSymbol, false);
        verify(mockQuantityLexicon, mockUnitParser);

        assertThat(reformatted, hasSize(1));
        assertThat(reformatted.get(0).toString(), is("m^2"));
    }


    @Test
    public void testParseToProduct_simpleUnit_pow2() throws Exception {
        String unitSymbol = "km2";
        expect(mockQuantityLexicon.getNameByInflection(unitSymbol)).andReturn(null);
        final UnitBlock block = new UnitBlock("k", "m", "2");
        expect(mockUnitParser.tagUnit(unitSymbol, false)).andReturn(Arrays.asList(block));

        replay(mockUnitParser, mockQuantityLexicon);
        List<UnitBlock> reformatted = target.parseToProduct(unitSymbol, false);
        verify(mockUnitParser, mockQuantityLexicon);

        assertThat(reformatted, hasSize(1));
        assertThat(reformatted.get(0).toString(), is("km^2"));
    }


    @Test
    public void testParseToProduct_notations_productUnit_pow2() throws Exception {
        String unitSymbol = "m/s^2";
        expect(mockQuantityLexicon.getNameByInflection(unitSymbol)).andReturn(null);
        final UnitBlock block1 = new UnitBlock("m");
        final UnitBlock block2 = new UnitBlock(null, "s", "-2");
        expect(mockUnitParser.tagUnit(unitSymbol, false)).andReturn(Arrays.asList(block1, block2));

        replay(mockUnitParser, mockQuantityLexicon);
        List<UnitBlock> reformatted = target.parseToProduct(unitSymbol, false);
        verify(mockUnitParser, mockQuantityLexicon);

        assertThat(reformatted, hasSize(2));
        assertThat(reformatted.get(0).toString(), is("m"));
        assertThat(reformatted.get(1).toString(), is("s^-2"));

        assertThat(UnitBlock.asProduct(reformatted), is("m·s^-2"));
    }

    @Test
    public void testParseToProduct_inflections_productUnit_pow2() throws Exception {
        String unitSymbol = "meters per square seconds";
        String unitSymbolNotation = "m/s^2";
        expect(mockQuantityLexicon.getNameByInflection(unitSymbol)).andReturn(unitSymbolNotation);
        final UnitBlock block1 = new UnitBlock("m");
        final UnitBlock block2 = new UnitBlock(null, "s", "-2");
        expect(mockUnitParser.tagUnit(unitSymbol, false)).andReturn(Arrays.asList(block1, block2));
        expect(mockUnitParser.tagUnit(unitSymbolNotation, false)).andReturn(Arrays.asList(block1, block2));

        replay(mockUnitParser, mockQuantityLexicon);
        List<UnitBlock> reformatted = target.parseToProduct(unitSymbol, false);
        verify(mockUnitParser, mockQuantityLexicon);

        assertThat(reformatted, hasSize(2));
        assertThat(reformatted.get(0).toString(), is("m"));
        assertThat(reformatted.get(1).toString(), is("s^-2"));

        assertThat(UnitBlock.asProduct(reformatted), is("m·s^-2"));
    }

    @Test
    public void testParseToProduct_inflections_singleUnit() throws Exception {
        String unitSymbol = "meters";
        String unitSymbolNotation = "m";
        expect(mockQuantityLexicon.getNameByInflection(unitSymbol)).andReturn(unitSymbolNotation);
        final UnitBlock block1 = new UnitBlock("incorrectRecognition");
        expect(mockUnitParser.tagUnit(unitSymbol, false)).andReturn(Arrays.asList(block1));
        expect(mockUnitParser.tagUnit(unitSymbolNotation, false)).andReturn(Arrays.asList(block1));

        replay(mockUnitParser, mockQuantityLexicon);
        List<UnitBlock> reformatted = target.parseToProduct(unitSymbol, false);
        verify(mockUnitParser, mockQuantityLexicon);

        assertThat(reformatted, hasSize(1));
        assertThat(reformatted.get(0).toString(), is(unitSymbolNotation));
        assertThat(UnitBlock.asProduct(reformatted), is(unitSymbolNotation));
    }

    @Test
    public void testDecompositionOfBlocks_shouldReturnTheNotation() throws Exception {
        List<UnitBlock> blocks = new ArrayList<>();

        blocks.add(new UnitBlock("k", "m", null));
        blocks.add(new UnitBlock(null, "hours", "-1"));

        expect(mockQuantityLexicon.getNameByInflection("hours")).andReturn("h");

        replay(mockQuantityLexicon);

        final List<UnitBlock> result = target.transformInflectionsToNotationInBlocks(blocks);
        assertThat(result, hasSize(2));
        assertThat(result.get(0).getBase(), is("m"));
        assertThat(result.get(0).getPrefix(), is("k"));
        assertThat(result.get(1).getBase(), is("h"));

        verify(mockQuantityLexicon);
    }

    @Test
    public void testParseToProduct_complexUnit() throws Exception {
        String unitSymbol = "kg/cc";

        List<UnitBlock> blocks = new ArrayList<>();

        blocks.add(new UnitBlock("k", "g", null));
        blocks.add(new UnitBlock(null, "cc", "-1"));

        expect(mockQuantityLexicon.getNameByInflection(unitSymbol)).andReturn(unitSymbol);
        expect(mockUnitParser.tagUnit(unitSymbol, false)).andReturn(blocks);
        expect(mockUnitParser.tagUnit(unitSymbol, false)).andReturn(blocks);

        replay(mockQuantityLexicon, mockUnitParser);

        target.parseToProduct(unitSymbol, false);

        verify(mockQuantityLexicon, mockUnitParser);

    }

}