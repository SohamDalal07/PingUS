# AWS S3 Integration Setup Guide

## Overview
This guide walks you through setting up AWS S3 for image storage in your UniFind (BackToYou) Android app.

## Prerequisites
- AWS Account (create one at https://aws.amazon.com)
- AWS S3 bucket created
- IAM user with S3 permissions

## Step 1: Create an AWS S3 Bucket

1. **Go to AWS Console**: https://console.aws.amazon.com/s3
2. **Create a New Bucket**:
   - Bucket name: `unifind-images` (or your preferred name)
   - Region: Choose your region (e.g., `us-east-1`)
   - Block Public Access settings: Keep as default for now (we'll update later if needed)
   - Click "Create bucket"

3. **Enable CORS (if serving images publicly)**:
   - Select your bucket
   - Go to **Permissions** tab
   - Scroll to **Cross-origin resource sharing (CORS)**
   - Add the following JSON:
   ```json
   [
       {
           "AllowedHeaders": ["*"],
           "AllowedMethods": ["GET", "PUT", "POST", "DELETE"],
           "AllowedOrigins": ["*"],
           "ExposeHeaders": [],
           "MaxAgeSeconds": 3000
       }
   ]
   ```

## Step 2: Create an IAM User with S3 Permissions

1. **Go to IAM Console**: https://console.aws.amazon.com/iam
2. **Create a New User**:
   - Click **Users** → **Create user**
   - Username: `unifind-app-user`
   - Uncheck "Provide user access to the AWS Management Console"
   - Click **Next**

3. **Attach Permissions**:
   - Click **Attach policies directly**
   - Search and attach: `AmazonS3FullAccess` (or create a custom policy for your bucket)
   - Click **Next** → **Create user**

4. **Generate Access Keys**:
   - Click on the newly created user
   - Go to **Security credentials** tab
   - Click **Create access key**
   - Choose "Application running outside AWS"
   - Click **Next** → **Create access key**
   - **IMPORTANT**: Copy your Access Key ID and Secret Access Key

## Step 3: Update AwsS3Config.java

Open `app/src/main/java/com/example/backtoyou/aws/AwsS3Config.java` and replace:

```java
public static final String AWS_ACCESS_KEY = "YOUR_AWS_ACCESS_KEY";
public static final String AWS_SECRET_KEY = "YOUR_AWS_SECRET_KEY";
public static final String AWS_REGION = "us-east-1";
public static final String S3_BUCKET_NAME = "unifind-images";
```

With your actual AWS credentials and bucket details.

⚠️ **SECURITY WARNING**: Never commit real credentials to version control!

### For Production:
Use environment variables or AWS Secrets Manager:

```java
// Instead of hardcoding, load from environment:
public static final String AWS_ACCESS_KEY = System.getenv("AWS_ACCESS_KEY");
public static final String AWS_SECRET_KEY = System.getenv("AWS_SECRET_KEY");
```

Or use AWS Cognito for temporary credentials (recommended).

## Step 4: Add Permissions to AndroidManifest.xml

The following permissions are already included or need to be added:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.CAMERA" />
```

## Step 5: Using AWS S3 Service in PostActivity

The `AwsS3Service` class handles all S3 operations. Here's how to use it:

### Initialize Service:
```java
private AwsS3Service s3Service;

@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_post);
    
    s3Service = new AwsS3Service(this);
}
```

### Upload Image:
```java
// After user selects an image
Uri imageUri = selectedImageUri;

s3Service.uploadImageFromUriAsync(imageUri, new AwsS3Service.UploadProgressListener() {
    @Override
    public void onProgress(long bytesUploaded, long totalBytes) {
        Log.d("Upload", "Progress: " + bytesUploaded + "/" + totalBytes);
    }

    @Override
    public void onSuccess(String s3Url) {
        Log.i("Upload", "Success: " + s3Url);
        photoUrl = s3Url; // Store URL in Firestore
        Toast.makeText(PostActivity.this, "Image uploaded!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onError(String errorMessage) {
        Log.e("Upload", "Error: " + errorMessage);
        Toast.makeText(PostActivity.this, "Upload failed: " + errorMessage, Toast.LENGTH_SHORT).show();
    }
});
```

### In Firestore:
```java
// When posting the item
item.put("photoUrl", s3Url); // Use the S3 URL from upload success
```

### Cleanup:
```java
@Override
protected void onDestroy() {
    super.onDestroy();
    if (s3Service != null) {
        s3Service.shutdown();
    }
}
```

## Step 6: Build & Test

1. **Sync Gradle**:
   ```bash
   ./gradlew build
   ```

2. **Run the app**:
   - Build and run on emulator/device
   - Navigate to PostActivity
   - Try uploading an image

3. **Monitor logs**:
   ```
   adb logcat | grep "AwsS3Service"
   ```

## Troubleshooting

### "AWS S3 is NOT configured"
- Check that you've updated `AwsS3Config.java` with real credentials
- Verify bucket name matches

### "No connected devices"
- Start an Android emulator or connect a physical device
- Enable USB debugging on device

### Upload fails with 403 error
- Check IAM user permissions (should have S3 access)
- Verify bucket exists and is accessible
- Check bucket CORS settings

### "File not found"
- Ensure image file path is correct
- Check app has READ_EXTERNAL_STORAGE permission

## Security Best Practices

1. ❌ **Don't**: Hardcode AWS credentials in production
2. ✅ **Do**: Use AWS Cognito for temporary credentials
3. ✅ **Do**: Use IAM roles with minimal permissions
4. ✅ **Do**: Enable encryption on S3 bucket
5. ✅ **Do**: Enable versioning and lifecycle policies

## AWS Cognito Setup (Recommended for Production)

Use temporary credentials instead of hardcoding:

```java
// Use AWS Cognito Identity for temporary credentials
CognitoCredentialsProvider credentialsProvider = new CognitoCredentialsProvider(
    "IDENTITY_POOL_ID",
    "REGION"
);

AWSCredentials credentials = credentialsProvider.getCredentials();
```

## References

- [AWS S3 Documentation](https://docs.aws.amazon.com/s3/)
- [AWS SDK for Java v2](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/)
- [Android Permissions](https://developer.android.com/training/permissions)
- [AWS Cognito](https://aws.amazon.com/cognito/)

## Support

For issues or questions:
1. Check CloudWatch logs in AWS Console
2. Review S3 bucket permissions
3. Verify IAM user has correct policies
4. Check Android app logs: `adb logcat | grep -i "s3\|aws"`

