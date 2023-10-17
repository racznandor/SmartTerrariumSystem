package hu.unideb.inf.smartterrariumsystem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageReference;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ImagesListActivity extends AppCompatActivity {

    private RecyclerView myRecyclerView;
    private Button moreImagesButton;
    private List<String> moreImages = new ArrayList<>();
    private List<String> tenImages = new ArrayList<>();

    FirebaseStorage storage;
    StorageReference storageRef;
    StorageReference imagesRef;

    FirebaseDatabase database;
    DatabaseReference databaseRef;
    DatabaseReference imagesDataRef;

    public void moreClicked(View view) {
        loadMoreImages();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_images_list);

        myRecyclerView = findViewById(R.id.my_recycler_view);
        moreImagesButton = findViewById(R.id.moreImagesButton);
        myRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();
        imagesRef = storageRef.child("images");

        database = FirebaseDatabase.getInstance();
        databaseRef = database.getReference();
        imagesDataRef = databaseRef.child("images");

        loadTenImages();
    }

    private void loadMoreImages() {
        imagesRef.listAll().addOnSuccessListener(new OnSuccessListener<ListResult>() {
            @Override
            public void onSuccess(ListResult listResult) {
                List<StorageReference> items = listResult.getItems();
                for (StorageReference item : items) {
                    item.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            String imageUrl = uri.toString();
                            moreImages.add(imageUrl);
                            Collections.sort(moreImages, new Comparator<String>() {
                                @Override
                                public int compare(String fileName1, String fileName2) {
                                    try {
                                        String f1 = imageDate(fileName1);
                                        String f2 = imageDate(fileName2);
                                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.ENGLISH);
                                        Date date1 = dateFormat.parse(f1.split("\\.")[0]);
                                        Date date2 = dateFormat.parse(f2.split("\\.")[0]);

                                        return date1.compareTo(date2);
                                    } catch (ParseException e) {
                                        e.printStackTrace();
                                        return 0;
                                    }
                                }
                            });
                            Collections.reverse(moreImages);
                            if (moreImages.size() == items.size()){
                                myRecyclerView.setAdapter(new ImageAdapter(ImagesListActivity.this, moreImages));
                            }
                        }
                    });
                }
            }
        });
    }

    private String imageDate(String filename){
        String pattern = "\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2}";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(filename);

        if (m.find()) {
            String extractedDate = m.group(0);
            return extractedDate;
        } else {
            return "";
        }
    }

    private void loadTenImages(){
        imagesDataRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot imageSnapshot : dataSnapshot.getChildren()) {
                    String imageName = imageSnapshot.child("fileName").getValue(String.class);
                    StorageReference imageRef = imagesRef.child(imageName);
                    imageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            String imageUrl = uri.toString();
                            tenImages.add(imageUrl);
                            Collections.sort(tenImages, new Comparator<String>() {
                                @Override
                                public int compare(String fileName1, String fileName2) {
                                    try {
                                        String f1 = imageDate(fileName1);
                                        String f2 = imageDate(fileName2);
                                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.ENGLISH);
                                        Date date1 = dateFormat.parse(f1.split("\\.")[0]);
                                        Date date2 = dateFormat.parse(f2.split("\\.")[0]);

                                        return date1.compareTo(date2);
                                    } catch (ParseException e) {
                                        e.printStackTrace();
                                        return 0;
                                    }
                                }
                            });
                            Collections.reverse(tenImages);
                            myRecyclerView.setAdapter(new ImageAdapter(ImagesListActivity.this, tenImages));
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            System.out.println("Error");
                        }
                    });
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.out.println("Error");
            }
        });
    }
}