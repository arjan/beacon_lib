package nl.miraclethings.beaconlib;

import org.altbeacon.beacon.Beacon;

import java.util.ArrayList;
import java.util.List;

public class Zone {

    String id;
    public List<Beacon> beacons;
    String title;

    public static class Beacon {
        public String uuid;
        public int major;
        public int minor;

        public Beacon(String uuid, int major, int minor) {
            this.uuid = uuid;
            this.major = major;
            this.minor = minor;
        }

        @Override
        public String toString() {
            return "Beacon{" +
                    "uuid='" + uuid + '\'' +
                    ", major='" + major + '\'' +
                    ", minor='" + minor + '\'' +
                    '}';
        }

        public boolean isEqualToBeacon(org.altbeacon.beacon.Beacon beacon) {
            if (beacon == null) return false;
            return uuid.equalsIgnoreCase(beacon.getId1().toUuidString())
                    && major == beacon.getId2().toInt()
                    && minor == beacon.getId3().toInt();
        }
    }

    public Zone() {
        beacons = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public List<Beacon> getBeacons() {
        return beacons;
    }

    public String getTitle() {
        return title;
    }
}
