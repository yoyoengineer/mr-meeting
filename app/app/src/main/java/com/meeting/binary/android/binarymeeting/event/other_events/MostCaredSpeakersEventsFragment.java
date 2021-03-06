package com.meeting.binary.android.binarymeeting.event.other_events;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.meeting.binary.android.binarymeeting.R;
import com.meeting.binary.android.binarymeeting.contact.ContactEventActivity;
import com.meeting.binary.android.binarymeeting.model.Event;
import com.meeting.binary.android.binarymeeting.other.EventImages;
import com.meeting.binary.android.binarymeeting.service.generetor.GeneralServiceGenerator;
import com.meeting.binary.android.binarymeeting.service.request_interface.RequestWebServiceInterface;
import com.meeting.binary.android.binarymeeting.utils.DateUtils;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MostCaredSpeakersEventsFragment extends Fragment {
    private static final String TAG = "latest_event_list_tag";
    private static final String EVENT_ALL_PARSE_EXTRA_MESSAGE = "com.binary.event_all_parcelable";

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView recommendedEventListRecycler;
    private MostCaredSpeakersEventsFragment.EventListAdapter recommendedEventListAdapter;

    private List<Event> recommendedEvents;
    private Event recommendedEventToSend;
    private int mLastItem = -1;



    public static MostCaredSpeakersEventsFragment newInstance(){
        MostCaredSpeakersEventsFragment fragment = new MostCaredSpeakersEventsFragment();
        return fragment;
    }

    /**
     * ==================================
     * load the data from the web service
     * ==================================
     */
    public void requestData(){
        RequestWebServiceInterface requestWebServiceInterface =
                GeneralServiceGenerator.CreateService(RequestWebServiceInterface.class, getActivity());
        Call<List<Event>> call = requestWebServiceInterface.getMostAttentedEventsWithOrganizer();
        call.enqueue(new Callback<List<Event>>() {
            @Override
            public void onResponse(Call<List<Event>> call, Response<List<Event>> response) {
                recommendedEvents = response.body();
                if (recommendedEvents != null){
                    updateUi();
                } else {
                    Log.i(TAG, "onResponse: recommendedEvents is null");
                }
            }

            @Override
            public void onFailure(Call<List<Event>> call, Throwable t) {

            }
        });
    }

    private void updateUi() {
        if (recommendedEventListAdapter == null){
            recommendedEventListAdapter = new EventListAdapter(recommendedEvents);
            recommendedEventListRecycler.setAdapter(recommendedEventListAdapter);
        } else{
            if(mLastItem < 0){
                recommendedEventListAdapter.setEventList(recommendedEvents);
                recommendedEventListAdapter.notifyDataSetChanged();
            } else{
                recommendedEventListAdapter.notifyItemChanged(mLastItem);
                mLastItem = -1;
            }

        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        requestData();
        Log.d(TAG, "onCreate: " + recommendedEvents);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_most_cared_speakers_events, container, false);

        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle("List of Events");
        ((AppCompatActivity) getActivity()).getSupportActionBar().setIcon(getResources().getDrawable(R.drawable.bm));

        SwipeRefreshLayout refreshLayout = view.findViewById(R.id.swipe_refresh);
        refreshLayout.setProgressBackgroundColorSchemeColor(getResources().getColor(R.color.primary_blue));
        

        recommendedEventListRecycler = view.findViewById(R.id.event_list_recycler);
        recommendedEventListRecycler.setLayoutManager(new LinearLayoutManager(getActivity()));

//        requestDataEvent();
        return view;
    }


    @Override
    public void onResume() {
        super.onResume();
        requestData();
    }


//    /**
//     * ===========
//     * sets up UI
//     * ===========
//     */
//    private void updateUI(){
//
//    }



    /**
     * =========================================
     * load the data event from the web service
     * =========================================
     */
    public void requestDataEvent(int position){
        RequestWebServiceInterface requestWebServiceInterface =
                GeneralServiceGenerator.CreateService(RequestWebServiceInterface.class, getActivity());
        Log.d(TAG, "requestDataEvent: " + "mEvent:" + recommendedEvents.get(position));
        Log.d(TAG, "requestDataEvent: " + "mEvents.get(position).getId():" + recommendedEvents.get(position).getId());
        Call<Event> call = requestWebServiceInterface.getEvent(recommendedEvents.get(position).getId());
        call.enqueue(new Callback<Event>() {
            @Override
            public void onResponse(Call<Event> call, Response<Event> response) {
                recommendedEventToSend = response.body();
                if (recommendedEventToSend != null){
                    Intent intent = ContactEventActivity.newIntent(getActivity(), recommendedEventToSend);
                    startActivity(intent);
                } else {
                }
                Log.i(TAG, "onResponse: " + recommendedEventToSend.getId() + " " + recommendedEventToSend.getTopic());
            }

            @Override
            public void onFailure(Call<Event> call, Throwable t) {
                Log.i(TAG, "onFailure: requestDataEvent failed");
            }
        });

    }




    /**
     * ====================================================
     * holder to create the view and be bind to the adapter
     * ====================================================
     */
    class EventListHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        private Event mEvent;

        private TextView mEventDateCreated;
        private TextView mEventTopic;
        private android.widget.ImageView eventImg;

        public EventListHolder(View itemView) {
            super(itemView);
        }

        public EventListHolder(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.item_most_cared_speaker, parent, false));
            itemView.setOnClickListener(this);

            mEventDateCreated = itemView.findViewById(R.id.event_date_created);
            mEventTopic = itemView.findViewById(R.id.event_topic);
            eventImg = itemView.findViewById(R.id.event_pic);
        }

        @Override
        public void onClick(View view) {
            mLastItem = this.getAdapterPosition();
            Log.d(TAG, "onClick: " + "mLastItem:"+ mLastItem);
            Toast.makeText(getActivity(), "item cliced", Toast.LENGTH_LONG).show();
            requestDataEvent(mLastItem);
        }

        public void bind(Event event, int position){
            mEvent = event;
            mEventTopic.setText(mEvent.getTopic());
            eventImg.setImageResource(EventImages.eventImages[position]);
            mEventDateCreated.setText(DateUtils.getReadableModifyDate(mEvent.getTime()));
        }
    }




    /**
     * =========================================
     * adapter to be set in the recycler
     * =========================================
     */
    class EventListAdapter extends RecyclerView.Adapter<EventListHolder>{

        List<Event> mEvents;

        public EventListAdapter(List<Event> events) {
            mEvents = events;
        }

        @Override
        public MostCaredSpeakersEventsFragment.EventListHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            return new MostCaredSpeakersEventsFragment.EventListHolder(inflater, parent);
        }

        @Override
        public void onBindViewHolder(MostCaredSpeakersEventsFragment.EventListHolder holder, int position) {
            holder.bind(mEvents.get(position), position);
        }

        @Override
        public int getItemCount() {
            return mEvents.size();
        }

        public void setEventList(List<Event> events){
            mEvents = events;
        }
    }
}
