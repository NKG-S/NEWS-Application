package com.uoc.fot.ict.edunews;

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

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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

public class MyPosts extends AppCompatActivity {

    private SearchView searchView;
    private RecyclerView myPostsRecyclerView;
    private NewsArticleAdapter newsArticleAdapter;
    private TextView emptyStateText;
    private ProgressBar progressBar;

    // Data and Firebase
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private final List<NewsArticle> articlesList = new ArrayList<>();
    private final List<NewsArticle> filteredArticlesList = new ArrayList<>();

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
        setContentView(R.layout.activity_my_posts);

        // Initialize UI components
        // UI Components
        TextView myPostsTitle = findViewById(R.id.myPostsTitle);
        ImageButton backButton = findViewById(R.id.backButton);
        ImageButton sortButton = findViewById(R.id.sortButton);
        searchView = findViewById(R.id.searchView);
        myPostsRecyclerView = findViewById(R.id.myPostsRecyclerView);
        emptyStateText = findViewById(R.id.emptyStateText);
        progressBar = findViewById(R.id.progressBar);

        // Initialize Firebase instances
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Set up the RecyclerView with its adapter and layout manager
        // IMPORTANT: Pass article.getId() to navigateToEditPost
        newsArticleAdapter = new NewsArticleAdapter(new ArrayList<>(), article -> navigateToEditPost(article.getId()));
        myPostsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        myPostsRecyclerView.setAdapter(newsArticleAdapter);

        // Set up click listeners for the back button and sort button
        backButton.setOnClickListener(v -> onBackPressed());
        sortButton.setOnClickListener(this::showSortPopupMenu);

        // Set up SearchView listener
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterArticles(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterArticles(newText);
                return true;
            }
        });

        // Fetch news articles from Firestore for the current user
        fetchMyPosts();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh posts when returning to this activity, in case an edit/delete occurred
        fetchMyPosts();
    }

    /**
     * Fetches news articles from Firestore created by the currently logged-in user.
     * Articles are initially ordered by postDate in descending order (latest to oldest).
     */
    private void fetchMyPosts() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "You need to be logged in to view your posts.", Toast.LENGTH_SHORT).show();
            emptyStateText.setText("Please log in to view your posts.");
            emptyStateText.setVisibility(View.VISIBLE);
            myPostsRecyclerView.setVisibility(View.GONE);
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        emptyStateText.setVisibility(View.GONE);
        myPostsRecyclerView.setVisibility(View.GONE); // Hide RecyclerView during loading

        // Construct a Firestore query:
        // - Collection "posts"
        // - Filter by "userId" equal to current user's UID
        // - Order results by "postDate" in descending order (latest first)
        db.collection("posts")
                .whereEqualTo("userId", currentUser.getUid())
                .orderBy("postDate", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        articlesList.clear(); // Clear any previously loaded articles
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            NewsArticle article = document.toObject(NewsArticle.class);
                            article.setId(document.getId()); // Set the document ID
                            articlesList.add(article);
                        }

                        if (articlesList.isEmpty()) {
                            emptyStateText.setText("No posts found.");
                            emptyStateText.setVisibility(View.VISIBLE);
                            myPostsRecyclerView.setVisibility(View.GONE);
                        } else {
                            emptyStateText.setVisibility(View.GONE);
                            myPostsRecyclerView.setVisibility(View.VISIBLE);
                            // Apply initial sorting
                            sortArticles(currentSortOrder);
                            // Apply search filter (if any text already in searchView)
                            filterArticles(searchView.getQuery().toString());
                        }
                    } else {
                        Log.e("MyPostsActivity", "Error getting documents: ", task.getException());
                        Toast.makeText(this, "Failed to load your posts.", Toast.LENGTH_SHORT).show();
                        emptyStateText.setText("Failed to load your posts.");
                        emptyStateText.setVisibility(View.VISIBLE);
                        myPostsRecyclerView.setVisibility(View.GONE);
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
        popup.getMenuInflater().inflate(R.menu.menu_my_posts_sort, popup.getMenu());
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.action_sort_latest_to_oldest) {
                    currentSortOrder = SortOrder.LATEST_TO_OLDEST;
                    sortArticles(currentSortOrder);
                    filterArticles(searchView.getQuery().toString());
                    return true;
                } else if (id == R.id.action_sort_oldest_to_latest) {
                    currentSortOrder = SortOrder.OLDEST_TO_LATEST;
                    sortArticles(currentSortOrder);
                    filterArticles(searchView.getQuery().toString());
                    return true;
                }
                return false;
            }
        });
        popup.show();
    }

    /**
     * Sorts the articlesList based on the specified SortOrder.
     * Uses a custom Comparator to parse and compare dates.
     * This sorts the *master* list (`articlesList`). Filtering will then happen on this sorted list.
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

                    // Handle null dates gracefully (e.g., place them at the end or beginning)
                    if (date1 == null && date2 == null) return 0;
                    if (date1 == null) return (order == SortOrder.LATEST_TO_OLDEST) ? 1 : -1; // Nulls last for latest, first for oldest
                    if (date2 == null) return (order == SortOrder.LATEST_TO_OLDEST) ? -1 : 1; // Nulls last for latest, first for oldest

                    return (order == SortOrder.LATEST_TO_OLDEST) ? date2.compareTo(date1) : date1.compareTo(date2);

                } catch (ParseException e) {
                    Log.e("MyPostsActivity", "Date parsing error for article: " + a1.getTitle() + " or " + a2.getTitle() + " - " + e.getMessage());
                    // Fallback: maintain original order or sort by title if dates are unparseable
                    return 0; // or a1.getTitle().compareTo(a2.getTitle());
                }
            }
        });
    }

    /**
     * Filters the articles based on the search query.
     * This operates on the already sorted `articlesList`.
     * @param query The search text entered by the user.
     */
    private void filterArticles(String query) {
        filteredArticlesList.clear();
        if (query == null || query.isEmpty()) {
            filteredArticlesList.addAll(articlesList);
        } else {
            String lowerCaseQuery = query.toLowerCase(Locale.getDefault());
            for (NewsArticle article : articlesList) {
                // Ensure title is not null before converting to lowercase
                if (article.getTitle() != null && article.getTitle().toLowerCase(Locale.getDefault()).contains(lowerCaseQuery)) {
                    filteredArticlesList.add(article);
                }
            }
        }

        newsArticleAdapter.updateData(filteredArticlesList);

        if (filteredArticlesList.isEmpty() && !articlesList.isEmpty()) {
            emptyStateText.setText("No posts found matching your search.");
            emptyStateText.setVisibility(View.VISIBLE);
            myPostsRecyclerView.setVisibility(View.GONE);
        } else if (articlesList.isEmpty()) {
            emptyStateText.setText("No posts found.");
            emptyStateText.setVisibility(View.VISIBLE);
            myPostsRecyclerView.setVisibility(View.GONE);
        } else {
            emptyStateText.setVisibility(View.GONE);
            myPostsRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Callback method when a news article card is clicked.
     * Navigates to the EditPost activity for the selected article.
     *
     * @param postId The document ID of the NewsArticle in Firestore.
     */
    private void navigateToEditPost(String postId) {
        Intent intent = new Intent(this, EditPost.class);
        intent.putExtra("postId", postId); // Pass only the postId
        startActivity(intent);

        Toast.makeText(this, "Opening post for editing.", Toast.LENGTH_SHORT).show();
    }
}