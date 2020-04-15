package com.example.edittest.View;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.edittest.Adapter.AutoCompleteRecyclerViewAdapter;
import com.example.edittest.Model.AutoTextModel;
import com.example.edittest.R;
import com.example.edittest.Util.JavaWordUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class AutoCompletePopupWindow {
    private View editView;
    private int width = 300;
    private int height = 300;
    private List<AutoTextModel> list = new ArrayList<>();
    private RecyclerView recyclerView;
    private AutoCompleteRecyclerViewAdapter adapter;
    private PopupWindow popupWindow;
    private LinearLayout linearLayout;
    private String inputString = "";

    public AutoCompletePopupWindow(View editView) {
        this.editView = editView;
        initList();
        initPopupWindow();
        initRecyclerView();
    }

    public void setWh(int width, int height) {
        this.width = width;
        this.height = height;
        popupWindow.setWidth(width);
        popupWindow.setHeight(height);
    }

    private void initRecyclerView() {
        recyclerView = ((View) linearLayout).findViewById(R.id.autoCompleteRecycle);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(editView.getContext(), LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(linearLayoutManager);
        adapter = new AutoCompleteRecyclerViewAdapter(list);
        recyclerView.setAdapter(adapter);
    }

    private void initPopupWindow() {
        linearLayout = (LinearLayout) LayoutInflater.from(editView.getContext()).inflate(R.layout.autocomplete_recyclerview, null);
        popupWindow = new PopupWindow(linearLayout, 1000, height);
    }

    public void initList() {
        for (String s : JavaWordUtil.getJavaKeyWords()) {
            list.add(new AutoTextModel(s, "关键字"));
        }
    }

    public void show(int offX, int offY) {
        //popupWindow.showAsDropDown(editView, 0, 0);
    }

    public void dismiss() {
        /*popupWindow.dismiss();
        list.clear();
        cleanInput();*/
    }

    public boolean isShow() {
        return popupWindow.isShowing();
    }

    public void input(String input) {
        inputString = inputString + input;
        if (list.size() == 0) initList();
        if (!isShow()) show(0, 0);
        changeData();
    }

    public void delete() {
        if (!TextUtils.isEmpty(inputString)) {
            inputString = inputString.substring(0, inputString.length() - 1);
            if (!TextUtils.isEmpty(inputString)) {
                list.clear();
                initList();
                changeData();
            } else {
                dismiss();
            }
        }
    }

    public void cleanInput() {
        inputString = "";
    }

    public void changeData() {
        Iterator<AutoTextModel> iterator = list.iterator();
        while (iterator.hasNext()) {
            AutoTextModel model = iterator.next();
            if (!model.text.startsWith(inputString)) {
                iterator.remove();
            }
        }
        adapter.notifyDataSetChanged();
        if (list.size() == 0) {
            dismiss();
        }
    }
}
