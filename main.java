package io.github.njiasafe.template.ui.activities;

import android.Manifest;
import android.app.Dialog;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;
import butterknife.OnClick;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import io.github.njiasafe.data.model.RouteApiResponse;
import io.github.njiasafe.domain.Constants;
import io.github.njiasafe.domain.models.RouteModel;
import io.github.njiasafe.domain.models.RouteRating;
import io.github.njiasafe.presentation.alerts.AlertPresenter;
import io.github.njiasafe.presentation.mainmappresenter.MainMapPresenter;
import io.github.njiasafe.template.R;
import io.github.njiasafe.template.ui.adapters.RouteAdapter;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;

public class MainActivity extends BaseActivity
    implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback,
    MainMapPresenter.MyView {

  private static final String TAG = "mainactivity";
  @Inject MainMapPresenter mainMapPresenter;
  @Inject AlertPresenter alertPresenter;

  private GoogleMap map;
  private Marker myLocation;
  private ArrayList<Polyline> polylines = new ArrayList<Polyline>();
  private List<LatLng> directionList = new ArrayList<LatLng>();
  private List<LatLng> accidepoints = new ArrayList<LatLng>();
  private Dialog routeDialog;
  private PlaceAutocompleteFragment originInputATV;
  private PlaceAutocompleteFragment destinationInputAtTV;
  private String startPlace = Constants.EMPTY_STRING;
  private String destinationPlace = Constants.EMPTY_STRING;
  private LinearLayout layout;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    injector().inject(MainActivity.this);
    Toolbar toolbar = findViewById(R.id.toolbar);
    setTitle("");
    setSupportActionBar(toolbar);
    layout = findViewById(R.id.placeInput);
    SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
        .findFragmentById(R.id.maproute);
    mapFragment.getMapAsync(this);

    setPlaceDestinationInput();
    setPlaceOriginInput();

    FloatingActionButton fab = findViewById(R.id.fab);
    fab.setOnClickListener(view -> layout.setVisibility(View.VISIBLE));

    DrawerLayout drawer = findViewById(R.id.drawer_layout);

    ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
        this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
    drawer.addDrawerListener(toggle);
    toggle.syncState();

    NavigationView navigationView = findViewById(R.id.nav_view);
    navigationView.setNavigationItemSelectedListener(this);
  }

  @Override
  public void onBackPressed() {
    DrawerLayout drawer = findViewById(R.id.drawer_layout);
    if (drawer.isDrawerOpen(GravityCompat.START)) {
      drawer.closeDrawer(GravityCompat.START);
    } else {
      super.onBackPressed();
    }
  }

  @Override protected void onStart() {
    super.onStart();
    mainMapPresenter.setView(this);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    //getMenuInflater().inflate(R.menu.main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    int id = item.getItemId();

    //noinspection SimplifiableIfStatement
    if (id == R.id.action_settings) {
      return true;
    }

    return super.onOptionsItemSelected(item);
  }

  @SuppressWarnings("StatementWithEmptyBody")
  @Override
  public boolean onNavigationItemSelected(MenuItem item) {
    // Handle navigation view item clicks here.
    //int id = item.getItemId();
    //
    //if (id == R.id.nav_camera) {
    //  // Handle the camera action
    //} else if (id == R.id.nav_gallery) {
    //
    //} else if (id == R.id.nav_slideshow) {
    //
    //} else if (id == R.id.nav_manage) {
    //
    //} else if (id == R.id.nav_share) {
    //
    //} else if (id == R.id.nav_send) {

    //}

    DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
    drawer.closeDrawer(GravityCompat.START);
    return true;
  }

  @OnClick(R.id.findRoute) public void findRouteClick() {
    getRouteRating();
  }

  @Override public void route(RouteModel list) {
    Toast.makeText(this, list.getStatus(), Toast.LENGTH_SHORT).show();
  }

  @Override public void routeRating(List<RouteRating> routeRatingList) {
    routeDialog(routeRatingList);
  }

  @Override public void setMap(RouteApiResponse response) {

  }

  @Override public void message(String s) {
    Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
  }

  @Override public void setOriginPrediction(String[] places) {

  }

  @Override public void setPolyLine(
      ArrayList<io.github.njiasafe.data.model.mapmodels.Polyline> polylineArrayList) {

    getLatLong(polylineArrayList);
  }

  @Override public void map(String route, String accidents, int position) {
    drawRouteOnMap(map, decodePoly(route), returnColor(position));
    ;
    if (routeDialog != null) {
      routeDialog.dismiss();
    }
    if (!accidents.isEmpty()) {
      accidepoints=decodePoly(accidents);
      addAcidents(accidepoints);
    }
  }

  @Override public void onMapReady(GoogleMap googleMap) {
    map = googleMap;

    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        != PackageManager.PERMISSION_GRANTED
        && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        != PackageManager.PERMISSION_GRANTED) {
      return;
    }
    map.setMyLocationEnabled(true);

    map.setOnMyLocationChangeListener(new GoogleMap.OnMyLocationChangeListener() {

      @Override
      public void onMyLocationChange(Location arg0) {
        if (myLocation != null) {
          myLocation.remove();
        }
        myLocation = map.addMarker(
            new MarkerOptions().position(new LatLng(arg0.getLatitude(), arg0.getLongitude()))
                .title("It's Me!"));
        LatLng current = new LatLng(arg0.getLatitude(), arg0.getLongitude());
        map.moveCamera(CameraUpdateFactory.newLatLng(current));
        CameraPosition cameraPosition = new CameraPosition.Builder()
            .target(current)
            .zoom(17)
            .build();
        map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
      }
    });
  }

  private void drawRouteOnMap(GoogleMap map, List<LatLng> positions, int color) {
    PolylineOptions options = new PolylineOptions().width(5).color(color).geodesic(true);
    options.addAll(positions);
    Polyline polyline = map.addPolyline(options);

    CameraPosition cameraPosition = new CameraPosition.Builder()
        .target(new LatLng(positions.get(1).latitude, positions.get(1).longitude))
        .zoom(10)
        .build();
    map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
  }

  private void getLatLong(ArrayList<io.github.njiasafe.data.model.mapmodels.Polyline> polylines) {
    for (int i = 0; i < polylines.size(); i++) {
      String points = polylines.get(i).getPoints();
      List<LatLng> singlePolyline = decodePoly(points);
      directionList.addAll(singlePolyline);
      drawRouteOnMap(map, directionList, returnColor(i));
    }
  }

  private int returnColor(int c) {
    switch (c) {
      case 0:
        return Color.BLUE;

      case 1:
        return Color.GREEN;

      case 2:
        return Color.RED;

      case 3:
        return Color.RED;

      default:
        return Color.BLACK;
    }
  }

  private void setPlaceOriginInput() {
    originInputATV = (PlaceAutocompleteFragment) this.getFragmentManager()
        .findFragmentById(R.id.place_autocomplete_fragment);
    originInputATV.setOnPlaceSelectedListener(new PlaceSelectionListener() {
      @Override
      public void onPlaceSelected(Place place) {
        drawrMaker(place.getLatLng(), place.getName().toString());
        startPlace = place.getName().toString();
      }

      @Override
      public void onError(Status status) {

        Log.i(TAG, getString(R.string.error_occured) + status);
      }
    });
  }

  private void setPlaceDestinationInput() {
    destinationInputAtTV = (PlaceAutocompleteFragment) this.getFragmentManager()
        .findFragmentById(R.id.place_autocomplete_fragmentd);
    destinationInputAtTV.setOnPlaceSelectedListener(new PlaceSelectionListener() {
      @Override
      public void onPlaceSelected(Place place) {
        drawrMaker(place.getLatLng(), place.getName().toString());
        destinationPlace = place.getName().toString();
      }

      @Override
      public void onError(Status status) {

        Log.i(TAG, getString(R.string.error_occured) + status);
      }
    });
  }

  public void placeDialog() {

  }

  public void routeDialog(List<RouteRating> routeRatings) {
    dismissProgressDialog();
    routeDialog = new Dialog(this, R.style.Theme_dialog);
    routeDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
    Window window = routeDialog.getWindow();
    WindowManager.LayoutParams wlp = window.getAttributes();

    wlp.gravity = Gravity.CENTER;
    wlp.horizontalMargin = 20;
    wlp.flags &= ~WindowManager.LayoutParams.FLAG_DIM_BEHIND;
    wlp.dimAmount = (float) 0.0;
    window.setAttributes(wlp);
    window.setLayout(LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.MATCH_PARENT);
    routeDialog.setContentView(R.layout.dialog_route);
    routeDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
    ListView routeListView = routeDialog.findViewById(R.id.routeList);
    routeListView.setAdapter(
        new RouteAdapter(this, R.layout.item_route, routeRatings, mainMapPresenter));
    routeDialog.show();
  }

  private void drawrMaker(LatLng latLng, String title) {
    MarkerOptions markerOptions = new MarkerOptions().position(latLng).title(title);

    map.addMarker(markerOptions);
  }

  private void addAcidents(List<LatLng> accideponts) {

    for (int i = 0; i < accideponts.size(); i++) {
      double latitude = accideponts.get(i).latitude;
      double longitude = accideponts.get(i).longitude;
      // create marker
      MarkerOptions marker = new MarkerOptions().position(new LatLng(latitude, longitude));
      marker.icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_launcher_foregroundcvbvc));
      marker.title("Accident");
      marker.flat(true);
      map.addMarker(marker);
    }
  }

  private void getRouteRating() {
    if (!startPlace.isEmpty() && !destinationPlace.isEmpty()) {
      layout = findViewById(R.id.placeInput);
      layout.setVisibility(View.GONE);
      mainMapPresenter.getRouteRating(startPlace, destinationPlace);
      showProgressDialog();
    } else {
      customToast(getString(R.string.requestOP));
    }
  }

  //never wrote this code
  private List<LatLng> decodePoly(String encoded) {
    List<LatLng> poly = new ArrayList<>();
    int index = 0, len = encoded.length();
    int lat = 0, lng = 0;

    while (index < len) {
      int b, shift = 0, result = 0;
      do {
        b = encoded.charAt(index++) - 63;
        result |= (b & 0x1f) << shift;
        shift += 5;
      } while (b >= 0x20);
      int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
      lat += dlat;

      shift = 0;
      result = 0;
      do {
        b = encoded.charAt(index++) - 63;
        result |= (b & 0x1f) << shift;
        shift += 5;
      } while (b >= 0x20);
      int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
      lng += dlng;

      LatLng p = new LatLng((((double) lat / 1E5)),
          (((double) lng / 1E5)));
      poly.add(p);
    }

    return poly;
  }
}
