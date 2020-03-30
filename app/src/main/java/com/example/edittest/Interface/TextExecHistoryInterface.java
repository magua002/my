package com.example.edittest.Interface;

public interface TextExecHistoryInterface {
    //增加前的位置
    void append(String text,int firstRow,int firstCol,int secondRow,int secondCol);
    //删除后的位置
    void delete(String text,int firstRow,int firstCol,int secondRow,int secondCol);
    boolean canUndo();
    boolean canRedo();
    void undo(boolean isAppend,String text,int firstRow,int firstCol,int secondRow,int secondCol);
    void redo(boolean isAppend,String text,int firstRow,int firstCol,int secondRow,int secondCol);
    void executeUndo();
    void executeRedo();
    void nowHistory();
}
