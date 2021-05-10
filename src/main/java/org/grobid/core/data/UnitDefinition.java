package org.grobid.core.data;

import com.fasterxml.jackson.core.io.JsonStringEncoder;
import com.fasterxml.jackson.core.util.BufferRecyclers;
import org.grobid.core.utilities.UnitUtilities;

import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

/**
 * Created by lfoppiano on 18.02.16.
 */
public class UnitDefinition {

    public UnitDefinition() {}

    public UnitDefinition(UnitUtilities.Unit_Type type, UnitUtilities.System_Type system) {
        this.type = type;
        this.system = system;
    }
    // usual full names for the unit, e.g. metre, meter
    private List<String> names = null;

    // standard notation, e.g. g for gram - there might be several notations for an unit
    private List<String> notations = null;

    private boolean supportsPrefixes = false;

    // If this unit doesn't have any transformation (e.g. pH intended as acidity)
    private boolean skipNormalisation = false;

    private UnitUtilities.Unit_Type type;               // type of measurement

    private UnitUtilities.System_Type system;           // type of system of unit

    public List<String> getNames() {
        return names;
    }

    public void setNames(List<String> names) {
        this.names = names;
    }

    public void addName(String name) {
        if (names == null) {
            names = new ArrayList<>();
        }
        names.add(name);
    }

    public List<String> getNotations() {
        return notations;
    }

    public void setNotations(List<String> not) {
        this.notations = not;
    }

    public void addNotation(String not) {
        if (notations == null)
            notations = new ArrayList<>();
        notations.add(not);
    }

    public UnitUtilities.System_Type getSystem() {
        return system;
    }

    public void setSystem(UnitUtilities.System_Type si) {
        system = si;
    }

    public UnitUtilities.Unit_Type getType() {
        return type;
    }

    public void setType(UnitUtilities.Unit_Type ty) {
        type = ty;
    }


    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[ ");

        if (notations != null) {
            builder.append(notations.toString()).append("\t");
        }

        if (type != null) {
            builder.append(type.getName()).append("\t");
        }

        if (system != null) {
            builder.append(system.getName()).append("\t");
        }

        if (names != null) {
            builder.append(names.toString()).append("\t");
        }

        builder.append(" ]");
        return builder.toString();
    }

    public String toJson() {
        JsonStringEncoder encoder = JsonStringEncoder.getInstance();
        StringBuilder json = new StringBuilder();
        boolean started = false;
        if (type != null) {
            byte[] encodedName = encoder.quoteAsUTF8(type.getName());
            String outputName = new String(encodedName);
            if (!started) {
                started = true;
            } else
                json.append(", ");
            json.append("\"type\" : \"" + outputName + "\"");
        }
        if (system != null) {
            byte[] encodedSystem = encoder.quoteAsUTF8(system.getName());
            String outputSystem = new String(encodedSystem);
            if (!started) {
                started = true;
            } else
                json.append(", ");
            json.append("\"system\" : \"" + outputSystem + "\"");
        }
        return json.toString();
    }

    public String toJsonComplete() {
        JsonStringEncoder encoder = JsonStringEncoder.getInstance();
        StringBuilder json = new StringBuilder();
        boolean started = false;
        json.append("{ ");
        if (isNotEmpty(notations)) {
            String notation = notations.get(0);
            byte[] encodedNotation = encoder.quoteAsUTF8(notation);
            String outputNotation = new String(encodedNotation);
            json.append("\"notation\" : \"" + outputNotation + "\"");
            started = true;
        }
        if (type != null) {
            byte[] encodedName = encoder.quoteAsUTF8(type.getName());
            String outputName = new String(encodedName);
            if (!started) {
                started = true;
            } else
                json.append(", ");
            json.append("\"name\" : \"" + outputName + "\"");
        }
        if (system != null) {
            byte[] encodedSystem = encoder.quoteAsUTF8(system.getName());
            String outputSystem = new String(encodedSystem);
            if (!started) {
                started = true;
            } else
                json.append(", ");
            json.append("\"system\" : \"" + outputSystem + "\"");
        }

        json.append(" }");
        return json.toString();
    }

    public boolean isSkipNormalisation() {
        return skipNormalisation;
    }

    public void setSkipNormalisation(boolean skipNormalisation) {
        this.skipNormalisation = skipNormalisation;
    }

    public boolean isSupportsPrefixes() {
        return supportsPrefixes;
    }

    public void setSupportsPrefixes(boolean supportsPrefixes) {
        this.supportsPrefixes = supportsPrefixes;
    }
}
