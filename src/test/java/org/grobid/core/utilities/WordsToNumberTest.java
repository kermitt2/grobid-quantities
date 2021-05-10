package org.grobid.core.utilities;

import org.grobid.core.data.normalization.NormalizationException;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Locale;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class WordsToNumberTest {

    private WordsToNumber wordsToNumber;

    @Before
    public void setUp() throws Exception {
        wordsToNumber = WordsToNumber.getInstance();
    }

    @Test
    public void testNonDecimal_1() throws Exception {
        String input = "two hundred two million fifty three thousand and four";
        BigDecimal number = wordsToNumber.normalize(input, Locale.ENGLISH);
        assertThat(number, is(new BigDecimal("202053004")));
    }

    @Test
    public void testNonDecimal_2() throws Exception {
        String input = "fifteen";
        BigDecimal number = wordsToNumber.normalize(input, Locale.ENGLISH);
        assertThat(number, is(new BigDecimal("15")));
    }


    @Test
    public void testDecimal_1() throws Exception {
        String input = "two hundred two million fifty three thousand point zero eight five eight zero two";
        BigDecimal number = wordsToNumber.normalize(input, Locale.ENGLISH);
        assertThat(number, is(new BigDecimal("202053000.085802")));
    }

    @Test
    public void testDecimal_2() throws Exception {
        String input = "point zero eight five eight zero two";
        BigDecimal number = wordsToNumber.normalize(input, Locale.ENGLISH);
        assertThat(number, is(new BigDecimal("0.085802")));
    }

    @Test(expected = NormalizationException.class)
    public void testCornerCase() throws Exception {
        //Sometimes the word 'point' alone is recognised as value... this test was used to simulate the
        // exception
        String input = "point";
        wordsToNumber.normalize(input, Locale.ENGLISH);
    }
}