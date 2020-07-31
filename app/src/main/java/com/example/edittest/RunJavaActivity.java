package com.example.edittest;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.example.edittest.Util.JavaProjectUtil;
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

                if (!RunJavaUtil.compile(JavaProjectUtil.getBoot().getPath(),
                        JavaProjectUtil.getLib().getPath(),
                        JavaProjectUtil.getClasses().getPath(),
                        JavaProjectUtil.getSrc().getPath())) throw new Exception("编译失败");
                if (!RunJavaUtil.dex(JavaProjectUtil.getLib().getPath(),
                        JavaProjectUtil.getClasses().getPath())) throw new Exception("dex失败");

                System.out.println("开始运行");
                System.out.println("--------------------------------");

                DexClassLoader dexClassLoader = new DexClassLoader(JavaProjectUtil.getClasses().getPath() + "/classes.dex",
                        JavaProjectUtil.getClasses().getPath() + "/dex/output/",
                        null,
                        getClassLoader());
                Class mainClass = dexClassLoader.loadClass("Main");
                Method mainMethod = mainClass.getMethod("main", String[].class);
                mainMethod.invoke(null, (Object) new String[]{});

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
