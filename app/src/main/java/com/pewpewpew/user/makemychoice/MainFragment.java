package com.pewpewpew.user.makemychoice;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseQueryAdapter;
import com.parse.ParseUser;

import junit.framework.Test;

import java.util.List;
import java.util.Random;

/**
 * Created by User on 24/9/14.
 */

/**
 * Main Fragment hosting just a listview.
 *
 * Future - pager view with user's posts, favourites in another page, private/local options for posts eg open to friends only
 */
public class MainFragment extends Fragment {
    private static final String TAG = "MainFragment_Debug";
    public static final String KEY_POST_TITLE = "post_title_key";
    public static final String KEY_POST_ID = "post_id";
    private static final int REQUEST_NEW_POST = 88;
    public static final String KEY_POST_OP = "post_op_key";
    private ParseQueryAdapter<Post> mAdapter;
    SharedPreferences mSharedPreferences;
    private static String sortMode;
    public MainFragment(){

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        sortMode = mSharedPreferences.getString(getActivity().getString(R.string.pref_sort_mode),getActivity().getString(R.string.pref_sort_mode_default));
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Sort options and stuff

        inflater.inflate(R.menu.main_fragment, menu);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // dev option to add new data onto Parse database
        int id = item.getItemId();
      if (id == R.id.action_sortOptions){
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.sort_dialogue_title)
                    .setItems(R.array.sort_options,new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int pos) {
                            Log.i(TAG,"Current sort mode: "+sortMode);
                            if(pos == 0){
                                // Sort by Top
                                mSharedPreferences.edit()
                                        .putString(getActivity().getString(R.string.pref_sort_mode),"top")
                                        .apply();
                                if (!sortMode.equals("top")) {
                                    sortMode = "top";
                                }
                            }else if(pos==1){
                                mSharedPreferences.edit()
                                        .putString(getActivity().getString(R.string.pref_sort_mode),"new")
                                        .apply();
                                if (!sortMode.equals("new")) {
                                    sortMode = "new";
                                    mAdapter.loadObjects();
                                }
                            }else{
                                Log.i(TAG, "Sort by Hot (Not implemented)");
                            }
                        }
                    });
            AlertDialog dialog = builder.create();
            dialog.show();

        }else if (id == R.id.action_refresh){
            mAdapter.notifyDataSetChanged();
            mAdapter.loadObjects();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        //Inflate custom view here and instantiate
        final View view = inflater.inflate(R.layout.fragment_main,container, false);

        // TODO - bundle in stuff to retain instance on config change etc etc

        ListView listView = (ListView) view.findViewById(R.id.listView_main);

        ParseQueryAdapter.QueryFactory<Post> factory = new ParseQueryAdapter.QueryFactory<Post>() {
            @Override
            public ParseQuery<Post> create() {
                ParseQuery<Post> query = ParseQuery.getQuery(Post.class);

                //TODO - change query methods based on which activity it's in, use switch case or wtv, or maybe just implement diff adapters for those activities
                // Not putting datetime constraints for now, since data is planned to be released after
                // a week or so, unless user feedback says otherwise.
                if (sortMode.equals("new")){
                    query.orderByDescending("createdAt");
                }else if(sortMode.equals("top")){
                    query.orderByDescending("points");
                }

                // TODO- delete posts that have no activity for the last X days eg no comment no views
                return query;
            }
        };
        mAdapter = new ParseQueryAdapter<Post>(getActivity(), factory){
            @Override
            public View getItemView(Post post, View v, ViewGroup parent) {

                if (v == null){
                    v= View.inflate(getContext(),R.layout.list_post_item, null);
                }

                ((TextView)v.findViewById(R.id.post_title)).setText(post.getTitle());

                TextView postTimeSince = (TextView)v.findViewById(R.id.post_time);
                // Get timeSince, numComments and numPoints for post data

                String timeSince = Utility.getTimeSince(post.getCreatedAt());
                postTimeSince.setText(timeSince);

                //Number of comments, use increment on the field every time there is a new post
                // TODO- implement numComments
                TextView postNumComments = (TextView)v.findViewById(R.id.post_comment);
                String numComments = "0";
                postNumComments.setText("0 comments");


                // Get the username string. This was added as loading classes seem to take super long on Parse. Call getUser when needed.
                TextView postUsername = (TextView)v.findViewById(R.id.post_user);

                String username = post.getUserStr();
                postUsername.setText(username);

                if(post.getOutcome() != null){
                    v.setBackgroundColor(getResources().getColor(R.color.list_item_outcome));
                }
                return v;
            }
        };
        mAdapter.addOnQueryLoadListener(new ParseQueryAdapter.OnQueryLoadListener<Post>() {
            @Override
            public void onLoading() {
                Log.i(TAG, "Loading...");
                view.findViewById(R.id.main_progressBar).setVisibility(View.VISIBLE);
            }

            @Override
            public void onLoaded(List<Post> posts, Exception e) {
                Log.i(TAG, "Loaded.");
                view.findViewById(R.id.main_progressBar).setVisibility(View.GONE);
            }
        });
        listView.setAdapter(mAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                Intent intent = new Intent(getActivity(), DetailActivity.class);
                // Pass in post ID here to retrieve anything extra
                ParseQueryAdapter adapter = (ParseQueryAdapter) adapterView.getAdapter();
                Post post = (Post)adapter.getItem(i);

//                String title = post.getTitle();
//                intent.putExtra(KEY_POST_TITLE,title);

                intent.putExtra(KEY_POST_ID , post.getObjectId());
                intent.putExtra(KEY_POST_OP, post.getUserStr());
                startActivity(intent);
            }
        });
        return view; //return own view here
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_NEW_POST){
            if( resultCode == Activity.RESULT_OK){
                Log.i(TAG, "Result received successfully.");
                mAdapter.notifyDataSetChanged();
                mAdapter.loadObjects();
            }else{
                Log.i(TAG,"User cancelled.");
            }
        }else{
            Log.i(TAG, "Incorrect request code. This shouldn't happen at all.");
        }
    }

    public static Fragment newInstance(int i) {
        MainFragment f = new MainFragment();

        // Supply num input as an argument.
        Bundle args = new Bundle();
        args.putInt("num", i);
        f.setArguments(args);


        return f;
    }
}
