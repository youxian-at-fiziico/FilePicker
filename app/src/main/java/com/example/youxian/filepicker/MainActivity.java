package com.example.youxian.filepicker;

import android.app.Activity;
import android.os.Environment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getName();
    private TextView mPathText;
    private Button mCancelButton;
    private Button mOkButton;
    private ListView mFilesList;

    private FileListAdapter mFileListAdapter;
    private List<File> mFiles;
    private File currentFile;
    private File rootFile;

    private VelocityTracker mVelocityTracker;
    private int mSwipingSlop;

    private boolean hasSwiped = false;
    private boolean mSwiping = false;
    private boolean openState = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mFiles = new ArrayList<>();
        initView();
    }

    @Override
    public void onBackPressed() {
        if (currentFile.equals(rootFile)) {
            this.finish();
        } else {
            showDirectory(currentFile.getParentFile());
        }
    }

    private void initView() {
        mPathText = (TextView) findViewById(R.id.path_text_main);

        mCancelButton = (Button) findViewById(R.id.cancel_button_main);

        mOkButton = (Button) findViewById(R.id.ok_button_main);

        scanFile();
        mFilesList = (ListView) findViewById(R.id.files_list_main);
        mFileListAdapter = new FileListAdapter(mFiles);
        mFilesList.setAdapter(mFileListAdapter);
    }

    private void scanFile() {
        rootFile = Environment.getExternalStorageDirectory();
        File[] files = rootFile.listFiles();
        for (File file: files) {
            mFiles.add(file);
        }
        currentFile = rootFile;
        showPath(rootFile.getPath());
    }

    private void showPath(String path) {
        mPathText.setText(path);
    }

    public void showDirectory(File file) {
        mFiles.clear();
        Collections.addAll(mFiles, file.listFiles());
        for (File f : mFiles) {
            Log.d(TAG, "file: " + f.getName());
        }
        mFileListAdapter.notifyDataSetChanged();
        currentFile = file;
        showPath(currentFile.getPath());
    }

    private class FileListAdapter extends BaseAdapter {
        private int mWidth;
        private List<File> mListFiles;
        private LayoutInflater mInflater;
        public FileListAdapter(List<File> files) {
            mListFiles = files;
            mInflater = getLayoutInflater();
        }

        @Override
        public int getCount() {
            return mListFiles.size();
        }

        @Override
        public Object getItem(int position) {
            return mListFiles.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final File file = mListFiles.get(position);
            Log.d(TAG, "file: " + file.getName() + " is folder: " + file.isDirectory());
            if (!file.isDirectory()) {
                convertView = mInflater.inflate(R.layout.listrow_file, null);
                TextView titleFile = (TextView) convertView.findViewById(R.id.title_file_item);
                TextView sizeFile = (TextView) convertView.findViewById(R.id.size_file_item);
                final CheckBox checkBox = (CheckBox) convertView.findViewById(R.id.checkbox_file_item);
                checkBox.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.d(TAG, "Checkbox on clicked");
                    }
                });
                titleFile.setText(file.getName());
                sizeFile.setText(file.length() / 1024 + " KB");
                mWidth = 300;
                convertView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        checkBox.performClick();
                        Log.d(TAG, "onClick");
                    }
                });
                RelativeLayout relativeLayout = (RelativeLayout) convertView.findViewById(R.id.layout_file_item);
                relativeLayout.setOnTouchListener(mTouchListener);
            } else {
                convertView = mInflater.inflate(R.layout.listrow_folder, null);
                TextView titleFolder = (TextView) convertView.findViewById(R.id.title_folder_item);
                titleFolder.setText(file.getName());
                convertView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.d(TAG, "onClick");
                        showDirectory(file);
                    }
                });
            }
            return convertView;
        }

        private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
            private float mDownX;
            private int mSlop = -1;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (mSlop < 0) {
                    mSlop = ViewConfiguration.get(MainActivity.this).
                            getScaledTouchSlop();
                    Log.d(TAG, "slop: " + mSlop);
                }
                switch (event.getActionMasked()) {

                    case MotionEvent.ACTION_DOWN:
                        Log.d(TAG, "Action down");
                        mDownX = event.getRawX();
                        mVelocityTracker = VelocityTracker.obtain();
                        mVelocityTracker.addMovement(event);
                        break;

                    case MotionEvent.ACTION_CANCEL:
                        Log.d(TAG, "Action cancel");
                        v.animate().translationX(0)
                                .alpha(1)
                                .setDuration(450);
                        mVelocityTracker.recycle();
                        mVelocityTracker = null;
                        mSwiping = false;
                        mDownX = 0;
                        hasSwiped = false;
                        openState = false;
                        break;

                    case MotionEvent.ACTION_MOVE:
                        Log.d(TAG, "Action move");
                        mVelocityTracker.addMovement(event);
                        float deltaX = event.getRawX() - mDownX;

                        if (!mSwiping && Math.abs(deltaX) > mSlop && deltaX < 0) {
                            mSwiping = true;
                            mSwipingSlop = (deltaX > 0 ? mSlop : -mSlop);
                            mFilesList.requestDisallowInterceptTouchEvent(true);
                        }
                        if (mSwiping) {
                            Log.d(TAG, "swiping");
                            hasSwiped = true;
                            mFilesList.invalidate();
                            if (Math.abs(deltaX) < mWidth + mSlop
                                    && deltaX <= mSwipingSlop) {
                                Log.d(TAG, "view move");
                                v.setTranslationX(deltaX - mSwipingSlop);
                                openState = false;
                            }
                        }
                        break;

                    case MotionEvent.ACTION_UP:
                        Log.d(TAG, "Action up");
                        if (openState) {
                            v.animate().translationX(0)
                                    .alpha(1)
                                    .setDuration(450);
                            openState = false;
                        } else if (!openState && hasSwiped){
                            v.animate().translationX(-mWidth)
                                    .alpha(1)
                                    .setDuration(450);
                            openState = true;
                        } else {
                            Log.d(TAG, "layout clicked");
                        }
                        if(mVelocityTracker != null) mVelocityTracker.recycle();
                        mVelocityTracker = null;
                        mDownX = 0;
                        mSwiping = false;
                        hasSwiped = false;
                        break;

                    default:
                        return false;
                }
                return true;
            }
        };
    }
}
