package com.example.edittest;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.example.edittest.Util.RunJavaUtil;
import com.example.edittest.View.RunEditView;

import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;

import dalvik.system.DexClassLoader;

public class RunJavaActivity extends AppCompatActivity {

    private RunEditView runEditView;
    private InputStream systemIn;
    private PrintStream systemOut;
    private PrintStream systemErr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_run);
        runEditView = findViewById(R.id.runEditView);
        runJava();
    }

    private void runJava() {

        new Thread(() -> {
            try {
                systemIn = System.in;
                systemOut = System.out;
                systemErr = System.err;

                System.setIn(runEditView.getIn());
                System.setOut(new PrintStream(runEditView.getOut()));
                System.setErr(new PrintStream(runEditView.getErr()));

                RunJavaUtil.compile();
                RunJavaUtil.dex();

                DexClassLoader dexClassLoader = new DexClassLoader("/storage/emulated/0/麻瓜/java项目测试/classes/dex/classes.dex",
                        "/storage/emulated/0/麻瓜/java项目测试/classes/dex/output/",
                        null,
                        getClassLoader());
                Class mainClass=dexClassLoader.loadClass("Main");
                Method mainMethod=mainClass.getMethod("Main",String[].class);
                mainMethod.invoke(null,(Object)new String[]{});

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                System.setIn(systemIn);
                System.setOut(systemOut);
                System.setErr(systemErr);
            }
        }).start();
    }
}
