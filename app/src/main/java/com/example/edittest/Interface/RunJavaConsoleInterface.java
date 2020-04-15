package com.example.edittest.Interface;

import java.io.InputStream;
import java.io.OutputStream;

public interface RunJavaConsoleInterface {
    //输入
    void sendText(String input);

    //输出
    void printOut(String out);

    //输出错误
    void printErr(String err);

    //输入流
    InputStream getIn();

    //输出流
    OutputStream getOut();

    //错误流
    OutputStream getErr();
}
