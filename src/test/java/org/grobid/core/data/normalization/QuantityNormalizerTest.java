package org.grobid.core.data.normalization;

import org.grobid.core.data.Quantity;
import org.grobid.core.engines.ValueParser;
import org.grobid.core.data.Unit;
import org.grobid.core.data.UnitDefinition;
import org.grobid.core.main.LibraryLoader;
import org.grobid.core.utilities.UnitUtilities;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import tec.uom.se.unit.TransformedUnit;

import javax.measure.format.UnitFormat;
import javax.measure.spi.ServiceProvider;
import javax.measure.spi.UnitFormatService;
import java.math.BigDecimal;
import java.util.Arrays;

import static org.easymock.EasyMock.*;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Created by lfoppiano on 14.02.16.
 */
@Ignore
public class QuantityNormalizerTest {

    //TODO: we should mock also uom-se .. but for now is better to leave it out so that we can spot other errors
    private QuantityNormalizer target;
    UnitNormalizer mockUnitNormalizer;
    ValueParser valueParser = ValueParser.getInstance();

    @BeforeClass
    public static void setUpClass() throws Exception {
        LibraryLoader.load();
    }

    @Before
    public void setUp() throws Exception {
        target = new QuantityNormalizer();
        mockUnitNormalizer = createMock(UnitNormalizer.class);
        target.setUnitNormalizer(mockUnitNormalizer);
    }

    @Test
    public void testNormalizeQuantity_simpleUnitWithNormalization_kmToMeters() throws Exception {
        Quantity input = new Quantity();
        input.setRawValue("2");
        input.setParsedValue(valueParser.parseValue(input.getRawValue()));
        Unit raw = new Unit();
        raw.setRawName("km");
        input.setRawUnit(raw);
        final Unit parsedUnit = new Unit("km");
        parsedUnit.setUnitDefinition(generateLenghtUnitDefinition());
        expect(mockUnitNormalizer.parseUnit(raw)).andReturn(parsedUnit);
        final UnitDefinition fakeDefinition = new UnitDefinition();
        fakeDefinition.setNotations(Arrays.asList(new String[]{"km"}));
        fakeDefinition.setNames(Arrays.asList(new String[]{"kilometer", "kilometers", "kilometres"}));
        expect(mockUnitNormalizer.findDefinition(anyObject())).andReturn(fakeDefinition);

        replay(mockUnitNormalizer);
        Quantity.Normalized output = target.normalizeQuantity(input);

        verify(mockUnitNormalizer);
        assertThat(output.getUnit().getRawName(), is("m"));
        assertThat(output.getValue().doubleValue(), is(2000.0));
    }

    private UnitDefinition generateLenghtUnitDefinition() {
        final UnitDefinition parsedUnitDefinition = new UnitDefinition();
        parsedUnitDefinition.setType(UnitUtilities.Unit_Type.LENGTH);
        parsedUnitDefinition.setSystem(UnitUtilities.System_Type.SI_BASE);
        parsedUnitDefinition.setNotations(Arrays.asList(new String[]{"km"}));
        parsedUnitDefinition.setNames(Arrays.asList(new String[]{"kilometer", "kilometers", "kilometres"}));
        return parsedUnitDefinition;
    }

    @Test
    public void testNormalizeQuantity_wordsValue_simpleUnitWithNormalization_kmToMeters() throws Exception {
        Quantity input = new Quantity();
        input.setRawValue("twenty two");
        input.setParsedValue(valueParser.parseValue(input.getRawValue()));
        Unit raw = new Unit();
        raw.setRawName("km");
        input.setRawUnit(raw);
        final Unit parsedUnit = new Unit("km");
        parsedUnit.setUnitDefinition(generateLenghtUnitDefinition());
        expect(mockUnitNormalizer.parseUnit(raw)).andReturn(parsedUnit);
        final UnitDefinition fakeDefinition = new UnitDefinition();
        fakeDefinition.setNotations(Arrays.asList(new String[]{"km"}));
        fakeDefinition.setNames(Arrays.asList(new String[]{"kilometer", "kilometers", "kilometres"}));
        expect(mockUnitNormalizer.findDefinition(anyObject())).andReturn(fakeDefinition);

        replay(mockUnitNormalizer);
        Quantity.Normalized output = target.normalizeQuantity(input);

        verify(mockUnitNormalizer);
        assertThat(output.getUnit().getRawName(), is("m"));
        assertThat(output.getValue().doubleValue(), is(22000.0));
    }

