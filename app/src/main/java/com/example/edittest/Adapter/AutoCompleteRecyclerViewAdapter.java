package com.example.edittest.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.edittest.Model.AutoTextModel;
import com.example.edittest.R;

import java.util.List;

public class AutoCompleteRecyclerViewAdapter extends RecyclerView.Adapter<AutoCompleteRecyclerViewAdapter.ViewHolder> {

    private List<AutoTextModel> list;

    public AutoCompleteRecyclerViewAdapter(List<AutoTextModel> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.autocomplete_recyclerview_item, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);
        viewHolder.autoCompleteText.setOnClickListener(v -> {
            int position = viewHolder.getAdapterPosition();
            AutoTextModel model = list.get(position);
            Toast.makeText(parent.getContext(), model.type + ":" + model.text, Toast.LENGTH_SHORT).show();
        });
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AutoTextModel model = list.get(position);
        holder.autoCompleteType.setText(model.type);
        holder.autoCompleteText.setText(model.text);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private TextView autoCompleteText;
        private TextView autoCompleteType;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            autoCompleteText = itemView.findViewById(R.id.autoCompleteText);
            autoCompleteType = itemView.findViewById(R.id.autoCompleteType);
        }
    }
}
