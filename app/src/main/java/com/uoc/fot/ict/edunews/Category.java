package com.uoc.fot.ict.edunews;

public class Category {
    private final String name;
    private final int iconResId; // Drawable resource ID for the icon

    public Category(String name, int iconResId) {
        this.name = name;
        this.iconResId = iconResId;
    }

    public String getName() {
        return name;
    }

    public int getIconResId() {
        return iconResId;
    }
}