package com.example.edittest.Util;

import com.example.edittest.Interface.TextExecHistoryInterface;

import java.util.Stack;

public abstract class TextExecHistoryAbst implements TextExecHistoryInterface {

    public final String APPEND = "APPEND";
    public final String DELETE = "DELETE";

    private Stack<TextExecHistory> undoStack = new Stack<>();
    private Stack<TextExecHistory> redoStack = new Stack<>();

    private long lastTime = -1;

    private boolean isNowHistory = false;

    @Override
    public final void append(String text, int firstRow, int firstCol, int secondRow, int secondCol) {
        TextExecHistory history = undoStack.empty()?null:undoStack.pop();
        long nowTime = System.currentTimeMillis();
        if (isNowHistory || lastTime == -1 || nowTime - lastTime > 2000 || undoStack.empty()) {
            undoStack.push(history);
            history = new TextExecHistory(APPEND, text, firstRow, firstCol, secondRow, secondCol);
        } else {
            if (DELETE.equals(history.getAppendOrDelete())) {
                undoStack.push(history);
                history = new TextExecHistory(APPEND, text, firstRow, firstCol, secondRow, secondCol);
            } else if (history.getFirstRow() != firstRow || history.getSecondCol() != firstCol) {
                undoStack.push(history);
                history = new TextExecHistory(APPEND, text, firstRow, firstCol, secondRow, secondCol);
            } else {
                String hitext = history.getText() + text;
                history.setText(hitext);
                history.setSecondRow(firstRow);
                history.setSecondCol(firstCol);
            }
        }
        undoStack.push(history);
        lastTime = nowTime;
    }

    @Override
    public final void delete(String text, int firstRow, int firstCol, int secondRow, int secondCol) {
        TextExecHistory history = undoStack.empty()?null:undoStack.pop();
        long nowTime = System.currentTimeMillis();
        if (isNowHistory || lastTime == -1 || nowTime - lastTime > 2000 || undoStack.empty()) {
            undoStack.push(history);
            history = new TextExecHistory(DELETE, text, firstRow, firstCol, secondRow, secondCol);
        } else {
            if (APPEND.equals(history.getAppendOrDelete())) {
                undoStack.push(history);
                history = new TextExecHistory(DELETE, text, firstRow, firstCol, secondRow, secondCol);
            } else if (history.getFirstRow() != secondRow || history.getFirstCol() != secondCol) {
                undoStack.push(history);
                history = new TextExecHistory(DELETE, text, firstRow, firstCol, secondRow, secondCol);
            } else {
                String hitext = text + history.getText();
                history.setText(hitext);
                history.setFirstRow(firstRow);
                history.setFirstCol(firstCol);
            }
        }
        undoStack.push(history);
        lastTime = nowTime;
    }

    @Override
    public abstract void undo(boolean isAppend, String text, int firstRow, int firstCol, int secondRow, int secondCol);

    @Override
    public abstract void redo(boolean isAppend, String text, int firstRow, int firstCol, int secondRow, int secondCol);

    @Override
    public final boolean canUndo() {
        return undoStack.empty() ? false : true;
    }

    @Override
    public final boolean canRedo() {
        return redoStack.empty() ? false : true;
    }

    @Override
    public final void executeUndo() {
        TextExecHistory history = undoStack.pop();
        redoStack.push(history);
        boolean isAppend = APPEND.equals(history.getAppendOrDelete()) ? true : false;
        if (isAppend) {
            undo(!isAppend, null, history.getFirstRow(), history.getFirstCol(), history.getSecondRow(), history.getSecondCol());
        } else {
            undo(!isAppend, history.getText(), history.getFirstRow(), history.getFirstCol(), history.getSecondRow(), history.getSecondCol());
        }
    }

    @Override
    public final void executeRedo() {
        TextExecHistory history = redoStack.pop();
        undoStack.push(history);
        boolean isAppend = APPEND.equals(history.getAppendOrDelete()) ? true : false;
        if (isAppend) {
            redo(!isAppend, null, history.getFirstRow(), history.getFirstCol(), history.getSecondRow(), history.getSecondCol());
        } else {
            redo(!isAppend, history.getText(), history.getFirstRow(), history.getFirstCol(), history.getSecondRow(), history.getSecondCol());
        }
    }

    @Override
    public void nowHistory() {
        isNowHistory = true;
    }

    private static class TextExecHistory {
        private String appendOrDelete;
        private String text;
        private int firstRow;
        private int firstCol;
        private int secondRow;
        private int secondCol;

        public TextExecHistory(String appendOrDelete, String text, int firstRow, int firstCol, int secondRow, int secondCol) {
            this.appendOrDelete = appendOrDelete;
            this.text = text;
            this.firstRow = firstRow;
            this.firstCol = firstCol;
            this.secondRow = secondRow;
            this.secondCol = secondCol;
        }

        private String getAppendOrDelete() {
            return appendOrDelete;
        }

        private String getText() {
            return text;
        }

        private int getFirstRow() {
            return firstRow;
        }

        private int getFirstCol() {
            return firstCol;
        }

        public int getSecondRow() {
            return secondRow;
        }

        public int getSecondCol() {
            return secondCol;
        }

        private void setText(String text) {
            this.text = text;
        }

        public void setFirstRow(int firstRow) {
            this.firstRow = firstRow;
        }

        public void setFirstCol(int firstCol) {
            this.firstCol = firstCol;
        }

        public void setSecondRow(int secondRow) {
            this.secondRow = secondRow;
        }

        public void setSecondCol(int secondCol) {
            this.secondCol = secondCol;
        }
    }
}
