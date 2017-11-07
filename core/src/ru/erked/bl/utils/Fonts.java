package ru.erked.bl.utils;

import com.badlogic.gdx.graphics.Color;

import ru.erked.bl.systems.BLFont;

public class Fonts {

    public BLFont large;
    public BLFont largeB;
    public BLFont largeS;
    public BLFont medium;
    public BLFont mediumB;
    public BLFont mediumS;
    public BLFont small;
    public BLFont smallB;
    public BLFont smallS;
    public BLFont exSmall;
    public BLFont exSmallB;
    public BLFont exSmallS;
    public BLFont exxSmall;
    public BLFont exxSmallS;

    public Fonts (String FONT_CHARS) {
        large  = new BLFont("fonts/regular.otf", 13, Color.WHITE, FONT_CHARS);
        largeB = new BLFont("fonts/regular.otf", 13, Color.WHITE, 10, Color.BLACK, FONT_CHARS);
        largeS = new BLFont("fonts/regular.otf", 13, Color.WHITE, 5, 5, Color.BLACK, FONT_CHARS);
        medium  = new BLFont("fonts/regular.otf", 15, Color.WHITE, FONT_CHARS);
        mediumB = new BLFont("fonts/regular.otf", 15, Color.WHITE, 6, Color.BLACK, FONT_CHARS);
        mediumS = new BLFont("fonts/regular.otf", 15, Color.WHITE, 3, 3, Color.BLACK, FONT_CHARS);
        small  = new BLFont("fonts/regular.otf", 21, Color.WHITE, FONT_CHARS);
        smallB = new BLFont("fonts/regular.otf", 21, Color.WHITE, 4, Color.BLACK, FONT_CHARS);
        smallS = new BLFont("fonts/regular.otf", 21, Color.WHITE, 2, 2, Color.BLACK, FONT_CHARS);
        exSmall  = new BLFont("fonts/regular.otf", 24, Color.WHITE, FONT_CHARS);
        exSmallB = new BLFont("fonts/regular.otf", 24, Color.WHITE, 2, Color.BLACK, FONT_CHARS);
        exSmallS = new BLFont("fonts/regular.otf", 24, Color.WHITE, 2, 2, Color.BLACK, FONT_CHARS);
        exxSmall = new BLFont("fonts/regular.otf", 30, Color.WHITE, FONT_CHARS);
        exxSmallS = new BLFont("fonts/regular.otf", 30, Color.WHITE, 2, 2, Color.BLACK, FONT_CHARS);
    }

}
