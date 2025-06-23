package com.sparrowwallet.sparrow.terminal;

import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.SimpleTheme;
import com.googlecode.lanterna.gui2.AbstractListBox;

public class CustomActionListBoxTheme extends SimpleTheme {

    public static final TextColor ASHIGARU_RED = TextColor.Factory.fromString("#c61500");
    public static final TextColor GREY = TextColor.Factory.fromString("#343434");

    public CustomActionListBoxTheme() {
        super(
                TextColor.ANSI.BLACK,
                GREY
        );
    }

    @Override
    public Definition getDefinition(Class<?> componentType) {
        //if (componentType == AbstractListBox.class) {
            final SimpleTheme theme = SimpleTheme.makeTheme(
                    true,
                    TextColor.ANSI.WHITE, TextColor.ANSI.BLACK,
                    TextColor.ANSI.BLACK, ASHIGARU_RED,
                    TextColor.ANSI.WHITE, ASHIGARU_RED,
                    TextColor.ANSI.BLACK);
            return theme.getDefinition(componentType);
        //}
        //return super.getDefinition(componentType);
    }
}
