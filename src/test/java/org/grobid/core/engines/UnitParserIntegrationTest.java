package org.grobid.core.engines;

import org.grobid.core.data.UnitBlock;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.main.LibraryLoader;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.*;

/**
 * Created by lfoppiano on 22.02.16.
 */
@Ignore("Only for manual checks since it depends on the model")
public class UnitParserIntegrationTest {

    UnitParser target;

    @Before
    public void setUp() throws Exception {
        LibraryLoader.load();
        target = UnitParser.getInstance();
    }

    @Test
    public void testTagUnit1() throws Exception {

        String input = "fm/g2";
        List<UnitBlock> output = target.tagUnit(input, false);
        System.out.println(input + " -> " + output);

        assertThat(output.size(), is(2));
        assertThat(output.get(0).getPrefix(), is("f"));
        assertThat(output.get(0).getBase(), is("m"));
        assertThat(output.get(0).getPow(), is(""));
        assertThat(output.get(1).getPrefix(), is(""));
        assertThat(output.get(1).getBase(), is("g"));
        assertThat(output.get(1).getPow(), is("-2"));
    }

    @Test
    public void testTagUnit2() throws Exception {

        String input = "K";
        List<UnitBlock> output = target.tagUnit(input, false);
        System.out.println(input + " -> " + output);

        assertThat(output.size(), is(1));
        assertThat(output.get(0).getBase(), is("K"));
        assertThat(output.get(0).getPrefix(), is(""));
        assertThat(output.get(0).getPow(), is(""));
    }

    @Test
    public void testTagUnit3() throws Exception {

        String input = "m2g2";
        List<UnitBlock> output = target.tagUnit(input, false);
        System.out.println(input + " -> " + output);

        assertThat(output.size(), is(2));
        assertThat(output.get(0).getBase(), is("m"));
        assertThat(output.get(0).getPrefix(), is(""));
        assertThat(output.get(0).getPow(), is("2"));
        assertThat(output.get(1).getBase(), is("g"));
        assertThat(output.get(1).getPrefix(), is(""));
        assertThat(output.get(1).getPow(), is("2"));
    }

    @Test
    public void testTagUnit4() throws Exception {

        String input = "m2/g2";
        List<UnitBlock> output = target.tagUnit(input, false);
        System.out.println(input + " -> " + output);

        assertThat(output.size(), is(2));
        assertThat(output.get(0).getBase(), is("m"));
        assertThat(output.get(0).getPrefix(), is(""));
        assertThat(output.get(0).getPow(), is("2"));
        assertThat(output.get(1).getBase(), is("g"));
        assertThat(output.get(1).getPrefix(), is(""));
        assertThat(output.get(1).getPow(), is("-2"));
    }

    @Test
    public void testTagUnit5() throws Exception {

        String input = "m22";
        List<UnitBlock> output = target.tagUnit(input, false);
        System.out.println(input + " -> " + output);

        assertThat(output.size(), is(1));
        assertThat(output.get(0).getBase(), is("m"));
        assertThat(output.get(0).getPrefix(), is(""));
        assertThat(output.get(0).getPow(), is("22"));
    }

    @Test
    public void testTagUnit6() throws Exception {

        String input = "Hz/s";
        List<UnitBlock> output = target.tagUnit(input, false);
        System.out.println(input + " -> " + output);

        assertThat(output.size(), is(2));
        assertThat(output.get(0).getBase(), is("Hz"));
        assertThat(output.get(0).getPrefix(), is(""));
        assertThat(output.get(0).getPow(), is(""));
        assertThat(output.get(1).getBase(), is("s"));
        assertThat(output.get(1).getPrefix(), is(""));
        assertThat(output.get(1).getPow(), is("-1"));
    }

    @Test
    public void testTagUnit7() throws Exception {

        String input = "Hzs";
        List<UnitBlock> output = target.tagUnit(input, false);
        System.out.println(input + " -> " + output);

        assertThat(output.size(), is(2));
        assertThat(output.get(0).getBase(), is("Hz"));
        assertThat(output.get(0).getPrefix(), is(""));
        assertThat(output.get(0).getPow(), is(""));
        assertThat(output.get(1).getBase(), is("s"));
        assertThat(output.get(1).getPrefix(), is(""));
        assertThat(output.get(1).getPow(), is(""));
    }

    @Test
    public void testTagUnit8() throws Exception {

        String input = "Db/s";
        List<UnitBlock> output = target.tagUnit(input, false);
        System.out.println(input + " -> " + output);

        assertThat(output.size(), is(2));
        assertThat(output.get(0).getBase(), is("Db"));
        assertThat(output.get(0).getPrefix(), is(""));
        assertThat(output.get(0).getPow(), is(""));
        assertThat(output.get(1).getBase(), is("s"));
        assertThat(output.get(1).getPrefix(), is(""));
        assertThat(output.get(1).getPow(), is("-1"));
    }

