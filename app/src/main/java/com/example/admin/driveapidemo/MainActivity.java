package com.example.admin.driveapidemo;

import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.Image;
import android.media.MediaPlayer;
import android.net.Uri;
import android.net.rtp.AudioStream;
import android.nfc.Tag;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.drive.CreateFileActivityOptions;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveClient;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.DriveResourceClient;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.OpenFileActivityOptions;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;
import com.google.android.gms.drive.widget.DataBufferAdapter;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URL;

import static java.io.File.createTempFile;

public class MainActivity extends AppCompatActivity {
    com.google.android.gms.common.SignInButton btnSignIn;
    GoogleSignInOptions gso;
    GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 100;
    private static final int REQUEST_CODE_CREATE_FILE = 200;
    private static final int REQUEST_CODE_OPEN_ITEM = 300;
    public static String extra_link = "link";
    public static String extra_title = "title";
    private TaskCompletionSource<DriveId> mOpenItemTaskSource;
    TextView tvTextFile;
    ImageView imgView;
    DriveResourceClient mDriveResourceClient;
    DriveClient mDriveClient;
    DriveFolder mDriveFolder;
    Metadata mMetadata;
    //private DataBufferAdapter<Metadata> mResultsAdapter;
    //ListView mListView = findViewById(R.id.listViewResults);
    String TAG = "OnActivity ";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SignInButton signInButton = (SignInButton) findViewById(R.id.sign_in_button_gg);
        signInButton.setSize(SignInButton.SIZE_STANDARD);
        tvTextFile = (TextView)findViewById(R.id.tvTextFile);
        imgView = (ImageView)findViewById(R.id.imgView);
        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                //.requestScopes(new Scope(Scopes.DRIVE_APPFOLDER))
                .requestScopes(Drive.SCOPE_FILE)
                .requestScopes(Drive.SCOPE_APPFOLDER)
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(getApplicationContext(), gso);
        //TEST
        //mGoogleSignInClient = buildGoogleSignInClient();
        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signIn();
                //mResultsAdapter = new ResultsAdapter(getApplicationContext());
                //mListView.setAdapter(mResultsAdapter);
            }
        });
    }
    protected void onStart()
    {
        super.onStart();
        //GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        //mDriveClient = Drive.getDriveClient(getApplicationContext(),account);
        //mDriveResourceClient = Drive.getDriveResourceClient(getApplicationContext(),account);
    }
    //First Call When Sign In
    private void signIn() {
        final Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);

    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case RC_SIGN_IN:
                Log.i(TAG, "Result");
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                handleSignInResult(task);
                break;
            case REQUEST_CODE_OPEN_ITEM:
                if (resultCode == RESULT_OK) {
                    DriveId driveId = data.getParcelableExtra(
                            OpenFileActivityOptions.EXTRA_RESPONSE_DRIVE_ID);
                    mOpenItemTaskSource.setResult(driveId);
                }
        }
        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Log.i(TAG, "Result");
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }
    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            Log.w(TAG,"sigInResult: Success");
            mDriveClient = Drive.getDriveClient(getApplicationContext(), account);
            mDriveResourceClient = Drive.getDriveResourceClient(getApplicationContext(), account);
        } catch (ApiException e) {
            Log.w(TAG, "signInResult:failed code=" + e.getStatusCode());
            finish();
        }
    }
    private void getFiles(GoogleSignInAccount account)
    {
        //Build a drive client.
        mDriveClient = Drive.getDriveClient(getApplicationContext(), account);

        mDriveResourceClient = Drive.getDriveResourceClient(getApplicationContext(), account);
//        Task<DriveFolder> driveFolderTask = mDriveResourceClient.getRootFolder();
//        driveFolderTask.addOnSuccessListener(this,new OnSuccessListener<DriveFolder>() {
//            @Override
//            public void onSuccess(DriveFolder driveFolder) {
//                // Handle results...
//                mDriveFolder = driveFolder;
//                Log.w(TAG,"onSuccess retrieving root folder ");
//            }
//        } ).addOnFailureListener(this, new OnFailureListener() {
//            @Override
//            public void onFailure(@NonNull Exception e) {
//                // Handle failure...
//                Log.e(TAG, "Error retrieving root folder", e);
//                int b;
//                finish();
//            }
//        });
//        //Build query files
//        Query query = new Query.Builder()
//                .addFilter(Filters.eq(SearchableField.TITLE,"filetext"))
//                .build();
//        mDriveClient.requestSync();
//        // [START query_files]
//        Task<MetadataBuffer> queryTask = mDriveResourceClient.query(query);
//        // [END query_files]
//        // [START query_results]
//        queryTask
//                .addOnSuccessListener(this,
//                        new OnSuccessListener<MetadataBuffer>() {
//                            @Override
//                            public void onSuccess(MetadataBuffer metadataBuffer) {
//                                // Handle results...
//                                Log.w(TAG,"onSuccess retrieving files "+ metadataBuffer.getCount());
//                                //metadataBuffer.get(1).getWebViewLink();
//                                metadataBuffer.release();
//                            }
//                        })
//                .addOnFailureListener(this, new OnFailureListener() {
//                    @Override
//                    public void onFailure(@NonNull Exception e) {
//                        // Handle failure...
//                        Log.e(TAG, "Error retrieving files", e);
//                        int b;
//                        //finish();
//                    }
//                });
    }
    //Event SignOut
    public void onClickSignOut(View view) {
        SignOut();
    }
    private void SignOut() {
        mGoogleSignInClient.signOut().addOnCompleteListener(this, new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                Log.w(TAG, "SignOut Success!");
            }
        });
    }
    //Event Upload
    public void onClickUpload(View view) {
        createFiles();
    }
    private void createFiles() {
        Task<DriveContents> createContentsTask = mDriveResourceClient.createContents();
        createContentsTask
                .continueWithTask(new Continuation<DriveContents, Task<IntentSender>>() {
                    @Override
                    public Task<IntentSender> then(@NonNull Task<DriveContents> task)
                            throws Exception {
                        DriveContents contents = task.getResult();
                        OutputStream outputStream = contents.getOutputStream();
                        try (Writer writer = new OutputStreamWriter(outputStream)) {
                            writer.write("Hello World!");
                        }

                        MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                                .setTitle("New file")
                                .setMimeType("text/plain")
                                .setStarred(true)
                                .build();

                        CreateFileActivityOptions createOptions =
                                new CreateFileActivityOptions.Builder()
                                        .setInitialDriveContents(contents)
                                        .setInitialMetadata(changeSet)
                                        .build();
                        return mDriveClient.newCreateFileActivityIntentSender(createOptions);
                    }
                })
                .addOnSuccessListener(this,
                        new OnSuccessListener<IntentSender>() {
                            @Override
                            public void onSuccess(IntentSender intentSender) {
                                try {
                                    startIntentSenderForResult(
                                            intentSender, REQUEST_CODE_CREATE_FILE, null, 0, 0, 0);
                                } catch (IntentSender.SendIntentException e) {
                                    Log.e(TAG, "Unable to create file", e);
                                    //showMessage(getString(R.string.file_create_error));
                                    finish();
                                }
                            }
                        })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Unable to create file", e);
                        //showMessage(getString(R.string.file_create_error));
                        finish();
                    }
                });
    }
    //Event Open
    public void onClickOpen(View view) {
        pickFile()
                .addOnSuccessListener(this,
                        new OnSuccessListener<DriveId>() {
                            @Override
                            public void onSuccess(DriveId driveId) {
                                getMetadata(driveId.asDriveFile());
                                openFiles(driveId.asDriveFile());
                            }
                        })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "No file selected", e);
                        //showMessage(getString(R.string.file_not_selected));
                        finish();
                    }
                });
    }
    private void openFiles(final DriveFile file)
    {
        //Log.w("Link WebView",mDriveResourceClient.getMetadata(file).getResult().getWebViewLink());
        // [START open_file]
        //mDriveResourceClient.getMetadata(file).getResult().getWebViewLink();
        Task<DriveContents> openFileTask =
                mDriveResourceClient.openFile(file, DriveFile.MODE_READ_ONLY);
        // [END open_file]
        // [START read_contents]
        openFileTask
                .continueWithTask(new Continuation<DriveContents, Task<Void>>() {
                    @Override
                    public Task<Void> then(@NonNull Task<DriveContents> task) throws Exception {

                        //Get inputStream here
                        DriveContents contents = task.getResult();
                        InputStream inputStream =  contents.getInputStream();
                        String mimetype = mMetadata.getMimeType().toString();

                        if(mimetype.equals("audio/mp3") ) {
                            try {
                                File tempFile = createTempFile("tempFile", ".dat", getDir("filez", 0));
                                FileOutputStream out = new FileOutputStream(tempFile);
                                byte buffer[] = new byte[16384];
                                int length = 0;
                                while ((length = inputStream.read(buffer)) != -1) {
                                    out.write(buffer, 0, length);
                                }
                                out.close();
                                String path = tempFile.getAbsolutePath();
                                MediaPlayer mp = new MediaPlayer();
                                mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
                                mp.setDataSource(path);
                                mp.prepare();
                                mp.start();
                            } catch (Exception e) {
                            }
                        }
                        else if((mimetype.equals("image/jpeg"))||(mimetype.equals("image/png"))) {
                            try {
                                Bitmap img = BitmapFactory.decodeStream(inputStream);
                                imgView.setVisibility(View.VISIBLE);
                                imgView.setImageBitmap(img);
                            } catch (Exception e) {
                            }
                        }
                        else
                        // Process contents...
                        // [START_EXCLUDE]
                        // [START read_as_string]
                        {
                            try (BufferedReader reader = new BufferedReader(
                                    new InputStreamReader(inputStream))) {
                                StringBuilder builder = new StringBuilder();
                                String line;
                                while ((line = reader.readLine()) != null) {
                                    builder.append(line).append("\n");
                                }
                                // showMessage(getString(R.string.content_loaded));
                                tvTextFile.setVisibility(View.VISIBLE);
                                tvTextFile.setText(builder.toString());
                                //finish();
                            }
                        }
                        // [END read_as_string]
                        // [END_EXCLUDE]
                        // [START discard_contents]
                        Task<Void> discardTask = mDriveResourceClient.discardContents(contents);
                        // [END discard_contents]
                        return discardTask;
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Handle failure
                        // [START_EXCLUDE]
                        Log.e(TAG, "Unable to read contents", e);
                        //showMessage(getString(R.string.read_failed));
                        finish();
                        // [END_EXCLUDE]
                    }
                });
        // [END read_contents]
    }

    private void getMetadata(final DriveFile file)
    {
        Task<Metadata> getMetadataTask = mDriveResourceClient.getMetadata(file);

        getMetadataTask
                .addOnSuccessListener(this,
                        new OnSuccessListener<Metadata>() {
                            @Override
                            public void onSuccess(Metadata metadata) {
                                mMetadata = metadata;
                                String link = metadata.getEmbedLink();
                                String title = metadata.getTitle();
                                String mimeType = metadata.getMimeType();

//                                if(mimeType=="image/png"||mimeType=="image/jpeg") {
//                                    viewImage(link,title);
//                                }
                                if(mimeType=="audio/mp3")
                                    playMusic(link,title);
//                                if(mimeType=="text/plain"||mimeType=="application/vnd.google-apps.document")
//                                    openFiles(file);
                                Log.w("Link", link);
                                Log.w("MimeType", mimeType);
                                //finish();
                            }
                        })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Unable to retrieve metadata", e);
                        //showMessage(getString(R.string.read_failed));
                        //finish();
                    }
                });
    }
    //Event Delete
    public void onClickDelete(View view) {
        pickFile().addOnSuccessListener(this,
                new OnSuccessListener<DriveId>() {
                    @Override
                    public void onSuccess(DriveId driveId) {
                        deleteFile(driveId.asDriveFile());
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "No file selected", e);
                        //showMessage(getString(R.string.file_not_selected));
                        finish();
                    }
                });
    }
    private void deleteFile(DriveFile file) {
        // [START delete_file]
        mDriveResourceClient
                .delete(file)
                .addOnSuccessListener(this,
                        new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                //showMessage(getString(R.string.file_deleted));
                                Log.w(TAG,"Deleted file");
                                Toast.makeText(MainActivity.this, "Deleted", Toast.LENGTH_SHORT).show();
                                //finish();
                            }
                        })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Unable to delete file", e);
                        //showMessage(getString(R.string.delete_failed));
                        finish();
                    }
                });
        // [END delete_file]
    }
    //Call when Selected A File

    public void viewImage(String link, String title)
    {
//        webView.setVisibility(View.VISIBLE);
//        webView.getSettings().setJavaScriptEnabled(true);
//        webView.loadUrl(link);
//        Uri uri = Uri.parse(link);
//        Intent intent = new Intent(Intent.ACTION_VIEW,uri);
//        mDriveClient.requestSync();
//        startActivity(intent);
//        try {
//            URL url = new URL(link);
//            Bitmap img = BitmapFactory.decodeStream(url.openConnection().getInputStream());
//            imgView.setImageBitmap(img);
//        }
//        catch (Exception e)
//        {
//
//        }
    }
    private void playMusic(String link, String title)
    {
        MediaPlayer mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {

            mediaPlayer.setDataSource(link);
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mp.start();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //Event Selected File
    private Task<DriveId> pickItem(OpenFileActivityOptions openOptions) {
        mOpenItemTaskSource = new TaskCompletionSource<>();
        mDriveClient
                .newOpenFileActivityIntentSender(openOptions)
                .continueWith(new Continuation<IntentSender, Void>() {
                    @Override
                    public Void then(@NonNull Task<IntentSender> task) throws Exception {
                        startIntentSenderForResult(
                                task.getResult(), REQUEST_CODE_OPEN_ITEM, null, 0, 0, 0);
                        return null;
                    }
                });
        return mOpenItemTaskSource.getTask();
    }
    protected Task<DriveId> pickFile() {
        OpenFileActivityOptions openOptions =
                new OpenFileActivityOptions.Builder()
                        .setSelectionFilter(Filters.or(Filters.eq(SearchableField.MIME_TYPE, "image/jpeg"),
                                Filters.eq(SearchableField.MIME_TYPE,"text/plain"),
                                Filters.eq(SearchableField.MIME_TYPE,"application/vnd.google-apps.document"),
                                Filters.eq(SearchableField.MIME_TYPE,"audio/mp3"),
                                Filters.eq(SearchableField.MIME_TYPE,"image/png")))
                        .setActivityTitle(getString(R.string.select_file))
                        .build();
        return pickItem(openOptions);
    }






}
