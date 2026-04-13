package com.example.backtoyou;
import android.util.Log;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
public class ImageStorageConfig {
    private static final String IMAGE_STORAGE_APP_NAME = "ImageStorageApp";
    private static FirebaseApp imageStorageApp;
    private static FirebaseStorage imageStorage;
    private static StorageReference imageStorageReference;
    public static void initializeImageStorage(String projectId, String apiKey, String appId, String storageBucket) {
        try {
            FirebaseOptions imageStorageOptions = new FirebaseOptions.Builder()
                    .setProjectId(projectId)
                    .setApplicationId(appId)
                    .setApiKey(apiKey)
                    .setStorageBucket(storageBucket)
                    .build();
            imageStorageApp = FirebaseApp.initializeApp(imageStorageOptions, IMAGE_STORAGE_APP_NAME);
            imageStorage = FirebaseStorage.getInstance(imageStorageApp);
            imageStorageReference = imageStorage.getReference().child("images");
            Log.d("ImageStorage", "✅ Image Storage initialized");
            Log.d("ImageStorage", "Bucket: " + imageStorage.getReference().getBucket());
        } catch (IllegalStateException e) {
            try {
                imageStorageApp = FirebaseApp.getInstance(IMAGE_STORAGE_APP_NAME);
                imageStorage = FirebaseStorage.getInstance(imageStorageApp);
                imageStorageReference = imageStorage.getReference().child("images");
                Log.d("ImageStorage", "⚠️ Image Storage already initialized");
            } catch (Exception ex) {
                Log.e("ImageStorage", "❌ Failed to get Image Storage", ex);
            }
        } catch (Exception e) {
            Log.e("ImageStorage", "❌ Failed to initialize Image Storage", e);
        }
    }
    public static FirebaseStorage getImageStorage() {
        if (imageStorage == null) {
            throw new IllegalStateException("Image Storage not initialized");
        }
        return imageStorage;
    }
    public static StorageReference getImageStorageReference() {
        if (imageStorageReference == null) {
            throw new IllegalStateException("Image Storage Reference not initialized");
        }
        return imageStorageReference;
    }
}