    @Test
    public void testTagUnit9() throws Exception {

        String input = "Db s";
        List<UnitBlock> output = target.tagUnit(input, false);
        System.out.println(input + " -> " + output);

        assertThat(output.size(), is(2));
        assertThat(output.get(0).getBase(), is("Db"));
        assertThat(output.get(0).getPrefix(), is(""));
        assertThat(output.get(0).getPow(), is(""));
        assertThat(output.get(1).getBase(), is("s"));
        assertThat(output.get(1).getPrefix(), is(""));
        assertThat(output.get(1).getPow(), is(""));
    }

    @Test
    public void testTagUnit10() throws Exception {

        String input = "Db*s";
        List<UnitBlock> output = target.tagUnit(input, false);
        System.out.println(input + " -> " + output);

        assertThat(output.size(), is(2));
        assertThat(output.get(0).getBase(), is("Db"));
        assertThat(output.get(0).getPrefix(), is(""));
        assertThat(output.get(0).getPow(), is(""));
        assertThat(output.get(1).getBase(), is("s"));
        assertThat(output.get(1).getPrefix(), is(""));
        assertThat(output.get(1).getPow(), is(""));
    }

    @Test
    public void testTagUnit11() throws Exception {

        String input = "km/h*kg";
        List<UnitBlock> output = target.tagUnit(input, false);
        System.out.println(input + " -> " + output);

        assertThat(output.size(), is(3));
        assertThat(output.get(0).getBase(), is("m"));
        assertThat(output.get(0).getPrefix(), is("k"));
        assertThat(output.get(0).getPow(), is(""));
        assertThat(output.get(1).getBase(), is("h"));
        assertThat(output.get(1).getPrefix(), is(""));
        assertThat(output.get(1).getPow(), is("-1"));
        assertThat(output.get(2).getBase(), is("g"));
        assertThat(output.get(2).getPrefix(), is("k"));
        assertThat(output.get(2).getPow(), is("-1"));
    }

    @Test
    public void testTagUnit12() throws Exception {

        String input = "m*s^-1";
        List<UnitBlock> output = target.tagUnit(input, false);
        System.out.println(input + " -> " + output);

        assertThat(output.size(), is(2));
        assertThat(output.get(0).getBase(), is("m"));
        assertThat(output.get(0).getPrefix(), is(""));
        assertThat(output.get(0).getPow(), is(""));
        assertThat(output.get(1).getBase(), is("s"));
        assertThat(output.get(1).getPrefix(), is(""));
        assertThat(output.get(1).getPow(), is("-1"));
    }

    @Test
    public void testTagUnit13() throws Exception {

        String input = "mol dm–3";
        List<UnitBlock> output = target.tagUnit(input, false);
        System.out.println(input + " -> " + output);

        assertThat(output.size(), is(2));
        assertThat(output.get(0).getPrefix(), is("f"));
        assertThat(output.get(0).getBase(), is("m"));
        assertThat(output.get(0).getPow(), is(""));
        assertThat(output.get(1).getPrefix(), is(""));
        assertThat(output.get(1).getBase(), is("g"));
        assertThat(output.get(1).getPow(), is("-2"));
    }


    @Test
    public void resultExtraction_kgmm2_liters() throws Exception {
        String result = "k\t0\t0\t1\t1\tNOPUNCT\t0\tI-<prefix>\n" +
                "g\t0\t0\t0\t0\tNOPUNCT\t0\tI-<base>\n" +
                "/\t0\t0\t1\t0\tNOPUNCT\t0\tI-<pow>\n" +
                "m\t1\t0\t0\t0\tSLASH\t0\tI-<prefix>\n" +
                "m\t0\t0\t1\t0\tNOPUNCT\t0\tI-<base>\n"+
                "2\t0\t0\t1\t0\tNOPUNCT\t0\tI-<pow>";

        List<UnitBlock> blocks = target.resultExtraction(result, generateTokenisation("kg/mm2"));
        assertThat(blocks.size(), is(2));
        assertThat(blocks.get(0).getPrefix(), is("k"));
        assertThat(blocks.get(0).getBase(), is("g"));
        assertThat(blocks.get(1).getPrefix(), is("m"));
        assertThat(blocks.get(1).getBase(), is("m"));
        assertThat(blocks.get(1).getPow(), is("-2"));
    }

