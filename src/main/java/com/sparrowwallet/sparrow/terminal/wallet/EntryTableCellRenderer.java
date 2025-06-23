package com.sparrowwallet.sparrow.terminal.wallet;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.TextGUIGraphics;
import com.googlecode.lanterna.gui2.table.DefaultTableCellRenderer;
import com.googlecode.lanterna.gui2.table.Table;
import com.sparrowwallet.sparrow.terminal.wallet.table.TableCell;

import static com.sparrowwallet.sparrow.terminal.CustomActionListBoxTheme.ASHIGARU_RED;

public class EntryTableCellRenderer extends DefaultTableCellRenderer<TableCell> {

    @Override
    protected String[] getContent(TableCell cell) {
        String[] lines;
        if(cell == null) {
            lines = new String[] { "" };
        } else {
            lines = new String[] { cell.formatCell() };
        }

        return lines;
    }

    @Override
    public void drawCell(Table<TableCell> table, TableCell cell, int columnIndex, int rowIndex, TextGUIGraphics textGUIGraphics) {
        boolean isSelectedRow = (rowIndex == table.getSelectedRow());

        if (isSelectedRow) {
            textGUIGraphics.setBackgroundColor(ASHIGARU_RED);
            textGUIGraphics.setForegroundColor(TextColor.ANSI.WHITE);
            textGUIGraphics.enableModifiers(SGR.BOLD);
        } else {
            textGUIGraphics.setBackgroundColor(TextColor.ANSI.BLACK);
            textGUIGraphics.setForegroundColor(TextColor.ANSI.WHITE);
        }
        textGUIGraphics.fill(' ');
        String textToDraw = (cell == null) ? "" : cell.formatCell();
        textGUIGraphics.putString(0, 0, textToDraw);
    }

}
