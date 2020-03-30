package com.example.edittest.Util;

import android.text.TextUtils;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ParseUtil {
    private List<String> classList = new ArrayList<>();
    private List<String> globalVariableList = new ArrayList<>();

    public void parse(InputStream inputStream) {
        CompilationUnit compilationUnit = StaticJavaParser.parse(inputStream);
        compilationUnit.findAll(ClassOrInterfaceDeclaration.class).forEach(classOrInterfaceDeclaration -> {
            classOrInterfaceDeclaration.getFields().forEach(fieldDeclaration -> {
                fieldDeclaration.getVariables().forEach(variableDeclarator -> {
                    String var = variableDeclarator.getNameAsString();
                    globalVariableList.add(var);
                    System.out.println("全局变量："+var);
                });
            });
        });
    }

    public void parse(String code) {
        CompilationUnit compilationUnit = StaticJavaParser.parse(code);
        compilationUnit.findAll(ClassOrInterfaceDeclaration.class).forEach(classOrInterfaceDeclaration -> {
            classOrInterfaceDeclaration.getFields().forEach(fieldDeclaration -> {
                fieldDeclaration.getVariables().forEach(variableDeclarator -> {
                    String var = variableDeclarator.getNameAsString();
                    globalVariableList.add(var);
                    System.out.println("全局变量："+var);
                });
            });
        });
    }

    public void parseImport(List<String> list) {

        classList.add("String");

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
                        classList.add(cla);
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
        for (String name : classList) {
            if (name.equals(className)) {
                return true;
            }
        }
        return false;
    }

    public boolean isGlobalVariable(String var) {
        for (String v : globalVariableList) {
            if (v.equals(var)) return true;
        }
        return false;
    }
}
