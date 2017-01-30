package com.filemanager.fragment;


import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.blankj.utilcode.utils.FileUtils;
import com.bumptech.glide.Glide;
import com.filemanager.R;
import com.filemanager.adapter.WordAdapter;
import com.filemanager.util.ACache;
import com.google.gson.Gson;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class WordFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    private RecyclerView mRecyclerView;
    private List<File> mFiles;
    private WordAdapter mAdapter;
    private SwipeRefreshLayout mRefreshLayout;
    private Gson mGson;
    private ImageView mLoading;
    private TextView mLoadingText;
    private ACache mCatch;
    private SharedPreferences mPreferences;


    public WordFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View ret = inflater.inflate(R.layout.fragment_word, container, false);

        mLoading = (ImageView) ret.findViewById(R.id.loading_gif);
        mRecyclerView = (RecyclerView) ret.findViewById(R.id.id_recyclerview);
        mLoadingText = (TextView) ret.findViewById(R.id.loading_text);
        mRecyclerView = (RecyclerView) ret.findViewById(R.id.id_recyclerview);
        mRefreshLayout = (SwipeRefreshLayout) ret.findViewById(R.id.word_refresh);
        Glide.with(getContext()).load(R.drawable.loading)
                .asGif().into(mLoading);
        mFiles = new ArrayList<>();
        mGson = new Gson();
        mCatch = ACache.get(getContext());
        mRecyclerView = (RecyclerView) ret.findViewById(R.id.id_recyclerview);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mRefreshLayout.setColorSchemeResources(R.color.colorPrimary);
        mRefreshLayout.setOnRefreshListener(this);
        initDate();

        return ret;
    }

    private void initDate() {
        //开线程初始化数据
        Thread mThread = new Thread(new Runnable() {
            @Override
            public void run() {

                judge();

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        mRecyclerView.setAdapter(mAdapter = new WordAdapter(getContext(), mFiles));
                        mLoading.setVisibility(View.INVISIBLE);
                        mLoadingText.setVisibility(View.INVISIBLE);
                        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
                        mAdapter.setOnItemClickLitener(new WordAdapter.OnItemClickLitener() {
                            @Override
                            public void onItemClick(View view, int position) {
                            }

                            @Override
                            public void onItemLongClick(View view, int position) {
                            }
                        });
                    }
                });
            }
        });
        mThread.start();
    }

    private void judge() {
        mPreferences = getContext().getSharedPreferences("table", Context.MODE_PRIVATE);

        boolean first = mPreferences.getBoolean("firstWord", true);
        int num = mPreferences.getInt("numWord", 0);
        if (!first) {
            for (int i = 0; i < num; i++) {
                String s = String.valueOf(i);
                String string = mCatch.getAsString(s + "word");
                if (!string.equals("null")) {
                    File file = mGson.fromJson(string, File.class);
                    mFiles.add(file);
                }

            }
        } else {

            mFiles = FileUtils.listFilesInDirWithFilter(Environment.getExternalStorageDirectory(), ".txt");
            addCatch();
        }
    }

    private void addCatch() {
        ArrayList<String> strings = new ArrayList<>();
        for (int i = 0; i < mFiles.size(); i++) {
            String s = mGson.toJson(mFiles.get(i));
            strings.add(s);
        }
        for (int i = 0; i < strings.size(); i++) {
            String s = String.valueOf(i);
            mCatch.put(s + "word", strings.get(i), ACache.TIME_DAY);
        }


        SharedPreferences.Editor edit = mPreferences.edit();
        edit.putBoolean("firstWord", false);
        edit.putInt("numWord", strings.size());
        edit.commit();
    }

    @Override
    public void onRefresh() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                mFiles = FileUtils.listFilesInDirWithFilter(Environment.getExternalStorageDirectory(), ".txt");
                addCatch();
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        mAdapter.notifyDataSetChanged();
                        mRefreshLayout.setRefreshing(false);
                        Toast.makeText(getContext(), "刷新完成", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).start();


    }
}
