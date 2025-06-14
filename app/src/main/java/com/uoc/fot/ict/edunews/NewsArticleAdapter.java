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

    public NewsArticleAdapter(List<NewsArticle> newsArticleList, Consumer<NewsArticle> onArticleClick) {
        this.newsArticleList = newsArticleList;
        this.onArticleClick = onArticleClick;
    }

    @NonNull
    @Override
    public ArticleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_news_article, parent, false);
        return new ArticleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ArticleViewHolder holder, int position) {
        NewsArticle article = newsArticleList.get(position);
        holder.bind(article);
    }

    @Override
    public int getItemCount() {
        return newsArticleList.size();
    }

    public void updateData(List<NewsArticle> newNewsArticleList) {
        this.newsArticleList = newNewsArticleList;
        notifyDataSetChanged();
    }

    class ArticleViewHolder extends RecyclerView.ViewHolder {
        ImageView articleImage;
        TextView articleTitle;
        TextView articleDate;
        TextView articleDescription;
        TextView readMoreButton;

        public ArticleViewHolder(@NonNull View itemView) {
            super(itemView);
            articleImage = itemView.findViewById(R.id.articleImage);
            articleTitle = itemView.findViewById(R.id.articleTitle);
            articleDate = itemView.findViewById(R.id.articleDate);
            articleDescription = itemView.findViewById(R.id.articleDescription);
            readMoreButton = itemView.findViewById(R.id.readMoreButton);
        }

        public void bind(NewsArticle article) {
            Glide.with(itemView.getContext())
                    .load(article.getImageUrl())
                    .placeholder(R.drawable.image_placeholder_background)
                    .error(R.drawable.image_placeholder_background)
                    .into(articleImage);
            articleTitle.setText(article.getTitle());

            // Format the date string
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
                        articleDate.setText(formattedTime + "(" + formattedDate + ")");
                        articleDate.setVisibility(View.VISIBLE);
                    } else {
                        articleDate.setVisibility(View.GONE); // Hide if parsing fails
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                    articleDate.setVisibility(View.GONE); // Hide on parsing error
                }
            } else {
                articleDate.setVisibility(View.GONE); // Hide if date string is null or empty
            }

            articleDescription.setText(article.getDescription());

            itemView.setOnClickListener(v -> onArticleClick.accept(article));
            readMoreButton.setOnClickListener(v -> onArticleClick.accept(article));
        }
    }
}