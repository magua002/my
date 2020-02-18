package com.example.edittest;

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
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class CodeEdit extends View {
    //文本
    private List<String> textList = new ArrayList<String>();
    //光标行
    private int cursorRowIndex = 0;
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
    private Map<String, Paint> paintMap = new HashMap<String, Paint>();
    //文本大小
    private int textSize = 50;
    //行高
    private int lineHeight;
    //考虑行高的基线
    private int lineHeightDescent;
    //开始行，结束行
    private int startRow, stopRow;
    //手势监听
    private GDListener gdl;
    private GestureDetector gestureDetector;
    //背景图片
    Bitmap bitmap;

    private IC ic;

    public CodeEdit(Context context) {
        super(context);
    }

    public CodeEdit(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initPaint();
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
                int heig = lineHeight * (cursorRowIndex + 1);
                int heig_low = heig + viewY + lineHeightDescent;
                int heig_d = getHeight() - softHeight;
                if (heig_low > heig_d) {
                    viewY = viewY - (heig_low - heig_d);
                    postInvalidate();
                }
            }
        });
        setFocusable(true);
        setFocusableInTouchMode(true);
        //防止越界
        textList.add("");

    }

    private void initPaint() {
        paintMap.put("默认", paintBuild(Color.BLACK));//默认画笔
        paintMap.put("光标", paintBuild(Color.parseColor("#00574B")));
        paintMap.put("选中行", paintBuild(Color.argb(200, 230, 230, 230)));
        paintMap.put("行号", paintBuild(Color.BLUE));
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
            if (cursorPositionInRowIndex == -1) {
                Log.d("提交", "第一情况");
                textList.set(cursorRowIndex, String.valueOf(text));
            } else {
                Log.d("提交", "第二情况");
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
            Log.d("commitText", "cursorPositionInRowIndex->" + cursorPositionInRowIndex);
            postInvalidate();
            return true;
        }

        @Override
        public boolean sendKeyEvent(KeyEvent event) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                switch (event.getKeyCode()) {
                    //删除键
                    case KeyEvent.KEYCODE_DEL:
                        Log.d("sendKeyEvent", "删除键");
                        deleteText();
                        break;
                    //回车键
                    case KeyEvent.KEYCODE_ENTER:
                        //防止处理字符串时越界
                        String curText = textList.get(cursorRowIndex);
                        if (cursorPositionInRowIndex > -1 && cursorPositionInRowIndex < curText.length() - 1) {
                            Log.d("回车键", "第一情况");
                            StringBuffer newTextBuffer = new StringBuffer();
                            StringBuffer lastTextBuffer = new StringBuffer();
                            char[] chars = curText.toCharArray();
                            for (int i = 0; i < chars.length; i++) {
                                if (i <= cursorPositionInRowIndex) {
                                    lastTextBuffer.append(chars[i]);
                                } else {
                                    newTextBuffer.append(chars[i]);
                                }
                            }
                            textList.set(cursorRowIndex, lastTextBuffer.toString());
                            cursorRowIndex++;
                            textList.add(cursorRowIndex, newTextBuffer.toString());
                            cursorPositionInRowIndex = -1;
                        } else if (cursorPositionInRowIndex == -1) {
                            Log.d("回车键", "第二情况");
                            textList.set(cursorRowIndex++, "");
                            textList.add(cursorRowIndex, curText);
                        } else if (cursorPositionInRowIndex == curText.length() - 1) {
                            Log.d("回车键", "第三情况");
                            textList.add(++cursorRowIndex, "");
                            cursorPositionInRowIndex = -1;
                        }
                        //防止超过键盘，确定是否上移
                        int softHeight = getSoftKeyboardHeight();
                        int heig = lineHeight * (cursorRowIndex + 1);
                        int heig_low = heig + viewY + lineHeightDescent;
                        int heig_d = getHeight() - softHeight;
                        if (heig_low > heig_d) {
                            viewY = viewY - (heig_low - heig_d);
                        }
                        break;
                }
                postInvalidate();
            }
            return true;
        }

        @Override
        public boolean deleteSurroundingText(int beforeLength, int afterLength) {
            deleteText();
            postInvalidate();
            return true;
        }

        //自己加的一个处理删除的方法
        private void deleteText() {
            String curText = textList.get(cursorRowIndex);
            if (cursorRowIndex == 0 && cursorPositionInRowIndex == -1) {
                Log.d("删除", "第一情况");
                return;//如果光标在第一行且在最前面，不处理
            } else {
                if (cursorPositionInRowIndex == -1) {
                    Log.d("删除", "第二情况");
                    textList.remove(cursorRowIndex);
                    String lastText = textList.get(--cursorRowIndex);
                    cursorPositionInRowIndex = lastText.length() - 1;
                    lastText += curText;
                    textList.set(cursorRowIndex, lastText);
                } else if (cursorPositionInRowIndex >= 0) {
                    Log.d("删除", "第三情况");
                    StringBuffer stringBuffer = new StringBuffer();
                    char[] chars = curText.toCharArray();
                    for (int i = 0; i < chars.length; i++) {
                        if (i != cursorPositionInRowIndex) {
                            stringBuffer.append(chars[i]);
                        }
                    }
                    textList.set(cursorRowIndex, stringBuffer.toString());
                    cursorPositionInRowIndex--;
                }
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event);
    }

    //手势监听
    private class GDListener implements GestureDetector.OnGestureListener {

        //是否纵向移动
        private boolean isDirection;
        //是否判断过
        private boolean isFirstMove;

        @Override
        public boolean onDown(MotionEvent e) {
            isFirstMove = false;
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
            } else {
            }
            float x = e.getX();
            float y = e.getY();
            //确定选中的是哪一行
            int index = (int) ((Math.abs(viewY) + y - lineHeightDescent) / lineHeight);
            int maxLine = textList.size() - 1;
            cursorRowIndex = index > maxLine ? maxLine : index;
            //确定选中的是哪一个字
            String curText = textList.get(cursorRowIndex);
            if (TextUtils.isEmpty(curText)) {
                cursorPositionInRowIndex = -1;
            } else {
                Paint paint = paintMap.get("默认");
                float w = paint.measureText(curText);
                float realX = Math.abs(viewX) + x - tranX;
                if (realX > w) {
                    cursorPositionInRowIndex = curText.length() - 1;
                } else {
                    cursorPositionInRowIndex = getCursorPositionInRowIndex(curText, realX, paint) + 1;
                }
            }
            postInvalidate();
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (!isFirstMove) {
                isDirection = Math.abs(distanceY) > Math.abs(distanceX) ? true : false;
                isFirstMove = true;
            }
            if (isDirection) viewY -= distanceY;
            else viewX -= distanceX;

            if (viewX >= 0) viewX = 0;
            if (viewY >= 0) viewY = 0;

            invalidate();
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("操作");
            builder.setView(R.layout.long_press);
            builder.show();
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return true;

        }

        //计算光标处于哪一个位置
        private int getCursorPositionInRowIndex(String curText, float realX, Paint paint) {
            if (paint.measureText(String.valueOf(curText.charAt(0))) > realX) {
                return -1;
            }
            for (int i = 2; i <= curText.length(); i++) {
                if (paint.measureText(curText.substring(0, i)) > realX) {
                    return i - 2;
                }
            }
            return -1;
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        Log.d("onSizeChanged","x:"+viewX+",y:"+viewY);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        //背景图
        drawBackgroundImage(canvas);
        //在背景图上铺一层半透明，不然看不清，本来想直接在图片上弄的，但是不会
        canvas.drawColor(Color.argb(100, 255, 255, 255));
        //计算开始和结束行
        computeStartAndStop();
        //偏移到指定位置
        canvas.translate(viewX, viewY);
        //画当前选中行矩形
        drawRowRect(canvas);
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
                scale=imageH<getHeight()?getHeight() / imageH:imageH/getHeight();
                float i_w = imageW * scale;
                startImageX = Math.abs(i_w - getWidth()) / 2 / scale;
                image_W=imageW-startImageX*2;
            } else {
                scale=imageW<getWidth()?getWidth()/imageW:imageW/getWidth();
                float i_h = imageH * scale;
                startImageY = Math.abs(i_h - getHeight()) / 2 / scale;
                image_H=imageH-startImageY*2;
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
        canvas.drawRect(-viewX, cursorRowIndex * lineHeight + lineHeightDescent, getWidth() - viewX, (cursorRowIndex + 1) * lineHeight + lineHeightDescent, paint);
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
            canvas.drawLine(w, cursorRowIndex * lineHeight + lineHeightDescent, w, (cursorRowIndex + 1) * lineHeight + lineHeightDescent, paint);
        }
    }

    //画字
    private void drawText(Canvas canvas) {
        Paint paint = paintMap.get("默认");
        for (int i = startRow; i <= stopRow; i++) {
            String text = textList.get(i);
            canvas.drawText(text, 0, (i + 1) * lineHeight, paint);
        }
    }

    //粘贴
    private void paste() {
        ClipboardManager cm = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData data = cm.getPrimaryClip();
        ClipData.Item item = data.getItemAt(0);
        String text = item.getText().toString();
    }
}