    @Test
    public void testNormalizeQuantity_simpleUnitWithNormalization_CelsiusToKelvin() throws Exception {
        Quantity input = new Quantity();
        input.setRawValue("10");
        input.setParsedValue(valueParser.parseValue(input.getRawValue()));
        Unit raw = new Unit();
        raw.setRawName("°C");
        input.setRawUnit(raw);
        final Unit parsedUnit = new Unit("°C");
        parsedUnit.setUnitDefinition(generateTemperatureUnitDefinition());
        expect(mockUnitNormalizer.parseUnit(raw)).andReturn(parsedUnit);
        expect(mockUnitNormalizer.findDefinition(anyObject())).andReturn(new UnitDefinition());

        replay(mockUnitNormalizer);
        Quantity.Normalized output = target.normalizeQuantity(input);
        verify(mockUnitNormalizer);
        assertThat(output.getUnit().getRawName(), is("K"));
        assertThat(output.getValue().doubleValue(), is(283.15));
    }

    private UnitDefinition generateTemperatureUnitDefinition() {
        final UnitDefinition parsedUnitDefinition = new UnitDefinition();
        parsedUnitDefinition.setType(UnitUtilities.Unit_Type.TEMPERATURE);
        parsedUnitDefinition.setSystem(UnitUtilities.System_Type.SI_DERIVED);
        parsedUnitDefinition.setNotations(Arrays.asList(new String[]{"°C"}));
        parsedUnitDefinition.setNames(Arrays.asList(new String[]{"celsius", "celcius"}));
        return parsedUnitDefinition;

    }

    @Test
    public void testNormalizeQuantity_kmHourToMetersSecond() throws Exception {
        Quantity input = new Quantity();
        input.setRawValue("2");
        input.setParsedValue(valueParser.parseValue(input.getRawValue()));
        Unit raw = new Unit();
        raw.setRawName("km/h");
        input.setRawUnit(raw);
        final Unit parsedUnit = new Unit("km/h");
        UnitDefinition unitDefinition = new UnitDefinition();
        unitDefinition.setSystem(UnitUtilities.System_Type.SI_DERIVED);
        unitDefinition.setType(UnitUtilities.Unit_Type.VELOCITY);
        parsedUnit.setUnitDefinition(unitDefinition);

        expect(mockUnitNormalizer.parseUnit(raw)).andReturn(parsedUnit);
        expect(mockUnitNormalizer.findDefinition(anyObject())).andReturn(new UnitDefinition());

        replay(mockUnitNormalizer);
        Quantity.Normalized output = target.normalizeQuantity(input);
        verify(mockUnitNormalizer);
        assertThat(output.getUnit().getRawName(), is("m/s"));
        assertThat(output.getValue().doubleValue(), is(0.5555555555555556));
    }

    @Test
    public void testNormalizeQuantity2_3composedUnits() throws Exception {
        Quantity input = new Quantity();
        input.setRawValue("2000");
        input.setParsedValue(valueParser.parseValue(input.getRawValue()));
        Unit raw = new Unit();
        raw.setRawName("km*g/h");
        input.setRawUnit(raw);
        final Unit parsedUnit = new Unit("km·g/h");
        UnitDefinition unitDefinition = new UnitDefinition();
        unitDefinition.setSystem(UnitUtilities.System_Type.SI_DERIVED);
        unitDefinition.setType(UnitUtilities.Unit_Type.VELOCITY);
        parsedUnit.setUnitDefinition(unitDefinition);
        expect(mockUnitNormalizer.parseUnit(raw)).andReturn(parsedUnit);
        expect(mockUnitNormalizer.findDefinition(anyObject())).andReturn(new UnitDefinition());

        replay(mockUnitNormalizer);

        Quantity.Normalized output = target.normalizeQuantity(input);
        verify(mockUnitNormalizer);
        assertThat(output.getValue().doubleValue(), is(0.5555555555555556));
        assertThat(output.getUnit().getRawName(), is("m·kg/s"));
    }

    @Test
    public void testNormalizeQuantity3_2composedUnits() throws Exception {
        Quantity input = new Quantity();
        input.setRawValue("2000");
        input.setParsedValue(valueParser.parseValue(input.getRawValue()));
        Unit raw = new Unit();
        raw.setRawName("km*kg/h");
        input.setRawUnit(raw);

        final Unit parsedUnit = new Unit("km·kg/h");
        UnitDefinition unitDefinition = new UnitDefinition();
        unitDefinition.setSystem(UnitUtilities.System_Type.SI_DERIVED);
        unitDefinition.setType(UnitUtilities.Unit_Type.VELOCITY);
        parsedUnit.setUnitDefinition(unitDefinition);

        expect(mockUnitNormalizer.parseUnit(raw)).andReturn(parsedUnit);
        expect(mockUnitNormalizer.findDefinition(anyObject())).andReturn(new UnitDefinition());

        replay(mockUnitNormalizer);

        Quantity.Normalized output = target.normalizeQuantity(input);
        verify(mockUnitNormalizer);
        assertThat(output.getValue().doubleValue(), is(555.5555555555555));
        assertThat(output.getUnit().getRawName(), is("m·kg/s"));
    }

