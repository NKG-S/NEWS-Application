
# EduNews App

EduNews is a robust, intuitive, and highly functional Android news and article management application, meticulously crafted and refined for the Faculty of Technology, University of Colombo. Designed with a keen understanding of the needs of both content creators and information consumers, this platform facilitates a seamless and secure workflow for viewing, creating, editing, and deleting news articles. Its foundation is built upon sophisticated image handling capabilities and a structured, easily navigable category-based organization system. Ultimately, EduNews aims to significantly streamline information dissemination within an academic or broader news environment, providing a rich, engaging, and efficient user experience across various Android devices.

**Developed by**: Nethmin Kavindu (Undergraduate Student, BICT Degree, Faculty of Technology, University of Colombo)

## 🌟 Features & Functionality

EduNews delivers a comprehensive suite of meticulously designed features to ensure a dynamic, secure, and highly efficient news management experience for all its users.

### User-Friendly News Feed
Upon launching the application, users are greeted with a beautifully designed, clean, and highly responsive news feed. This feed presents an infinitely scrollable list of news articles, ensuring continuous access to fresh content without manual refreshes. Each individual article item is thoughtfully designed to provide a concise yet informative overview, including a captivating headline, the precise publication date, a compelling snippet of the article's description, and a thumbnail image.

### Detailed Article View
Tapping on any news article presents the full article content, transforming the screen into an immersive and focused reading experience. This dedicated view supports high-quality, adaptable images that are dynamically adjusted to preserve their original aspect ratio, ensuring clarity and accessibility across a diverse range of Android devices and screen sizes.

### Article Creation
Empowering content creators is a core strength of EduNews. The application offers an intuitive interface for creating new articles. Users can easily enter a title, description, and select the most relevant category from a predefined list. A robust image upload mechanism allows for easy inclusion of visual content, making each article more engaging.

### Post Management (My Posts)
A personalized dashboard allows authenticated users to effortlessly manage their contributions. This dedicated section provides a clear overview of all articles they have published, facilitating easy access for review or further action.

### Intuitive Article Editing
Editing posts is a seamless experience, designed for maximum flexibility:
- **Dual Mode Interface**: Users can fluidly switch between a "View Mode" (for reading) and an "Edit Mode" (for modification) for any of their existing posts.
- **Comprehensive Content Modification**: In "Edit Mode," all core article components—title, description, and category—become fully editable, allowing for quick updates and corrections.
- **Advanced Image Handling**: The app provides intelligent image management. Users can easily replace an existing article image with a new one or opt to clear the image entirely. 
- **Refined Category Selection**: The category dropdown offers a smooth user experience during mode switching.

### Secure Article Deletion
Users have full control over their content, with the ability to permanently delete their own published articles. A clear confirmation prompt precedes any deletion, preventing accidental data loss. The deletion process is comprehensive, safely removing both the article's data from Cloud Firestore and its associated image from Firebase Storage.

### Firebase Integration
The backbone of EduNews is built upon Google Firebase, leveraging its powerful suite of cloud services for a scalable, secure, and real-time backend.

### User Profile Navigation
A readily accessible profile icon in the top navigation bar provides quick access to user-specific information.

## 🛠️ Technologies Used

This project is developed using a modern Android development stack, deeply integrated with the robust and scalable services offered by Google Firebase.

### Frontend Development:
- **Android (Java)**: The primary language for implementing core application logic, user interface components, and managing overall app flow.
- **Android Jetpack**: A suite of libraries to help developers follow best practices.
  - **AppCompatActivity**: For backward compatibility and consistent look across Android versions.
  - **ConstraintLayout**: A powerful layout manager for building complex and responsive UIs.
  - **RecyclerView**: Efficiently displays large, scrollable lists of data.
  - **Material Design Components**: Adhering to Google's Material Design guidelines for a consistent, intuitive, and visually appealing user experience.
  - **Glide**: Efficiently handles image fetching, caching, and display.

### Backend & Cloud Services (Google Firebase):
- **Firebase Authentication**: Provides secure and robust user authentication capabilities.
- **Cloud Firestore**: A NoSQL cloud database used for real-time data storage and synchronization.
- **Firebase Cloud Storage**: For securely storing media files like images.

## 🎯 Suitable Use Cases

EduNews offers a versatile foundation adaptable to various scenarios requiring content publishing and consumption:

- **University/College News Portals**: Ideal for educational institutions to disseminate important announcements and events.
- **Departmental Communication Hubs**: Used within specific departments for publishing news and updates.
- **Student Blogging/Journalism Platforms**: A platform for students to publish articles, share perspectives, and build portfolios.
- **Learning & Demonstration Project**: Great for aspiring Android developers to learn and demonstrate Firebase integration.

## 🚀 Local Setup Guide

Follow these detailed steps to set up and run the EduNews app on your local development environment.

### Prerequisites:
Before you begin, ensure you have the following installed:
- **Java Development Kit (JDK) 11 or higher**
- **Android Studio (latest stable version recommended)**
- **A physical Android device or an Android Emulator**

### Steps:

1. **Clone the Repository**:
   ```bash
   git clone https://github.com/nethminkavindu/EduNews.git
   cd EduNews
   ```

2. **Open in Android Studio**:
   - Launch Android Studio and open the EduNews directory.

3. **Create dropdown_item.xml (if missing)**:
   Ensure the file `dropdown_item.xml` exists in the `res/layout` directory. If not, create it.

4. **Add Required Drawables**:
   Ensure all drawable resources for icons and placeholders are present in your `res/drawable` folder.

5. **Build and Run**:
   Connect your Android device or use the Android Emulator, and click on the "Run" button in Android Studio.

## ☁️ Firebase Setup & Security Rules

### Firebase Project Setup:
1. Create a Firebase project and add your Android app.
2. Add the `google-services.json` configuration file to your project.
3. Enable necessary Firebase services like **Cloud Firestore** and **Firebase Storage**.
4. Set Firebase security rules for **Firestore** and **Firebase Storage** to ensure proper access control.

### Firebase Security Rules:
Here are the security rules for both Firestore and Storage:

**Firestore Rules:**
```plaintext
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /posts/{postId} {
      allow read: true;
      allow create: if request.auth != null;
      allow update, delete: if request.auth != null && request.auth.uid == resource.data.userId;
    }
    match /users/{userId} {
      allow read: if request.auth != null;
      allow create: if request.auth != null && request.auth.uid == userId;
      allow update: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

**Storage Rules:**
```plaintext
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /{allPaths=**} {
      allow read: true;
      allow write: if request.auth != null;
    }
    match /post_images/{imageId} {
      allow read: true;
      allow write: if request.auth != null;
    }
    match /profile_pictures/{userId}/{fileName} {
      allow read: true;
      allow write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

### Publishing the Rules:
Ensure you click the "Publish" button in the Firebase console after updating your rules.

## 💡 Future Development & Contributions

EduNews provides a solid foundation for a comprehensive news and content management application. Contributions from developers are welcome! Some potential areas for future development include:

- **Social Logins Integration** (Google, Facebook, Apple)
- **Dedicated Admin Panel** for content moderation and user management
- **Push Notifications** for breaking news and personalized updates
- **Search Functionality** to find articles by keywords or author
- **User Engagement Features** like comments, likes, and bookmarks
- **Enhanced User Profiles** for tracking published articles and adding bios

### How to Contribute:
1. Fork this repository.
2. Create a new branch for your feature or bug fix.
3. Implement your changes and submit a pull request.

Let's build something amazing together! Thank you for checking out EduNews! ✨
