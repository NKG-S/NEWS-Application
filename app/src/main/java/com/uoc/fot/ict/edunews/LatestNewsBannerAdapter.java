package com.uoc.fot.ict.edunews;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying the latest news articles in a banner format, typically in a ViewPager2.
 * It binds NewsArticle objects to the `item_latest_news_banner.xml` layout,
 * separating title and description display.
 */
public class LatestNewsBannerAdapter extends RecyclerView.Adapter<LatestNewsBannerAdapter.BannerViewHolder> {

    private final List<NewsArticle> latestNewsList;
    private final Consumer<NewsArticle> onArticleClick;

    /**
     * Constructor for the LatestNewsBannerAdapter.
     * @param latestNewsList The list of NewsArticle objects to display.
     * @param onArticleClick A Consumer interface to handle click events on individual articles.
     */
    public LatestNewsBannerAdapter(List<NewsArticle> latestNewsList, Consumer<NewsArticle> onArticleClick) {
        this.latestNewsList = latestNewsList;
        this.onArticleClick = onArticleClick;
    }

    /**
     * Called when RecyclerView needs a new {@link BannerViewHolder} of the given type to represent
     * an item.
     * @param parent The ViewGroup into which the new View will be added after it is bound to
     * a position.
     * @param viewType The view type of the new View.
     * @return A new BannerViewHolder that holds a View of the given view type.
     */
    @NonNull
    @Override
    public BannerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the layout for a single news article banner item
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_latest_news_banner, parent, false);
        return new BannerViewHolder(view);
    }

    /**
     * Called by RecyclerView to display the data at the specified position.
     * This method updates the contents of the {@link BannerViewHolder#itemView} to reflect the item at the given position.
     * @param holder The BannerViewHolder which should be updated to represent the contents of the
     * item at the given `position` in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull BannerViewHolder holder, int position) {
        NewsArticle article = latestNewsList.get(position);
        holder.bind(article, onArticleClick); // Pass onArticleClick to bind method
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     * @return The total number of items in this adapter.
     */
    @Override
    public int getItemCount() {
        return latestNewsList.size();
    }

    /**
     * Updates the data set of the adapter and notifies the RecyclerView/ViewPager2 to refresh.
     * @param newNewsList The new list of NewsArticle objects.
     * Dependencies: `notifyDataSetChanged()`.
     */
    public void updateData(List<NewsArticle> newNewsList) {
        this.latestNewsList.clear(); // Clear existing data
        this.latestNewsList.addAll(newNewsList); // Add new data
        notifyDataSetChanged(); // Notify adapter that data has changed
    }

    /**
     * ViewHolder class to hold references to the views in each latest news banner item layout.
     */
    class BannerViewHolder extends RecyclerView.ViewHolder {
        ImageView bannerImage;
        TextView bannerNewsTitle; // Changed ID to match XML
        TextView bannerDescription; // New TextView for description
        TextView articleDate;
        ImageView bannerArrow;

        /**
         * Constructor for BannerViewHolder.
         * @param itemView The view for a single item in the RecyclerView/ViewPager.
         */
        public BannerViewHolder(@NonNull View itemView) {
            super(itemView);
            // Initialize UI elements by finding them within the item view
            bannerImage = itemView.findViewById(R.id.bannerImage);
            bannerNewsTitle = itemView.findViewById(R.id.bannerNewsTitle); // Initialize new ID
            bannerDescription = itemView.findViewById(R.id.bannerDescription); // Initialize new TextView
            articleDate = itemView.findViewById(R.id.articleDate);
            bannerArrow = itemView.findViewById(R.id.bannerArrow);
        }

        /**
         * Binds the data from a NewsArticle object to the views in the ViewHolder.
         * @param article The NewsArticle object containing the data.
         * @param onArticleClick A Consumer interface to handle click events.
         * Dependencies: `Glide` for image loading, `SimpleDateFormat` for date formatting.
         */
        public void bind(NewsArticle article, Consumer<NewsArticle> onArticleClick) {
            // Load banner image using Glide
            Glide.with(itemView.getContext())
                    .load(article.getImageUrl())
                    .placeholder(R.drawable.rounded_background_card) // Placeholder while loading
                    .error(R.drawable.rounded_background_card) // Image to show if loading fails
                    .into(bannerImage);

            // Set banner title (now a dedicated TextView)
            if (article.getTitle() != null && !article.getTitle().isEmpty()) {
                bannerNewsTitle.setText(article.getTitle());
                bannerNewsTitle.setVisibility(View.VISIBLE);
            } else {
                bannerNewsTitle.setVisibility(View.GONE);
            }

            // Set banner description (new dedicated TextView)
            if (article.getDescription() != null && !article.getDescription().isEmpty()) {
                bannerDescription.setText(article.getDescription());
                bannerDescription.setVisibility(View.VISIBLE);
            } else {
                bannerDescription.setVisibility(View.GONE);
            }

            // Format and display the post date
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
                        // Display date and time on separate lines
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

            // Set click listeners for the entire item view and the arrow button
            // Both will trigger the same click event to view the full article
            itemView.setOnClickListener(v -> onArticleClick.accept(article));
            bannerArrow.setOnClickListener(v -> onArticleClick.accept(article));
        }
    }
}