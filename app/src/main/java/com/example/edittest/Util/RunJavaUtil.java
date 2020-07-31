package com.example.edittest.Util;

import org.eclipse.jdt.internal.compiler.batch.Main;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class RunJavaUtil {

    public static boolean compile(String boot, String lib, String classes, String src) {
        boolean success = false;
        ByteArrayOutputStream info = new ByteArrayOutputStream();
        ByteArrayOutputStream error = new ByteArrayOutputStream();
        Main main = new Main(new PrintWriter(info), new PrintWriter(error), false, null, null);
        String arg[] = {
                "-verbose",
                "-bootclasspath", boot/*"/storage/emulated/0/麻瓜/android.jar"*/,//类似于rt.jar
                "-extdirs", lib/*"/storage/emulated/0/麻瓜/java项目测试/libs/"*/,//第三方jar
                "-classpath", lib/*"/storage/emulated/0/麻瓜/java项目测试/libs/"*/,//java文件和第三方jar，多个用:隔开
                "-d", classes/*"/storage/emulated/0/麻瓜/java项目测试/classes/"*/,//class文件位置
                "-1.8",
                "-target", "1.8",
                src/*"/storage/emulated/0/麻瓜/java项目测试/src/"*/
        };
        success = main.compile(arg);
        System.out.println("编译结果:" + success);
        System.out.println("信息：\n" + info.toString());
        System.out.println("错误：\n" + error.toString());

        return success;
    }

    public static boolean dex(String lib, String classes) {
        boolean success = false;
        String[] args = new String[]{
                "--debug",
                "--verbose",
                "--min-sdk-version=27",
                "--num-threads=" + Runtime.getRuntime().availableProcessors(),//核心数
                "--output=" + classes + "/classes.dex"/*/classes.dex*/,//classes.dex输出文件路径
                classes,//class文件位置
                lib //第三方jar文件位置
        };

        try {
            com.android.dx.command.dexer.Main.Arguments arguments = new com.android.dx.command.dexer.Main.Arguments();
            Method parseMethod = arguments.getClass().getDeclaredMethod("parse", String[].class);
            parseMethod.setAccessible(true);
            parseMethod.invoke(arguments, new Object[]{args});
            int code = com.android.dx.command.dexer.Main.run(arguments);
            if (code != 0) {
                success = false;
            } else {
                success = true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } finally {
            if (success) {
                System.out.println("dex成功");
            } else {
                System.out.println("dex失败");
            }
        }
        return success;
    }
}
