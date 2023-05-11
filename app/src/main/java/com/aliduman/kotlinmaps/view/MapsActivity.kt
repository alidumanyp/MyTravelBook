package com.aliduman.kotlinmaps.view

import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.room.Room
import com.aliduman.kotlinmaps.R
import com.aliduman.kotlinmaps.databinding.ActivityMapsBinding
import com.aliduman.kotlinmaps.model.Place
import com.aliduman.kotlinmaps.roomdb.PlaceDao
import com.aliduman.kotlinmaps.roomdb.PlaceDatabase
import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMapLongClickListener {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding

    private lateinit var locationManager : LocationManager
    private lateinit var locationListener : LocationListener

    private lateinit var permissionLauncher : ActivityResultLauncher<String>

    private lateinit var sharedPreferences : SharedPreferences
    private var trackBoolean : Boolean? = null

    private var selectedLatitude : Double? = null
    private var selectedLongitude : Double? = null

    private lateinit var db : PlaceDatabase
    private lateinit var placeDao : PlaceDao

    val compositeDisposable = CompositeDisposable() //hafıza sorunlarıyla karşılaşmamak için kullan at kullanıyoruz.

    var placeFromMain : Place? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar!!.hide()

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)



        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        registerLauncher()


        sharedPreferences = this.getSharedPreferences("com.aliduman.kotlinmaps", MODE_PRIVATE)
        trackBoolean = false

        selectedLongitude = 0.0
        selectedLatitude = 0.0

        db = Room.databaseBuilder(applicationContext, PlaceDatabase::class.java,"Places")
            //.allowMainThreadQueries()
            .build()

        placeDao = db.placeDao()

        binding.saveButton.isEnabled = false
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setOnMapLongClickListener(this)//bunu kullancagımızı söyleyip, bu şekilde bağlıyoruz.


        val intent = intent
        val info = intent.getStringExtra("info")

        if(info == "new") {
            binding.placeText.setText("")
            binding.deleteButton.visibility = View.GONE
            binding.saveButton.visibility = View.VISIBLE

            locationManager = this.getSystemService(LOCATION_SERVICE) as LocationManager //casting

            locationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {//her konum degistiginde verir.
                    trackBoolean = sharedPreferences.getBoolean("track",false)
                    if(!trackBoolean!!) {
                        val userLocation = LatLng(location.latitude,location.longitude)
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation,15f))
                        sharedPreferences.edit().putBoolean("track",true).apply()
                    }


                }
            }

            if (ContextCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                if (ActivityCompat.shouldShowRequestPermissionRationale(this@MapsActivity,android.Manifest.permission.ACCESS_FINE_LOCATION)){
                    //request permission with rationale
                    Snackbar.make(binding.root,"Permission needed for location",Snackbar.LENGTH_INDEFINITE).setAction("Give Pemrission") {
                        //request permission
                        permissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
                    }.show()
                } else{
                    //request permission
                    permissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
                }

            } else{
                //permission granted
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0f,locationListener)
                val lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)//son bilinen konumu almak

                if (lastLocation != null) {
                    val lastUserLocation = LatLng(lastLocation.latitude,lastLocation.longitude)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastUserLocation,15f))
                }
                mMap.isMyLocationEnabled = true
            }

            /*
            val canadaDogPark = LatLng(43.638920, -79.396719)
            mMap.addMarker(MarkerOptions().position(canadaDogPark).title("Canada Dog Park"))
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(canadaDogPark,16.4f))
            */

        } else {
            mMap.clear()

            placeFromMain = intent.getSerializableExtra("selectedPlace") as Place?

            placeFromMain?.let {

                val latLng = LatLng(it.latitude,it.longitude)
                mMap.addMarker(MarkerOptions().position(latLng).title(it.name))
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,15f))

                binding.placeText.setText(it.name)
                binding.deleteButton.visibility = View.VISIBLE
                binding.saveButton.visibility = View.GONE
            }


        }



    }

    private fun registerLauncher() {

        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it){
                if (ContextCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0f,locationListener)

                    val lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)//son bilinen konumu almak
                    if (lastLocation != null) {
                        val lastUserLocation = LatLng(lastLocation.latitude,lastLocation.longitude)
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastUserLocation,15f))
                    }
                    mMap.isMyLocationEnabled = true
                }
            } else {
                //izin vermedi
                Toast.makeText(this@MapsActivity,"Permission needed",Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onMapLongClick(p0: LatLng) {
        mMap.clear()
        mMap.addMarker(MarkerOptions().position(p0))

        binding.saveButton.isEnabled = true

        selectedLatitude = p0.latitude
        selectedLongitude = p0.longitude
    }

    fun save(view: View){

        //Main thread(UI) -> kullanıcı arayüzünün işlemleri yapılır.
        //Default thread -> CPU, çok yoğun işlemleri yaparken kullanılır.
        //IO thread -> internet/Database

        val place = Place(binding.placeText.text.toString(),selectedLatitude!!,selectedLatitude!!)
        compositeDisposable.add(
            placeDao.insert(place)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::handleResponse)//bitince bu fonksiyon çalıştırılacak..
        )

    }

    private fun handleResponse() {
        val intent = Intent(this,MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
    }

    fun delete(view: View) {

        placeFromMain?.let {
            compositeDisposable.add(
                placeDao.delete(it)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::handleResponse)//bitince bu fonksiyon çalıştırılacak..
            )

        }



    }

    override fun onDestroy() {
        super.onDestroy()

        compositeDisposable.clear()
    }
}