EduNews App
EduNews is a robust and intuitive Android news and article management application, meticulously crafted for the Faculty of Technology, University of Colombo. Designed with both content creators and consumers in mind, this platform facilitates seamless viewing, creation, editing, and deletion of news articles, underpinned by sophisticated image handling and a structured, category-based organization system. It aims to streamline information dissemination within an academic or general news environment, providing a rich and engaging user experience.

Developed by: Nethmin Kavindu (Undergraduate Student, BICT Degree, Faculty of Technology, University of Colombo)

🌟 Features & Functionality
EduNews delivers a comprehensive suite of features to ensure a dynamic and efficient news management experience:

User-Friendly News Feed: The application greets users with a clean, responsive, and infinitely scrollable list of news articles. Each item provides a concise overview, including a captivating title, publication date, a snippet of the description, and a compelling thumbnail image, encouraging users to explore further.

Detailed Article View: Tapping on any article transforms the screen into an immersive reading experience. This dedicated view presents the full article content, featuring a high-quality, adaptable image that dynamically adjusts its height to preserve its original aspect ratio, ensuring no distortion. Readers can delve into the complete description, identify the author, and see the precisely formatted publication date and time, offering all necessary context at a glance.

Article Creation: Empowering content creators, EduNews offers an intuitive interface for composing new news articles. Users can effortlessly input a compelling title, a comprehensive description, and select the most relevant category from a predefined list. A robust image upload mechanism allows for easy inclusion of visual content, making each article more engaging.

Post Management (My Posts): A personalized dashboard allows authenticated users to effortlessly manage their contributions. This dedicated section provides a clear overview of all articles they have published, facilitating easy access for review or further action.

Intuitive Article Editing: Editing posts is a seamless experience, designed for maximum flexibility:

Dual Mode Interface: Users can fluidly switch between a "View Mode" (for reading) and an "Edit Mode" (for modification) for any of their existing posts.

Comprehensive Content Modification: In "Edit Mode," all core article components—title, description, and category—become fully editable, allowing for quick updates and corrections.

Advanced Image Handling: The app provides intelligent image management. Users can easily replace an existing article image with a new one or opt to clear the image entirely. Crucially, the system efficiently handles Firebase Storage operations in the background, including the automatic deletion of old images when a new one is uploaded or removed. This prevents the accumulation of orphaned files in cloud storage, ensuring data integrity and optimizing resource usage.

Refined Category Selection: The category dropdown now offers a perfectly smooth user experience. When transitioning into edit mode, the dropdown correctly displays all available category options upon user interaction (click or focus), rather than automatically expanding, eliminating any potential annoyance during mode switching.

Secure Article Deletion: Users have full control over their content, with the ability to permanently delete their own published articles. A clear confirmation prompt precedes any deletion, preventing accidental data loss. The deletion process is comprehensive, safely removing both the article's data from Cloud Firestore and its associated image from Firebase Storage.

Firebase Integration: The backbone of EduNews is built upon Google Firebase, leveraging its powerful suite of cloud services for a scalable, secure, and real-time backend.

User Profile Navigation: A readily accessible profile icon in the top navigation bar provides quick access to user-specific information (assuming the UserInfo activity is implemented to display relevant user details).

🛠️ Technologies Used
This project is developed using a modern Android development stack, deeply integrated with the robust and scalable services offered by Google Firebase.

Frontend Development:

Android (Java): The primary language for implementing core application logic, user interface components, and managing overall app flow.

Android Jetpack: A suite of libraries to help developers follow best practices, reduce boilerplate code, and write code that works consistently across Android versions and devices. Key components used include:

AppCompatActivity: For backward compatibility and consistent look across Android versions.

ConstraintLayout: A powerful layout manager for building complex and responsive UIs.

RecyclerView: Efficiently displays large, scrollable lists of data, essential for the news feed.

ScrollView: Enables vertical scrolling for long content pages, such as detailed article views.

Material Design Components: Adhering to Google's Material Design guidelines for a consistent, intuitive, and visually appealing user experience. Components like MaterialCardView for news items, TextInputEditText for form inputs, AutoCompleteTextView for dropdown selections, and ShapeableImageView for circular profile pictures contribute to the modern aesthetic.

