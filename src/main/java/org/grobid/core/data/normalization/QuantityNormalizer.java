package org.grobid.core.data.normalization;

import org.grobid.core.data.Quantity;
import org.grobid.core.data.Unit;
import org.grobid.core.data.UnitDefinition;
import org.grobid.core.utilities.MeasurementOperations;
import org.grobid.core.utilities.UnitUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.uom.ucum.format.UCUMFormat;
import systems.uom.ucum.internal.format.TokenException;
import systems.uom.ucum.internal.format.TokenMgrError;
import tec.uom.se.unit.ProductUnit;
import tec.uom.se.unit.TransformedUnit;

import javax.measure.format.ParserException;
import javax.measure.format.UnitFormat;
import javax.measure.spi.ServiceProvider;
import javax.measure.spi.UnitFormatService;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static systems.uom.ucum.format.UCUMFormat.Variant.CASE_INSENSITIVE;

/**
 * This class is responsible for the normalization of the quantity.
 * The quantity normalization requires:
 * - parsed value
 * - parsed unit
 * <p>
 * Created by lfoppiano on 14.02.16.
 */
public class QuantityNormalizer {

    private static final Logger LOGGER = LoggerFactory.getLogger(QuantityNormalizer.class);
    //    private final UnitFormat ucumFormatService = UCUMFormat.getInstance(UCUMFormat.Variant.CASE_SENSITIVE);
    private UnitFormat ucumFormatService;
    private UnitFormat defaultFormatService;
    private UnitFormat unicodeFormatService;
    private UnitFormat siFormatService;

    private MeasurementOperations measurementOperations;
    private UnitNormalizer unitNormalizer;

    public QuantityNormalizer() {
        ServiceProvider defaultProvider = ServiceProvider.current(); // just a fallback to avoid uninitialized variable
        for (ServiceProvider provider : ServiceProvider.available()) {
            UnitFormatService formatService = provider.getUnitFormatService();
            switch (provider.getClass().getSimpleName()) {
                case "DefaultServiceProvider":
                    this.defaultFormatService = formatService.getUnitFormat();
                    break;

                case "UCUMServiceProvider":
                    this.ucumFormatService = formatService.getUnitFormat();
                    break;

                case "UnicodeServiceProvider":
                    this.unicodeFormatService = formatService.getUnitFormat();
                    break;

                case "SIServiceProvider":
                    this.siFormatService = formatService.getUnitFormat();
                    break;

            }
        }


        measurementOperations = new MeasurementOperations();

        unitNormalizer = new UnitNormalizer();

    }

    public Quantity.Normalized normalizeQuantity(Quantity quantity) throws NormalizationException {
        if (quantity.isEmpty() || quantity.getRawUnit() == null || isEmpty(quantity.getRawUnit().getRawName())) {
            return null;    //or throw new NormalizationException() :-)
        }

        Unit parsedUnit = unitNormalizer.parseUnit(quantity.getRawUnit());
        quantity.setParsedUnit(parsedUnit);

        //The unit cannot be found between the known units - we should try to decompose it
        if (parsedUnit.getUnitDefinition() == null) {
            //not sure this is working
            return normalizeNonSIQuantities(quantity);
        } else if (((parsedUnit.getUnitDefinition().getSystem() == UnitUtilities.System_Type.SI_BASE) ||
                (parsedUnit.getUnitDefinition().getSystem() == UnitUtilities.System_Type.SI_DERIVED))) {

            //I normalize with the simpleUnitFormat only SI and SI_DERIVED units...
            return normalizeSIQuantities(quantity);
        } else {
            return normalizeNonSIQuantities(quantity);
        }
    }

//    private Quantity.Normalized normalizeObscureQuantity(Quantity quantity) {
//        Map<String, Integer> wrappedUnitProducts = new HashMap<>();
//        Quantity.Normalized normalizedQuantity = new Quantity().new Normalized();
//
//        javax.measure.Unit unit = defaultFormatService.parse(quantity.getParsedUnit().getRawName());
//
//        composeUnit(quantity, wrappedUnitProducts, normalizedQuantity, unit);
//
//        if (quantity.isNormalized()) {
//            UnitDefinition definition = unitNormalizer.findDefinition(quantity.getNormalizedQuantity().getUnit());
//            if (definition != null) {
//                quantity.getNormalizedQuantity().getUnit().setUnitDefinition(definition);
//            }
//        }
//
//        return normalizedQuantity;
//    }

