package com.example.edittest.View;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.InputType;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.Scroller;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.example.edittest.Interface.AutoCompleteInterface;
import com.example.edittest.Interface.TextExecInterface;
import com.example.edittest.R;
import com.example.edittest.Util.JavaWordUtil;
import com.example.edittest.Util.ParseUtil;
import com.example.edittest.Util.TextExecHistoryAbst;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class CodeEdit extends View implements AutoCompleteInterface {
    //文件
    private File file;
    //文本
    private volatile List<String> textList = new ArrayList<String>();
    //光标行
    private int cursorRowIndex = 0;
    //光标在每一行中的位置
    private int cursorPositionInRowIndex = -1;
    //光标viewX
    //private float cursorViewX=0f;//本来想避免重复计算，但是不行
    //光标闪烁时间，毫秒
    private int cursorFlashTime = 500;
    //光标闪烁标识
    private boolean isFlash = true;
    //视图X轴
    //private int viewX = 0;
    //视图Y轴
    //private int viewY = 0;
    //滑动
    private Scroller scroller;
    //文本偏移量
    private float tranX = 100;
    //画笔集合
    private Map<String, Paint> paintMap = new HashMap<String, Paint>();
    //文本大小
    private int textSize = 50;
    //行高
    private int lineHeight;
    //考虑行高的基线
    private int lineHeightDescent;
    //开始行，结束行
    private int startRow, stopRow;
    //当前显示的最长行
    private String curMaxLine = "";
    //被选中文本，第一选中行，第一选中列，第二选中行，第二选中列
    private int firstSelectRow = -2;
    private int firstSelectCol = -2;
    private int secondSelectRow = -2;
    private int secondSelectCol = -2;
    //选中文本光标位置
    private float firstSelectX = 0f;
    private float firstSelectY = 0f;
    private float secondSelectX = 0f;
    private float secondSelectY = 0f;
    //手势监听
    private GDListener gdl;
    private GestureDetector gestureDetector;
    //操作历史
    private TextExecHistoryAbst textExecHistoryAbst;
    //背景图片
    Bitmap bitmap;
    //提示框
    private AutoCompletePopupWindow autoCompletePopupWindow;
    //java字符
    private final char[] javaChars = " \r\n!~`#%^&*()-+={}|[]:\";'<>?,./".toCharArray();

    private IC ic;

    //解析注释用到
    //private JavaWordUtil javaWordUtil = new JavaWordUtil();
    //分析用到
    private ParseUtil parseUtil = new ParseUtil();
    //解析线程
    private volatile boolean isParseing = false;

    public CodeEdit(Context context) {
        super(context);
    }

    public CodeEdit(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        //滑动
        scroller = new Scroller(context);
        //自动补全框
        autoCompletePopupWindow = new AutoCompletePopupWindow(this);
        //加载画笔
        initPaint();
        //计算行高
        computeLineHeight();
        //光标闪烁
        cursorFlash();
        //手势监听
        gdl = new GDListener();
        gestureDetector = new GestureDetector(context, gdl);
        //操作历史
        initTextExecHistory();
        //输入法状态监听
        layoutListener();
        //焦点
        setFocusable(true);
        setFocusableInTouchMode(true);

        //防止越界
        //textList.add("");
        //initParseThread();
        /*try {
            InputStream inputStream = getResources().getAssets().open("CodeEdit.java");
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String line = null;
            StringBuffer buffer = new StringBuffer();
            while ((line = bufferedReader.readLine()) != null) {
                buffer.append(line).append("\r\n");
                textList.add(line);
            }
            parseAndPostInvalidata();
        } catch (IOException e) {
            e.printStackTrace();
        }*/
    }

    public void openFile(File file) {
        this.file = file;
        FileReader fileReader = null;
        BufferedReader bufferedReader = null;
        try {
            fileReader = new FileReader(file);
            bufferedReader = new BufferedReader(fileReader);

            textList.clear();

            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                textList.add(line);
            }

            //防止打开的是空文本
            if (textList.size() == 0) {
                textList.add("");
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fileReader != null) fileReader.close();
                if (bufferedReader != null) bufferedReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        parseAndPostInvalidata();
        postInvalidate();
    }

    public void saveFile() {
        saveFile(file);
    }

    private void parseAndPostInvalidata() {

        /*if (textList!=null&&parseUtil!=null&&!isParseing()){
            new Thread(()->{
                setParseing(true);
                Iterator<String> iterator=textList.iterator();
                StringBuffer buffer=new StringBuffer();
                while (iterator.hasNext()){
                    buffer.append(iterator.next()).append("\r\n");
                }
                parseUtil.parse(buffer.toString());
                postInvalidate();
                setParseing(false);
            }).start();
        }*/
    }

    private synchronized boolean isParseing() {
        return isParseing;
    }

    private synchronized void setParseing(boolean b) {
        isParseing = b;
    }

    public void saveFile(File file) {
        FileWriter fileWriter = null;
        BufferedWriter bufferedWriter = null;
        try {
            fileWriter = new FileWriter(file);
            bufferedWriter = new BufferedWriter(fileWriter);
            for (String s : textList) {
                bufferedWriter.write(s);
                bufferedWriter.newLine();
            }
            bufferedWriter.flush();
            Toast.makeText(getContext(), "文件已保存", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (fileWriter != null) fileWriter.close();
                if (bufferedWriter != null) bufferedWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void layoutListener() {
        this.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                //确定是否上移
                int softHeight = getSoftKeyboardHeight();
                int heig = lineHeight * (cursorRowIndex + 1);
                //int heig_low = heig + viewY + lineHeightDescent;
                int heig_low = heig + (-getScrollY()) + lineHeightDescent;
                int heig_d = getHeight() - softHeight;
                if (heig_low > heig_d) {
                    //viewY = viewY - (heig_low - heig_d);
                    //scroller.startScroll(getScrollX(), getScrollY(), 0, (heig_low - heig_d));
                    scrollBy(0,(heig_low - heig_d));
                    postInvalidate();
                } else {
                    if (autoCompletePopupWindow != null && autoCompletePopupWindow.isShow())
                        autoCompletePopupWindow.dismiss();
                }
            }
        });
    }

    private void initPaint() {
        paintMap.put("默认", paintBuild(Color.BLACK));//默认画笔
        paintMap.put("光标", paintBuild(Color.parseColor("#00574B")));
        paintMap.put("选中行", paintBuild(Color.argb(200, 230, 230, 230)));
        paintMap.put("行号", paintBuild(Color.BLUE));
        paintMap.put("关键字", paintBuild(Color.MAGENTA));
        paintMap.put("字符串", paintBuild(Color.RED));
        paintMap.put("注释", paintBuild(Color.GRAY));
        paintMap.put("类", paintBuild(Color.rgb(160, 191, 214)));
        paintMap.put("变量", paintBuild(Color.rgb(186, 85, 211)));
        paintMap.put("方法", paintBuild(Color.rgb(227, 179, 37)));
        paintMap.put("数字", paintBuild(Color.rgb(147, 224, 255)));
        paintMap.put("运算符", paintBuild(Color.rgb(131, 175, 155)));
    }

    private Paint paintBuild(int color) {
        Paint paint = new Paint();
        paint.setColor(color);
        paint.setAntiAlias(true);//抗锯齿
        paint.setTextSize(textSize);
        paint.setTypeface(Typeface.MONOSPACE);//等宽字体
        return paint;
    }

    //光标闪烁
    private void cursorFlash() {
        Timer timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                isFlash = isFlash ? false : true;
                if (!isSelectedText()) postInvalidate();
            }
        };
        timer.schedule(timerTask, cursorFlashTime, cursorFlashTime);
    }

    //计算行高
    private void computeLineHeight() {
        Paint paint = paintMap.get("默认");
        Paint.FontMetricsInt metricsInt = paint.getFontMetricsInt();
        lineHeight = metricsInt.descent - metricsInt.top + metricsInt.leading;
        lineHeightDescent = metricsInt.descent;
    }

    //计算输入法高度(偷懒，包含提示框高度)
    private int getSoftKeyboardHeight() {
        int screenHeight = ((Activity) getContext()).getWindow().getDecorView().getRootView().getHeight();
        Rect rect = new Rect();
        ((Activity) getContext()).getWindow().getDecorView().getWindowVisibleDisplayFrame(rect);
        if (autoCompletePopupWindow != null && autoCompletePopupWindow.isShow())
            return screenHeight - rect.bottom + 500;
        else
            return screenHeight - rect.bottom;
    }

    //操作历史
    private void initTextExecHistory() {
        textExecHistoryAbst = new TextExecHistoryAbst() {
            @Override
            public void undo(boolean isAppend, String text, int firstRow, int firstCol, int secondRow, int secondCol) {
            }

            @Override
            public void redo(boolean isAppend, String text, int firstRow, int firstCol, int secondRow, int secondCol) {
                Toast.makeText(getContext(), "redo", Toast.LENGTH_SHORT).show();
            }
        };
    }

    @Override
    public boolean onCheckIsTextEditor() {
        return true;
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        outAttrs.imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI;
        outAttrs.inputType = InputType.TYPE_NULL;
        ic = new IC(this, true);
        return ic;
    }

    @Override
    public void autoInputText(String text) {
        inputTextToList(text);
        invalidate();
    }

    //输入法
    private class IC extends BaseInputConnection {

        public IC(View targetView, boolean fullEditor) {
            super(targetView, fullEditor);
        }

        @Override
        public boolean commitText(CharSequence text, int newCursorPosition) {

            autoCompletePopupWindow.input(String.valueOf(text));

            if (isSelectedText()) {
                deleteSelectText();
                deselectText();
            }

            if (cursorPositionInRowIndex == -1) {
                String curText = textList.get(cursorRowIndex);
                textList.set(cursorRowIndex, text + curText);

                //textExecHistoryAbst.append(text.toString(), cursorRowIndex, -1, cursorRowIndex, -1 + text.length());
            } else {

                /*textExecHistoryAbst.append(text.toString(),
                        cursorRowIndex,
                        cursorPositionInRowIndex,
                        cursorRowIndex,
                        cursorPositionInRowIndex + text.length());*/

                String curText = textList.get(cursorRowIndex);
                char[] chars = curText.toCharArray();
                StringBuffer stringBuffer = new StringBuffer();
                for (int i = 0; i < chars.length; i++) {
                    stringBuffer.append(chars[i]);
                    if (i == cursorPositionInRowIndex) {
                        stringBuffer.append(text);
                    }
                }
                textList.set(cursorRowIndex, stringBuffer.toString());
            }
            cursorPositionInRowIndex += text.length();
            postInvalidate();
            parseAndPostInvalidata();
            return true;
        }

        @Override
        public boolean sendKeyEvent(KeyEvent event) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                switch (event.getKeyCode()) {
                    //删除键
                    case KeyEvent.KEYCODE_DEL:
                        deleteText();
                        break;
                    //回车键
                    case KeyEvent.KEYCODE_ENTER:
                        //选中文本处理
                        if (isSelectedText()) {
                            deleteSelectText();
                            deselectText();
                        }
                        //防止处理字符串时越界
                        String curText = textList.get(cursorRowIndex);
                        String space = getSpace(curText);
                        int spaceCount = space.length();
                        if (cursorPositionInRowIndex > -1 && cursorPositionInRowIndex < curText.length() - 1) {
                            StringBuffer newTextBuffer = new StringBuffer();
                            StringBuffer lastTextBuffer = new StringBuffer();
                            //新行添加空格
                            newTextBuffer.append(space);
                            char[] chars = curText.toCharArray();
                            for (int i = 0; i < chars.length; i++) {
                                if (i <= cursorPositionInRowIndex) {
                                    lastTextBuffer.append(chars[i]);
                                } else {
                                    newTextBuffer.append(chars[i]);
                                }
                            }

                            /*textExecHistoryAbst.append("\r\n" + space,
                                    cursorRowIndex,
                                    cursorPositionInRowIndex - 1,
                                    cursorRowIndex + 1,
                                    -1 + spaceCount);*/

                            textList.set(cursorRowIndex, lastTextBuffer.toString());
                            cursorRowIndex++;
                            textList.add(cursorRowIndex, newTextBuffer.toString());
                            cursorPositionInRowIndex = -1 + spaceCount;
                        } else if (cursorPositionInRowIndex == -1) {
                            textList.set(cursorRowIndex++, "");
                            textList.add(cursorRowIndex, curText);

                            textExecHistoryAbst.append("\r\n", cursorRowIndex - 1, -1, cursorRowIndex, -1);
                        } else if (cursorPositionInRowIndex == curText.length() - 1) {
                            textList.add(++cursorRowIndex, "" + space);
                            cursorPositionInRowIndex = -1 + spaceCount;

                            /*textExecHistoryAbst.append("\r\n" + space,
                                    cursorRowIndex - 1,
                                    textList.get(cursorRowIndex - 1).length() - 1,
                                    cursorRowIndex, cursorPositionInRowIndex);*/
                        }
                        //防止超过键盘，确定是否上移
                        int softHeight = getSoftKeyboardHeight();
                        int heig = lineHeight * (cursorRowIndex + 1);
                        //int heig_low = heig + viewY + lineHeightDescent;
                        int heig_low = heig + (-getScrollY()) + lineHeightDescent;
                        int heig_d = getHeight() - softHeight;
                        if (heig_low > heig_d) {
                            //viewY = viewY - (heig_low - heig_d);
                            scroller.startScroll(getScrollX(), getScrollY(), 0, (heig_low - heig_d));
                        }
                        break;
                }
                invalidate();
                parseAndPostInvalidata();
            }
            return true;
        }

        @Override
        public boolean deleteSurroundingText(int beforeLength, int afterLength) {
            deleteText();
            invalidate();
            return true;
        }

        //自己加的一个处理删除的方法
        private void deleteText() {
            //如果处于选中文本状态
            if (isSelectedText()) {
                //删除选中文本
                deleteSelectText();
                //取消选中状态
                deselectText();
                return;
            }

            autoCompletePopupWindow.delete();

            String curText = textList.get(cursorRowIndex);
            if (cursorRowIndex == 0 && cursorPositionInRowIndex == -1) {
                return;//如果光标在第一行且在最前面，不处理文本
            } else {
                if (cursorPositionInRowIndex == -1) {
                    textList.remove(cursorRowIndex);
                    String lastText = textList.get(--cursorRowIndex);
                    cursorPositionInRowIndex = lastText.length() - 1;
                    lastText += curText;
                    textList.set(cursorRowIndex, lastText);

                    //textExecHistoryAbst.delete("\r\n", cursorRowIndex, cursorPositionInRowIndex, cursorRowIndex + 1, -1);

                } else if (cursorPositionInRowIndex >= 0) {
                    if (!deleteSpace(curText)) {
                        StringBuffer stringBuffer = new StringBuffer();
                        char[] chars = curText.toCharArray();
                        for (int i = 0; i < chars.length; i++) {
                            if (i != cursorPositionInRowIndex) {
                                stringBuffer.append(chars[i]);
                            } else {

                            }
                        }
                        textList.set(cursorRowIndex, stringBuffer.toString());
                        cursorPositionInRowIndex--;
                    }
                }
            }
        }

        //删除最前面的4个空格
        private boolean deleteSpace(String curText) {
            boolean deleted = false;
            if (cursorPositionInRowIndex >= 3) {
                char[] chars = curText.toCharArray();
                for (int i = cursorPositionInRowIndex; i > cursorPositionInRowIndex - 4; i--) {
                    if (' ' != chars[i]) {
                        return false;
                    }
                }

                if (cursorPositionInRowIndex - 4 == -1) {
                    curText = curText.substring(4, curText.length());
                } else {
                    curText = curText.substring(0, cursorPositionInRowIndex - 4 + 1) +
                            curText.substring(cursorPositionInRowIndex + 1, curText.length());
                }
                textList.set(cursorRowIndex, curText);
                cursorPositionInRowIndex = cursorPositionInRowIndex - 4;

                /*textExecHistoryAbst.delete("    ", cursorRowIndex,
                        cursorPositionInRowIndex,
                        cursorRowIndex,
                        cursorPositionInRowIndex + 4);*/

                deleted = true;
            }
            return deleted;
        }
    }

    //得到该行前面所有空格
    private String getSpace(String text) {
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < text.length(); i++) {
            if (' ' == text.charAt(i)) {
                buffer.append(' ');
            } else {
                break;
            }
        }
        return buffer.toString();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (textList.size() == 0) return false;
        return gestureDetector.onTouchEvent(event);
    }

    //手势监听
    private class GDListener implements GestureDetector.OnGestureListener {

        //是否纵向移动
        private boolean isDirection;
        //是否判断过
        private boolean isFirstMove;
        //抛掷动画
        private ValueAnimator onflingValueAnimator;
        //按中的是第一个还是第二个选中文本光标
        private boolean isTouchOnFirstSelectCursor;

        private float selectDX, selectDY;

        private boolean isFirstScrollSelectCursor;

        @Override
        public boolean onDown(MotionEvent e) {
            isFirstMove = false;
            if (onflingValueAnimator != null && onflingValueAnimator.isRunning()) {
                onflingValueAnimator.cancel();
            }
            if (isFirstTouchOnSelectCursor(e)) {
                isFirstScrollSelectCursor = true;
            } else {
                isFirstScrollSelectCursor = false;
            }
            return true;
        }

        @Override
        public void onShowPress(MotionEvent e) {

        }

        //单击
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            //检查输入法是否打开
            int softHeight = getSoftKeyboardHeight();
            if (softHeight < 100) {
                //打开输入法
                InputMethodManager inputMethodManager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
            }

            //在选中本文状态，单击会取消选中文本
            if (isSelectedText()) {
                deselectText();
            }

            float x = e.getX();
            float y = e.getY();
            //确定选中的是哪一行
            int index = (int) (/*(Math.abs(viewY)*/(getScrollY() + y - lineHeightDescent) / lineHeight);
            int maxLine = textList.size() - 1;
            cursorRowIndex = index > maxLine ? maxLine : index;
            //确定选中的是哪一个字
            String curText = textList.get(cursorRowIndex);
            if (TextUtils.isEmpty(curText)) {
                cursorPositionInRowIndex = -1;
            } else {
                Paint paint = paintMap.get("默认");
                float w = paint.measureText(curText);
                float realX = /*Math.abs(viewX)*/getScrollX() + x - tranX;
                if (realX > w) {
                    cursorPositionInRowIndex = curText.length() - 1;
                } else {
                    cursorPositionInRowIndex = getCursorPositionInRowIndex(curText, realX, paint);
                }
            }
            invalidate();
            return true;
        }

        @Override//e1是第一次触摸，e2是当前
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {

            //滑动选中光标
            if (isFirstScrollSelectCursor) {
                if (isTouchOnFirstSelectCursor) {
                    float x = e2.getX();
                    float y = e2.getY();
                    //确定选中的是哪一行
                    int index = (int) ((/*Math.abs(viewY)*/getScrollY() + y - lineHeightDescent) / lineHeight);
                    int maxLine = textList.size() - 1;
                    firstSelectRow = index > maxLine ? maxLine : index;
                    //if (firstSelectRow>secondSelectRow) firstSelectRow=secondSelectRow;
                    //确定选中的是哪一个字
                    String curText = textList.get(firstSelectRow);
                    if (TextUtils.isEmpty(curText)) {
                        firstSelectCol = -1;
                    } else {
                        Paint paint = paintMap.get("默认");
                        float w = paint.measureText(curText);
                        float realX = /*Math.abs(viewX)*/getScrollX() + x - tranX;
                        if (realX > w) {
                            firstSelectCol = curText.length() - 1;
                        } else {
                            firstSelectCol = getCursorPositionInRowIndex(curText, realX, paint);
                        }
                    }
                } else {
                    float x = e2.getX();
                    float y = e2.getY();
                    //确定选中的是哪一行
                    int index = (int) ((/*Math.abs(viewY)*/getScrollY() + y - lineHeightDescent) / lineHeight);
                    int maxLine = textList.size() - 1;
                    secondSelectRow = index > maxLine ? maxLine : index;
                    //if (firstSelectRow>secondSelectRow) firstSelectRow=secondSelectRow;
                    //确定选中的是哪一个字
                    String curText = textList.get(secondSelectRow);
                    if (TextUtils.isEmpty(curText)) {
                        secondSelectCol = -1;
                    } else {
                        Paint paint = paintMap.get("默认");
                        float w = paint.measureText(curText);
                        float realX = /*Math.abs(viewX)*/getScrollX() + x - tranX;
                        if (realX > w) {
                            secondSelectCol = curText.length() - 1;
                        } else {
                            secondSelectCol = getCursorPositionInRowIndex(curText, realX, paint);
                        }
                    }
                }

                if (firstSelectRow > secondSelectRow) {
                    int rowTemp = firstSelectRow;
                    firstSelectRow = secondSelectRow;
                    secondSelectRow = rowTemp;
                    int colTemp = firstSelectCol;
                    firstSelectCol = secondSelectCol;
                    secondSelectCol = colTemp;
                    isTouchOnFirstSelectCursor = !isTouchOnFirstSelectCursor;
                } else if (firstSelectRow == secondSelectCol && firstSelectCol > secondSelectCol) {
                    int colTemp = firstSelectCol;
                    firstSelectCol = secondSelectCol;
                    secondSelectCol = colTemp;
                    isTouchOnFirstSelectCursor = !isTouchOnFirstSelectCursor;
                }

                invalidate();
                return true;
            }

            //滑动视图
            if (!isFirstMove) {
                isDirection = Math.abs(distanceY) > Math.abs(distanceX) ? true : false;
                isFirstMove = true;
            }
            if (isDirection) {
                //viewY -= distanceY;//y=y-dy
                //scroller.startScroll(getScrollX(), getScrollY(), 0, (int) distanceY);
                scrollBy(0, (int) distanceY);
            } else {
                //viewX -= distanceX;
                //scroller.startScroll(getScrollX(), getScrollY(), (int) distanceX, 0);
                scrollBy((int) distanceX, 0);
            }

            //画布越界处理
            if (textList.size() * lineHeight - 100 < /*-viewY*/getScrollY()) {
                //viewY = -(textList.size() * lineHeight - 100);
                setScrollY((textList.size() * lineHeight - 100));
            }
            Paint paint = paintMap.get("默认");
            float maxLineWidth = paint.measureText(curMaxLine);
            if (maxLineWidth - tranX - 100 < /*-viewX*/getScrollX()) {
                //viewX = -(int) (maxLineWidth - tranX - 100);
                setScrollX((int) (maxLineWidth - tranX - 100));
            }

            if (/*viewX*/(-getScrollX()) >= 0) {
                //viewX = 0;
                setScrollX(0);
            }
            if (/*viewY*/(-getScrollY()) >= 0) {
                //viewY = 0;
                setScrollY(0);
            }

            invalidate();
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            longPressSelectText(e);
            if (isSelectedText()) invalidate();
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            //Log.d("onFling", "x:" + velocityX + ",y:" + velocityY);
            onflingValueAnimator = isDirection ? ValueAnimator.ofInt(0, (int) (velocityY / 50)) : ValueAnimator.ofInt(0, (int) (velocityX / 50));
            onflingValueAnimator.setDuration(isDirection ? (int) Math.abs(velocityY / 10) : (int) Math.abs(velocityX / 10));
            onflingValueAnimator.setInterpolator(new DecelerateInterpolator());
            onflingValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    int value = (int) animation.getAnimatedValue();
                    //Log.d("valueAnim:", "value:" + value);
                    if (isDirection) {
                        //viewY = viewY + value;
                        scrollBy(0,-value);
                    } else {
                        //viewX = viewX + value;
                        scrollBy(-value,0);
                    }
                    //判断是否超出范围
                    if (textList.size() * lineHeight - 100 < /*-viewY*/getScrollY()) {
                        //viewY = -(textList.size() * lineHeight - 100);
                        setScrollY(textList.size() * lineHeight - 100);
                        //onflingValueAnimator.cancel();
                    }
                    Paint paint = paintMap.get("默认");
                    float maxLineWidth = paint.measureText(curMaxLine);
                    if (maxLineWidth - tranX - 100 < /*-viewX*/getScrollX()) {
                        //viewX = -(int) (maxLineWidth - tranX - 100);
                        setScrollX((int) (maxLineWidth - tranX - 100));
                        //onflingValueAnimator.cancel();
                    }
                    if (/*viewX*/(-getScrollX()) >= 0) {
                        //viewX = 0;
                        setScrollX(0);
                        //onflingValueAnimator.cancel();
                    }

                    if (/*viewY*/(-getScrollY()) >= 0) {
                        //viewY = 0;
                        setScrollY(0);
                        //onflingValueAnimator.cancel();
                    }

                    invalidate();
                }
            });
            onflingValueAnimator.start();

            /*if (isDirection) {
                scroller.startScroll(getScrollX(), getScrollY(), 0, (int) (-velocityY / 50), 500);
            } else {
                scroller.startScroll(getScrollX(), getScrollY(), (int) (-velocityX / 50), 0, 500);
            }*/

            return true;
        }

        //计算光标处于哪一个位置
        private int getCursorPositionInRowIndex(String curText, float realX, Paint paint) {
            char[] chars = curText.toCharArray();
            float len = 0f;
            int index = -1;
            for (char c : chars) {
                float c_l = paint.measureText(String.valueOf(c));
                if (len + c_l / 2 > realX) {
                    break;
                } else {
                    len = len + c_l;
                    index++;
                }
            }
            //cursorViewX=len;
            return index;
        }

        //长按选择文本
        private void longPressSelectText(MotionEvent event) {
            float x = event.getX();
            float y = event.getY();
            //确定选中的是哪一行
            int index = (int) ((/*Math.abs(viewY)*/getScrollY() + y - lineHeightDescent) / lineHeight);
            int maxLine = textList.size() - 1;
            //cursorRowIndex = index > maxLine ? maxLine : index;
            //如果长按位置不在某一行上，则不进行任何操作
            if (index > maxLine) return;
            //确定选中的是哪一个字
            String curText = textList.get(index);
            if (TextUtils.isEmpty(curText)) {
                //如果选中该行为空，不进行任何操作
                return;
            } else {
                firstSelectRow = index;
                secondSelectRow = index;
                cursorRowIndex = index;
                Paint paint = paintMap.get("默认");
                float w = paint.measureText(curText);
                float realX = /*Math.abs(viewX)*/getScrollX() + x - tranX;
                int indexCol;
                if (realX > w) {
                    //若没选中字，则不进行任何操作,并取消选中
                    deselectText();
                    return;
                } else {
                    indexCol = getCursorPositionInRowIndex(curText, realX, paint);

                    if (indexCol == -1) {
                        deselectText();
                        return;
                    }

                    firstSelectCol = indexCol - 1;
                    secondSelectCol = indexCol;
                    //向前
                    for (int i = indexCol - 1; i >= 0; i--) {
                        if (isJavaChar(curText.charAt(i))) {
                            break;
                        }
                        firstSelectCol--;
                    }
                    //向后
                    for (int i = indexCol + 1; i < curText.length(); i++) {
                        if (isJavaChar(curText.charAt(i))) {
                            break;
                        }
                        secondSelectCol++;
                    }
                    cursorPositionInRowIndex = secondSelectCol;
                }
            }
        }

        //是否第一次触摸在选中文本光标上
        private boolean isFirstTouchOnSelectCursor(MotionEvent event) {
            if (!isSelectedText()) return false;
            float x = event.getX() - /*viewX*/(-getScrollX()) - tranX;
            float y = event.getY() - /*viewY*/(-getScrollY());
            //在第一个光标上
            if (firstSelectX - 25 <= x &&
                    firstSelectX + 25 >= x &&
                    firstSelectY + 25 <= y &&
                    firstSelectY + 75 >= y) {
                isTouchOnFirstSelectCursor = true;
                return true;
            }
            //在第二个光标上
            if (secondSelectX - 25 <= x &&
                    secondSelectX + 25 >= x &&
                    secondSelectY + 25 <= y &&
                    secondSelectY + 75 >= y) {
                isTouchOnFirstSelectCursor = false;
                return true;
            }
            return false;
        }

    }

    //是否选中了文本
    private boolean isSelectedText() {
        return !(firstSelectRow == -2 ||
                firstSelectCol == -2 ||
                secondSelectRow == -2 ||
                secondSelectCol == -2);
    }

    //取消选中文本,在完成任何改变文本的操作后都要取消选中文本
    private void deselectText() {
        firstSelectRow = firstSelectCol = secondSelectRow = secondSelectCol = -2;
    }

    //删除选中文本
    private void deleteSelectText() {
        if (!isSelectedText()) return;

        if (firstSelectRow == secondSelectRow) {
            String selectRowText = textList.get(firstSelectRow);
            char[] chars = selectRowText.toCharArray();
            StringBuffer stringBuffer = new StringBuffer();
            StringBuffer deleteBuffer = new StringBuffer();
            for (int i = 0; i < chars.length; i++) {
                if (i > firstSelectCol && i <= secondSelectCol) {
                    deleteBuffer.append(chars[i]);
                } else {
                    stringBuffer.append(chars[i]);
                }
            }
            //textExecHistoryAbst.delete(deleteBuffer.toString(), firstSelectRow, firstSelectCol, secondSelectRow, secondSelectCol);
            textList.set(firstSelectRow, stringBuffer.toString());
        } else {
            /*//删除操作历史
            StringBuffer deleteBuffer = new StringBuffer();
            deleteBuffer.append(textList.get(firstSelectRow).substring(firstSelectCol + 1)).append("\r\n");
            for (int i = firstSelectRow + 1; i < secondSelectRow; i++) {
                deleteBuffer.append(textList.get(i)).append("\r\n");
            }*/
            //deleteBuffer.append(textList.get(secondSelectRow).substring(0, secondSelectCol));//secondSelectCol=-1错误
            //textExecHistoryAbst.delete(deleteBuffer.toString(), firstSelectRow, firstSelectCol, secondSelectRow, secondSelectCol);

            String firstRowText = null;
            String secondRowText = null;
            String line = null;
            Collection<String> collection = new ArrayList<>();
            for (int i = firstSelectRow; i <= secondSelectRow; i++) {
                line = textList.get(i);
                if (i == firstSelectRow) {
                    firstRowText = line;
                    continue;//第一行不删
                } else if (i == secondSelectRow) {
                    secondRowText = line;
                }
                collection.add(line);
            }
            textList.removeAll(collection);
            secondRowText = secondRowText.substring(secondSelectCol + 1);
            if (firstSelectCol == -1) {
                textList.set(firstSelectRow, secondRowText);
            } else {
                firstRowText = firstRowText.substring(0, firstSelectCol + 1);
                textList.set(firstSelectRow, firstRowText + secondRowText);
            }
        }
        //删除后更新光标位置
        cursorRowIndex = firstSelectRow;
        cursorPositionInRowIndex = firstSelectCol;
    }

    //得到选中文本内容，多行用回车
    private String getSelectText() {
        if (!isSelectedText()) return null;
        StringBuffer buffer = new StringBuffer();
        if (firstSelectRow == secondSelectRow) {
            String selectRowText = textList.get(firstSelectRow);
            char[] chars = selectRowText.toCharArray();
            for (int i = 0; i < chars.length; i++) {
                if (i > firstSelectCol && i <= secondSelectCol) {
                    buffer.append(chars[i]);
                }
            }
        } else {
            for (int i = firstSelectRow; i <= secondSelectRow; i++) {
                String text = textList.get(i);
                if (i == firstSelectRow) {
                    //防止越界
                    if (firstSelectCol >= text.length() - 1) {
                        buffer.append("");
                    } else {
                        buffer.append(text.substring(firstSelectCol + 1, text.length()));
                    }
                } else if (i == secondSelectRow) {
                    if (secondSelectCol == -1) {
                        buffer.append("");
                    } else {
                        buffer.append(text.substring(0, secondSelectCol + 1));
                    }
                } else {
                    buffer.append(text);
                }
                buffer.append("\r\n");
            }
        }
        return buffer.toString();
    }

    //是不是Java符号
    private boolean isJavaChar(char cc) {
        for (char c : javaChars) {
            if (c == cc) {
                return true;
            }
        }
        return false;
    }

    private TextExecInterface textExecInterface;

    public TextExecInterface getTextExecInterface() {
        if (textExecInterface == null) {
            textExecInterface = new TextExecImpl();
        }
        return textExecInterface;
    }

    private class TextExecImpl implements TextExecInterface {
        @Override
        public void copy() {
            if (textList.size() == 0) return;
            if (!isSelectedText()) return;
            String text = getSelectText();
            if (text != null) {
                clipPut(text);
            }
            deselectText();
            invalidate();
        }

        @Override
        public void paste() {
            if (textList.size() == 0) return;
            if (isSelectedText()) {
                deleteSelectText();
                deselectText();
            }
            String text = clipGet();
            inputTextToList(text);
            //textExecHistoryAbst.append(text, curRow, curCol, cursorRowIndex, cursorPositionInRowIndex);
            invalidate();
        }

        @Override
        public void cut() {
            if (textList.size() == 0) return;
            if (isSelectedText()) {
                clipPut(getSelectText());
                deleteSelectText();
                deselectText();
                invalidate();
            }
        }

        @Override
        public void undo() {
            if (isSelectedText()) {
                deselectText();
                invalidate();
                return;
            }

            if (textExecHistoryAbst.canUndo()) {
                textExecHistoryAbst.executeUndo();
                invalidate();
            }
        }

        @Override
        public void redo() {
            if (textList.size() == 0) return;
            if (textList.size() == 0) return;
            if (isSelectedText()) {
                deselectText();
                invalidate();
                return;
            }

            if (textExecHistoryAbst.canRedo()) {
                textExecHistoryAbst.executeRedo();
                invalidate();
            }
        }

        private void clipPut(String text) {
            if (TextUtils.isEmpty(text)) return;
            ClipboardManager clipboardManager = (ClipboardManager) CodeEdit.this.getContext()
                    .getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clipData = ClipData.newPlainText("麻瓜CodeEdit", text);
            clipboardManager.setPrimaryClip(clipData);
        }

        private String clipGet() {
            String text = null;
            ClipboardManager clipboardManager = (ClipboardManager) CodeEdit.this.getContext()
                    .getSystemService(Context.CLIPBOARD_SERVICE);
            if (!clipboardManager.hasPrimaryClip()) return null;
            ClipData clipData = clipboardManager.getPrimaryClip();
            text = clipData.getItemAt(0).getText().toString();
            //获取 ClipDescription
            //ClipDescription clipDescription = clipboardManager.getPrimaryClipDescription();
            //获取 lable
            //String lable = clipDescription.getLabel().toString();
            return text;
        }
    }

    private void inputTextToList(String text) {
        if (TextUtils.isEmpty(text)) return;

        String[] strings = text.split("\n");
        String curText = textList.get(cursorRowIndex);
        String space = getSpace(curText);
        String frontText = curText.substring(0, cursorPositionInRowIndex + 1);
        String laterText = curText.substring(cursorPositionInRowIndex + 1);
        if (strings.length == 1) {
            String s = strings[0];
            textList.set(cursorRowIndex, frontText + s + laterText);
            cursorPositionInRowIndex = cursorPositionInRowIndex + s.length();
        } else {
            for (int i = 0; i < strings.length; i++) {
                String s = strings[i];
                if (i == 0) {
                    textList.set(cursorRowIndex, frontText + s);
                    //cursorPositionInRowIndex = cursorPositionInRowIndex + s.length();
                    cursorRowIndex++;
                } else if (i == strings.length - 1) {
                    s = space + s;
                    textList.add(cursorRowIndex, s + laterText);
                    cursorPositionInRowIndex = s.length() - 1;
                } else {
                    textList.add(cursorRowIndex, space + s);
                    cursorRowIndex++;
                }
            }
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        //Log.d("onSizeChanged", "x:" + viewX + ",y:" + viewY);
        autoCompletePopupWindow.setWh(w, 500);
    }

    @Override
    public void computeScroll() {
        if (scroller.computeScrollOffset()) {
            scrollTo(scroller.getCurrX(), scroller.getCurrY());
            /*viewX=-scroller.getCurrX();
            viewY=-scroller.getCurrY();*/
            //画布越界处理
            if (textList.size() * lineHeight - 100 < /*-viewY*/getScrollY()) {
                //viewY = -(textList.size() * lineHeight - 100);
                setScrollY(textList.size() * lineHeight - 100);
            }
            Paint paint = paintMap.get("默认");
            float maxLineWidth = paint.measureText(curMaxLine);
            if (maxLineWidth - tranX - 100 < /*-viewX*/getScrollX()) {
                //viewX = -(int) (maxLineWidth - tranX - 100);
                setScrollX((int) (maxLineWidth - tranX - 100));
            }

            if (/*viewX*/(-getScrollX()) >= 0) {
                //viewX = 0;
                setScrollX(0);
            }
            if (/*viewY*/(-getScrollY()) >= 0) {
                //viewY = 0;
                setScrollY(0);
            }
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {

        if (textList.size() == 0) {
            Paint paint = paintMap.get("默认");
            String notOpenFile = "未打开文件";
            float w = paint.measureText(notOpenFile);
            canvas.drawText(notOpenFile, (getWidth() - w) / 2, getHeight() / 2, paint);
            return;
        }

        //背景图
        //drawBackgroundImage(canvas);
        //在背景图上铺一层半透明，不然看不清，本来想直接在图片上弄的，但是不会
        //canvas.drawColor(Color.argb(100, 255, 255, 255));
        //计算开始和结束行
        computeStartAndStop();
        //偏移到指定位置
        //canvas.translate(viewX, viewY);
        Log.d("偏移值", "getScrollX:" + getScrollX());
        //画当前选中行矩形
        if (!isSelectedText()) drawRowRect(canvas);
        //画行号
        drawRowNumber(canvas);
        //先画行号再偏移文本
        canvas.translate(tranX, 0);
        //画选中文本
        if (isSelectedText()) drawSelectTextRect(canvas);
        //画文本
        drawText(canvas);
        //画选中文本光标
        if (isSelectedText()) drawSelectCursor(canvas);
        //光标闪烁
        if (isFlash && !isSelectedText()) drawCursor(canvas);
    }

    //计算显示的行号
    private void computeStartAndStop() {
        startRow = Math.abs(/*viewY*/(-getScrollY()) + lineHeightDescent) / lineHeight;
        int maxLine = textList.size() - 1;
        if (startRow > maxLine) return;
        int stopIndex = (getHeight() / lineHeight) + 1;
        if ((maxLine - startRow) > stopIndex) {
            stopRow = startRow + stopIndex;
            /*stopRow=stopRow>maxLine?maxLine:stopRow;*/
        } else {
            stopRow = maxLine;
        }
    }

    //背景图片
    private void drawBackgroundImage(Canvas canvas) {
        if (bitmap == null) {
            bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.g).copy(Bitmap.Config.ARGB_8888, true);
            float imageW = bitmap.getWidth();
            float imageH = bitmap.getHeight();
            float startImageX = 0f;
            float startImageY = 0f;
            float image_W = imageW;
            float image_H = imageH;
            float scale = 0f;
            Matrix matrix = new Matrix();
            if (imageW < imageH) {
                scale = imageH < getHeight() ? getHeight() / imageH : imageH / getHeight();
                float i_w = imageW * scale;
                startImageX = Math.abs(i_w - getWidth()) / 2 / scale;
                image_W = imageW - startImageX * 2;
            } else {
                scale = imageW < getWidth() ? getWidth() / imageW : imageW / getWidth();
                float i_h = imageH * scale;
                startImageY = Math.abs(i_h - getHeight()) / 2 / scale;
                image_H = imageH - startImageY * 2;
            }

            matrix.setScale(scale, scale);
            Log.d("setImage", "scale:" + scale + ",startImageX:" + startImageX + ",startImageY:" + startImageY + ",image_W:" + image_W + ",image_H:" + image_H + ",w:" + imageW + ".h:" + imageH);

            bitmap = Bitmap.createBitmap(bitmap, (int) startImageX, (int) startImageY, (int) image_W, (int) image_H, matrix, true);
        }
        canvas.drawBitmap(bitmap, 0, 0, paintMap.get("默认"));
    }

    //当前选中行矩形
    private void drawRowRect(Canvas canvas) {
        Paint paint = paintMap.get("选中行");
        canvas.drawRect(/*-viewX*/getScrollX(),
                cursorRowIndex * lineHeight + lineHeightDescent,
                getWidth() - /*viewX*/(-getScrollX()),
                (cursorRowIndex + 1) * lineHeight + lineHeightDescent,
                paint);
    }

    private void drawRowNumber(Canvas canvas) {
        //测量行号的宽
        Paint paint = paintMap.get("行号");
        String rowText = String.valueOf(textList.size() * 10);
        tranX = paint.measureText(rowText);
        for (int i = startRow + 1; i <= stopRow + 1; i++) {
            canvas.drawText(String.valueOf(i), 0, i * lineHeight, paint);
        }
    }

    private void drawSelectTextRect(Canvas canvas) {
        Paint paint = paintMap.get("行号");
        if (firstSelectRow == secondSelectRow) {
            char[] curTextChars = textList.get(firstSelectRow).toCharArray();
            float firstWidth = 0f;
            for (int i = 0; i < firstSelectCol + 1; i++) {
                firstWidth = firstWidth + paint.measureText(String.valueOf(curTextChars[i]));
            }
            float secondWidth = firstWidth;
            //记录第一个选中文本光标X轴位置
            firstSelectX = firstWidth;
            for (int i = firstSelectCol + 1; i < secondSelectCol + 1; i++) {
                secondWidth = secondWidth + paint.measureText(String.valueOf(curTextChars[i]));
            }
            //记录第二个选中文本光标X轴位置
            secondSelectX = secondWidth;
            //记录第一个和第二个选中文本光标Y轴位置
            float bottom = firstSelectY = secondSelectY = (firstSelectRow + 1) * lineHeight + lineHeightDescent;
            canvas.drawRect(firstWidth,
                    firstSelectRow * lineHeight + lineHeightDescent,
                    secondWidth,
                    bottom,
                    paint);
        } else {

            Log.d("drawSelectTextRect", "firstSelectRow:" + firstSelectRow + " secondSelectRow:" + secondSelectRow);

            char[] firstRowText = textList.get(firstSelectRow).toCharArray();
            char[] secondRowText = textList.get(secondSelectRow).toCharArray();
            float firstWidth = 0f;
            float secondWidth = 0f;
            for (int i = 0; i < firstSelectCol + 1; i++) {
                firstWidth = firstWidth + paint.measureText(String.valueOf(firstRowText[i]));
            }
            for (int i = 0; i < secondSelectCol + 1; i++) {
                secondWidth = secondWidth + paint.measureText(String.valueOf(secondRowText[i]));
            }

            for (int i = firstSelectRow; i <= secondSelectRow; i++) {
                if (i == firstSelectRow) {
                    firstSelectX = firstWidth;
                    firstSelectY = (firstSelectRow + 1) * lineHeight + lineHeightDescent;
                    //第一行
                    canvas.drawRect(firstWidth,
                            firstSelectRow * lineHeight + lineHeightDescent,
                            paint.measureText(textList.get(firstSelectRow)),
                            (firstSelectRow + 1) * lineHeight + lineHeightDescent,
                            paint);
                } else if (i == secondSelectRow) {
                    secondSelectX = secondWidth;
                    secondSelectY = (i + 1) * lineHeight + lineHeightDescent;
                    //最后一行
                    float left = tranX > -(-getScrollX()) ? 0 : -(-getScrollX()) - tranX;
                    canvas.drawRect(left,
                            i * lineHeight + lineHeightDescent,
                            secondWidth,
                            (i + 1) * lineHeight + lineHeightDescent,
                            paint);
                } else {
                    //其它行
                    float left = tranX > -(-getScrollX()) ? 0 : -(-getScrollX()) - tranX;
                    float w = paint.measureText(textList.get(i));
                    float r = getWidth() - (tranX + (-getScrollX()));
                    float right = w < r ? w : r;
                    canvas.drawRect(left,
                            i * lineHeight + lineHeightDescent,
                            right,
                            (i + 1) * lineHeight + lineHeightDescent,
                            paint);
                }
            }
        }
    }

    //画光标
    private void drawCursor(Canvas canvas) {
        Paint paint = paintMap.get("光标");
        if (cursorPositionInRowIndex == -1) {
            canvas.drawLine(0, cursorRowIndex * lineHeight + lineHeightDescent, 0, (cursorRowIndex + 1) * lineHeight + lineHeightDescent, paint);
        } else {
            String curText = textList.get(cursorRowIndex);
            char[] chars = curText.toCharArray();
            StringBuffer buffer = new StringBuffer();
            for (int i = 0; i < chars.length; i++) {
                buffer.append(chars[i]);
                if (i == cursorPositionInRowIndex) break;
            }
            float w = paint.measureText(buffer.toString());
            canvas.drawLine(w,
                    cursorRowIndex * lineHeight + lineHeightDescent,
                    w,
                    (cursorRowIndex + 1) * lineHeight + lineHeightDescent,
                    paint);
            //避免重复计算       算了，避免不了
            /*canvas.drawLine(cursorViewX,
                    cursorRowIndex * lineHeight + lineHeightDescent,
                    cursorViewX,
                    (cursorRowIndex + 1) * lineHeight + lineHeightDescent,
                    paint);*/
        }
    }

    //画字
    private void drawText(Canvas canvas) {
        Paint paint = paintMap.get("默认");
        Paint keyPaint = paintMap.get("关键字");
        Paint stringPaint = paintMap.get("字符串");
        Paint commentPaint = paintMap.get("注释");
        Paint classPaint = paintMap.get("类");
        Paint varPaint = paintMap.get("变量");
        Paint methodPaint = paintMap.get("方法");
        Paint numPaint = paintMap.get("数字");
        Paint operatorPaint = paintMap.get("运算符");

        for (int i = startRow; i <= stopRow; i++) {
            String text = textList.get(i);
            canvas.drawText(text, 0, (i + 1) * lineHeight, paint);
            //觉得量行宽太麻烦，直接每行字符数差不多得了
            if (i == startRow) {
                curMaxLine = text;
            } else if (curMaxLine.length() < text.length()) {
                curMaxLine = text;
            }
        }

        String line = null;
        for (int row = startRow; row <= stopRow; row++) {
            line = textList.get(row);
            int f_row = row;
            for (int col = 0; col < line.length(); col++) {
                char c = line.charAt(col);
                int f_col = col;
                //字符串处理
                if (c == '"') {
                    if (col < line.length() - 1) {
                        col++;
                        c = line.charAt(col);
                    }
                    while (c != '"' || (col > 0 && line.charAt(col - 1) == '\\')) {
                        col++;
                        if (col < line.length()) {
                            c = line.charAt(col);
                        } else {
                            break;
                        }
                    }
                    //画字符串
                    if (col < line.length()) {
                        String str = line.substring(f_col, col + 1);
                        float w = paint.measureText(line.substring(0, f_col));
                        canvas.drawText(str, w, (row + 1) * lineHeight, stringPaint);
                        //System.out.println("字符串 " + f_col + "," + (col + 1) + " " + str);
                    } else {
                        String str = line.substring(f_col, col);
                        float w = paint.measureText(line.substring(0, f_col));
                        canvas.drawText(str, w, (row + 1) * lineHeight, stringPaint);
                        //System.out.println("字符串 " + f_col + "," + col + " " + str);
                    }
                } else if (c == '\'') {
                    if (col < line.length() - 1) {
                        col++;
                        c = line.charAt(col);
                    }
                    while (c != '\'' || (col > 0 && line.charAt(col - 1) == '\\')) {
                        col++;
                        if (col < line.length()) {
                            c = line.charAt(col);
                        } else {
                            break;
                        }
                    }
                    //画字符串
                    if (col < line.length()) {
                        String str = line.substring(f_col, col + 1);
                        float w = paint.measureText(line.substring(0, f_col));
                        canvas.drawText(str, w, (row + 1) * lineHeight, stringPaint);
                        //System.out.println("字符串 " + f_col + "," + (col + 1) + " " + str);
                    } else {
                        String str = line.substring(f_col, col);
                        float w = paint.measureText(line.substring(0, f_col));
                        canvas.drawText(str, w, (row + 1) * lineHeight, stringPaint);
                        //System.out.println("字符串 " + f_col + "," + col + " " + str);
                    }
                }
                //注释处理
                else if (c == '/') {
                    //是否为行末，是则退出循环
                    if (col < line.length() - 1) {
                        col++;
                        c = line.charAt(col);
                    } else {
                        break;
                    }
                    //单行注释
                    if (c == '/') {
                        String str = line.substring(col - 1);
                        float w = paint.measureText(line.substring(0, col - 1));
                        canvas.drawText(str, w, (row + 1) * lineHeight, commentPaint);
                        //System.out.println("单行注释 " + (col - 1) + "," + line.length() + " " + str);
                        break;
                    }
                    //多行注释
                    else if (c == '*') {
                        if (col < line.length() - 1) {
                            col++;
                            c = line.charAt(col);
                        } else {
                            row++;
                            col = 0;
                        }
                        boolean isBreak = false;
                        while (row <= stopRow) {
                            if (col == 0) line = textList.get(row);
                            while (col < line.length()) {
                                c = line.charAt(col);
                                if (col < line.length() - 1 && c == '*' && line.charAt(col + 1) == '/') {
                                    col = col + 1;
                                    isBreak = true;
                                    break;
                                }
                                col++;
                            }
                            if (isBreak) break;
                            row++;
                            col = 0;
                        }
                        //多行注释位置
                        //System.out.println("多行注释 " + f_row + "," + f_col + " " + row + "," + col + " ");
                        int ind = (row < stopRow) ? row : stopRow;


                        //记录多行注释位置，只是一行不记录

                        if (f_row == ind) {
                            if (line.length() != 0) {
                                if (f_col < col + 1 && f_col >= 0 && col + 1 <= line.length()) {
                                    String str = line.substring(f_col, col + 1);
                                    float w = paint.measureText(line.substring(0, f_col));
                                    canvas.drawText(str, w, (row + 1) * lineHeight, commentPaint);
                                    //System.out.println(str);
                                }
                            }
                        } /*else {
                            //[开始和结束表示0为开始1为结束， 列位置]
                            javaWordUtil.setCommentPos(f_row, new int[]{0, f_col});
                            if (!(ind == stopRow && col == 0)) {
                                javaWordUtil.setCommentPos(ind,new int[]{1,col});
                                //col=line.length()-1;
                            }
                        }*/
                        //只记录位置，不画
                        else {
                            for (int in = f_row; in <= ind; in++) {
                                line = textList.get(in);
                                if (in == f_row) {
                                    if (line.length() != 0) {
                                        String str = line.substring(f_col, line.length());
                                        float w = paint.measureText(line.substring(0, f_col));
                                        canvas.drawText(str, w, (in + 1) * lineHeight, commentPaint);
                                        //System.out.println("-" + str);
                                    }
                                } else if (in == ind) {
                                    if (line.length() != 0) {
                                        if (in == stopRow && col == 0) {
                                            col = line.length() - 1;
                                        }
                                        String str = line.substring(0, col + 1);
                                        canvas.drawText(str, 0, (in + 1) * lineHeight, commentPaint);
                                        //System.out.println("-" + str);
                                    }
                                } else {
                                    canvas.drawText(line, 0, (in + 1) * lineHeight, commentPaint);
                                    //System.out.println("-" + line);
                                }
                            }
                        }
                    }
                }
                //关键字处理
                else if (!isJavaChar(c)) {
                    if (col < line.length() - 1) {
                        col++;
                        c = line.charAt(col);
                    } else {
                        break;
                    }
                    while (!isJavaChar(c)) {
                        if (col < line.length() - 1) {
                            col++;
                            c = line.charAt(col);
                        } else {
                            col++;
                            break;
                        }
                    }
                    String key = line.substring(f_col, col);
                    col--;//col--后，此时col对应key的最后一个字符
                    if (JavaWordUtil.isKey(key)) {
                        float w = paint.measureText(line.substring(0, f_col));
                        canvas.drawText(key, w, (row + 1) * lineHeight, keyPaint);
                        //System.out.println("关键字 " + f_col + "," + (col + 1) + " " + key);
                    } else if (key.startsWith("@")) {//注解
                        float w = paint.measureText(line.substring(0, f_col));
                        canvas.drawText(key, w, (row + 1) * lineHeight, classPaint);
                        //System.out.println("注解 " + f_col + "," + (col + 1) + " " + key);
                    }/* else if (parseUtil.isClassName(key) || JavaWordUtil.isConstructor(f_col, col + 1, line)) {//类名
                        float w = paint.measureText(line.substring(0, f_col));
                        canvas.drawText(key, w, (row + 1) * lineHeight, classPaint);
                        //System.out.println("类 " + f_col + "," + (col + 1) + " " + key);
                    } else if (JavaWordUtil.isNum(key)) {//数字
                        float w = paint.measureText(line.substring(0, f_col));
                        canvas.drawText(key, w, (row + 1) * lineHeight, numPaint);
                    } else if (parseUtil.isGlobalVariable(key)) {//全局变量
                        float w = paint.measureText(line.substring(0, f_col));
                        canvas.drawText(key, w, (row + 1) * lineHeight, varPaint);
                    } else if (JavaWordUtil.isMethod(col + 1, line)) {//方法
                        float w = paint.measureText(line.substring(0, f_col));
                        canvas.drawText(key, w, (row + 1) * lineHeight, methodPaint);
                    }*/
                }
                //运算符
                else if (JavaWordUtil.isOperator(c)) {
                    float w = f_row > 0 ? paint.measureText(line.substring(0, f_col)) : 0;
                    canvas.drawText(String.valueOf(c), w, (row + 1) * lineHeight, operatorPaint);
                }
            }
        }

        //画多行注释
    }

    //画选中文本光标
    private void drawSelectCursor(Canvas canvas) {
        Paint paint = paintMap.get("光标");
        //第一个选中文本光标
        Path firstPath = new Path();
        RectF firstRectF = new RectF(firstSelectX - 25, firstSelectY + 25, firstSelectX + 25, firstSelectY + 75);
        firstPath.addArc(firstRectF, -30, 240);
        firstPath.lineTo(firstSelectX, firstSelectY);
        firstPath.close();
        canvas.drawPath(firstPath, paint);
        //第二个选中文本光标
        Path secondPath = new Path();
        RectF secondRectF = new RectF(secondSelectX - 25, secondSelectY + 25, secondSelectX + 25, secondSelectY + 75);
        secondPath.addArc(secondRectF, -30, 240);
        secondPath.lineTo(secondSelectX, secondSelectY);
        secondPath.close();
        canvas.drawPath(secondPath, paint);
    }
}