Glide: An industry-standard, fast, and highly performant open-source media management and image loading framework for Android. It efficiently handles image fetching, caching, and display, crucial for smooth scrolling and quick loading of article images and profile pictures.

Backend & Cloud Services (Google Firebase):

Firebase Authentication: Provides secure and robust user authentication capabilities, including email/password login. It manages user sessions and ensures that only authenticated users can perform sensitive operations like creating, editing, or deleting posts.

Cloud Firestore: A flexible, scalable NoSQL cloud database used for real-time data storage and synchronization. All news article data (stored in a posts collection) and user profile information (in a users collection) are managed here, providing real-time updates across all connected clients.

Firebase Cloud Storage: A powerful and cost-effective object storage service used for securely storing all media files, specifically uploaded news article images and user profile pictures. It integrates seamlessly with Firestore for managing image URLs.

🎯 Suitable Use Cases
EduNews offers a versatile foundation adaptable to various scenarios requiring content publishing and consumption:

University/College News Portals: An ideal solution for educational institutions to establish a dedicated mobile news hub. It enables quick dissemination of important announcements, campus events, academic achievements, and general news updates directly to students, faculty, and staff. Its structured category system makes it easy to filter relevant information.

Departmental Communication Hubs: Specific departments, faculties, or research groups within an organization can leverage EduNews as a private or public communication channel. This allows them to publish specialized news, project updates, research findings, and internal memos effectively to their members or broader audience.

Student Blogging/Journalism Platforms: EduNews can serve as a personal blogging platform or a collective journalism project for students. It provides a simple yet powerful tool for students to publish their articles, share their perspectives, and build their portfolios in a structured mobile environment.

Learning & Demonstration Project: For aspiring Android developers, EduNews stands as an excellent open-source codebase for learning and demonstration. It showcases practical implementation of core Android components, integrates deeply with multiple Firebase services (Authentication, Firestore, Storage), and demonstrates best practices in UI/UX design (like aspect ratio image handling and controlled dropdowns).

🚀 Local Setup Guide
Follow these detailed steps to set up and run the EduNews app on your local development environment.

Prerequisites
Before you begin, ensure you have the following installed:

Java Development Kit (JDK) 11 or higher: Essential for compiling Java Android projects.

Android Studio (latest stable version recommended): The official IDE for Android development.

A physical Android device or an Android Emulator: For deploying and testing the application.

Steps
Clone the Repository:
Start by cloning the EduNews GitHub repository to your local machine using Git:

git clone https://github.com/nethminkavindu/EduNews.git # IMPORTANT: Replace with your actual repository URL
cd EduNews

Open in Android Studio:

Launch Android Studio.

From the welcome screen or File menu, select Open or Open an Existing Project.

Navigate to the EduNews directory you just cloned (select the root project folder) and click OK or Open.

Android Studio will begin syncing the Gradle project. This process downloads all necessary dependencies and configures the project, which may take some time depending on your internet connection.

Create dropdown_item.xml (if missing):
The AutoCompleteTextView for category selection relies on a custom layout for its dropdown items. Ensure this file exists:

In Android Studio, navigate to app/src/main/res/layout/.

If dropdown_item.xml is not present, create a new XML layout file named dropdown_item.xml with the following content:

<!-- res/layout/dropdown_item.xml -->
<TextView xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:padding="16dp"
    android:textSize="16sp"
    android:textColor="@android:color/black"
    android:fontFamily="@font/montserrat" />

Add Required Drawables:
The application uses several drawable resources for icons and placeholders. Ensure these are present in your res/drawable folder. You can use your own custom icons or create simple placeholder XML vector drawables for initial testing:

back.xml (for the back navigation button)

edit.xml (for the edit post button)

cross.xml (for the cancel edit / clear image button)

add_icon.xml (for the image selection placeholder / icon)

facebook.xml (used as a default/error image in news activity – Highly Recommended: Replace this with a more generic news placeholder like news_placeholder.xml to avoid brand confusion.)

user.xml (for the default profile icon)

circular_background_grey.xml (a general circular grey background drawable)

image_placeholder_background.xml (a general background for image placeholders in list items)

