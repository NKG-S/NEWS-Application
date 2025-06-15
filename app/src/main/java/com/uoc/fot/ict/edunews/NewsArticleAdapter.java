package com.uoc.fot.ict.edunews;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import androidx.core.util.Consumer; // Using AndroidX Consumer for better compatibility

public class NewsArticleAdapter extends RecyclerView.Adapter<NewsArticleAdapter.ArticleViewHolder> {

    private List<NewsArticle> newsArticleList;
    private Consumer<NewsArticle> onArticleClick;

    /**
     * Constructor for the NewsArticleAdapter.
     * @param newsArticleList The list of NewsArticle objects to display.
     * @param onArticleClick A Consumer interface to handle click events on individual articles.
     */
    public NewsArticleAdapter(List<NewsArticle> newsArticleList, Consumer<NewsArticle> onArticleClick) {
        this.newsArticleList = newsArticleList;
        this.onArticleClick = onArticleClick;
    }

    @NonNull
    @Override
    public ArticleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the layout for a single news article item
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_news_article, parent, false);
        return new ArticleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ArticleViewHolder holder, int position) {
        // Get the NewsArticle object for the current position
        NewsArticle article = newsArticleList.get(position);
        // Bind the data from the NewsArticle object to the ViewHolder's views
        holder.bind(article);
    }

    @Override
    public int getItemCount() {
        // Return the total number of items in the list
        return newsArticleList.size();
    }

    /**
     * Updates the data set of the adapter and notifies the RecyclerView to refresh.
     * @param newNewsArticleList The new list of NewsArticle objects.
     */
    public void updateData(List<NewsArticle> newNewsArticleList) {
        this.newsArticleList = newNewsArticleList;
        notifyDataSetChanged(); // Notify the adapter that the data has changed
    }

    /**
     * ViewHolder class to hold references to the views in each item layout.
     */
    class ArticleViewHolder extends RecyclerView.ViewHolder {
        ImageView articleImage;
        TextView articleTitle;
        TextView articleDate;
        TextView articleDescription;
        TextView readMoreButton;

        public ArticleViewHolder(@NonNull View itemView) {
            super(itemView);
            // Initialize UI elements by finding them within the item view
            articleImage = itemView.findViewById(R.id.articleImage);
            articleTitle = itemView.findViewById(R.id.articleTitle);
            articleDate = itemView.findViewById(R.id.articleDate);
            articleDescription = itemView.findViewById(R.id.articleDescription);
            readMoreButton = itemView.findViewById(R.id.readMoreButton);
        }

        /**
         * Binds the data from a NewsArticle object to the views in the ViewHolder.
         * @param article The NewsArticle object containing the data.
         */
        public void bind(NewsArticle article) {
            // Load article image using Glide
            Glide.with(itemView.getContext())
                    .load(article.getImageUrl())
                    .placeholder(R.drawable.image_placeholder_background) // Placeholder while loading
                    .error(R.drawable.image_placeholder_background) // Image to show if loading fails
                    .into(articleImage);

            // Set article title
            articleTitle.setText(article.getTitle());

            // Format and display the post date
            String postDateString = article.getPostDate();
            if (postDateString != null && !postDateString.isEmpty()) {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd", Locale.getDefault());
                SimpleDateFormat timeFormat = new SimpleDateFormat("hh.mm a", Locale.getDefault()); // For AM/PM

                try {
                    Date date = inputFormat.parse(postDateString);
                    if (date != null) {
                        String formattedDate = dateFormat.format(date);
                        String formattedTime = timeFormat.format(date);
                        articleDate.setText(formattedTime + " (" + formattedDate + ")"); // Example: "10:30 AM (2025.06.15)"
                        articleDate.setVisibility(View.VISIBLE); // Make sure the date TextView is visible
                    } else {
                        articleDate.setVisibility(View.GONE); // Hide if date parsing returns null
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                    articleDate.setVisibility(View.GONE); // Hide on parsing error
                }
            } else {
                articleDate.setVisibility(View.GONE); // Hide if date string is null or empty
            }

            // Set article description
            articleDescription.setText(article.getDescription());

            // Set click listeners for the entire item view and the "Read More..." button
            // Both will trigger the same click event to view the full article
            itemView.setOnClickListener(v -> onArticleClick.accept(article));
            readMoreButton.setOnClickListener(v -> onArticleClick.accept(article));
        }
    }
}