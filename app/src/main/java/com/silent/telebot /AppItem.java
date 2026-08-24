package com.silent.telebot;

import android.graphics.drawable.Drawable;

public class AppItem {
    public String name;
    public String packageName;
    public boolean blocked;
    public Drawable icon;

    public AppItem(String name, String packageName, boolean blocked, Drawable icon) {
        this.name = name;
        this.packageName = packageName;
        this.blocked = blocked;
        this.icon = icon;
    }
}