default_profile_icon.xml (a specific default for the top bar profile icon, if different from user.xml)

Example user.xml (a simple vector drawable placeholder):

<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#FF000000"
        android:pathData="M12,12c2.21,0 4,-1.79 4,-4s-1.79,-4 -4,-4 -4,1.79 -4,4 1.79,4 4,4zM12,14c-2.67,0 -8,1.34 -8,4v2h16v-2c0,-2.66 -5.33,-4 -8,-4z"/>
</vector>

Build and Run:

Connect your physical Android device to your computer and enable USB debugging, or start an Android Emulator from the Android Studio AVD Manager.

Click on the green Run 'app' button (a triangle icon) located in Android Studio's toolbar. The app will be built and deployed to your selected device/emulator.

☁️ Firebase Setup & Security Rules
This project's entire backend infrastructure is powered by Google Firebase. To run the app successfully, you must set up your own Firebase project and configure its services.

1. Create a Firebase Project
Navigate to the Firebase Console in your web browser.

Click the Add project button.

Follow the on-screen instructions, providing a project name and, optionally, enabling Google Analytics (though not strictly necessary for this app's core functionality).

2. Add Android App to Firebase Project
Once your Firebase project is created, click on the Android icon ( "</>" ) on the project overview page to add an Android app.

Android package name: This is crucial. Enter your app's unique package name (e.g., com.uoc.fot.ict.edunews). You can locate this value in your project's app/build.gradle (Module :app) file, under the android block, as applicationId.

App nickname (optional): Provide a user-friendly name for your app in the Firebase console (e.g., "EduNews App").

SHA-1 debug signing certificate (Highly Recommended for Authentication):

This is essential for Firebase Authentication (especially for Google Sign-In, if you decide to implement it later) and Firebase Dynamic Links.

In Android Studio, open the Gradle panel (usually located on the right side of the IDE).

Expand app > Tasks > android.

Double-click on signingReport to execute the task.

In the Run window at the bottom of Android Studio, you will find the SHA1 hash for your debug variant. Copy this hash.

Paste the copied SHA-1 hash into the designated field in the Firebase console.

Download google-services.json:

After successfully registering your app, Firebase will prompt you to download the google-services.json configuration file.

Place this downloaded file directly into your Android project's app/ directory (e.g., YourProjectName/app/google-services.json). This file contains all your Firebase project's configuration details.

Add Firebase SDK to your app: The Firebase console will provide instructions on adding the necessary Firebase SDK dependencies to your project's build.gradle (Project) and build.gradle (Module :app) files. Follow these instructions carefully.

3. Enable Firebase Services
You must enable the core Firebase services used by this application within your Firebase project:

Cloud Firestore:

In the Firebase Console, navigate to the Build section and select Firestore Database.

Click the Create database button.

Choose a starting security mode:

Start in production mode (Recommended): This sets up strict security rules by default, preventing unauthorized access. You will then manually update these rules with the specific rules provided in the next step to allow the app's functionality. This is the most secure approach.

Start in test mode: This allows open read and write access for 30 days. While convenient for quick initial testing, it is highly insecure and should never be used for a deployed or production application.

Select a Cloud Firestore location that is geographically close to your target users for optimal performance (e.g., asia-east1 for Asia Pacific).

Cloud Storage:

In the Firebase Console, navigate to Build and select Storage.

Click the Get started button.

Follow the prompts to set up your default storage bucket.

You will review and update your security rules for Cloud Storage in the next step.

Authentication:

In the Firebase Console, navigate to Build and select Authentication.

Click the Get started button.

Go to the Sign-in method tab.

Enable the Email/Password provider. This is the primary authentication method used in the current version of the app. If you plan to extend the app with social logins, you would enable those providers here (e.g., Google, Facebook, Apple).

4. Firebase Security Rules
It is PARAMOUNT to configure your Firebase Security Rules correctly to safeguard your data and prevent unauthorized access or manipulation.

Go to Build > Firestore Database > Rules tab and replace the entire contents with the following rules:

rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Rules for the 'posts' collection: Manages news articles
    match /posts/{postId} {
      allow read: true; // All users (authenticated or not) can read any news post.
      allow create: if request.auth != null; // Only authenticated users are allowed to create new posts.
      // Only the original author (identified by userId in the document) can update or delete their own post.
      allow update, delete: if request.auth != null && request.auth.uid == resource.data.userId;
    }

    // Rules for the 'users' collection: Stores user profile data (e.g., profile picture URL)
    match /users/{userId} {
      allow read: if request.auth != null; // Authenticated users can read other user profiles (e.g., to display author info).
                                          // Adjust to `request.auth.uid == userId` if profiles should be private.
      allow create: if request.auth != null && request.auth.uid == userId; // A user can only create their own profile document upon registration.
      allow update: if request.auth != null && request.auth.uid == userId; // A user can only update their own profile document.
    }
  }
}