    protected Quantity.Normalized normalizeNonSIQuantities(Quantity quantity) throws NormalizationException {
        Map<String, Integer> wrappedUnitProducts = new HashMap<>();
        Quantity.Normalized normalizedQuantity = new Quantity().new Normalized();

        javax.measure.Unit unit = null;
        try {
            unit = ucumFormatService.parse(quantity.getParsedUnit().getRawName());
        } catch (TokenException te) {
            throw new NormalizationException("Cannot normalise " + quantity.getParsedUnit().getRawName(), te);
        } catch (TokenMgrError error) {
            throw new NormalizationException("Cannot normalise " + quantity.getParsedUnit().getRawName());
        }

        composeUnit(quantity, wrappedUnitProducts, normalizedQuantity, unit);

        if (quantity.isNormalized()) {
            UnitDefinition definition = unitNormalizer.findDefinition(quantity.getNormalizedQuantity().getUnit());
            if (definition != null) {
                quantity.getNormalizedQuantity().getUnit().setUnitDefinition(definition);
            }
        }

        return normalizedQuantity;
    }


    protected Quantity.Normalized normalizeSIQuantities(Quantity quantity) throws NormalizationException {
        Map<String, Integer> wrappedUnitProducts = new HashMap<>();
        Quantity.Normalized normalizedQuantity = new Quantity().new Normalized();

        javax.measure.Unit unit = defaultFormatService.parse(quantity.getParsedUnit().getRawName());

        composeUnit(quantity, wrappedUnitProducts, normalizedQuantity, unit);

        if (quantity.isNormalized()) {
            UnitDefinition definition = unitNormalizer.findDefinition(quantity.getNormalizedQuantity().getUnit());
            if (definition != null) {
                quantity.getNormalizedQuantity().getUnit().setUnitDefinition(definition);
            }
        }

        return normalizedQuantity;
    }

    private void composeUnit(Quantity quantity, Map<String, Integer> wrappedUnitProducts, Quantity.Normalized normalizedQuantity, javax.measure.Unit unit) throws NormalizationException {
        if (unit instanceof TransformedUnit) {
            TransformedUnit transformedUnit = (TransformedUnit) unit;
            normalizedQuantity.setUnit(new Unit(transformedUnit.getParentUnit().toString()));
            try {
                normalizedQuantity.setValue(new BigDecimal(transformedUnit.getConverter().convert(quantity.getParsedValue().getNumeric()).toString()));
            } catch (Exception e) {
                throw new NormalizationException("The value " + quantity.getRawValue() + " cannot be normalized. It is either not a valid value " +
                        "or it is not recognized from the available parsers.", new ParserException(new RuntimeException()));
            }
            quantity.setNormalizedQuantity(normalizedQuantity);
            wrappedUnitProducts.put(transformedUnit.getSymbol(), 1);

        } else if (unit instanceof ProductUnit) {
            ProductUnit productUnit = (ProductUnit) unit;
            //Map<String, Integer> products = extractProduct(productUnit);
            normalizedQuantity.setUnit(new Unit(productUnit.toSystemUnit().toString()));

            quantity.setNormalizedQuantity(normalizedQuantity);
            try {
                normalizedQuantity.setValue(new BigDecimal(productUnit.getSystemConverter().convert(quantity.getParsedValue().getNumeric()).toString()));
            } catch (Exception e) {
                throw new NormalizationException("The value " + quantity.getRawValue() + " cannot be normalized. It is either not a valid value " +
                        "or it is not recognized from the available parsers.");
            }

        } else {
            normalizedQuantity.setRawValue(unit.getSymbol());
            normalizedQuantity.setUnit(new Unit(unit.getSymbol()));
            try {
                if (quantity.getParsedValue() != null) {
                    normalizedQuantity.setValue(quantity.getParsedValue().getNumeric());
                } else {
                    normalizedQuantity.setValue(new BigDecimal(quantity.getRawValue()));
                }
            } catch (Exception e) {
                throw new NormalizationException("The value " + quantity.getRawValue() + " cannot be normalized. It is either not a valid value " +
                        "or it is not recognized from the available parsers.");
            }
            quantity.setNormalizedQuantity(normalizedQuantity);
        }
    }

    protected Map<String, Integer> extractProduct(ProductUnit productUnit) {
        Map<javax.measure.Unit, Integer> products = productUnit.getBaseUnits();
        Map<String, Integer> wrappedUnitProducts = new HashMap<>();

        for (Map.Entry<javax.measure.Unit, Integer> productFactor : products.entrySet()) {
            if (productFactor.getKey().getSymbol() != null) {
                wrappedUnitProducts.put(productFactor.getKey().getSymbol(), productFactor.getValue());
            } else {
                String unitName = this.defaultFormatService.format(productFactor.getKey());
                wrappedUnitProducts.put(unitName, productFactor.getValue());
            }
        }
        return wrappedUnitProducts;
    }

    public void setUnitNormalizer(UnitNormalizer unitNormalizer) {
        this.unitNormalizer = unitNormalizer;
    }
}
