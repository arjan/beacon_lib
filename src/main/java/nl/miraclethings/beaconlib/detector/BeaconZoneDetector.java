package nl.miraclethings.beaconlib.detector;

import org.altbeacon.beacon.Beacon;

import java.util.Collection;

import nl.miraclethings.beaconlib.ZoneMap;

/**
 * Created by arjan on 23-9-15.
 */
public interface BeaconZoneDetector {
    void setZoneMap(ZoneMap zoneMap);
    boolean isConfigured();

    DetectorResult run(Collection<Beacon> beacons);
}
