package com.example.edittest;

import android.content.Context;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.View;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.Nullable;

public class ggg extends View {
    public ggg(Context context) {
        super(context);
    }

    public ggg(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setFocusable(true);
        setFocusableInTouchMode(true);
        InputMethodManager inputMethodManager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
    }

    @Override
    public boolean onCheckIsTextEditor() {
        return true;
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        outAttrs.imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI;
        outAttrs.inputType = InputType.TYPE_NULL;
        return new ICm(this, true);
    }
    private class ICm extends BaseInputConnection{
        public ICm(View targetView, boolean fullEditor) {
            super(targetView, fullEditor);
        }

        @Override
        public boolean commitText(CharSequence text, int newCursorPosition) {
            //return super.commitText(text, newCursorPosition);
            Toast.makeText(getContext(),"commit:"+text,Toast.LENGTH_SHORT).show();
            return true;
        }
    }
}