Next, go to Build > Storage > Rules tab and replace the entire contents with the following rules:

rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    // Global rule for all files in the bucket
    match /{allPaths=**} {
      // Allows anyone to read any file (e.g., article images, profile pictures).
      // If you want stricter privacy, change to `allow read: if request.auth != null;`
      allow read: true;
      // Only authenticated users can write (upload, overwrite) or delete files.
      // This prevents unauthorized users from uploading malicious files or deleting content.
      allow write: if request.auth != null;
    }

    // More granular rule for post images (optional but good for organization and specific rules)
    match /post_images/{imageId} {
      allow read: true; // Publicly readable news images.
      allow write: if request.auth != null; // Only authenticated users can manage (upload/delete) post images.
    }

    // More granular rule for user profile pictures (optional, if you have a specific folder)
    // This assumes profile pictures are stored directly under 'profile_pictures/{userId}/'
    match /profile_pictures/{userId}/{fileName} {
      allow read: true; // Profile pictures can be publicly viewed (e.g., in author bios).
      allow write: if request.auth != null && request.auth.uid == userId; // Only the logged-in user can upload/replace their own profile picture.
    }
  }
}

After updating your rules in the Firebase console, ensure you click the "Publish" button to make them active.

💡 Future Development & Contributions
EduNews provides a solid foundation for a comprehensive news and content management application. There are numerous exciting avenues for future development, and we welcome contributions from other developers! If you're passionate about building impactful applications and want to contribute to an open-source project, consider joining us.

Here are some key areas for expansion:

Social Logins Integration:

Google Sign-In: Allow users to easily register and log in using their existing Google accounts. This significantly reduces friction for new users.

Facebook Login: Provide an option for users to authenticate via Facebook, catering to a broader audience.

Apple Sign-In: Essential for a seamless experience on iOS devices (though this is an Android-only project currently, planning for cross-platform is good, or specifically for Android devices linked to Apple IDs).

How to Contribute: This involves integrating Firebase Authentication SDKs for these providers, designing the UI for login buttons, and handling the authentication callbacks in Java.

Dedicated Admin Panel:

Develop a separate web-based or Android-based admin panel for content moderators and administrators.

Features: This panel could include capabilities to:

View, edit, or delete any post (overriding standard user rules).

Manage user accounts (e.g., block users, reset passwords).

Approve/reject pending articles (if a submission workflow is implemented).

Manage categories (add, edit, delete categories).

View app analytics and user engagement.

How to Contribute: This is a substantial project that could involve new technologies like React/Angular/Vue.js for a web frontend, or another Android application. Firebase Cloud Functions could be used for secure administrative actions.

Notification System: Implement push notifications for breaking news, popular articles, or personalized updates.

Search Functionality: Add a robust search feature to allow users to find articles by keywords, author, or specific date ranges.

User Engagement Features:

Commenting system on articles.

Like/share functionality for articles.

Bookmark/save articles for later reading.

Enhanced User Profiles: Allow users to customize their profiles, add bios, and track their published articles.

UI/UX Enhancements: Continuous refinement of the user interface and experience based on user feedback and best practices.

Are you ready to make a difference and learn along the way?

We invite developers of all skill levels to contribute. If you're interested in tackling any of these features or have your own ideas, please:

Fork this repository.

Create a new branch for your feature or bug fix.

Implement your changes.

Submit a pull request detailing your contributions.

Let's build something amazing together!

Thank you for checking out EduNews!
