package com.example.edittest;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.example.edittest.Interface.TextExecInterface;
import com.example.edittest.Util.JavaProjectUtil;
import com.example.edittest.View.CodeEdit;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

import dalvik.system.DexClassLoader;

public class MainActivity extends AppCompatActivity {

    private CodeEdit codeEdit;

    private TextExecInterface textExec;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //DexClassLoader dexClassLoader=new DexClassLoader("/storage/emulated/0/麻瓜/EcjNeed/EcjNeed.dex","/storage/emulated/0/麻瓜/EcjNeed/OutPut",null,getClassLoader());

        checkPermisson();
        codeEdit = findViewById(R.id.codeEdit);
        textExec = codeEdit.getTextExecInterface();
        File main = JavaProjectUtil.getMainJavaFile();
        if (main == null || !main.exists()) {
            JavaProjectUtil.createProject();
            main = JavaProjectUtil.getMainJavaFile();
        }
        codeEdit.openFile(main);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.actionbar_menu_item, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_run:
                runJava();
                break;
            case R.id.menu_copy:
                textExec.copy();
                break;
            case R.id.menu_paste:
                textExec.paste();
                break;
            case R.id.menu_cut:
                textExec.cut();
                break;
            case R.id.menu_undo:
                //textExec.undo();
                //break;
            case R.id.menu_redo:
                //textExec.redo();
                Toast.makeText(this, "有错误，已经停用", Toast.LENGTH_SHORT).show();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private void runJava() {
        codeEdit.saveFile();
        Intent intent = new Intent(this, RunJavaActivity.class);
        startActivity(intent);
    }


    private static final String[] permissions = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.INTERNET
    };

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void checkPermisson() {
        boolean flag = true;//默认全部被申请过
        for (int i = 0; i < permissions.length; i++) {
            //只要有一个没有申请成功
            if (!(ActivityCompat.checkSelfPermission(this, permissions[i]) == PackageManager.PERMISSION_GRANTED)) {
                flag = false;
            }
        }
        if (!flag) {
            //动态申请权限
            requestPermissions(permissions, 100);
        }
    }

    //动态申请权限的结果
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            boolean flag = true;
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    flag = false;
                }
            }
            if (flag) {
                //Toast.makeText(this, "ok ", Toast.LENGTH_SHORT).show();
                /*File file = new File("/storage/emulated/0/麻瓜/java项目测试/src/Main.java");
                codeEdit.openFile(file);*/
                File main = JavaProjectUtil.getMainJavaFile();
                if (!main.exists()) {
                    JavaProjectUtil.createProject();
                    main = JavaProjectUtil.getMainJavaFile();
                }
                codeEdit.openFile(main);
            } else {
                Toast.makeText(this, "error", Toast.LENGTH_SHORT).show();
                System.exit(0);
            }
        }
    }
}