    @Test
    public void resultExtraction_mol_divided_liters() throws Exception {
        String result = "m\t0\t0\t1\t1\tNOPUNCT\t0\tI-<base>\n" +
                "o\t0\t0\t0\t0\tNOPUNCT\t0\t<base>\n" +
                "l\t0\t0\t1\t0\tNOPUNCT\t0\t<base>\n" +
                "/\t1\t0\t0\t0\tSLASH\t0\tI-<pow>\n" +
                "l\t0\t0\t1\t0\tNOPUNCT\t0\tI-<base>";

        List<UnitBlock> blocks = target.resultExtraction(result, generateTokenisation("mol/l"));
        assertThat(blocks.size(), is(2));
        assertThat(blocks.get(0).getBase(), is("mol"));
        assertThat(blocks.get(1).getBase(), is("l"));
        assertThat(blocks.get(1).getPow(), is("-1"));
    }

    @Test
    public void resultExtraction_C_divided_hours() throws Exception {
        String result = "°\t0\t0\t1\t1\tNOPUNCT\t0\tI-<base>\n" +
                "C\t0\t0\t0\t0\tNOPUNCT\t0\t<base>\n" +
                "/\t0\t0\t1\t0\tNOPUNCT\t0\tI-<pow>\n" +
                "h\t1\t0\t0\t0\tSLASH\t0\tI-<base>";

        List<UnitBlock> blocks = target.resultExtraction(result, generateTokenisation("°C /h"));
        assertThat(blocks.size(), is(2));
        assertThat(blocks.get(0).getBase(), is("°C"));
        assertThat(blocks.get(1).getBase(), is("h"));
        assertThat(blocks.get(1).getPow(), is("-1"));
        assertThat(blocks.get(0).getRawTaggedValue(), is("<base>°C</base> <pow>/</pow><base>h</base>"));
        assertThat(blocks.get(1).getRawTaggedValue(), is("<base>°C</base> <pow>/</pow><base>h</base>"));
    }

    @Test
    public void resultExtraction_multiple_divisionMarks() throws Exception {
        String result = "k\t0\t0\t1\t1\tNOPUNCT\t0\tI-<prefix>\n" +
                "m\t0\t0\t1\t1\tNOPUNCT\t0\tI-<base>\n" +
                "/\t1\t0\t0\t0\tSLASH\t0\tI-<pow>\n" +
                "h\t0\t0\t1\t1\tNOPUNCT\t0\tI-<base>\n" +
                "/\t1\t0\t0\t0\tSLASH\t0\tI-<pow>\n" +
                "m\t0\t0\t1\t1\tNOPUNCT\t0\tI-<prefix>\n" +
                "l\t0\t0\t1\t0\tNOPUNCT\t0\tI-<base>\n" +
                "/\t1\t0\t0\t0\tSLASH\t0\tI-<pow>\n" +
                "k\t0\t0\t1\t1\tNOPUNCT\t0\tI-<prefix>\n" +
                "c\t0\t0\t1\t1\tNOPUNCT\t0\tI-<base>\n" +
                "a\t0\t0\t1\t1\tNOPUNCT\t0\t<base>\n" +
                "l\t0\t0\t1\t0\tNOPUNCT\t0\t<base>";

//        target.tagUnit("km/h/ml/kcal");

        List<UnitBlock> blocks = target.resultExtraction(result, generateTokenisation("km/h/ml/kcal"));
        assertThat(blocks.size(), is(4));
        assertThat(blocks.get(0).getPrefix(), is("k"));
        assertThat(blocks.get(0).getBase(), is("m"));

        assertThat(blocks.get(1).getBase(), is("h"));
        assertThat(blocks.get(1).getPow(), is("-1"));

        assertThat(blocks.get(2).getPrefix(), is("m"));
        assertThat(blocks.get(2).getBase(), is("l"));
        assertThat(blocks.get(2).getPow(), is("-1"));

        assertThat(blocks.get(3).getPrefix(), is("k"));
        assertThat(blocks.get(3).getBase(), is("cal"));
        assertThat(blocks.get(3).getPow(), is("-1"));

        assertThat(blocks.get(0).getRawTaggedValue(), is("<prefix>k</prefix><base>m</base><pow>/</pow><base>h</base><pow>/</pow><prefix>m</prefix><base>l</base><pow>/</pow><prefix>k</prefix><base>cal</base>"));

    }

    public static List<LayoutToken> generateTokenisation(String input) {
        List<LayoutToken> tokenisation = new ArrayList<>();

        final char[] chars = input.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            tokenisation.add(new LayoutToken(String.valueOf(chars[i])));
        }

