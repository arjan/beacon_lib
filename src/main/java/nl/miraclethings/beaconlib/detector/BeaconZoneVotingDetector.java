package nl.miraclethings.beaconlib.detector;

import org.altbeacon.beacon.Beacon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import nl.miraclethings.beaconlib.Zone;
import nl.miraclethings.beaconlib.ZoneMap;

/**
 * Created by arjan on 23-9-15.
 */
public class BeaconZoneVotingDetector implements BeaconZoneDetector {

    static Logger logger = LoggerFactory.getLogger(BeaconZoneVotingDetector.class);

    private int mCandidateRegionCount;

    private String mCandidateZone;
    private ZoneMap mZoneMap;
    private String mCurrentZone;

    @Override
    public DetectorResult run(Collection<Beacon> beacons) {
        int numBeaconsFound = 0;
        List<String> zoneCandidates = new ArrayList<String>();
        Beacon foundBeacon = null;

        for (Beacon beacon : beacons) {

            if (beacon.getDistance() > 50) {
                logger.info("Skipping beacon {}, distance too big", beacon.toString());
                continue;
            }

            //logger.info("Beacon {} {} {} distance {}", beacon.getId1().toUuidString(), beacon.getId2().toInt(), beacon.getId3().toInt(), beacon.getDistance());

            for (Map.Entry<String, Zone> entry : mZoneMap.entrySet()) {

                String zoneIdentifier = entry.getKey();
                Zone zone = entry.getValue();

                for (Zone.Beacon checkBeacon : zone.beacons) {
                    if (checkBeacon.uuid != null && !checkBeacon.uuid.equalsIgnoreCase(beacon.getId1().toUuidString())) {
                        continue;
                    }
                    if (checkBeacon.major != 0 && checkBeacon.major != beacon.getId2().toInt()) {
                        continue;
                    }
                    if (checkBeacon.minor != 0 && checkBeacon.minor != beacon.getId3().toInt()) {
                        continue;
                    }

                    foundBeacon = beacon;

                    if (!zoneCandidates.contains(zoneIdentifier)) {
                        zoneCandidates.add(zoneIdentifier);
                    }
                    numBeaconsFound++;
                }
            }
        }

        int zonesFound = zoneCandidates.size();
        boolean zoneChanged = determineRegionWinner(zonesFound == 1 ? zoneCandidates.get(0) : "", zonesFound > 1);

        String msg = "No beacons";
        if (numBeaconsFound >= 1) {
            msg = "B: " + numBeaconsFound+ " L: " + zonesFound;
        }
        msg += " Z: " + mCurrentZone;
        return new DetectorResult(mCurrentZone, zoneChanged, msg);
    }

    private boolean determineRegionWinner(String region, boolean hasMultiple) {

        boolean zoneChanged = false;

        if (mCurrentZone == null && !region.equals("")) {
            // shortcut
            mCurrentZone = region;
            zoneChanged = true;
        } else if (!hasMultiple) {
            if (!region.equals(mCurrentZone)) {
                if (region.equals(mCandidateZone)) {
                    mCandidateRegionCount++;
                } else {
                    mCandidateZone = region;
                    mCandidateRegionCount = 0;
                }

                int limit = 2;
                if (mCandidateZone.equals("")) {
                    limit = 2;
                }

                if (mCandidateRegionCount >= limit) {
                    mCurrentZone = mCandidateZone;
                    mCandidateRegionCount = 0;
                    zoneChanged = true;
                    logger.info("Change region to: " + mCurrentZone);
                }
            }
        }

        if (zoneChanged && "".equals(mCurrentZone)) {
                mCurrentZone = null;
        }

        return zoneChanged;
    }

    @Override
    public void setZoneMap(ZoneMap zoneMap) {
        mZoneMap = zoneMap;
    }

    @Override
    public boolean isConfigured() {
        return mZoneMap != null;
    }
}
