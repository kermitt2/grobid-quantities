package org.grobid.core.engines.training;

import nu.xom.Attribute;
import nu.xom.Element;
import org.grobid.core.data.Measurement;
import org.grobid.core.data.QuantifiedObject;
import org.grobid.core.data.Quantity;
import org.grobid.core.data.Unit;
import org.grobid.core.utilities.UnitUtilities;

import java.util.List;
import java.util.UUID;

import static org.grobid.core.document.xml.XmlBuilderUtils.teiElement;

public class QuantifiedObjectTrainingFormatter {

    protected Element trainingExtraction(List<Measurement> measurements, String text) {
        Element p = teiElement("p");

        int pos = 0;
        for (Measurement measurement : measurements) {
            Element measure = teiElement("measure");

            int startO = -1;
            int endO = -1;
            final QuantifiedObject quantifiedObject = measurement.getQuantifiedObject();
            Element quantifiedObjectElement = teiElement("quantifiedObject");
            String quantifiedObjectID = null;
            if (quantifiedObject != null) {
                startO = quantifiedObject.getOffsetStart();
                endO = quantifiedObject.getOffsetEnd();

                final String textObject = text.substring(startO, endO);
                quantifiedObjectID = UUID.randomUUID().toString();
                quantifiedObjectElement.appendChild(textObject);
                quantifiedObjectElement.addAttribute(new Attribute("id", quantifiedObjectID));
            }

            if (measurement.getType() == UnitUtilities.Measurement_Type.VALUE) {
                measure.addAttribute(new Attribute("type", "value"));
                Quantity quantity = measurement.getQuantityAtomic();
                if (quantity == null)
                    continue;

                int startQ = quantity.getOffsetStart();
                int endQ = quantity.getOffsetEnd();

                Unit unit = quantity.getRawUnit();
                int endU = -1;
                int startU = -1;

                if (unit != null) {
                    startU = unit.getOffsetStart();
                    endU = unit.getOffsetEnd();
                }

                boolean addedMeasure = false;
                int initPos = pos;

                while (pos < text.length()) {

                    if (pos == startQ) {
                        if (!addedMeasure) {
                            if (quantifiedObject != null) {
                                measure.addAttribute(new Attribute("ptr", "#" + quantifiedObjectID));
                            }
                            p.appendChild(text.substring(initPos, startQ));
                            p.appendChild(measure);
                            addedMeasure = true;
                        } else {
                            measure.appendChild(text.substring(initPos, startQ));
                        }
                        measure.appendChild(text.substring(startQ, endQ));
                        pos = endQ;
                        initPos = pos;
                    }
                    if (pos == startU) {
                        if (!addedMeasure) {
                            if (quantifiedObject != null) {
                                measure.addAttribute(new Attribute("ptr", "#" + quantifiedObjectID));
                            }
                            p.appendChild(text.substring(initPos, startU));
                            p.appendChild(measure);
                            addedMeasure = true;
                        } else {
                            measure.appendChild(text.substring(initPos, startU));
                        }
                        measure.appendChild(text.substring(startU, endU));
                        pos = endU;
                        initPos = pos;

//                        measure.appendChild(text.substring(initPos, startU));
//                        measure.appendChild(text.substring(startU, endU));
//                        pos = endU;
//                        initPos = pos;
                    }
                    if (pos == startO) {
                        p.appendChild(text.substring(initPos, startO));
                        p.appendChild(quantifiedObjectElement);
                        pos = endO;
                        initPos = pos;
                    }

                    if (pos >= endQ && pos >= endU && pos >= endO)
                        break;
                    pos++;
                }
            } else if (measurement.getType() == UnitUtilities.Measurement_Type.INTERVAL_MIN_MAX ||
                    measurement.getType() == UnitUtilities.Measurement_Type.INTERVAL_BASE_RANGE) {
                measure.addAttribute(new Attribute("type", "interval"));

                Quantity quantityLeast = measurement.getQuantityLeast();
                Quantity quantityMost = measurement.getQuantityMost();

                if(measurement.getType() == UnitUtilities.Measurement_Type.INTERVAL_BASE_RANGE) {
                    quantityLeast = measurement.getQuantityBase();
                    quantityMost = measurement.getQuantityRange();
                }

                if (quantityLeast == null && quantityMost == null)
                    continue;

                int startQL = -1;
                int endQL = -1;
                int startUL = -1;
                int endUL = -1;

                if (quantityLeast != null) {
                    startQL = quantityLeast.getOffsetStart();
                    endQL = quantityLeast.getOffsetEnd();

                    Unit unitL = quantityLeast.getRawUnit();
                    if (unitL != null) {
                        startUL = unitL.getOffsetStart();
                        endUL = unitL.getOffsetEnd();
                    }
                }

                int startQM = -1;
                int endQM = -1;
                int startUM = -1;
                int endUM = -1;

                if (quantityMost != null) {
                    startQM = quantityMost.getOffsetStart();
                    endQM = quantityMost.getOffsetEnd();

                    Unit unitM = quantityMost.getRawUnit();
                    if (unitM != null) {
                        startUM = unitM.getOffsetStart();
                        endUM = unitM.getOffsetEnd();
                    }
                }

                boolean addedMeasure = false;
                int initPos = pos;
                while (pos < text.length()) {
                    if (pos == startQL) {
                        if (!addedMeasure) {
                            if (quantifiedObject != null) {
                                measure.addAttribute(new Attribute("ptr", "#" + quantifiedObjectID));
//                                markPosition(measure, quantifiedObject, startQL);
                            }
                            p.appendChild(text.substring(initPos, startQL));
                            p.appendChild(measure);
                            addedMeasure = true;
                        } else {
                            measure.appendChild(text.substring(initPos, startQL));
                        }
                        measure.appendChild(text.substring(startQL, endQL));
                        pos = endQL;
                        initPos = pos;
                    }
                    if (pos == startQM) {
                        if (!addedMeasure) {
                            if (quantifiedObject != null) {
                                measure.addAttribute(new Attribute("ptr", "#" + quantifiedObjectID));
//                                markPosition(measure, quantifiedObject, startQM);
                            }
                            p.appendChild(text.substring(initPos, startQM));
                            p.appendChild(measure);
                            addedMeasure = true;
                        } else {
                            measure.appendChild(text.substring(initPos, startQM));
                        }
                        measure.appendChild(text.substring(startQM, endQM));
                        pos = endQM;
                        initPos = pos;
                    }
                    if (pos == startUL) {
//                            measure.appendChild(text.substring(initPos, endUL));
//
//                            pos = endUL;
//                            initPos = pos;

                        if (!addedMeasure) {
                            if (quantifiedObject != null) {
                                measure.addAttribute(new Attribute("ptr", "#" + quantifiedObjectID));
                            }
                            p.appendChild(text.substring(initPos, startUL));
                            p.appendChild(measure);
                            addedMeasure = true;
                        } else {
                            measure.appendChild(text.substring(initPos, startUL));
                        }
                        measure.appendChild(text.substring(startUL, endUL));
                        pos = endUL;
                        initPos = pos;
                    }
                    if ((pos == startUM) && (startUM != startUL)) {
//                        measure.appendChild(text.substring(initPos, endUM));
//                        pos = endUM;
//                        initPos = pos;

                        if (!addedMeasure) {
                            if (quantifiedObject != null) {
                                measure.addAttribute(new Attribute("ptr", "#" + quantifiedObjectID));
                            }
                            p.appendChild(text.substring(initPos, startUM));
                            p.appendChild(measure);
                            addedMeasure = true;
                        } else {
                            measure.appendChild(text.substring(initPos, startUM));
                        }
                        measure.appendChild(text.substring(startUM, endUM));
                        pos = endUM;
                        initPos = pos;
                    }
                    if (pos == startO) {
                        p.appendChild(text.substring(initPos, startO));
                        p.appendChild(quantifiedObjectElement);
                        pos = endO;
                        initPos = pos;
                    }

                    if ((pos >= endQL) && (pos >= endQM)
                            && (pos >= endUL) && (pos >= endUM)
                            && (pos >= endO))
                        break;
                    pos++;
                }
            } else if (measurement.getType() == UnitUtilities.Measurement_Type.CONJUNCTION) {
                measure.addAttribute(new Attribute("type", "list"));
                List<Quantity> quantities = measurement.getQuantityList();
                int initPos = pos;

                boolean measureAdded = false;
                for (Quantity quantity : quantities) {
                    int startQ = quantity.getOffsetStart();
                    int endQ = quantity.getOffsetEnd();

                    Unit unit = quantity.getRawUnit();
                    int startU = -1;
                    int endU = -1;
                    if (unit != null) {
                        startU = unit.getOffsetStart();
                        endU = unit.getOffsetEnd();
                    }

                    while (pos < text.length()) {
                        if (pos == startQ) {
                            if (!measureAdded) {
                                p.appendChild(text.substring(initPos, startQ));
                            } else {
                                measure.appendChild(text.substring(initPos, startQ));
                            }
                            measure.appendChild(text.substring(startQ, endQ));
                            if (!measureAdded) {
                                if (quantifiedObject != null) {
                                    measure.addAttribute(new Attribute("ptr", "#" + quantifiedObjectID));
//                                    markPosition(measure, quantifiedObject, startQ);
                                }
                                p.appendChild(measure);
                                measureAdded = true;
                            }
                            pos = endQ;
                            initPos = endQ;
                        }
                        if (pos == startU) {
                            measure.appendChild(text.substring(initPos, endU));
                            pos = endU;
                            initPos = endU;
                        }
                        if (pos == startO) {
                            p.appendChild(text.substring(initPos, startO));
                            p.appendChild(quantifiedObjectElement);
                            pos = endO;
                            initPos = pos;
                        }

                        if (pos >= endQ && pos >= endU && pos >= endO) {
                            break;
                        }

                        pos++;
                    }
                }
            }
        }

        p.appendChild(text.substring(pos));

        return p;
    }

    private void markPosition(Element measure, QuantifiedObject quantifiedObject, int startQL) {
        if (quantifiedObject.getOffsetStart() > -1 && quantifiedObject.getOffsetStart() > startQL) {
            measure.addAttribute(new Attribute("pos", "left"));
        } else {
            measure.addAttribute(new Attribute("pos", "right"));
        }
    }
}
