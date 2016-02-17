package org.grobid.core.data.normalization;

import org.grobid.core.data.Quantity;
import org.grobid.core.data.Unit;
import tec.units.ri.unit.BaseUnit;
import tec.units.ri.unit.ProductUnit;
import tec.units.ri.unit.TransformedUnit;

import javax.measure.Dimension;
import javax.measure.format.ParserException;
import javax.measure.format.UnitFormat;
import javax.measure.spi.Bootstrap;
import javax.measure.spi.UnitFormatService;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by lfoppiano on 14.02.16.
 */
public class NormalizationWrapper {


    private final UnitFormat format;

    public NormalizationWrapper() {
        UnitFormatService formatService = Bootstrap.getService(UnitFormatService.class);
        format = formatService.getUnitFormat();
    }


    public Quantity normalizeQuantityToBaseUnits(Quantity quantity) throws NormalizationException {
        if (quantity.isEmpty()) {
            return quantity;    //or throw new NormalizationException() :-)
        }
        javax.measure.Unit parsedUnit = parseUnit(quantity.getRawUnit().getRawName());

        return normalizeQuantityToBaseUnits(quantity, parsedUnit);
    }


    protected javax.measure.Unit parseUnit(String rawUnit) throws NormalizationException {
        javax.measure.Unit parsedUnit = null;

        try {
            parsedUnit = format.parse(rawUnit);
        } catch (ParserException pe) {
            throw new NormalizationException("The value " + rawUnit + "cannot be normalized. It is either not a valid unit" +
                    "or it is not recognized from the available parsers.", pe);
        }

        /**
         * workaround to avoid passing througt with a null parsedUnit - in unit-ri version 0.9 the parse() method is
         *  (more correctly, IMHO) throwing a ParserException (see above)
         */
        if (parsedUnit == null) {
            throw new NormalizationException("The value " + rawUnit + "cannot be normalized. It is either not a valid unit" +
                    "or it is not recognized from the available parsers.", new ParserException(new RuntimeException()));
        }

        return parsedUnit;
    }

    protected Quantity normalizeQuantityToBaseUnits(Quantity quantity, javax.measure.Unit unit) {
        Map<String, Integer> wrappedUnitProducts = new HashMap<>();
        Unit normalizedUnit = new Unit();
        if (unit instanceof TransformedUnit) {

            TransformedUnit transformedUnit = (TransformedUnit) unit;
            normalizedUnit.setRawName(transformedUnit.getParentUnit().toString());
            quantity.setNormalizedUnit(normalizedUnit);
            quantity.setNormalizedValue(transformedUnit.getConverter().convert(Double.parseDouble(quantity.getRawValue())));
            wrappedUnitProducts.put(transformedUnit.getSymbol(), 1);

        } else if (unit instanceof ProductUnit) {

            ProductUnit productUnit = (ProductUnit) unit;
            Map<String, Integer> products = extractProduct(productUnit);
            quantity.setProductForm(products);
            normalizedUnit.setRawName(productUnit.toSystemUnit().toString());
            quantity.setNormalizedUnit(normalizedUnit);
            quantity.setNormalizedValue(productUnit.getSystemConverter().convert(Double.parseDouble(quantity.getRawValue())));

        } else {

            normalizedUnit.setRawName(unit.getSymbol());
            quantity.setNormalizedUnit(normalizedUnit);
            quantity.setNormalizedValue(Double.parseDouble(quantity.getRawValue()));
        }

        quantity.setProductForm(wrappedUnitProducts);
        return quantity;
    }

    protected Map<String, Integer> extractProduct(ProductUnit productUnit) {
        Map<javax.measure.Unit, Integer> products = productUnit.getProductUnits();
        Map<String, Integer> wrappedUnitProducts = new HashMap<>();

        for (Map.Entry<javax.measure.Unit, Integer> productFactor : products.entrySet()) {
            if (productFactor.getKey().getSymbol() != null) {
                wrappedUnitProducts.put(productFactor.getKey().getSymbol(), productFactor.getValue());
            } else {
                String unitName = this.format.format(productFactor.getKey());
                wrappedUnitProducts.put(unitName, productFactor.getValue());
            }
        }
        return wrappedUnitProducts;
    }
}
