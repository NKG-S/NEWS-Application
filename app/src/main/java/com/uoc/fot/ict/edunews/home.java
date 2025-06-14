package com.uoc.fot.ict.edunews;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class home extends AppCompatActivity {
    private ViewPager2 latestNewsViewPager;
    private RecyclerView categoriesRecyclerView;
    private RecyclerView olderNewsRecyclerView;
    private ShapeableImageView profileIcon;
    private SwipeRefreshLayout swipeRefreshLayout;

    private LatestNewsBannerAdapter latestNewsBannerAdapter;
    private CategoryAdapter categoryAdapter;
    private NewsArticleAdapter newsArticleAdapter;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser; // Added to store the current user

    // To keep track of the last document from the latest news query for pagination
    private DocumentSnapshot lastDocumentOfLatestNews;

    // Added TAG for logging errors
    private static final String TAG = "HomeActivity";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        initializeFirebase();
        initializeViews();
        setupAdapters();
        setupRefreshLayout();
        loadCurrentUserProfilePicture(); // Calling the new, more robust method
        fetchData(); // Unified method to fetch all initial data
        setupListeners();
    }

    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser(); // Initialize currentUser here
    }

    private void initializeViews() {
        latestNewsViewPager = findViewById(R.id.latestNewsViewPager);
        categoriesRecyclerView = findViewById(R.id.categoriesRecyclerView);
        olderNewsRecyclerView = findViewById(R.id.olderNewsRecyclerView);
        profileIcon = findViewById(R.id.profileIcon);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
    }

    private void setupRefreshLayout() {
        swipeRefreshLayout.setOnRefreshListener(() -> {
            fetchData(); // Re-fetch all data on refresh
        });
    }

    private void fetchData() {
        swipeRefreshLayout.setRefreshing(true); // Show refreshing indicator
        fetchLatestNews();
        setupCategories();
    }

    private void setupAdapters() {
        latestNewsBannerAdapter = new LatestNewsBannerAdapter(new ArrayList<>(), this::navigateToNewsDetail);
        latestNewsViewPager.setAdapter(latestNewsBannerAdapter);
        latestNewsViewPager.setOffscreenPageLimit(1);

        categoryAdapter = new CategoryAdapter(new ArrayList<>(), this::onCategorySelected);
        categoriesRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        categoriesRecyclerView.setAdapter(categoryAdapter);

        newsArticleAdapter = new NewsArticleAdapter(new ArrayList<>(), this::navigateToNewsDetail);
        olderNewsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        olderNewsRecyclerView.setAdapter(newsArticleAdapter);
    }

    private void setupListeners() {
        profileIcon.setOnClickListener(v -> navigateToUserProfile());
    }

    private void navigateToUserProfile() {
        if (currentUser != null) {
            Intent intent = new Intent(this, UserInfo.class);
            intent.putExtra("userId", currentUser.getUid());
            startActivity(intent);
        } else {
            Toast.makeText(this, "Please login to view profile", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Loads the current user's profile picture into the top bar icon by fetching from Firestore.
     */
    private void loadCurrentUserProfilePicture() {
        if (currentUser != null) {
            String userId = currentUser.getUid();
            db.collection("users").document(userId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String profilePictureUrl = documentSnapshot.getString("profilePictureUrl");
                            if (profilePictureUrl != null && !profilePictureUrl.isEmpty()) {
                                Glide.with(this)
                                        .load(profilePictureUrl)
                                        .placeholder(R.drawable.user) // Default user icon
                                        .error(R.drawable.user)
                                        .into(profileIcon);
                            } else {
                                profileIcon.setImageResource(R.drawable.user); // Default icon if URL is null/empty
                            }
                        } else {
                            profileIcon.setImageResource(R.drawable.user); // Default icon if user doc not found
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to load profile picture for top bar: " + e.getMessage());
                        profileIcon.setImageResource(R.drawable.user); // Fallback to default icon on error
                    });
        } else {
            profileIcon.setImageResource(R.drawable.user); // Default icon if no user logged in
        }
    }


    private void fetchLatestNews() {
        db.collection("posts")
                .orderBy("postDate", Query.Direction.DESCENDING)
                .limit(5)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<NewsArticle> latestNews = new ArrayList<>();
                        for (DocumentSnapshot document : task.getResult().getDocuments()) {
                            latestNews.add(mapDocumentToNewsArticle(document));
                        }
                        latestNewsBannerAdapter.updateData(latestNews);

                        if (!task.getResult().getDocuments().isEmpty()) {
                            lastDocumentOfLatestNews = task.getResult().getDocuments().get(task.getResult().size() - 1);
                            fetchOlderNews();
                        } else {
                            lastDocumentOfLatestNews = null;
                            newsArticleAdapter.updateData(new ArrayList<>());
                            Toast.makeText(this, "No latest news found for banners.", Toast.LENGTH_SHORT).show();
                            swipeRefreshLayout.setRefreshing(false);
                        }
                    } else {
                        Log.e(TAG, "Error fetching latest news: " + task.getException().getMessage());
                        Toast.makeText(this, "Error fetching latest news: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });
    }

    private void setupCategories() {
        List<Category> categories = new ArrayList<>();
        categories.add(new Category("Business", R.drawable.business));
        categories.add(new Category("Crime", R.drawable.crime));
        categories.add(new Category("Editorials", R.drawable.editorial));
        categories.add(new Category("Political", R.drawable.political));
        categories.add(new Category("Sports", R.drawable.sports));
        categories.add(new Category("Social", R.drawable.social));
        categories.add(new Category("International", R.drawable.international));
        categories.add(new Category("Technology", R.drawable.technology));
        categories.add(new Category("Health", R.drawable.health));
        categories.add(new Category("Education", R.drawable.education));
        categories.add(new Category("Environment", R.drawable.environment));
        categories.add(new Category("Art & Culture", R.drawable.art_and_culture));
        categories.add(new Category("Science", R.drawable.science));
        categories.add(new Category("Lifestyle", R.drawable.life));
        categories.add(new Category("Travel", R.drawable.travel));

        categoryAdapter.updateData(categories);
    }

    private void onCategorySelected(Category category) {
        Toast.makeText(this, "Category selected: " + category.getName(), Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(home.this, CategoryNews.class);
        intent.putExtra(CategoryNews.EXTRA_CATEGORY_NAME, category.getName());
        startActivity(intent);
    }

    private void fetchOlderNews() {
        Query query = db.collection("posts")
                .orderBy("postDate", Query.Direction.DESCENDING);

        if (lastDocumentOfLatestNews != null) {
            query = query.startAfter(lastDocumentOfLatestNews);
        } else {
            Toast.makeText(this, "No specific starting point for older news, fetching overall latest.", Toast.LENGTH_SHORT).show();
        }

        query.limit(25)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<NewsArticle> olderNews = new ArrayList<>();
                        for (DocumentSnapshot document : task.getResult().getDocuments()) {
                            olderNews.add(mapDocumentToNewsArticle(document));
                        }
                        newsArticleAdapter.updateData(olderNews);
                        if (olderNews.isEmpty() && lastDocumentOfLatestNews != null) {
                            Toast.makeText(this, "No more older news found.", Toast.LENGTH_SHORT).show();
                        } else if (olderNews.isEmpty() && lastDocumentOfLatestNews == null) {
                            Toast.makeText(this, "No news articles found in the database.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.e(TAG, "Error fetching older news: " + task.getException().getMessage());
                        Toast.makeText(this, "Error fetching older news: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                    swipeRefreshLayout.setRefreshing(false);
                });
    }

    private NewsArticle mapDocumentToNewsArticle(DocumentSnapshot document) {
        String id = document.getId();
        String title = document.getString("title");
        String category = document.getString("category");
        String description = document.getString("description");
        String imageUrl = document.getString("imageUrl");
        String author = document.getString("author");

        String postDate = document.getString("postDate");
        if (postDate == null) {
            postDate = "";
        }

        String userId = document.getString("userId");

        return new NewsArticle(id, title, description, imageUrl, postDate, category, author, userId);
    }

    /**
     * Callback method when a news article card is clicked.
     * Navigates to the News activity for the selected article.
     *
     * @param newsArticle The NewsArticle object that was clicked.
     */
    private void navigateToNewsDetail(NewsArticle newsArticle) {
        Intent intent = new Intent(this, news.class);
        intent.putExtra("NEWS_ARTICLE_ID", newsArticle.getId());
        startActivity(intent);

        Toast.makeText(this, "Opening article: " + newsArticle.getTitle(), Toast.LENGTH_SHORT).show();
    }
}