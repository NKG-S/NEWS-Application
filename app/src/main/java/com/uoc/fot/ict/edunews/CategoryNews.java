package com.uoc.fot.ict.edunews; // Ensure this matches your package name

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback; // NEW IMPORT: For modern back press handling
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CategoryNews extends AppCompatActivity {

    // Key for passing the category name via Intent
    public static final String EXTRA_CATEGORY_NAME = "extra_category_name";

    private NewsArticleAdapter newsArticleAdapter; // Reusing your existing adapter for news cards
    private TextView emptyStateText;
    private ProgressBar progressBar;

    // Data and Firebase
    private String currentCategory;
    private FirebaseFirestore db;
    private final List<NewsArticle> articlesList = new ArrayList<>();

    // Enum to keep track of the current sort order for articles
    private SortOrder currentSortOrder = SortOrder.LATEST_TO_OLDEST;

    // Enum defining the possible sorting orders
    private enum SortOrder {
        LATEST_TO_OLDEST,
        OLDEST_TO_LATEST
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_news); // Set the layout for this activity

        // Initialize UI components by finding them by their IDs from the layout
        TextView categoryNameTitle = findViewById(R.id.categoryNameTitle);
        ImageButton backButton = findViewById(R.id.backButton);
        ImageButton sortButton = findViewById(R.id.sortButton);
        RecyclerView categoryNewsRecyclerView = findViewById(R.id.categoryNewsRecyclerView);
        emptyStateText = findViewById(R.id.emptyStateText);
        progressBar = findViewById(R.id.progressBar);

        // Initialize Firestore instance
        db = FirebaseFirestore.getInstance();

        // Retrieve the category name passed from the previous activity (e.g., HomeFragment)
        if (getIntent().hasExtra(EXTRA_CATEGORY_NAME)) {
            currentCategory = getIntent().getStringExtra(EXTRA_CATEGORY_NAME);
            categoryNameTitle.setText(currentCategory); // Set the title of the screen to the category name
        } else {
            // If no category name is passed, display a toast message and close the activity
            Toast.makeText(this, "Category not specified.", Toast.LENGTH_SHORT).show();
            finish(); // Close the activity as it cannot function without a category
            return;
        }

        // Set up the RecyclerView with its adapter and layout manager
        newsArticleAdapter = new NewsArticleAdapter(new ArrayList<>(), this::navigateToNewsDetail);
        categoryNewsRecyclerView.setLayoutManager(new LinearLayoutManager(this)); // Vertical list layout
        categoryNewsRecyclerView.setAdapter(newsArticleAdapter);

        // NEW: Register OnBackPressedCallback for modern back press handling
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true /* enabled by default */) {
            @Override
            public void handleOnBackPressed() {
                Log.d("CategoryNewsActivity", "Back button pressed via dispatcher.");
                // Define the behavior for the back press here.
                // For example, navigate back to the home screen (or MyPosts if that's the usual entry point)
                Intent intent = new Intent(CategoryNews.this, home.class); // Assuming 'home.class' is your main feed activity
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK); // Clear back stack
                startActivity(intent);
                finish(); // Finish the current activity
            }
        });

        // Set up click listeners for the back button and sort button
        // NEW: backButton now triggers the OnBackPressedDispatcher
        backButton.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        sortButton.setOnClickListener(this::showSortPopupMenu); // Show sorting options when sort button is clicked

        // Fetch news articles from Firestore for the determined category
        fetchNewsArticles();
    }

    /**
     * Fetches news articles from Firestore based on the currentCategory.
     * Articles are initially ordered by postDate in descending order (latest to oldest).
     */
    private void fetchNewsArticles() {
        progressBar.setVisibility(View.VISIBLE); // Show progress bar while loading
        emptyStateText.setVisibility(View.GONE); // Hide empty state text

        db.collection("posts")
                .whereEqualTo("category", currentCategory)
                .orderBy("postDate", Query.Direction.DESCENDING) // Default sort for fetching
                .get()
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE); // Hide progress bar after loading
                    if (task.isSuccessful()) {
                        articlesList.clear(); // Clear any previously loaded articles
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            NewsArticle article = document.toObject(NewsArticle.class);
                            article.setId(document.getId()); // Explicitly set the document ID
                            articlesList.add(article); // Add to the list
                        }

                        if (articlesList.isEmpty()) {
                            emptyStateText.setVisibility(View.VISIBLE); // Show empty state if no articles found
                        } else {
                            sortArticles(currentSortOrder); // Apply the current sorting order to the fetched list
                            newsArticleAdapter.updateData(articlesList); // Update the RecyclerView adapter with the (sorted) data
                            emptyStateText.setVisibility(View.GONE); // Ensure empty state is hidden
                        }
                    } else {
                        Log.e("CategoryNewsActivity", "Error getting documents: " + (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                        Toast.makeText(this, "Failed to load articles: " + (task.getException() != null ? task.getException().getMessage() : "Unknown error"), Toast.LENGTH_SHORT).show();
                        emptyStateText.setVisibility(View.VISIBLE); // Show empty state on error
                    }
                });
    }

    /**
     * Displays a PopupMenu with sorting options (Latest to Oldest, Oldest to Latest).
     *
     * @param view The view to anchor the popup menu to (in this case, the sortButton).
     */
    private void showSortPopupMenu(View view) {
        PopupMenu popup = new PopupMenu(this, view);
        popup.getMenuInflater().inflate(R.menu.menu_category_news_sort, popup.getMenu());
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int id = item.getItemId(); // Get the ID of the clicked menu item
                if (id == R.id.action_sort_latest_to_oldest) {
                    currentSortOrder = SortOrder.LATEST_TO_OLDEST; // Set sort order
                    sortArticles(currentSortOrder); // Apply sort
                    return true;
                } else if (id == R.id.action_sort_oldest_to_latest) {
                    currentSortOrder = SortOrder.OLDEST_TO_LATEST; // Set sort order
                    sortArticles(currentSortOrder); // Apply sort
                    return true;
                }
                return false; // Return false for unhandled menu items
            }
        });
        popup.show(); // Show the popup menu
    }

    /**
     * Sorts the articlesList based on the specified SortOrder.
     * Uses a custom Comparator to parse and compare dates.
     *
     * @param order The desired sorting order (LATEST_TO_OLDEST or OLDEST_TO_LATEST).
     */
    private void sortArticles(SortOrder order) {
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

        Collections.sort(articlesList, new Comparator<NewsArticle>() {
            @Override
            public int compare(NewsArticle a1, NewsArticle a2) {
                try {
                    Date date1 = inputFormat.parse(a1.getPostDate());
                    Date date2 = inputFormat.parse(a2.getPostDate());

                    // Handle cases where date parsing might fail or dates are null
                    if (date1 == null && date2 == null) return 0;
                    if (date1 == null) return (order == SortOrder.LATEST_TO_OLDEST) ? 1 : -1; // Nulls last for L->O, first for O->L
                    if (date2 == null) return (order == SortOrder.LATEST_TO_OLDEST) ? -1 : 1; // Nulls last for L->O, first for O->L

                    return (order == SortOrder.LATEST_TO_OLDEST) ? date2.compareTo(date1) : date1.compareTo(date2);

                } catch (ParseException e) {
                    Log.e("CategoryNewsActivity", "Date parsing error for sorting: " + e.getMessage());
                    return 0; // Treat items as equal if date cannot be parsed for sorting purposes
                }
            }
        });
        newsArticleAdapter.updateData(articlesList); // Notify adapter that data has changed and is sorted
    }

    /**
     * Callback method when a news article card is clicked.
     * Navigates to the News activity for the selected article.
     *
     * @param article The NewsArticle object that was clicked.
     */
    private void navigateToNewsDetail(NewsArticle article) {
        Intent intent = new Intent(this, news.class);
        intent.putExtra("NEWS_ARTICLE_ID", article.getId());
        startActivity(intent);

        Toast.makeText(this, "Opening article: " + article.getTitle(), Toast.LENGTH_SHORT).show();
    }
}