package com.example.simplechatroom;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.server.ChatBean;

import java.util.ArrayList;

public class ChatAdapter extends RecyclerView.Adapter {
    private Context context;
    private ArrayList<ChatBean> data;
    private static final int TYPEONE = 1;//左
    private static final int TYPETWO = 2;//右

    public ChatAdapter(Context context) {
        this.context = context;
    }
    public void setData(ArrayList<ChatBean> data){
        this.data = data;
        notifyDataSetChanged();
    }
    @Override
    public int getItemViewType(int position) {
        return data.get(position).getType();
    }

    @Override
    public int getItemCount() {
        return  data != null && data.size() > 0 ? data.size() : 0;
    }

    class OneViewHolder extends RecyclerView.ViewHolder {
        private TextView tv1, name1, time1;

        public OneViewHolder(View view) {
            super(view);
            tv1 = view.findViewById(R.id.tv);
            name1 = view.findViewById(R.id.tv_name);
            time1 = view.findViewById(R.id.tv_time);
        }
    }

    class TwoViewHolder extends RecyclerView.ViewHolder {
        private TextView tv2, name2, time2;

        public TwoViewHolder(View view) {
            super(view);
            tv2 = view.findViewById(R.id.tv2);
            name2 = view.findViewById(R.id.tv_name2);
            time2 = view.findViewById(R.id.tv_time2);
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder viewHolder = null;
        switch (viewType) {
            case TYPEONE:
                View view = LayoutInflater.from(context).inflate(R.layout.left, parent, false);
                viewHolder = new OneViewHolder(view);
                break;
            case TYPETWO:
                View view2 = LayoutInflater.from(context).inflate(R.layout.right,parent,false);
                viewHolder = new TwoViewHolder(view2);
                break;
        }
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        int itemViewType = getItemViewType(position);
        switch (itemViewType){
            case TYPEONE:
                OneViewHolder oneViewHolder = (OneViewHolder)holder;
                oneViewHolder.tv1.setText(data.get(position).getContent());
                oneViewHolder.name1.setText(data.get(position).getName());
                oneViewHolder.time1.setText(data.get(position).getTime());
                break;
            case TYPETWO:
                TwoViewHolder twoViewHolder = (TwoViewHolder)holder;
                twoViewHolder.tv2.setText(data.get(position).getContent());
                twoViewHolder.name2.setText(data.get(position).getName());
                twoViewHolder.time2.setText(data.get(position).getTime());
                break;
        }
    }
}
