package solveast.slide;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.ProgressListener;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AppKeyPair;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Date;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FileChooser extends AppCompatActivity {

    private static final String[] AVAILABLE_IMG_EXT = new String[]{"jpg", "gif", "png", "bmp", "webp"};

    final static private String DROPBOX_APP_KEY = "c6fmg0pcsrrom3h";
    final static private String DROPBOX_APP_SECRET = "oaiegitwz72d114";

    private DropboxAPI<AndroidAuthSession> mDBApi;
    private String dropboxName;
    private String dropboxPath;

    private File currentDir;
    private FileArrayAdapter adapter;

    private String sourceType;
    private boolean dirContainsImage;

    private Entry dirent;
    private FileOutputStream mFos;

    private boolean mCanceled;
    //private Long mFileLen;
    private String mErrorMsg;

    private List<String> thumbCachePaths = new ArrayList<>();
    private String dropboxCacheDirPath = "";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.file_chooser);
        Toolbar toolbar = (Toolbar) findViewById(R.id.tool_bar);
        setSupportActionBar(toolbar);

        sourceType = getIntent().getAction();

        switch (sourceType) {
            case "internal":
                currentDir = new File("/");
                fill(currentDir);
                break;
            case "external":
                currentDir = new File(Environment.getExternalStorageDirectory().getPath());
                fill(currentDir);
                break;
            case "dropbox":

                AppKeyPair appKeys = new AppKeyPair(DROPBOX_APP_KEY, DROPBOX_APP_SECRET);
                AndroidAuthSession session = new AndroidAuthSession(appKeys);
                mDBApi = new DropboxAPI<>(session);

                String accessToken = getToken();

                if (!accessToken.isEmpty()) {
                    // Set token if already authenticated
                    mDBApi.getSession().setOAuth2AccessToken(accessToken);
                    new GetFilesAndThumbs().execute("/");
                } else {
                    // Start browser to authenticate by email/password
                    mDBApi.getSession().startOAuth2Authentication(FileChooser.this);
                }
                break;
        }

        dirContainsImage = false;

    }

    private void fill(File f) {
        File[] dirs = f.listFiles();
        this.setTitle("Dir : " + f.getName());
        List<Item> dir = new ArrayList<>();
        List<Item> fls = new ArrayList<>();

        if (dirs != null) {
            for (File ff : dirs) {
                Date lastModDate = new Date(ff.lastModified());
                DateFormat formatter = DateFormat.getDateTimeInstance();
                String date_modify = formatter.format(lastModDate);
                if (ff.isDirectory()) {

                    File[] fbuf = ff.listFiles();
                    int buf;
                    if (fbuf != null) {
                        buf = fbuf.length;
                    } else buf = 0;
                    String num_item = String.valueOf(buf);
                    if (buf <= 1) num_item = num_item + " item";
                    else num_item = num_item + " items";

                    dir.add(new Item(ff.getName(), num_item, date_modify, ff.getAbsolutePath(), R.drawable.directory_icon));
                } else {
                    String fName = ff.getName();
                    String extension = fName.substring(fName.lastIndexOf(".") + 1);
                    if (Arrays.asList(AVAILABLE_IMG_EXT).contains(extension.toLowerCase())) {
                        fls.add(new Item(ff.getName(), ff.length() + " Byte", date_modify, ff.getAbsolutePath(), R.drawable.file_image));
                        dirContainsImage = true;
                    } else {
                        fls.add(new Item(ff.getName(), ff.length() + " Byte", date_modify, ff.getAbsolutePath(), R.drawable.file_icon));
                    }
                }
            }
        } else {
            Log.e("FileChooser Activity", "null dir");
        }

        Collections.sort(dir);
        Collections.sort(fls);
        dir.addAll(fls);

        if (!currentDir.getAbsolutePath().equals(Environment.getExternalStorageDirectory().getAbsolutePath()) && !currentDir.getAbsolutePath().equals("/")){
            dir.add(0, new Item("..", "Parent Directory", "", f.getParent(), R.drawable.directory_up));
        }

        adapter = new FileArrayAdapter(FileChooser.this, R.layout.file_view, dir);
        ListView lv = (ListView) findViewById(R.id.list_view);
        lv.setAdapter(adapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                //Toast.makeText(FileChooser.this, i+ " Folder Clicked: " + currentDir, Toast.LENGTH_SHORT).show();
                Item o = adapter.getItem(position);
                if (o.getImage() == R.drawable.directory_icon || o.getImage() == R.drawable.directory_up) {
                    currentDir = new File(o.getPath());
                    fill(currentDir);
                } else {
                    onFileClick();
                }
            }
        });
    }
    private void fillDropbox() {
        this.setTitle("Dir : " + dirent.fileName());
        dropboxPath = dirent.path;
        List<Item> dir = new ArrayList<>();
        List<Item> fls = new ArrayList<>();

        for (Entry ent : dirent.contents) {
            String modified = ent.modified;
            modified = modified.substring(modified.indexOf(", ") + 2, modified.indexOf(" +"));
            if (ent.isDir) {
                dir.add(new Item(ent.fileName(), "", modified, ent.path, R.drawable.directory_icon));
            } else if (ent.thumbExists) {
                if (thumbCachePaths.isEmpty()) {
                    fls.add(new Item(ent.fileName(), ent.bytes + " Byte", modified, ent.path, R.drawable.file_image));
                } else {
                    fls.add(new Item(ent.fileName(), ent.bytes + " Byte", modified, ent.path, thumbCachePaths.get(0)));
                    thumbCachePaths.remove(0);
                }
                dirContainsImage = true;
            } else {
                fls.add(new Item(ent.fileName(), ent.bytes + " Byte", modified, ent.path, R.drawable.file_icon));
            }

        }

        Collections.sort(dir);
        Collections.sort(fls);
        dir.addAll(fls);

        if (!dirent.parentPath().isEmpty()){
            dir.add(0, new Item("..", "Parent Directory", "", dirent.parentPath(), R.drawable.directory_up));
        }
        adapter = new FileArrayAdapter(FileChooser.this, R.layout.file_view, dir);
        ListView lv = (ListView) findViewById(R.id.list_view);
        lv.setAdapter(adapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                Item o = adapter.getItem(position);
                if (o.getImage() == R.drawable.directory_icon || o.getImage() == R.drawable.directory_up) {
                    new GetFilesAndThumbs().execute(o.getPath());

                } else {
                    onFileClick();
                }
            }
        });
    }
    private static void cleanDropboxCacheDir(File dir) {
        try {
            if (dir != null && dir.isDirectory()) {
                for(File file: dir.listFiles()) {
                    if (!file.delete()) {
                        Log.e("FileChooser Activity", file.getPath() + " couldnt be deleted");
                    }
                }
            }
        } catch (Exception e) {
            Log.e("FileChooser Activity", e.getLocalizedMessage());
        }
    }


    private void onFileClick() {
        showToast("You can select this folder in toolbar!");
        View select = findViewById(R.id.action_select);
        select.animate().rotationYBy(360).setDuration(1000);
    }


    /**
     * Save the access token not to re-authenticate next time
     * @param token Token to be stored
     */
    private void storeToken(String token) {
        SharedPreferences prefs = getSharedPreferences("dropbox_prefs", MODE_PRIVATE);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString("dropbox_access_token", token);
        edit.apply();
    }

    /**
     * Get access token
     * @return Access token
     */
    private String getToken() {
        SharedPreferences prefs = getSharedPreferences("dropbox_prefs", MODE_PRIVATE);
        return prefs.getString("dropbox_access_token", "");
    }

    private void showToast(String msg) {
        Toast toast = Toast.makeText(FileChooser.this, msg, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.TOP, 0, 100);
        toast.show();
    }



    @Override
    protected void onResume() {
        super.onResume();

        if (sourceType.equals("dropbox")) {
            if (mDBApi != null && mDBApi.getSession().authenticationSuccessful()) {
                try {
                    // Required to complete auth, sets the access token on the session
                    mDBApi.getSession().finishAuthentication();

                    String accessToken = mDBApi.getSession().getOAuth2AccessToken();
                    storeToken(accessToken);
                    new GetFilesAndThumbs().execute("/");
                } catch (IllegalStateException e) {
                    Log.i("DbAuthLog", "Error authenticating", e);
                }
            }
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.file_choser_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_select) {
            if (dirContainsImage) {
                if (sourceType.equals("dropbox")) {
                    new DownloadImages().execute();
                } else {
                    Intent intent = new Intent();
                    intent.putExtra("source_type", sourceType);
                    intent.putExtra("image_source_path", currentDir.getPath());
                    setResult(RESULT_OK, intent);

                    finish();
                }
            } else {
                showToast("This folder doesn't contain any image file");
            }
            return true;
        } else if (id == R.id.action_cancel) {
            Intent intent = new Intent();
            if (sourceType.equals("dropbox")) {
                // Delete token to reset authentication
                storeToken("");
                intent.putExtra("dropbox_cache_path", "");
            } else {
                intent.putExtra("source_type", sourceType);
                intent.putExtra("image_source_path", "");
            }
            setResult(RESULT_OK, intent);

            finish();
        }
        return super.onOptionsItemSelected(item);
    }


    private class GetFilesAndThumbs extends AsyncTask<String , Void, Boolean> {
        ProgressDialog pdLoading = new ProgressDialog(FileChooser.this);

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pdLoading.setMessage("Loading files and thumbnails...");
            pdLoading.setButton(ProgressDialog.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    mCanceled = true;
                    mErrorMsg = "Canceled";

                    // This will cancel the getThumbnail operation by closing
                    // its stream
                    if (mFos != null) {
                        try {
                            mFos.close();
                        } catch (IOException e) {
                            Log.e("FileChooser Activity", e.getLocalizedMessage());
                        }
                    }
                }
            });
            pdLoading.show();
        }
        @Override
        protected Boolean doInBackground(String... params) {
            try {
                dirent = mDBApi.metadata(params[0], 1000, null, true, null);
                dropboxName = mDBApi.accountInfo().displayName;
                for (Entry ent : dirent.contents) {
                    if (ent.thumbExists) {
                        String cachePath = FileChooser.this.getCacheDir().getAbsolutePath() + "/thumb-" + ent.fileName();
                        Log.w("siktir", "cache:  " + cachePath);
                        if (!new File(cachePath).exists()) {
                            try {
                                mFos = new FileOutputStream(cachePath);
                            } catch (FileNotFoundException e) {
                                mErrorMsg = "Couldn't create a local file to store the image";
                                Log.e("siktir", mErrorMsg);
                                return false;
                            }

                            // Download thumbnail and write to cache directory.
                            mDBApi.getThumbnail(ent.path, mFos, DropboxAPI.ThumbSize.ICON_128x128,
                                    DropboxAPI.ThumbFormat.JPEG, null);
                            if (mCanceled) {
                                return false;
                            }
                        }
                        thumbCachePaths.add(cachePath);
                    }
                }
                return true;

            } catch (DropboxException e) {
                e.printStackTrace();
            }
            return null;
        }
        @Override
        protected void onPostExecute(Boolean result) {
            fillDropbox();
            pdLoading.dismiss();
        }

    }


    private class DownloadImages extends AsyncTask<String , Void, Boolean> {
        ProgressDialog pdLoading = new ProgressDialog(FileChooser.this);

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pdLoading.setMessage("Downloading images...");
            pdLoading.setButton(ProgressDialog.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    mCanceled = true;
                    mErrorMsg = "Canceled";

                    // This will cancel the getThumbnail operation by closing
                    // its stream
                    if (mFos != null) {
                        try {
                            mFos.close();
                        } catch (IOException e) {
                            Log.e("FileChooser Activity", e.getLocalizedMessage());
                        }
                    }
                }
            });
            pdLoading.show();
        }
        @Override
        protected Boolean doInBackground(String... params) {
            try {
                //dirent = mDBApi.metadata(params[0], 1000, null, true, null);
                for (Entry ent : dirent.contents) {
                    if (ent.thumbExists) {
                        dropboxCacheDirPath = FileChooser.this.getCacheDir().getAbsolutePath() + "/Dropbox";
                        //String dropboxCacheDirPath = Environment.getExternalStorageDirectory().getPath() + "/Dropbox";
                        File cacheDir = new File(dropboxCacheDirPath);
                        if (!cacheDir.exists()) {
                            if (cacheDir.mkdir()) {
                                Log.d("FileChooser Activity", "Dropbox cache dir created");
                            }
                        } else {
                            cleanDropboxCacheDir(cacheDir);
                        }
                        String cacheImagePath = dropboxCacheDirPath + "/" + ent.fileName();
                        if (!new File(cacheImagePath).exists()) {
                            try {
                                mFos = new FileOutputStream(cacheImagePath);
                            } catch (FileNotFoundException e) {
                                mErrorMsg = "Couldn't create a local file to store the image";
                                Log.e("siktir", mErrorMsg);
                                return false;
                            }

                            // Download image and write to cache directory.
                            mDBApi.getFile(ent.path, null, mFos, new ProgressListener() {
                                @Override
                                public void onProgress(long l, long l1) {
                                    //TODO: show progress
                                }
                            });
                            if (mCanceled) {
                                return false;
                            }
                        }
                    }
                }
                return true;

            } catch (DropboxException e) {
                e.printStackTrace();
            }
            return false;
        }
        @Override
        protected void onPostExecute(Boolean result) {
            pdLoading.dismiss();
            if (result) {
                showToast("Dropbox images added to slide-show.");
                Intent intent = new Intent();
                intent.putExtra("source_type", sourceType);
                intent.putExtra("image_source_path", dropboxName + ": " + dropboxPath);
                intent.putExtra("dropbox_cache_path", dropboxCacheDirPath);
                setResult(RESULT_OK, intent);

                finish();
            } else {
                showToast("Couldn't download images.");
            }
        }

    }
}