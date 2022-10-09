package com.berkesoft.javamaps.view;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.berkesoft.javamaps.R;
import com.berkesoft.javamaps.model.Place;
import com.berkesoft.javamaps.roomdb.PlaceDao;
import com.berkesoft.javamaps.roomdb.PlaceDatabase;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.berkesoft.javamaps.databinding.ActivityMapsBinding;
import com.google.android.material.snackbar.Snackbar;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMapLongClickListener {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    ActivityResultLauncher<String> permissionLauncher;
    LocationManager locationManager;
    LocationListener locationListener;
    SharedPreferences sharedPreferences;
    boolean info;
    PlaceDatabase db;
    PlaceDao placeDao;
    Place selectedPlace;
    double selectedlatitude;
    double selectedlongitude;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        //3.2) registerLauncher'ımızı burada tanımlıyoruz.
        registerLauncher();
        selectedlatitude = 0.0;
        selectedlongitude = 0.0;

        info = false;
        sharedPreferences = this.getSharedPreferences("com.berkesoft.javamaps",MODE_PRIVATE);

        db = Room.databaseBuilder(getApplicationContext(),PlaceDatabase.class,"Places").build();
        placeDao = db.placeDao();
        binding.saveButton.setEnabled(false);


    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        //5) Burada haritamızın üzerine uzun tıklanınca ne olacağını belirlemek için ilk önce MapsActivity sınıfına GoogleMap.OnMapLongClickListener implements ettik.
        //5.1) Daha sonra aşağıdaki setOnMapLongClickListener diyerek implements ettiğimiz sınıfa contex kuruyoruz.
        mMap.setOnMapLongClickListener(this);



        //8) Burada yapılan şey kullanıcı yeni kayıt mı yoksa eski kayıtına mı gidecek diye ayrım yapıyoruz.
        //8.1) bunun için eski ve yeni kayıt yaptığımız yerlerde farklı value değerlerinde intent alıyoruz ve ifle ayırıyoruz.
        Intent intent = getIntent();
        String intentInfo = intent.getStringExtra("info");
         if (intentInfo.equals("new")){

             binding.saveButton.setVisibility(View.VISIBLE);
             binding.deleteButton.setVisibility(View.GONE);

             //2) LocationManager = Genel konum yöneticimiz tüm işlemler bunun üzerinden yürüyor.
             locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

             //2.1) LocationManagerden gelen konum değiti uyarılarını alabilmek için kulandığımız bir arayüz.
             locationListener = new LocationListener() {
                 //onLocationChanged = konum değiştiğinde ne yapacağım diye bize metot verdi.
                 @Override
                 public void onLocationChanged(@NonNull Location location) {

                     //4) Güncel konum bilgisi almak için Latlng sınıfından bir obje oluşturup location dan getLatitude() ve getLongitude() alıyorum.
                     //4.1) Daha sonra moveCamera ile oluşturduğum obje bilgilerini ve zoom mesafesini giriyorum.
                     //Dİkkat: BUrası sharedPreference ile tekrar düzenlendi.

                     //4.6)
                     info = sharedPreferences.getBoolean("info", false);

                     if (!info){
                         LatLng userLocation = new LatLng(location.getLatitude(),location.getLongitude());
                         mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation,15));
                         sharedPreferences.edit().putBoolean("info", true).apply();
                     }




                 }
             };


             //3) İzin istemem gerekli. O yüzden izin isteme işlemlerini tamamlıyorum.
             if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                 if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_FINE_LOCATION)){
                     Snackbar.make(binding.getRoot(),"Permission needed for maps", Snackbar.LENGTH_INDEFINITE).setAction("Give Permission", new View.OnClickListener() {
                         @Override
                         public void onClick(View view) {
                             //Burada izin isteyeceğiz.
                             permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
                         }
                     }).show();
                 } else {
                     //Burada izin isteyeceğiz.
                     permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
                 }
             }else{
                 //İzin veriilmişse konuma gideceğiz.
                 locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0, locationListener);

                 //4.2) Kullanıcı uygulamayı açtığında ilk olarak eski konumu gözükecek. Aslında bunu yapmamıza gerek yok bu opsiyonel bir şey.
                 //4.3) Location sınıfından bir obje oluşturuyorum ve getLastKnownLocation diyerek eski konumumu bu objeye eşitliyorum.
                 //4.4) if komutuyla bu eski objem boş değil ise ilk önce eski konumu göster diye bir döngü yazıyorum.
                 //4.5) aynısını launcherde yaptım.

                 Location lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                 if (lastLocation != null) {
                     LatLng lastUserLocation = new LatLng(lastLocation.getLatitude(),lastLocation.getLongitude());
                     mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastUserLocation,15));
                 }
                 mMap.setMyLocationEnabled(true);
             }


         } else{
             mMap.clear();
             selectedPlace = (Place) intent.getSerializableExtra("place");
             LatLng latLng = new LatLng(selectedPlace.latitude,selectedPlace.longitude);
             mMap.addMarker(new MarkerOptions().position(latLng).title(selectedPlace.name));
             mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,15));

             binding.saveButton.setVisibility(View.GONE);
             binding.deleteButton.setVisibility(View.VISIBLE);
         }






        /*
        //Örnek. Buraya ihtiyaç yoktur
        //1) Haritamızda mark işaretinin yerini belirlemek için enlem ve boylam girmemiz lazım.
        //Biz şimdi eyfel kulesinin koordinatlarını aldık.
        //Eyfel kulesi = 48.858093,2.294694

        //1.1) LatLng = Enlem ve boylam sınıfımız.
        LatLng eiffel = new LatLng(48.858093,2.294694);

        //1.2) Eyfel üzerine mark işareti koymak için addMarkerı kullanıyoruz ve isim veriyoruz.
        mMap.addMarker(new MarkerOptions().position(eiffel).title("Eiffel Tower"));

        //1.3) MoveCamera diyerek mark'ı eyfele odaklıyoruz. CameraUpdateFactory diyerek konumumu güncelledim.
        // ve newLatLngZoom sayesinde hem pozisyonumu hem de eyfele ne kadar zoom yapması gerektiğini yazıyorum.
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(eiffel,15));

         */
    }



    //3.1) Burada izin isteme launcherimizi oluşturuyoruz.
    private void registerLauncher(){

        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
            @Override
            public void onActivityResult(Boolean result) {
                if (result){
                    //locationManager hata verdiği için tekrar bir if içine yazıyoruz. Bu sefer izin verildi diyoruz.
                    if (ContextCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0, locationListener);
                    }

                    //4.5.1) Yukarıda yaptığımın aynısını burda yaptım.
                    Location lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (lastLocation != null) {
                        LatLng lastUserLocation = new LatLng(lastLocation.getLatitude(),lastLocation.getLongitude());
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastUserLocation,15));
                    }

                }else{
                    Toast.makeText(MapsActivity.this, "Permission needed!", Toast.LENGTH_LONG).show();
                }
            }
        });
    }



    //5.2) İmlements ettiğimiz sınıfı override ediyoruz ve mu metot oluşuyor.
    @Override
    public void onMapLongClick(@NonNull LatLng latLng) {
        //5.3) Markırımızı ekliyoruz ve sadece bir marker gözükmesi için clear diyoruz. Böylece
        mMap.clear();
        mMap.addMarker(new MarkerOptions().position(latLng));
        selectedlatitude = latLng.latitude;
        selectedlongitude = latLng.longitude;

        binding.saveButton.setEnabled(true);
    }

    public void save(View view){
        Place place = new Place(binding.placeText.getText().toString(),selectedlatitude,selectedlongitude);
        //7) compositeDisposable içinde placeDao insert'ünü yap. subscribeOn sayesinde io içinde yap ve observeOn sayesinde Anroid'in mainThreadinde gözlemle dedim.
        // Öncelikle sınıf içerisinde bir compositeDisposable objesi oluşturduk.
        //7.1) en son subscribe yapacağım işlemi yazacağım.
        //7.2) veriyi kaydettikten sonra referans gösterdiğimiz yer. Yapılacak işlemi referans ediyoruz.
        compositeDisposable.add(placeDao.insert(place)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(MapsActivity.this::HandleResponse));

    }

    private void HandleResponse(){
        Intent intent = new Intent(MapsActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);

    }


    public void delete(View view){

        if (selectedPlace != null){
            compositeDisposable.add(placeDao.delete(selectedPlace)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(MapsActivity.this::HandleResponse));

        }





    }


}