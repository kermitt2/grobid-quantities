package org.grobid.trainer;

import org.grobid.core.utilities.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lfoppiano on 24/04/16.
 */
public class ValueLabeled {

    private List<Pair<String, String>> labels = new ArrayList<>();

    public boolean addLabel(Pair<String, String> label) {
        return labels.add(label);
    }

    public List<Pair<String, String>> getLabels() {
        return labels;
    }

    public void setLabels(List<Pair<String, String>> labels) {
        this.labels = labels;
    }
}
