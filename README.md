# beacon_lib
This Android library contains a tracker service for locating iBeacons and performing zone detection. The main service to use is `BeaconScannerService`.


## usage

```
mBeaconServiceConnection = BeaconScannerService.ensureBeaconService(context, this);
```

