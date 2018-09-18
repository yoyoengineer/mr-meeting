package com.meeting.binary.android.binarymeeting.event.upload_download;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.meeting.binary.android.binarymeeting.R;
import com.meeting.binary.android.binarymeeting.event.admin.AdminFragment;
import com.meeting.binary.android.binarymeeting.event.other_events.LargePhotoFragment;
import com.meeting.binary.android.binarymeeting.model.FileModel;
import com.meeting.binary.android.binarymeeting.service.cookie.CookiePreferences;
import com.meeting.binary.android.binarymeeting.service.generetor.BaseUrlGenerator;
import com.meeting.binary.android.binarymeeting.service.generetor.GeneralServiceGenerator;
import com.meeting.binary.android.binarymeeting.service.request_interface.RequestWebServiceInterface;
import com.meeting.binary.android.binarymeeting.utils.FileUtils;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.content.Context.BIND_AUTO_CREATE;

public class ViewPhotoUploadedFragment extends Fragment {

    private List<FileModel> mPhotoModels = new ArrayList<>();

    private DownloadService.DownloadBinder downloadBinder;

    private static final int REQUEST_PHOTO = 0;
    private static final String DIALOG_PHOTO = "LargePhotoFragment";

    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            downloadBinder = (DownloadService.DownloadBinder) service;
        }

    };

    private RecyclerView mPhotoRecycler;
    private PhotoAdapter mPhotoAdapter;

    private static final String EXTRA_MESSAGE = "eventId";
    private static final String TAG = "viewphoto";

    private String eventId;

    public static ViewPhotoUploadedFragment newInstance(String eventId) {
        Bundle args = new Bundle();
        args.putString(EXTRA_MESSAGE, eventId);
        ViewPhotoUploadedFragment fragment = new ViewPhotoUploadedFragment();
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        eventId = getArguments().getString(EXTRA_MESSAGE);
        if (eventId != null){
            Log.i(TAG, "onCreate: the event id is not null " + eventId);
        } else {
            Log.i(TAG, "onCreate: id is null");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.view_image_uploaded, container, false);

        mPhotoRecycler = view.findViewById(R.id.photo_recycler);
        mPhotoRecycler.setLayoutManager(new GridLayoutManager(getActivity(), 4));
        requestPhoto();

        Intent intent = new Intent(getContext(), DownloadService.class);
        getActivity().startService(intent); // 启动服务
        getActivity().bindService(intent, connection, BIND_AUTO_CREATE); // 绑定服务
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{ Manifest.permission. WRITE_EXTERNAL_STORAGE }, 1);
        }
        return view;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(getActivity(), "拒绝权限将无法使用程序", Toast.LENGTH_SHORT).show();
                    getActivity().finish();
                }
                break;
            default:
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().unbindService(connection);
    }


    private void requestPhoto(){
        RequestWebServiceInterface requestWebServiceInterface =
                GeneralServiceGenerator.CreateService(RequestWebServiceInterface.class, getActivity());
        Call<List<FileModel>> call = requestWebServiceInterface.getPhotos(eventId);
        call.enqueue(new Callback<List<FileModel>>() {
            @Override
            public void onResponse(Call<List<FileModel>> call, Response<List<FileModel>> response) {
                if (response.isSuccessful()){
                    Log.d(TAG, "onResponse: response succeed");
                    List<FileModel> mFileModels = response.body();
                    if (mFileModels != null){
                        checkExtension(mFileModels);
                        updateUi();
                    } else {
                        Toast.makeText(getActivity(), "list of photos is empty", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.d(TAG, "onResponse: response failed");
                }
            }

            @Override
            public void onFailure(Call<List<FileModel>> call, Throwable t) {
                Log.d(TAG, "onFailure: failed to connect");
            }
        });
    }





    private void checkExtension(List<FileModel> fileModels){
        for (FileModel fileModel : fileModels){
            if (fileModel.getFileName() != null && !TextUtils.isEmpty(fileModel.getFileName()) && fileModel.getFileName().length() > 0){
                Log.d(TAG, "checkExtension: " + fileModel.getFileName());
                String extension = FileUtils.getExtension(fileModel.getFileName());
                Log.d(TAG, "This is the file extension: " + extension);
                if (extension.equals(".png") || extension.equals(".PNG") || extension.equals(".jpg") || extension.equals(".JPG") ||
                        extension.equals(".jpeg") || extension.equals(".JPEG") || extension.equals(".gif") ||
                        extension.equals(".GIF")){
                    mPhotoModels.add(fileModel);
                }
            }
        }
    }



    private void updateUi(){
        if (mPhotoAdapter == null){
            mPhotoAdapter = new PhotoAdapter(mPhotoModels);
            mPhotoRecycler.setAdapter(mPhotoAdapter);
        } else {
            mPhotoAdapter.setModels(mPhotoModels);
            mPhotoAdapter.notifyDataSetChanged();
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        updateUi();
    }

    /**
     * =========================================
     * holder
     */
    private class PhotoHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        FileModel mFileModel;

        private ImageView mPhotoUploaded;
        private ImageView mDowmload;
        private TextView mImageName;

        public PhotoHolder(View itemView) {
            super(itemView);
        }

        public PhotoHolder(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.item_uploaded_photo, parent, false));
            mDowmload = itemView.findViewById(R.id.download_photo);
            mImageName = itemView.findViewById(R.id.photo_name);
            mPhotoUploaded = itemView.findViewById(R.id.image_uploaded);

        }

        public void bind(FileModel fileModel){
            mFileModel = fileModel;
            mImageName.setText(mFileModel.getFileName());


            /**
             * display the photo uploaded
             */
            displayUploadedImage();



            /**
             * downloaded the photo for a specific event
             */
            mDowmload.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Toast.makeText(getActivity(), "Downloading....", Toast.LENGTH_SHORT).show();
                    if (downloadBinder != null) {
                        Toast.makeText(getActivity(), "Downloading....", Toast.LENGTH_LONG).show();
                        String url = BaseUrlGenerator.BINARY_BASE_URL + "/event/file/download/" + eventId + "/" + mFileModel.getFileId();
                        Log.i(TAG, "onClick: " + mFileModel.getFileName());
                        downloadBinder.startDownload(url, mFileModel.getFileName());
                    }
                }
            });




            // display large image
            String url = BaseUrlGenerator.BINARY_BASE_URL + "/event/file/download/" + eventId + "/" + mFileModel.getFileId();
            mPhotoUploaded.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    FragmentManager manager = getFragmentManager();
                    LargePhotoFragment dialog = LargePhotoFragment.newInstance(url, mFileModel.getFileName());
                    dialog.setTargetFragment(ViewPhotoUploadedFragment.this, REQUEST_PHOTO);
                    dialog.show(manager, DIALOG_PHOTO);
                }
            });
        }


        @Override
        public void onClick(View view) {
            Log.i("clicked", "onClick: clicked");
            if (downloadBinder != null && mFileModel.getFileId() != null) {
                Toast.makeText(getActivity(), "Downloading....", Toast.LENGTH_LONG).show();
                String url = BaseUrlGenerator.BINARY_BASE_URL + "/event/file/download/" + eventId + "/" + mFileModel.getFileId();
                downloadBinder.startDownload(url, mFileModel.getFileName());
            }
        }


        private void displayUploadedImage() {
            String recupCookie = CookiePreferences.getStoredCookie(getActivity());
            LazyHeaders.Builder builder = new LazyHeaders.Builder().addHeader("Cookie", recupCookie);
            GlideUrl glideUrl = new GlideUrl(BaseUrlGenerator.BINARY_BASE_URL + "/event/file/download/" + eventId + "/" + mFileModel.getFileId(), builder.build());

            Glide.with(ViewPhotoUploadedFragment.this)
                    .load(glideUrl)
                    .asBitmap()
                    .centerCrop()
                    .placeholder(getResources().getDrawable(R.drawable.photo_icon_placeholder))
                    .into(new BitmapImageViewTarget(mPhotoUploaded) {
                        @Override
                        protected void setResource(Bitmap resource) {
                            RoundedBitmapDrawable drawable = RoundedBitmapDrawableFactory.create(ViewPhotoUploadedFragment.this.getResources(),
                                    Bitmap.createScaledBitmap(resource, 95, 95, false));
                            drawable.setCircular(true);
                            mPhotoUploaded.setImageDrawable(drawable);
                        }
                    });
        }

    }


    /**
     * =========================================
     * adapter
     */
    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder>{

        List<FileModel> mModels;

        public PhotoAdapter(List<FileModel> models) {
            mModels = models;
        }

        @Override
        public PhotoHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            return new PhotoHolder(inflater, parent);
        }

        @Override
        public void onBindViewHolder(PhotoHolder holder, int position) {
            holder.bind(mModels.get(position));
        }

        @Override
        public int getItemCount() {
            return mModels.size();
        }

        public void setModels(List<FileModel> models) {
            mModels = models;
        }
    }

}
