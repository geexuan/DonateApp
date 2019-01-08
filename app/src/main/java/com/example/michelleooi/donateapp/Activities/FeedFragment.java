package com.example.michelleooi.donateapp.Activities;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.michelleooi.donateapp.Adapters.AdapterFeed;
import com.example.michelleooi.donateapp.Models.ModelFeed;
import com.example.michelleooi.donateapp.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;

public class FeedFragment extends Fragment {

    private final static int POST_ACTIVITY = 5;
    RecyclerView feedRecyclerView;
    ArrayList<ModelFeed> modelFeedArrayList = new ArrayList<>();
    AdapterFeed adapterFeed;
    Button btnPostFeed;
    SwipeRefreshLayout swipeLayout;
    ProgressBar progress;
    TextView emptyText;
    int firstVisibleItem, visibleItemCount, totalItemCount;
    private int previousTotal = 0;
    private boolean loading = true;
    private int visibleThreshold = 5;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_feed, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        super.onActivityCreated(savedInstanceState);
        progress = getActivity().findViewById(R.id.progress_bar);
        swipeLayout = (SwipeRefreshLayout) getActivity().findViewById(R.id.swipe_container);
        swipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                populateFeedRecyclerView(Query.Direction.DESCENDING);
            }
        });
        swipeLayout.setColorSchemeColors(getResources().getColor(android.R.color.holo_blue_bright),
                getResources().getColor(android.R.color.holo_green_light),
                getResources().getColor(android.R.color.holo_orange_light),
                getResources().getColor(android.R.color.holo_red_light));
        ((HomeActivity) getActivity()).setActionBarTitle("News Feed");

        feedRecyclerView = getActivity().findViewById(R.id.feedRecyclerView);

        final RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity());
        feedRecyclerView.setLayoutManager(layoutManager);
        emptyText = getActivity().findViewById(R.id.emptyText);

        populateFeedRecyclerView(Query.Direction.DESCENDING);
        btnPostFeed = getActivity().findViewById(R.id.btnPostFeed);
    }

    public void populateFeedRecyclerView(Query.Direction direction) {
        adapterFeed = new AdapterFeed(getActivity(), modelFeedArrayList);
        feedRecyclerView.setAdapter(adapterFeed);
        modelFeedArrayList.clear();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference modelFeedRef = db.collection("Feeds");
        modelFeedRef.orderBy("postTime", direction)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            ModelFeed modelFeed = document.toObject(ModelFeed.class);
                            modelFeed.setId(document.getId());
                            modelFeedArrayList.add(modelFeed);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getActivity(), "Failed To Load Feeds", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (modelFeedArrayList.isEmpty()) {
                            emptyText.setVisibility(View.VISIBLE);
                        } else {
                            emptyText.setVisibility(View.GONE);
                        }
                        if (swipeLayout.isRefreshing()) {
                            swipeLayout.setRefreshing(false);
                        }
                        adapterFeed.notifyDataSetChanged();
                        feedRecyclerView.setVisibility(View.VISIBLE);
                        progress.setVisibility(View.GONE);
                    }
                });
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        MenuItem item1 = menu.add(Menu.NONE, 999, 1, "Add Post");
        item1.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        item1.setIcon(R.drawable.ic_add_white_24dp);
        MenuItem item2 = menu.add(Menu.NONE, 998, 2, "Sort By");
        item2.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        item2.setIcon(R.drawable.ic_sort_white_24dp);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.option_logout:
                FirebaseAuth.getInstance().signOut();
                getActivity().finish();
                startActivity(new Intent(getActivity(), LoginActivity.class));
                break;
            case 998:
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle("Sort By");
                builder.setItems(new String[]{"New To Old", "Old To New"}, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (i == 0) {
                            populateFeedRecyclerView(Query.Direction.DESCENDING);
                        } else {
                            populateFeedRecyclerView(Query.Direction.ASCENDING);
                        }
                    }
                });
                builder.show();
                break;
            case 999:
                startActivityForResult(new Intent(getActivity(), PostFeedActivity.class), POST_ACTIVITY);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == POST_ACTIVITY) {
            if (resultCode == Activity.RESULT_OK) {
                populateFeedRecyclerView(Query.Direction.DESCENDING);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
