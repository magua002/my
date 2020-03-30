package com.example.edittest;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.example.edittest.Interface.TextExecInterface;
import com.example.edittest.View.CodeEdit;

public class MainActivity extends AppCompatActivity {

    private CodeEdit codeEdit;

    private TextExecInterface textExec;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        codeEdit=findViewById(R.id.codeEdit);
        textExec=codeEdit.getTextExecInterface();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.actionbar_menu_item,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
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
                Toast.makeText(this,"有错误，已经停用",Toast.LENGTH_SHORT).show();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }
}
