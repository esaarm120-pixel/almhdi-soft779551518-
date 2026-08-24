package com.silent.telebot;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AppAdapter extends RecyclerView.Adapter<AppAdapter.ViewHolder> {
    private Context context;
    private List<AppItem> appList;

    public AppAdapter(Context context, List<AppItem> appList) {
        this.context = context;
        this.appList = appList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_app, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppItem item = appList.get(position);
        holder.icon.setImageDrawable(item.icon);
        holder.name.setText(item.name);
        holder.switchBlocked.setChecked(item.blocked);

        holder.switchBlocked.setOnCheckedChangeListener((buttonView, isChecked) -> {
            item.blocked = isChecked;
            // حفظ الحالة في SharedPreferences
            saveBlockedState(item.packageName, isChecked);
        });
    }

    private void saveBlockedState(String packageName, boolean blocked) {
        // سيتم تنفيذه لاحقاً
    }

    @Override
    public int getItemCount() {
        return appList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView name;
        Switch switchBlocked;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.app_icon);
            name = itemView.findViewById(R.id.app_name);
            switchBlocked = itemView.findViewById(R.id.switch_block);
        }
    }
          }
