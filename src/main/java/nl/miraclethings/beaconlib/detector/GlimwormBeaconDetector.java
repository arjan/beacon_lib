package nl.miraclethings.beaconlib.detector;

import org.altbeacon.beacon.Beacon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import nl.miraclethings.beaconlib.Zone;
import nl.miraclethings.beaconlib.ZoneMap;

/**
 * This class implements the Glimworm beacon detection algorithm
 * Based on Objective C code received from Jonathan Carter 31-08-205.
 *
 * Created by Arjan on 23-9-15.
 */
public class GlimwormBeaconDetector implements BeaconZoneDetector {

    static Logger logger = LoggerFactory.getLogger(GlimwormBeaconDetector.class);

    final static long MINIMUM_SWITCH_TIME = 2000;    // minimum time between switching beacons 2s
    final static long BEACON_TIMEOUT = 5000;         // beacons are dead if not seen for 5s
    final static int MINIMUM_RSSI_DIFF = 4;         // minimum difference in RSSI
    private static final double CUTOFF_DISTANCE = 6;
    private static final double RSSI_MOMENTUM = 0.4;

    private String mCurrentZone;

    private ZoneMap mZoneMap;

    LinkedList<BeaconWithTime> filteredBeacons = new LinkedList<>();
    BeaconWithTime lastBeacon;
    long lastSwitchTime;


    @Override
    public DetectorResult run(Collection<Beacon> beacons) {
        if (beacons.size() == 0) {
            return new DetectorResult(mCurrentZone, false, "No beacons observed, Z: " + mCurrentZone);
        }

        // update filtered beacon list with observed beacons
        for (Beacon b : beacons) {
//            logger.info("beacon {} d = {}, rssi = {}", b.getId3().toInt(), b.getDistance(), b.getRssi());

            // ignore beacons with proximity "unknown"
            if (b.getDistance() > CUTOFF_DISTANCE) {
                continue;
            } else {
                if (filteredBeacons.contains(b)) {
                    BeaconWithTime f = filteredBeacons.get(filteredBeacons.indexOf(b));
                    f.markSeen();
                    f.updateMomentumRssi(b.getRssi());
                } else {
                    filteredBeacons.add(new BeaconWithTime(b));
                }
            }
        }

        // filter out old beacons and beacons not belonging to any zone
        for (Iterator<BeaconWithTime> iterator = filteredBeacons.iterator(); iterator.hasNext(); ) {
            BeaconWithTime fb = iterator.next();
            if (fb.isExpired()) {
                iterator.remove();
                continue;
            }
            // filter out beacons not belonging to any zone
            boolean hasZone = false;
            for (Map.Entry<String, Zone> z : mZoneMap.entrySet()) {
                for (Zone.Beacon b : z.getValue().getBeacons()) {
                    if (b.isEqualToBeacon(fb)) {
                        hasZone = true;
                        break;
                    }
                }
                if (hasZone) break;
            }
            if (!hasZone) {
                iterator.remove();
            }
        }

        if (filteredBeacons.size() == 0) {
            if (System.currentTimeMillis() - lastSwitchTime > MINIMUM_SWITCH_TIME) {
                mCurrentZone = null;
            }

            return new DetectorResult(mCurrentZone, false, "All beacons expired, Z: " + mCurrentZone);
        }

        BeaconWithTime currentBeacon = selectBestBeacon();
        logger.info("beacon winner = {}", currentBeacon.getId3().toInt());
        boolean changed;
        if (!currentBeacon.equals(lastBeacon)) {
            logger.info("Beacon change!!!");
            lastBeacon = currentBeacon;
        }

        // determine zone based on last beacon
        String foundZone = null;
        for (Map.Entry<String, Zone> z : mZoneMap.entrySet()) {
            for (Zone.Beacon b : z.getValue().getBeacons()) {
                if (b.isEqualToBeacon(lastBeacon)) {
                    foundZone = z.getKey();
                    break;
                }
            }
        }
        if (foundZone == null) {
            return new DetectorResult(mCurrentZone, false, "No zone found " + lastBeacon.toString());
        }

        if (foundZone.equals(mCurrentZone)) {
            changed = false;
        } else {
            mCurrentZone = foundZone;
            changed = true;
        }

        return new DetectorResult(mCurrentZone, changed, "B: " + lastBeacon.getId3().toInt() + " Z: " + mCurrentZone);
    }

    private BeaconWithTime selectBestBeacon() {
        // select beacon with max RSSI
        BeaconWithTime maxRssiBeacon = filteredBeacons.get(0);
        for (BeaconWithTime fb : filteredBeacons) {
            if (fb.getRssi() > maxRssiBeacon.getRssi()) {
                maxRssiBeacon = fb;
            }
        }

        long now = System.currentTimeMillis();

        if (lastBeacon == null) {
            // current beacon is the max RSSI beacon
            lastBeacon = maxRssiBeacon;
            lastSwitchTime = now;
            return maxRssiBeacon;
        }

        if (maxRssiBeacon.equals(lastBeacon)) {
            return lastBeacon;
        }

        if (now - lastSwitchTime < MINIMUM_SWITCH_TIME) {
            return lastBeacon;
        }

        int rssiDiff = maxRssiBeacon.getRssi() - lastBeacon.getRssi();
        if (rssiDiff < MINIMUM_RSSI_DIFF) {
            return lastBeacon;
        }

        return maxRssiBeacon;
    }

    @Override
    public void setZoneMap(ZoneMap zoneMap) {
        mZoneMap = zoneMap;
    }

    @Override
    public boolean isConfigured() {
        return mZoneMap != null;
    }

    public static class BeaconWithTime extends Beacon {
        private long timestamp;

        public BeaconWithTime(Beacon otherBeacon) {
            super(otherBeacon);
            markSeen();
        }

        public boolean isExpired() {
            return (System.currentTimeMillis() - timestamp) > BEACON_TIMEOUT;
        }

        public void markSeen() {
            this.timestamp = System.currentTimeMillis();
        }

        public void updateMomentumRssi(int rssi) {
            setRssi((int) ((1-RSSI_MOMENTUM) * getRssi() + RSSI_MOMENTUM * rssi));
        }
    }
}
