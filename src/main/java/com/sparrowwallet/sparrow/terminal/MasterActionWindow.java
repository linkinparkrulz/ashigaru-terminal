package com.sparrowwallet.sparrow.terminal;

import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.SimpleTheme;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.BorderLayout;
import com.googlecode.lanterna.gui2.Panel;

import java.util.List;

public class MasterActionWindow extends BasicWindow {
    public MasterActionWindow(SparrowTerminal sparrowTerminal) {
        setHints(List.of(Hint.CENTERED));

        final Panel panel = new Panel(new BorderLayout());
        final MasterActionListBox masterActionListBox = new MasterActionListBox(sparrowTerminal);
        panel.addComponent(masterActionListBox);
        setComponent(panel);
        setTheme(new CustomActionListBoxTheme());
    }
}
