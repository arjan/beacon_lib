package nl.miraclethings.beaconlib.detector;

/**
 * Created by arjan on 23-9-15.
 */
public class DetectorResult {
    public String zone;
    public boolean zoneChanged;
    public String debugMessage;

    public DetectorResult(String zone, boolean zoneChanged, String debugMessage) {
        this.zone = zone;
        this.zoneChanged = zoneChanged;
        this.debugMessage = debugMessage;
    }
}
