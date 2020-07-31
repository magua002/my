package com.example.edittest.Util;

import android.os.Environment;

import com.example.edittest.application.MyApplication;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class JavaProjectUtil {
    private static String projectPath = "JavaProject";
    private static File mainJavaFile = new File(MyApplication.getContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), projectPath+"/src/Main.java");
    private static File src = new File(MyApplication.getContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), projectPath+"/src");
    private static File classes = new File(MyApplication.getContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), projectPath+"/classes");
    private static File lib = new File(MyApplication.getContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), projectPath+"/lib");
    private static File boot = new File(MyApplication.getContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), projectPath+"/android.jar");

    public static void createProject() {
        File projectDir = new File(MyApplication.getContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), projectPath);
        if (projectDir.exists()) {
            deleteFileDir(projectDir);
        }
        projectDir.mkdir();
        src = new File(projectDir, "src");
        src.mkdir();
        classes = new File(projectDir, "classes");
        classes.mkdir();
        lib = new File(projectDir, "lib");
        lib.mkdir();
        boot = createBootJar(projectDir);
        mainJavaFile = createMainJavaFile(src);
    }

    private static void deleteFileDir(File dir) {
        if (!dir.isDirectory()) return;
        for (File f : dir.listFiles()) {
            if (f.isDirectory()) {
                deleteFileDir(f);
            } else {
                f.delete();
            }
        }
        dir.delete();
    }

    private static File createBootJar(File projectDir) {
        File file = new File(projectDir, "android.jar");
        if (file.exists()) file.delete();
        try {
            file.createNewFile();
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            InputStream inputStream = MyApplication.getContext().getResources().getAssets().open("android.jar");
            byte[] data = new byte[1024];
            int len = 0;
            while ((len = inputStream.read(data)) != -1) {
                fileOutputStream.write(data, 0, len);
            }
            inputStream.close();
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }

    private static File createMainJavaFile(File srcDir) {
        File main = new File(srcDir, "Main.java");
        if (main.exists()) {
            main.delete();
        }
        try {
            main.createNewFile();
            FileWriter fileWriter = new FileWriter(main);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

            InputStream inputStream = MyApplication.getContext().getResources().getAssets().open("Main.java");
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                bufferedWriter.write(line);
                bufferedWriter.newLine();
            }

            bufferedReader.close();
            inputStreamReader.close();
            inputStream.close();
            bufferedWriter.close();
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return main;
    }

    public static File getMainJavaFile() {
        return mainJavaFile;
    }

    public static File getSrc() {
        return src;
    }

    public static File getClasses() {
        return classes;
    }

    public static File getLib() {
        return lib;
    }

    public static File getBoot() {
        return boot;
    }
}
