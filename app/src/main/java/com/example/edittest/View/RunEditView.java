package com.example.edittest.View;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
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

import androidx.annotation.Nullable;

import com.example.edittest.Interface.RunJavaConsoleInterface;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class RunEditView extends View implements RunJavaConsoleInterface {

    //输入
    private String inputString = "";
    //文本
    private volatile List<String> textList = new ArrayList<>();
    //光标在每一行中的位置
    private int cursorPositionInRowIndex = -1;
    //光标闪烁时间，毫秒
    private int cursorFlashTime = 500;
    //光标闪烁标识
    private boolean isFlash = true;
    //视图X轴
    private int viewX = 0;
    //视图Y轴
    private int viewY = 0;
    //文本偏移量
    private float tranX = 100;
    //画笔集合
    private Map<String, Paint> paintMap = new HashMap<>();
    //文本大小
    private int textSize = /*50*/30;
    //行高
    private int lineHeight;
    //考虑行高的基线
    private int lineHeightDescent;
    //开始行，结束行
    private int startRow, stopRow;
    //当前显示的最长行
    private String curMaxLine = "";
    //手势监听
    private GDListener gdl;
    private GestureDetector gestureDetector;

    private IC ic;

    public RunEditView(Context context) {
        super(context);
    }

    public RunEditView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        //加载画笔
        initPaint();
        //计算行高
        computeLineHeight();
        //光标闪烁
        cursorFlash();
        //手势监听
        gdl = new GDListener();
        gestureDetector = new GestureDetector(context, gdl);
        //输入法状态监听
        this.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                //确定是否上移
                int softHeight = getSoftKeyboardHeight();
                int heig = lineHeight * ((textList.size() - 1) + 1);
                int heig_low = heig + viewY + lineHeightDescent;
                int heig_d = getHeight() - softHeight;
                if (heig_low > heig_d) {
                    viewY = viewY - (heig_low - heig_d);
                    invalidate();
                }
            }
        });
        //焦点
        setFocusable(true);
        setFocusableInTouchMode(true);
        //防止越界
        textList.add("");
    }

    private void initPaint() {
        paintMap.put("默认", paintBuild(Color.BLACK));//默认画笔
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
                postInvalidate();
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

    //计算输入法高度
    private int getSoftKeyboardHeight() {
        int screenHeight = ((Activity) getContext()).getWindow().getDecorView().getRootView().getHeight();
        Rect rect = new Rect();
        ((Activity) getContext()).getWindow().getDecorView().getWindowVisibleDisplayFrame(rect);
        return screenHeight - rect.bottom;
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

    //输入法
    private class IC extends BaseInputConnection {

        public IC(View targetView, boolean fullEditor) {
            super(targetView, fullEditor);
        }

        @Override
        public boolean commitText(CharSequence text, int newCursorPosition) {
            inputString = inputString + text;
            cursorPositionInRowIndex = textList.get(textList.size() - 1).length() + inputString.length() - 1;
            invalidate();
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

                        enter();

                        textList.set(textList.size() - 1, textList.get(textList.size() - 1) + inputString);
                        inputString = "";
                        textList.add("");
                        cursorPositionInRowIndex = -1;

                        //防止超过键盘，确定是否上移
                        int softHeight = getSoftKeyboardHeight();
                        int heig = lineHeight * (textList.size() - 1 + 1);
                        int heig_low = heig + viewY + lineHeightDescent;
                        int heig_d = getHeight() - softHeight;
                        if (heig_low > heig_d) {
                            viewY = viewY - (heig_low - heig_d);
                        }
                        break;
                }
                invalidate();
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
            if (!TextUtils.isEmpty(inputString)) {
                inputString = inputString.substring(0, inputString.length() - 1);
                invalidate();
            }
        }
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

        @Override
        public boolean onDown(MotionEvent e) {
            isFirstMove = false;
            if (onflingValueAnimator != null && onflingValueAnimator.isRunning()) {
                onflingValueAnimator.cancel();
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
            return true;
        }

        @Override//e1是第一次触摸，e2是当前
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {

            //滑动视图
            if (!isFirstMove) {
                isDirection = Math.abs(distanceY) > Math.abs(distanceX) ? true : false;
                isFirstMove = true;
            }
            if (isDirection) viewY -= distanceY;
            else viewX -= distanceX;

            if (textList.size() * lineHeight - 100 < -viewY)
                viewY = -(textList.size() * lineHeight - 100);
            Paint paint = paintMap.get("默认");
            float maxLineWidth = paint.measureText(curMaxLine);
            if (maxLineWidth - tranX - 100 < -viewX)
                viewX = -(int) (maxLineWidth - tranX - 100);

            if (viewX >= 0) viewX = 0;
            if (viewY >= 0) viewY = 0;

            invalidate();
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {

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
                        viewY = viewY + value;
                    } else {
                        viewX = viewX + value;
                    }
                    //判断是否超出范围
                    if (textList.size() * lineHeight - 100 < -viewY) {
                        viewY = -(textList.size() * lineHeight - 100);
                        //onflingValueAnimator.cancel();
                    }
                    Paint paint = paintMap.get("默认");
                    float maxLineWidth = paint.measureText(curMaxLine);
                    if (maxLineWidth - tranX - 100 < -viewX) {
                        viewX = -(int) (maxLineWidth - tranX - 100);
                        //onflingValueAnimator.cancel();
                    }
                    if (viewX >= 0) {
                        viewX = 0;
                        //onflingValueAnimator.cancel();
                    }

                    if (viewY >= 0) {
                        viewY = 0;
                        //onflingValueAnimator.cancel();
                    }

                    invalidate();
                }
            });
            onflingValueAnimator.start();
            return true;
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        Log.d("onSizeChanged", "x:" + viewX + ",y:" + viewY);
    }

    @Override
    protected void onDraw(Canvas canvas) {

        if (textList.size() == 0) return;

        //计算开始和结束行
        computeStartAndStop();
        //偏移到指定位置
        canvas.translate(viewX, viewY);
        //画当前选中行矩形
        //drawRowRect(canvas);
        //画行号
        drawRowNumber(canvas);
        //先画行号再偏移文本
        canvas.translate(tranX, 0);
        //画文本
        drawText(canvas);
        //光标闪烁
        if (isFlash) drawCursor(canvas);
    }

    //计算显示的行号
    private void computeStartAndStop() {
        startRow = Math.abs(viewY + lineHeightDescent) / lineHeight;
        int maxLine = textList.size() - 1;
        if (startRow > maxLine) return;
        int stopIndex = (getHeight() / lineHeight) + 1;
        if ((maxLine - startRow) > stopIndex) {
            stopRow = startRow + stopIndex;
        } else {
            stopRow = maxLine;
        }
    }

    private void drawRowNumber(Canvas canvas) {
        //测量行号的宽
        Paint paint = paintMap.get("默认");
        String rowText = String.valueOf(textList.size() * 10);
        tranX = paint.measureText(rowText);
        for (int i = startRow + 1; i <= stopRow + 1; i++) {
            canvas.drawText(String.valueOf(i), 0, i * lineHeight, paint);
        }
    }

    //画光标
    private void drawCursor(Canvas canvas) {
        Paint paint = paintMap.get("默认");
        String curText = textList.get((textList.size() - 1)) + inputString;
        float w = paint.measureText(curText);
        canvas.drawLine(w,
                (textList.size() - 1) * lineHeight + lineHeightDescent,
                w,
                ((textList.size() - 1) + 1) * lineHeight + lineHeightDescent,
                paint);
    }

    //画字
    private void drawText(Canvas canvas) {
        Paint paint = paintMap.get("默认");

        for (int i = startRow; i <= stopRow; i++) {
            String text = textList.get(i);
            if (i == textList.size() - 1) {
                text += inputString;
            }
            canvas.drawText(text, 0, (i + 1) * lineHeight, paint);
            //觉得量行宽太麻烦，直接每行字符数差不多得了
            if (i == startRow) {
                curMaxLine = text;
            } else if (curMaxLine.length() < text.length()) {
                curMaxLine = text;
            }
        }
    }

    private synchronized void inputTextToList(String text) {
        if (TextUtils.isEmpty(text)) return;
        int cursorRowIndex = textList.size() - 1;
        String curText = textList.get(cursorRowIndex);
        if (!text.contains("\n")) {
            textList.set(cursorRowIndex, curText + text);
            cursorPositionInRowIndex = cursorPositionInRowIndex + text.length();
        } else {
            text = text.replace("\n", "" + '\n' + "");
            String[] strings = text.split("\n");
            boolean isF = true;
            for (String s : strings) {
                if (isF) {
                    textList.set(cursorRowIndex, curText + s);
                    isF = false;
                } else {
                    textList.add(s);
                }
            }
            cursorPositionInRowIndex = textList.get(textList.size() - 1).length() - 1;
        }
    }

    private synchronized void inputTextToList(byte[] bytes) {
        if (bytes == null) return;

        ByteArrayInputStream byteArrayInputStream = null;
        InputStreamReader inputStreamReader = null;
        BufferedReader bufferedReader = null;
        try {
            byteArrayInputStream = new ByteArrayInputStream(bytes);
            inputStreamReader = new InputStreamReader(byteArrayInputStream);
            bufferedReader = new BufferedReader(inputStreamReader);

            int cursorRowIndex = textList.size() - 1;
            String curText = textList.get(cursorRowIndex);

            boolean isF = true;
            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                line = line.replace("\t", "    ");
                if (isF) {
                    textList.set(cursorRowIndex, curText + line);
                    isF = false;
                } else {
                    textList.add(line);
                }
            }

            if (bytes[bytes.length - 1] == '\n') textList.add("");

            cursorPositionInRowIndex = textList.get(textList.size() - 1).length() - 1;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (byteArrayInputStream != null) byteArrayInputStream.close();
                if (inputStreamReader != null) inputStreamReader.close();
                if (bufferedReader != null) bufferedReader.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    @Override
    public void sendText(String input) {
        ((MyIn) getIn()).addData(input.getBytes());
    }

    @Override
    public void printOut(String out) {
        inputTextToList(out);
        postInvalidate();
    }

    @Override
    public void printErr(String err) {
        inputTextToList(err);
        postInvalidate();
    }

    @Override
    public InputStream getIn() {
        if (in == null) in = new MyIn();
        return in;
    }

    @Override
    public OutputStream getOut() {
        if (out == null) out = new MyOut();
        return out;
    }

    @Override
    public OutputStream getErr() {
        if (err == null) err = new MyErr();
        return err;
    }

    private void enter() {
        sendText(inputString + "\n");
    }

    private MyIn in;

    private class MyIn extends InputStream {

        private byte[] data = null;
        private int pos;
        private int count;

        @Override
        public int read() throws IOException {
            return 0;
        }

        @Override
        public int read(byte[] b) throws IOException {
            return read(b, 0, b.length);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {

            if (b == null) {
                throw new NullPointerException();
            } else if (off < 0 || len < 0 || len > b.length - off) {
                throw new IndexOutOfBoundsException();
            }

            synchronized (MyIn.this) {
                while (data == null) {
                    try {
                        this.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                if (pos >= count) {
                    return -1;
                }

                int avail = count - pos;
                if (len > avail) {
                    len = avail;
                }
                if (len <= 0) {
                    return 0;
                }
                System.arraycopy(data, pos, b, off, len);
                pos += len;

                if (pos >= count) {
                    data = null;
                }
            }
            return len;
        }

        public synchronized void addData(byte[] bytes) {
            this.data = bytes;
            this.pos = 0;
            this.count = bytes.length;
            this.notifyAll();
        }
    }

    private MyOut out;

    private class MyOut extends OutputStream {
        private byte[] bytes = new byte[1];
        private int index = 0;

        @Override
        public void write(int b) throws IOException {
            bytes[index] = (byte) b;
            if ('\n' == b) {
                printString(new String(bytes));
                bytes = new byte[1];
                index = 0;
            } else {
                index++;
                bytes = Arrays.copyOf(bytes, index + 1);
            }
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            if (b == null) {
                throw new NullPointerException();
            } else if ((off < 0) || (off > b.length) || (len < 0) ||
                    ((off + len) > b.length) || ((off + len) < 0)) {
                throw new IndexOutOfBoundsException();
            } else if (len == 0) {
                return;
            }
            /*for (int i = 0 ; i < len ; i++) {
                write(b[off + i]);
            }*/
            byte[] bytes = new byte[len];
            System.arraycopy(b, off, bytes, 0, len);
            printString(bytes);
        }

        public void printString(String s) {
            printOut(s);
        }

        public void printString(byte[] bytes) {
            inputTextToList(bytes);
        }
    }

    private MyErr err;

    private class MyErr extends OutputStream {
        private byte[] bytes = new byte[1];
        private int index = 0;

        @Override
        public void write(int b) throws IOException {
            bytes[index] = (byte) b;
            if ('\n' == b) {
                printString(new String(bytes));
                bytes = new byte[1];
                index = 0;
            } else {
                index++;
                bytes = Arrays.copyOf(bytes, index + 1);
            }
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            if (b == null) {
                throw new NullPointerException();
            } else if ((off < 0) || (off > b.length) || (len < 0) ||
                    ((off + len) > b.length) || ((off + len) < 0)) {
                throw new IndexOutOfBoundsException();
            } else if (len == 0) {
                return;
            }
            /*for (int i = 0 ; i < len ; i++) {
                write(b[off + i]);
            }*/
            byte[] bytes = new byte[len];
            System.arraycopy(b, off, bytes, 0, len);
            printString(bytes);
        }

        public void printString(String s) {
            printErr(s);
        }

        public void printString(byte[] bytes) {
            Log.d("Err", new String(bytes));
            inputTextToList(bytes);
        }
    }
}
