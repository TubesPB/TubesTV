package com.capstone.tubestv.fragment;

import android.annotation.SuppressLint;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.capstone.tubestv.R;
import com.capstone.tubestv.adapter.TVSeriesAdapter;
import com.capstone.tubestv.model.ResultsItem;
import com.capstone.tubestv.model.RootTVSeriesModel;
import com.capstone.tubestv.rest.ApiConfig;
import com.capstone.tubestv.rest.ApiService;
import com.capstone.tubestv.room.TVSeriesViewModel;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PopularFragment extends Fragment {

    private RecyclerView rvTVSeries;
    private TVSeriesAdapter adapterTVSeries;
    private ArrayList<ResultsItem> listDataTVSeries = new ArrayList<>();
    final int gridColumnCount = 3;
    private TVSeriesViewModel mTVSeriesViewModel;

    public PopularFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_popular, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rvTVSeries = view.findViewById(R.id.recyclerView);

        mTVSeriesViewModel = ViewModelProviders.of(this).get(TVSeriesViewModel.class);
        listDataTVSeries = new ArrayList<>();
        adapterTVSeries = new TVSeriesAdapter(getContext());
        rvTVSeries.setAdapter(adapterTVSeries);
        rvTVSeries.setLayoutManager(new GridLayoutManager(getContext(), gridColumnCount));

        if (haveNetwork()) {
            mTVSeriesViewModel.deleteAllPopular();
            getAndSaveDataAPI();
        } else if (!haveNetwork()) {
            mTVSeriesViewModel.getAllDataPopular().observe(this, new Observer<List<ResultsItem>>() {
                @Override
                public void onChanged(@Nullable final List<ResultsItem> data) {
                    // Update the cached copy of the words in the adapter.
                    adapterTVSeries.setData((ArrayList<ResultsItem>) data);
                }
            });
        }
    }

    private void getAndSaveDataAPI() {
        ApiService apiService = ApiConfig.getApiService();
        apiService.getPopular("0dde3e9896a8c299d142e214fcb636f8", "en-US", "1")
                .enqueue(new Callback<RootTVSeriesModel>() {
                    @Override
                    public void onResponse(Call<RootTVSeriesModel> call, Response<RootTVSeriesModel> response) {
                        if (response.isSuccessful()) {
                            listDataTVSeries = response.body().getResults(); // Mengambil data dari JSON lalu ditampung ke model

                            // Menyimpan data ke database || Save data
                            for (int i = 0; i < listDataTVSeries.size(); i++) {
                                Integer id = listDataTVSeries.get(i).getId();
                                String name = listDataTVSeries.get(i).getName();
                                String firstAirDate = listDataTVSeries.get(i).getFirstAirDate();
                                Double voteAverage = listDataTVSeries.get(i).getVoteAverage();
                                String poster = listDataTVSeries.get(i).getPosterPath();
                                String overview = listDataTVSeries.get(i).getOverview();
                                ArrayList<Integer> genre = listDataTVSeries.get(i).getGenreIds();
                                Double popularity = listDataTVSeries.get(i).getPopularity();

                                ResultsItem tvSeries = new ResultsItem();
                                tvSeries.setId(id);
                                tvSeries.setName(name);
                                tvSeries.setFirstAirDate(firstAirDate);
                                tvSeries.setVoteAverage(voteAverage);
                                tvSeries.setPosterPath(poster);
                                tvSeries.setOverview(overview);
                                tvSeries.setGenreIds(genre);
                                tvSeries.setPopularity(popularity);
                                tvSeries.setVariety("popular");
                                mTVSeriesViewModel.insert(tvSeries);
                            }

                            adapterTVSeries.setData(listDataTVSeries);
                            adapterTVSeries.notifyDataSetChanged(); // Memberitahu adapter apabila ada data baru
                        }
                    }

                    @Override
                    public void onFailure(Call<RootTVSeriesModel> call, Throwable t) {
                        Toast.makeText(getContext(), "" + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private boolean haveNetwork() {
        boolean have_WIFI = false;
        boolean have_MobileData = false;
        ConnectivityManager connectivityManager = (ConnectivityManager) getActivity().getSystemService(getActivity().CONNECTIVITY_SERVICE);
        @SuppressLint ("MissingPermission") NetworkInfo[] networkInfos = connectivityManager.getAllNetworkInfo();
        for (NetworkInfo info : networkInfos) {
            if (info.getTypeName().equalsIgnoreCase("WIFI"))
                if (info.isConnected()) have_WIFI = true;

            if (info.getTypeName().equalsIgnoreCase("MOBILE"))
                if (info.isConnected()) have_MobileData = true;
        }
        return have_WIFI || have_MobileData;
    }
}