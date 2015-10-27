package nl.miraclethings.beaconlib;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;

import nl.miraclethings.beaconlib.detector.DetectorResult;
import nl.miraclethings.beaconlib.detector.GlimwormBeaconDetector;

public class BeaconScannerService extends Service implements BeaconConsumer, RangeNotifier {

    static Logger logger = LoggerFactory.getLogger(BeaconScannerService.class);

    private Handler handler;

    private BeaconManager beaconManager;
    private Region beaconRegion;
    private IForegroundListener mListener;
    private String mCurrentZone = null;

    private IBeaconZoneDetector mDetector;

    public interface ServiceConnectedCallback {
        void onBeaconServiceConnected(LocalBinder binder);

        void onBeaconServiceDisconnected();
    }

    public static ServiceConnection ensureBeaconService(Context context, final ServiceConnectedCallback cb) {
        Intent service = new Intent(context, BeaconScannerService.class);
        context.startService(service);

        ServiceConnection conn = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                Log.i(TAG, "Bound to service!");
                cb.onBeaconServiceConnected((LocalBinder) service);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.v(TAG, "Disconnected from service!");
                cb.onBeaconServiceDisconnected();
            }
        };

        context.bindService(service, conn, 0);
        return conn;
    }


    public class LocalBinder extends Binder {
        public void setForegroundListener(IForegroundListener listener) {
            mListener = listener;
            startScanning();
        }

        public String getCurrentZone() {
            return mCurrentZone;
        }

        public void setZoneMap(ZoneMap map) {
            mDetector.setZoneMap(map);
        }
    }

    public interface IForegroundListener {

        void setStatusMessage(String msg);

        void setCurrentRegion(String i);
    }

    private LocalBinder mBinder = new LocalBinder();

    private static final String TAG = "BeaconService";


    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        stopSelf();
        return true;
    }


    @Override
    public void onCreate() {
        super.onCreate();

        logger.info("BeaconScannerService starting");
        handler = new Handler();

        beaconManager = BeaconManager.getInstanceForApplication(this);
        configureBeaconManager();
        beaconManager.bind(this);

        mDetector = new GlimwormBeaconDetector();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopScanning();
    }


    private void configureBeaconManager() {

        //BeaconManager.setDebug(true);

        beaconRegion = new Region("nl.amsterdammuseum.goldenage", null, null, null);

        beaconManager.setBackgroundMode(false);
        beaconManager.setRangeNotifier(this);

        List<BeaconParser> p = beaconManager.getBeaconParsers();
        // kontakt
        p.add(new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25"));
        // estimote
        p.add(new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));
        // roximity
        p.add(new BeaconParser().setBeaconLayout("m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25"));
        // gimbale
        //p.add(new BeaconParser().setBeaconLayout("m:0-3=ad7700c6"));

        beaconManager.setForegroundScanPeriod(2000);
        beaconManager.setForegroundBetweenScanPeriod(500);
    }


    private void startScanning() {
        if (!beaconManager.isAnyConsumerBound()) return;
        try {
            beaconManager.startRangingBeaconsInRegion(beaconRegion);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void stopScanning() {
        if (!beaconManager.isAnyConsumerBound()) return;
        try {
            beaconManager.stopRangingBeaconsInRegion(beaconRegion);
        } catch (RemoteException e) {
            e.printStackTrace();
        } finally {
            beaconManager.unbind(this);
        }
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        return START_STICKY;
    }

    private void runOnUiThread(Runnable runnable) {
        handler.post(runnable);
    }

    @Override
    public void onBeaconServiceConnect() {
        startScanning();
    }

    @Override
    public void didRangeBeaconsInRegion(final Collection<Beacon> beacons, Region region) {

        if (!mDetector.isConfigured()) {
            logger.info("- No zones configured, skipping beacon update -");
            return;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                DetectorResult result = mDetector.run(beacons);

                mCurrentZone = result.zone;

                if (mListener != null) {
                    if (result.zoneChanged) {
                        mListener.setCurrentRegion(result.zone);
                    }

                    mListener.setStatusMessage(result.debugMessage);
                }

            }

        });
    }
}
