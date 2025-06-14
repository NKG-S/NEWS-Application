package com.uoc.fot.ict.edunews;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log; // Added for logging errors
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

    // To keep track of the last document from the latest news query for pagination
    private DocumentSnapshot lastDocumentOfLatestNews;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        initializeFirebase();
        initializeViews();
        setupAdapters();
        setupRefreshLayout();
        loadUserProfilePicture();
        fetchData(); // Unified method to fetch all initial data
        setupListeners();
    }

    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
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
        fetchLatestNews(); // This will, in turn, call fetchOlderNews if successful
        setupCategories(); // Categories are set up here, they are mostly static
    }

    private void setupAdapters() {
        latestNewsBannerAdapter = new LatestNewsBannerAdapter(new ArrayList<>(), this::navigateToNewsDetail);
        latestNewsViewPager.setAdapter(latestNewsBannerAdapter);
        latestNewsViewPager.setOffscreenPageLimit(1);

        // Ensure CategoryAdapter constructor expects List<Category> and Consumer<Category>
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
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            Intent intent = new Intent(this, UserInfo.class); // Assuming UserInfo is your profile activity
            intent.putExtra("userId", currentUser.getUid());
            startActivity(intent);
        } else {
            Toast.makeText(this, "Please login to view profile", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadUserProfilePicture() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null && currentUser.getPhotoUrl() != null) {
            Glide.with(this)
                    .load(currentUser.getPhotoUrl())
                    .placeholder(R.drawable.user) // Default user icon if no photo
                    .error(R.drawable.user) // Error icon if loading fails
                    .into(profileIcon);
        } else {
            profileIcon.setImageResource(R.drawable.user); // Set default icon
        }
    }

    private void fetchLatestNews() {
        db.collection("posts")
                .orderBy("postDate", Query.Direction.DESCENDING)
                .limit(5) // Fetch latest 5 news for the banner
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<NewsArticle> latestNews = new ArrayList<>();
                        for (DocumentSnapshot document : task.getResult().getDocuments()) {
                            latestNews.add(mapDocumentToNewsArticle(document));
                        }
                        latestNewsBannerAdapter.updateData(latestNews);

                        // Capture the last document from this query for pagination of "older news"
                        if (!task.getResult().getDocuments().isEmpty()) {
                            lastDocumentOfLatestNews = task.getResult().getDocuments().get(task.getResult().size() - 1);
                            fetchOlderNews(); // Only fetch older news if latest news were found
                        } else {
                            lastDocumentOfLatestNews = null; // No latest news, so no starting point for older news
                            newsArticleAdapter.updateData(new ArrayList<>()); // Clear older news if no latest
                            Toast.makeText(this, "No latest news found for banners.", Toast.LENGTH_SHORT).show();
                            swipeRefreshLayout.setRefreshing(false); // Stop refreshing if nothing to load
                        }
                    } else {
                        Log.e("home", "Error fetching latest news: " + task.getException().getMessage()); // Log the error
                        Toast.makeText(this, "Error fetching latest news: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        swipeRefreshLayout.setRefreshing(false); // Stop refreshing on error
                    }
                });
    }

    private void setupCategories() {
        List<Category> categories = new ArrayList<>();
        // These icons should be added to your drawable folder (e.g., ic_category_business.xml)
        // Ensure you have valid drawable resources for these categories.
        // Using `R.drawable.user` as a placeholder as per your example. You should replace these.
        categories.add(new Category("Business", R.drawable.user));
        categories.add(new Category("Crime", R.drawable.user));
        categories.add(new Category("Editorials", R.drawable.user));
        categories.add(new Category("Political", R.drawable.user));
        categories.add(new Category("Sports", R.drawable.user));
        categories.add(new Category("Social", R.drawable.user));
        categories.add(new Category("International", R.drawable.user));
        categories.add(new Category("Technology", R.drawable.user));
        categories.add(new Category("Health", R.drawable.user));
        categories.add(new Category("Education", R.drawable.user));
        categories.add(new Category("Environment", R.drawable.user));
        categories.add(new Category("Art & Culture", R.drawable.user));
        categories.add(new Category("Science", R.drawable.user));
        categories.add(new Category("Lifestyle", R.drawable.user));
        categories.add(new Category("Travel", R.drawable.user));

        categoryAdapter.updateData(categories); // Update the category adapter with the list
    }

    private void onCategorySelected(Category category) {
        Toast.makeText(this, "Category selected: " + category.getName(), Toast.LENGTH_SHORT).show();
        // Start CategoryNews activity and pass the selected category name
        Intent intent = new Intent(home.this, CategoryNews.class); // Changed from CategoryNewsActivity to CategoryNews based on file structure
        intent.putExtra(CategoryNews.EXTRA_CATEGORY_NAME, category.getName());
        startActivity(intent);
    }

    private void fetchOlderNews() {
        Query query = db.collection("posts")
                .orderBy("postDate", Query.Direction.DESCENDING);

        // Use startAfter for pagination to get news older than the latest banner news
        if (lastDocumentOfLatestNews != null) {
            query = query.startAfter(lastDocumentOfLatestNews);
        } else {
            // If no latest news were found, fetch the overall latest for older news (e.g., first 20 or 25)
            // This case handles situations where there might be fewer than 5 total posts, or zero.
            Toast.makeText(this, "No specific starting point for older news, fetching overall latest.", Toast.LENGTH_SHORT).show();
        }

        query.limit(25) // Fetch up to 25 older news articles as requested (changed from 20 to 25 as discussed previously)
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
                        Log.e("home", "Error fetching older news: " + task.getException().getMessage()); // Log the error
                        Toast.makeText(this, "Error fetching older news: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                    swipeRefreshLayout.setRefreshing(false); // Always stop refreshing when all data is loaded or on error
                });
    }

    // Helper method to map a Firestore DocumentSnapshot to a NewsArticle object
    private NewsArticle mapDocumentToNewsArticle(DocumentSnapshot document) {
        String id = document.getId();
        String title = document.getString("title");
        String category = document.getString("category");
        String description = document.getString("description");
        String imageUrl = document.getString("imageUrl");
        String author = document.getString("author");

        // Ensure postDate is read as a String. Handle potential null.
        String postDate = document.getString("postDate");
        if (postDate == null) {
            postDate = ""; // Default to empty string if field is missing
        }

        String userId = document.getString("userId");

        // Calling the correct 8-argument constructor for NewsArticle
        return new NewsArticle(id, title, description, imageUrl, postDate, category, author, userId);
    }

    private void navigateToNewsDetail(NewsArticle newsArticle) {
        // IMPORTANT: Assuming NewsDetailActivity is the correct class name for your news detail screen.
        // If your detail screen is named something else (e.g., MainActivity as was in your previous code),
        // please adjust this line accordingly. Based on common Android practices, a dedicated NewsDetailActivity
        // is most appropriate for displaying a single news article's full content.
        Intent intent = new Intent(this, MainActivity.class); // Corrected to NewsDetailActivity
        intent.putExtra("newsArticle", newsArticle); // Pass the NewsArticle object (NewsArticle must be Parcelable/Serializable)
        startActivity(intent);
    }
}