        return tokenisation;
    }

    @Test
    public void resultExtraction_speed() throws Exception {
        String result = "k\t0\t0\t1\t1\tNOPUNCT\t0\tI-<prefix>\n" +
                "m\t0\t0\t1\t1\tNOPUNCT\t0\tI-<base>\n" +
                "/\t1\t0\t0\t0\tSLASH\t0\tI-<pow>\n" +
                "s\t0\t0\t1\t0\tNOPUNCT\t0\tI-<base>";

        List<UnitBlock> blocks = target.resultExtraction(result, generateTokenisation("km / s"));
        assertThat(blocks.size(), is(2));
        assertThat(blocks.get(0).getPrefix(), is("k"));
        assertThat(blocks.get(0).getBase(), is("m"));
        assertThat(blocks.get(1).getBase(), is("s"));
        assertThat(blocks.get(1).getPow(), is("-1"));
        assertThat(blocks.get(0).getRawTaggedValue(), is("<prefix>k</prefix><base>m</base> <pow>/</pow> <base>s</base>"));
        assertThat(blocks.get(1).getRawTaggedValue(), is("<prefix>k</prefix><base>m</base> <pow>/</pow> <base>s</base>"));
    }


    @Test
    public void resultExtraction_wrong() throws Exception {
        String result = "a\t0\t0\t1\t1\tNOPUNCT\t0\tI-<base>\n" +
                "u\t0\t0\t0\t0\tNOPUNCT\t0\t<base>\n" +
                "/\t1\t0\t0\t0\tSLASH\t0\tI-<pow>\n" +
                "d\t0\t0\t1\t1\tNOPUNCT\t0\tI-<base>\n" +
                "2\t1\t1\t0\t0\tNOPUNCT\t0\tI-<pow>";

        List<UnitBlock> blocks = target.resultExtraction(result, generateTokenisation("au/d 2"));
        assertThat(blocks.size(), is(2));
        assertThat(blocks.get(0).getPrefix(), is(""));
        assertThat(blocks.get(0).getBase(), is("au"));
        assertThat(blocks.get(1).getBase(), is("d"));
        assertThat(blocks.get(1).getPow(), is("-2"));
        assertThat(blocks.get(0).getRawTaggedValue(), is("<base>au</base><pow>/</pow><base>d</base> <pow>2</pow>"));
        assertThat(blocks.get(1).getRawTaggedValue(), is("<base>au</base><pow>/</pow><base>d</base> <pow>2</pow>"));
    }


    @Test
    public void resultExtraction_wrong2() throws Exception {
        String result = "g\t0\t0\t1\t0\tNOPUNCT\t0\tI-<base>\n" +
                "·\t1\t0\t0\t0\tDOT\t0\t<other>\n" +
                "k\t0\t0\t1\t1\tNOPUNCT\t0\tI-<prefix>\n" +
                "g\t0\t0\t1\t0\tNOPUNCT\t0\tI-<base>\n" +
                "−\t1\t0\t0\t0\tHYPHEN\t0\tI-<pow>\n" +
                "1\t1\t1\t0\t0\tNOPUNCT\t0\t<pow>\n" +
                "·\t1\t0\t0\t0\tDOT\t0\t<other>\n" +
                "d\t0\t0\t1\t1\tNOPUNCT\t0\tI-<base>\n" +
                "a\t0\t0\t1\t1\tNOPUNCT\t0\t<base>\n" +
                "y\t0\t0\t1\t1\tNOPUNCT\t0\t<base>\n" +
                "−\t1\t0\t0\t0\tHYPHEN\t0\tI-<pow>\n" +
                "1\t1\t1\t0\t0\tNOPUNCT\t0\t<pow>";

        List<UnitBlock> blocks = target.resultExtraction(result, generateTokenisation("g · kg −1 · day −1"));
        assertThat(blocks.size(), is(3));
        assertThat(blocks.get(0).getPrefix(), is(""));
        assertThat(blocks.get(0).getBase(), is("g"));
        assertThat(blocks.get(1).getPrefix(), is("k"));
        assertThat(blocks.get(1).getBase(), is("g"));
        assertThat(blocks.get(1).getPow(), is("−1"));
        assertThat(blocks.get(2).getBase(), is("day"));
        assertThat(blocks.get(2).getPow(), is("−1"));
        assertThat(blocks.get(0).getRawTaggedValue(), is("<base>g</base> · <prefix>k</prefix><base>g</base> <pow>−1</pow> · <base>day</base> <pow>−1</pow>"));
        assertThat(blocks.get(1).getRawTaggedValue(), is("<base>g</base> · <prefix>k</prefix><base>g</base> <pow>−1</pow> · <base>day</base> <pow>−1</pow>"));
        assertThat(blocks.get(2).getRawTaggedValue(), is("<base>g</base> · <prefix>k</prefix><base>g</base> <pow>−1</pow> · <base>day</base> <pow>−1</pow>"));
    }



}