    @Test
    public void testNormalizeQuantity_simpleUnitWithoutNormalization_meters() throws Exception {
        Quantity input = new Quantity();
        input.setRawValue("2");
        input.setParsedValue(valueParser.parseValue(input.getRawValue()));
        Unit raw = new Unit();
        raw.setRawName("m");
        input.setRawUnit(raw);

        final Unit parsedUnit = new Unit("m");
        parsedUnit.setUnitDefinition(generateLenghtUnitDefinition());
        expect(mockUnitNormalizer.parseUnit(raw)).andReturn(parsedUnit);
        expect(mockUnitNormalizer.findDefinition(anyObject())).andReturn(new UnitDefinition());

        replay(mockUnitNormalizer);
        Quantity.Normalized output = target.normalizeQuantity(input);

        verify(mockUnitNormalizer);
        assertThat(output.getUnit().getRawName(), is("m"));
        assertThat(output.getValue().doubleValue(), is(2.0));
    }

    /*@Test
    public void testExtractProduct_productOfBaseUnits() throws Exception {
        String unitSymbol = "m/s";

        ProductUnit productUnit = (ProductUnit) target.parseUnit(unitSymbol);

        Map<String, Integer> output = target.extractProduct(productUnit);
        assertThat(output.size(), is(2));
        assertThat(output.get("s"), is(-1));
        assertThat(output.get("m"), is(1));
    }

    @Test
    public void testExtractProduct_productOfTransformedUnits() throws Exception {
        String unitSymbol = "km/h";

        ProductUnit productUnit = (ProductUnit) target.parseUnit(unitSymbol);

        Map<String, Integer> output = target.extractProduct(productUnit);
        assertThat(output.size(), is(2));
        assertThat(output.get("h"), is(-1));
        assertThat(output.get("km"), is(1));
    }

    @Test
    public void testExtractProduct_productOfSeveralTransformedUnits() throws Exception {
        String unitSymbol = "km/h*kg";

        ProductUnit productUnit = (ProductUnit) target.parseUnit(unitSymbol);

        Map<String, Integer> output = target.extractProduct(productUnit);
        assertThat(output.size(), is(3));
        assertThat(output.get("h"), is(-1));
        assertThat(output.get("km"), is(1));
        assertThat(output.get("kg"), is(-1));
    }*/

    @Test
    public void testCheckPrecision() throws Exception {
        UnitFormatService formatService = ServiceProvider.current().getUnitFormatService();
        UnitFormat defaultFormatService = formatService.getUnitFormat();

        TransformedUnit unit = (TransformedUnit) defaultFormatService.parse("g");
        System.out.println("Conversion using double: " + unit.getSystemConverter().convert(0.39));
        System.out.println("Conversion using BigDecimal: " + (unit.getSystemConverter().convert(new BigDecimal("0.39"))));
        System.out.println("Conversion using BigDecimal output Double: " + new BigDecimal(unit.getSystemConverter().convert(new BigDecimal("0.39")).toString()).doubleValue());

        unit = (TransformedUnit) defaultFormatService.parse("%");
        System.out.println("Conversion using double: " + unit.getSystemConverter().convert(0.009));
        System.out.println("Conversion using BigDecimal: " + (unit.getSystemConverter().convert(new BigDecimal("0.009"))));
        System.out.println("Conversion using BigDecimal output Double: " + new BigDecimal(unit.getSystemConverter().convert(new BigDecimal("0.009")).toString()).doubleValue());

        unit = (TransformedUnit) defaultFormatService.parse("ml");
        System.out.println("Conversion using double: " + unit.getSystemConverter().convert(0.39));
        System.out.println("Conversion using BigDecimal: " + (unit.getSystemConverter().convert(new BigDecimal("0.39"))));
        System.out.println("Conversion using BigDecimal output Double: " + new BigDecimal(unit.getSystemConverter().convert(new BigDecimal("0.39")).toString()).doubleValue());
    }
}