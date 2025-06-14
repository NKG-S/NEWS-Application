package com.uoc.fot.ict.edunews;

import android.os.Parcel;
import android.os.Parcelable;

// Implementing Parcelable for efficient data passing between activities
public class NewsArticle implements Parcelable {
    private String id;
    private String title;
    private String description;
    private String imageUrl;
    private String postDate; // Storing as String, as determined by CreatePost.java
    private String category;
    private String author;
    private String userId;

    public NewsArticle() {
        // Default constructor required for Firestore object mapping
    }

    // Main constructor to create a NewsArticle object with all properties
    public NewsArticle(String id, String title, String description, String imageUrl, String postDate, String category, String author, String userId) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.postDate = postDate;
        this.category = category;
        this.author = author;
        this.userId = userId;
    }

    // --- Getters for all properties ---
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getImageUrl() { return imageUrl; }
    public String getPostDate() { return postDate; }
    public String getCategory() { return category; }
    public String getAuthor() { return author; }
    public String getUserId() { return userId; }

    // --- Setters (optional, but good practice if you modify properties) ---
    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setPostDate(String postDate) { this.postDate = postDate; } // Corrected typo
    public void setCategory(String category) { this.category = category; }
    public void setAuthor(String author) { this.author = author; }
    public void setUserId(String userId) { this.userId = userId; }


    // --- Parcelable implementation methods ---

    // Constructor used when reconstructing the object from a Parcel
    protected NewsArticle(Parcel in) {
        id = in.readString();
        title = in.readString();
        description = in.readString();
        imageUrl = in.readString();
        postDate = in.readString();
        category = in.readString();
        author = in.readString();
        userId = in.readString();
    }

    // Required CREATOR static field for Parcelable
    public static final Creator<NewsArticle> CREATOR = new Creator<NewsArticle>() {
        @Override
        public NewsArticle createFromParcel(Parcel in) {
            return new NewsArticle(in);
        }

        @Override
        public NewsArticle[] newArray(int size) {
            return new NewsArticle[size];
        }
    };

    @Override
    public int describeContents() {
        return 0; // Indicate no special objects
    }

    // Method to write the object's data to a Parcel
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(title);
        dest.writeString(description);
        dest.writeString(imageUrl);
        dest.writeString(postDate);
        dest.writeString(category);
        dest.writeString(author);
        dest.writeString(userId);
    }
}