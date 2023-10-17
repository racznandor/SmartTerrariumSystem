package hu.unideb.inf.smartterrariumsystem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import de.nitri.gauge.Gauge;

public class MainActivity extends AppCompatActivity {

    FirebaseDatabase firebaseDatabase;

    DatabaseReference databaseReference;

    private Gauge tempGauge;
    private Gauge humGauge;

    public void imagesClicked(View view) {
        Intent intent = new Intent(this, ImagesListActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        firebaseDatabase = FirebaseDatabase.getInstance();

        databaseReference = firebaseDatabase.getReference("TempHum");

        tempGauge = findViewById(R.id.tempGauge);
        humGauge = findViewById(R.id.humGauge);

        getTempHum();
    }

    private void getTempHum() {

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                String tempData = snapshot.child("Temp").getValue().toString();
                String humData = snapshot.child("Hum").getValue().toString();
                String temp = tempData  + "°C";
                String hum = humData + "%";
                float t = Float.parseFloat(tempData);
                float h = Float.parseFloat(humData);

                if (t <= 100) {
                    tempGauge.moveToValue(t);
                    tempGauge.setLowerText(temp);
                }
                else{
                    tempGauge.moveToValue(100);
                    tempGauge.setLowerText(100 + "°C");
                }

                if(h <= 100) {
                    humGauge.moveToValue(h);
                    humGauge.setLowerText(hum);
                }
                else{
                    humGauge.moveToValue(100);
                    humGauge.setLowerText(100  + "%");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Fail to get data.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}