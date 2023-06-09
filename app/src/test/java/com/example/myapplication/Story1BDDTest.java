package com.example.myapplication;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

// TODO: Rewrite this as a robolectric test maybe?
public class Story1BDDTest {
/**
 * GIVEN I am facing North
 * WHEN I am viewing the Compass Screen (MainActivity)
 * THEN my app should display 'N'
 * WHEN I turn around to face South
 * THEN my app should display 'S'
 */
    @Test
    public void BDD1Test() {
        // Start facing North
        float orientation = 0f;
        assertEquals("N", CoordinateUtil.cardDirection(orientation));
        // Turn to south
        orientation = 180f;
        assertEquals("S", CoordinateUtil.cardDirection(orientation));
    }
}
