/**
 * @author fatih & Sean
 *
 * mbulut@buffalo.edu
 */

package edu.buffalo.cse.phonelab.statusmonitor;

import java.util.Timer;
import java.util.TimerTask;

import edu.buffalo.cse.phonelab.utilities.Locks;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

public class StatusMonitorLocation extends Service {

	Timer timer;
	LocationManager locationManager; 
	LocationListener locationListener;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i("PhoneLab-" + getClass().getSimpleName(), "Learning Location");

		Locks.acquireWakeLock(this);
		
		timer = new Timer();
		timer.schedule(new TimerTask() {
			public void run() {
				locationManager.removeUpdates(locationListener);
				Log.i("PhoneLab-" + getClass().getSimpleName(), "Couldn't learn location");
				Locks.releaseWakeLock();
				StatusMonitorLocation.this.stopSelf();
			}
		}, 60000*1);

		locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
		locationListener = new LocationListener() {
			public void onLocationChanged(Location location) {
				makeUseOfNewLocation(location);
			}

			private void makeUseOfNewLocation(Location location) {
				if (location.getAccuracy() < 3000 && location.getTime() > System.currentTimeMillis() - 20000) {
					locationManager.removeUpdates(this);
					timer.cancel();
					Log.i("PhoneLab-" + getClass().getSimpleName(),
							"Location: Latitude: "
									+ location.getLatitude()
									+ " Longitude: "
									+ location.getLongitude()
									+ " Accuracy: " + location.getAccuracy());

					Locks.releaseWakeLock();
					StatusMonitorLocation.this.stopSelf();
				}
			}

			public void onStatusChanged(String provider, int status, Bundle extras) {}
			public void onProviderEnabled(String provider) {}
			public void onProviderDisabled(String provider) {
				locationManager.removeUpdates(this);
				timer.cancel();
				StatusMonitorLocation.this.stopSelf();
			}
		};

		// Register the listener with the Location Manager to receive location updates
		locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);

		return START_STICKY;
	}

}
