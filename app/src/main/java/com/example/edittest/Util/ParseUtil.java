package com.example.edittest.Util;

import android.text.TextUtils;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ParseUtil {
    private List<String> globalVariableList = new ArrayList<>();
    private List<String> importClassList = new ArrayList<>();
    private String packageName;
    private CompilationUnit compilationUnit;

    public void parse(InputStream inputStream) {
        compilationUnit = StaticJavaParser.parse(inputStream);
        parse();
    }

    public void parse(File file) {
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            parse(fileInputStream);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void parse() {
        //导入类
        compilationUnit.getImports().forEach(importDeclaration -> {
            String name = importDeclaration.getNameAsString();
            importClassList.add(name);
            int index = name.lastIndexOf('.');
            if (index != -1 && index < name.length()) {
                importClassList.add(name.substring(index + 1));
            }
        });
        //全局变量
        compilationUnit.findAll(ClassOrInterfaceDeclaration.class).forEach(classOrInterfaceDeclaration -> {
            classOrInterfaceDeclaration.getFields().forEach(fieldDeclaration -> {
                fieldDeclaration.getVariables().forEach(variableDeclarator -> {
                    String var = variableDeclarator.getNameAsString();
                    globalVariableList.add(var);
                });
            });
        });
    }

    public void parse(String code) {
        try {
            compilationUnit = StaticJavaParser.parse(code);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        //导入类
        importClassList.clear();
        compilationUnit.getImports().forEach(importDeclaration -> {
            String name = importDeclaration.getNameAsString();
            importClassList.add(name);
        });
        //全局变量
        globalVariableList.clear();
        compilationUnit.findAll(ClassOrInterfaceDeclaration.class).forEach(classOrInterfaceDeclaration -> {
            classOrInterfaceDeclaration.getFields().forEach(fieldDeclaration -> {
                fieldDeclaration.getVariables().forEach(variableDeclarator -> {
                    String var = variableDeclarator.getNameAsString();
                    globalVariableList.add(var);
                });
            });
        });
    }

    public void parseImport(List<String> list) {

        importClassList.add("String");

        for (String line : list) {
            if (!TextUtils.isEmpty(line)) {
                int i = 0;
                for (char c : line.toCharArray()) {
                    if (c != ' ') {
                        break;
                    }
                    i++;
                }
                line = line.substring(i);
                if (line.startsWith("import")) {
                    int index = line.length() - 1;
                    for (; index >= 0; index--) {
                        if (line.charAt(index) == '.') {
                            break;
                        }
                    }
                    if (index < line.length() - 1) {
                        line = line.replace(";", "");
                        String cla = line.substring(index + 1);
                        System.out.println("类：" + cla);
                        importClassList.add(cla);
                    }
                } else if (line.startsWith("package")) {
                    continue;
                } else {
                    break;
                }
            }

        }
    }

    public boolean isClassName(String className) {
        Iterator<String> iterator = importClassList.iterator();
        while (iterator.hasNext()) {
            String name = iterator.next();
            int index = name.lastIndexOf('.');
            name = name.substring(index + 1);
            if (name.equals(className)) return true;
        }
        return false;
    }

    public boolean isGlobalVariable(String var) {
        Iterator<String> iterator = globalVariableList.iterator();
        while (iterator.hasNext()) {
            String name = iterator.next();
            if (name.equals(var)) return true;
        }
        return false;
    }
}
