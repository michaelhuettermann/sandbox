package com.huettermann.all;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestMe {

    @Test
    public void aComplexComputation() {
        String a = "a";
        String b = "a11";
        assertEquals(a, b);
    }

    @Test
    public void aTest() {
        Main main = new Main();
        assertEquals( 42, main.getValue() );
    }


}

