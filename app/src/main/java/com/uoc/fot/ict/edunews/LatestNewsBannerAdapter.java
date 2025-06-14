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
import androidx.core.util.Consumer;

public class LatestNewsBannerAdapter extends RecyclerView.Adapter<LatestNewsBannerAdapter.BannerViewHolder> {

    private List<NewsArticle> latestNewsList;
    private Consumer<NewsArticle> onArticleClick;

    public LatestNewsBannerAdapter(List<NewsArticle> latestNewsList, Consumer<NewsArticle> onArticleClick) {
        this.latestNewsList = latestNewsList;
        this.onArticleClick = onArticleClick;
    }

    @NonNull
    @Override
    public BannerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_latest_news_banner, parent, false);
        return new BannerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BannerViewHolder holder, int position) {
        NewsArticle article = latestNewsList.get(position);
        holder.bind(article);
    }

    @Override
    public int getItemCount() {
        return latestNewsList.size();
    }

    public void updateData(List<NewsArticle> newNewsList) {
        this.latestNewsList.clear(); // Clear existing data
        this.latestNewsList.addAll(newNewsList); // Add new data
        notifyDataSetChanged(); // Notify adapter that data has changed
    }

    class BannerViewHolder extends RecyclerView.ViewHolder {
        ImageView bannerImage;
        TextView bannerTitle;
        TextView articleDate;
        ImageView bannerArrow;

        public BannerViewHolder(@NonNull View itemView) {
            super(itemView);
            bannerImage = itemView.findViewById(R.id.bannerImage);
            bannerTitle = itemView.findViewById(R.id.bannerTitle);
            articleDate = itemView.findViewById(R.id.articleDate);
            bannerArrow = itemView.findViewById(R.id.bannerArrow);
        }

        public void bind(NewsArticle article) {
            Glide.with(itemView.getContext())
                    .load(article.getImageUrl())
                    .placeholder(R.drawable.image_placeholder_background)
                    .error(R.drawable.image_placeholder_background)
                    .into(bannerImage);

            String titleAndDescription = "";
            if (article.getTitle() != null) {
                titleAndDescription = article.getTitle();
            }
            if (article.getDescription() != null && !article.getDescription().isEmpty()) {
                if (!titleAndDescription.isEmpty()) {
                    titleAndDescription += "\n\n";
                }
                titleAndDescription += article.getDescription();
            }
            bannerTitle.setText(titleAndDescription);

            String postDateString = article.getPostDate();
            if (postDateString != null && !postDateString.isEmpty()) {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd", Locale.getDefault());
                SimpleDateFormat timeFormat = new SimpleDateFormat("hh.mm a", Locale.getDefault());

                try {
                    Date date = inputFormat.parse(postDateString);
                    if (date != null) {
                        String formattedDate = dateFormat.format(date);
                        String formattedTime = timeFormat.format(date);
                        articleDate.setText(formattedDate + "\n" + formattedTime);
                        articleDate.setVisibility(View.VISIBLE);
                    } else {
                        articleDate.setVisibility(View.GONE);
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                    articleDate.setVisibility(View.GONE);
                }
            } else {
                articleDate.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> onArticleClick.accept(article));
            bannerArrow.setOnClickListener(v -> onArticleClick.accept(article));
        }
    }
}