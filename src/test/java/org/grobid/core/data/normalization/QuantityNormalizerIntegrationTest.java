package org.grobid.core.data.normalization;

import org.easymock.Capture;
import org.grobid.core.data.*;
import org.grobid.core.main.LibraryLoader;
import org.grobid.core.utilities.UnitUtilities;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import javax.measure.format.UnitFormat;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.easymock.EasyMock.*;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;


//@Ignore("we should mock also uom .. but for now is better to leave it out so that we can spot other errors")
public class QuantityNormalizerIntegrationTest {

    private QuantityNormalizer target;
    private UnitNormalizer mockUnitNormalizer;

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
        input.setParsedValue(new Value(new BigDecimal(2)));
        Unit raw = new Unit();
        raw.setRawName("km");
        input.setRawUnit(raw);
        final Unit parsedUnit = new Unit("km");
        parsedUnit.setUnitDefinition(generateLenghtUnitDefinition());
        expect(mockUnitNormalizer.parseUnit(raw)).andReturn(parsedUnit);
        final UnitDefinition fakeDefinition = new UnitDefinition();
        fakeDefinition.setNotations(Arrays.asList("km"));
        fakeDefinition.setNames(Arrays.asList("kilometer", "kilometers", "kilometres"));
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
        input.setParsedValue(new Value(new BigDecimal(22))); //assuming the value parser works perfectly!
        Unit raw = new Unit();
        raw.setRawName("km");
        input.setRawUnit(raw);
        final Unit parsedUnit = new Unit("km");
        parsedUnit.setUnitDefinition(generateLenghtUnitDefinition());
        expect(mockUnitNormalizer.parseUnit(raw)).andReturn(parsedUnit);
        final UnitDefinition fakeDefinition = new UnitDefinition();
        fakeDefinition.setNotations(Arrays.asList("km"));
        fakeDefinition.setNames(Arrays.asList("kilometer", "kilometers", "kilometres"));
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
        input.setParsedValue(new Value(new BigDecimal(10)));
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
        parsedUnitDefinition.setNotations(Arrays.asList("°C"));
        parsedUnitDefinition.setNames(Arrays.asList("celsius", "celcius"));
        return parsedUnitDefinition;

    }

    @Test
    public void testNormalizeQuantity_kmHourToMetersSecond() throws Exception {
        Quantity input = new Quantity();
        input.setRawValue("2");
        input.setParsedValue(new Value(new BigDecimal(2)));
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
    @Ignore("At this stage the h^-1 is not correctly normalised")
    public void testNormalizeQuantity2_3composedUnits() throws Exception {
        Quantity input = new Quantity();
        input.setRawValue("2000");
        input.setParsedValue(new Value(new BigDecimal(2000)));
        Unit raw = new Unit();
        raw.setRawName("km*g/h");
        input.setRawUnit(raw);
        final Unit parsedUnit = new Unit("km·kg/h");
//        UnitDefinition unitDefinition = new UnitDefinition();
//        unitDefinition.setSystem(UnitUtilities.System_Type.SI_DERIVED);
//        unitDefinition.setType(UnitUtilities.Unit_Type.VELOCITY);
//        parsedUnit.setUnitDefinition(unitDefinition);
        expect(mockUnitNormalizer.parseUnit(raw)).andReturn(parsedUnit);
        expect(mockUnitNormalizer.findDefinition(anyObject())).andReturn(null);

        replay(mockUnitNormalizer);

        Quantity.Normalized output = target.normalizeQuantity(input);
        verify(mockUnitNormalizer);
        assertThat(output.getValue().doubleValue(), is(0.5555555555555556));
        assertThat(output.getUnit().getRawName(), is("m·kg/s"));
    }

    @Test
    @Ignore("At this stage the h^-1 is not correctly normalised")
    public void testNormalizeQuantity3_2composedUnits() throws Exception {
        Quantity input = new Quantity();
        input.setRawValue("2000");
        input.setParsedValue(new Value(new BigDecimal(2000)));
        Unit raw = new Unit();
        raw.setRawName("km*kg/h");
        input.setRawUnit(raw);

        final Unit parsedUnit = new Unit("km·kg/h");
//        UnitDefinition unitDefinition = new UnitDefinition();
//        unitDefinition.setSystem(UnitUtilities.System_Type.SI_DERIVED);
//        unitDefinition.setType(UnitUtilities.Unit_Type.VELOCITY);
//        parsedUnit.setUnitDefinition(unitDefinition);

        expect(mockUnitNormalizer.parseUnit(raw)).andReturn(parsedUnit);
//        expect(mockUnitNormalizer.findDefinition(anyObject())).andReturn(new UnitDefinition());
        expect(mockUnitNormalizer.findDefinition(anyObject())).andReturn(null);

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
        input.setParsedValue(new Value(new BigDecimal(2)));
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

    @Test
    public void testNormalise_imperialUnits() throws Exception {
        Quantity input = new Quantity();
        input.setRawValue("10");
        input.setParsedValue(new Value(new BigDecimal(1)));
        Unit raw = new Unit();
        raw.setRawName("miles");
        input.setRawUnit(raw);

        Unit parsedUnit = new Unit("miles");
        List<UnitBlock> blocks = new ArrayList<>();
        blocks.add(new UnitBlock(null, "miles", null));
        parsedUnit.setProductBlocks(blocks);
        input.setParsedUnit(parsedUnit);

        expect(mockUnitNormalizer.parseUnit(raw)).andReturn(parsedUnit);
        Capture<Unit> unitCapture = newCapture();
        expect(mockUnitNormalizer.findDefinition(capture(unitCapture))).andReturn(null);


        replay(mockUnitNormalizer);
        Quantity.Normalized normalized = target.normalizeQuantity(input);
        verify(mockUnitNormalizer);

        assertThat(unitCapture.getValue().getRawName(), is("m"));

        assertThat(normalized.getUnit().getRawName(), is("m"));
        assertThat(normalized.getValue(), is(new BigDecimal("1609.344")));

    }

    @Test
    public void testTryParsing() throws Exception {
        Unit parsedUnit = new Unit("kg/cc");

        List<UnitBlock> blocks = new ArrayList<>();

        blocks.add(new UnitBlock("k", "g", null));
        blocks.add(new UnitBlock(null, "cc", "-1"));

        parsedUnit.setProductBlocks(blocks);

        Map<String, UnitFormat> unitFormats = target.getUnitFormats();

//        Arrays.asList(unitFormats.get(UCUM_PROVIDER), unitFormats.get(COMMON_PROVIDER), unitFormats.get(INDYRIA_PROVIDER));

//        target.tryParsing(parsedUnit, );
    }
}