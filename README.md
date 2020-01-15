# Mobile application for MSP

## DJI App-key
- create a DJI developer account
- create a new MSDK app
- copy the key to the file AndroidManifest.xml
```
<application ...>

    <meta-data
            android:name="com.dji.sdk.API_KEY"
            android:value="MY_DJI_KEY"/>
```


## Google api-key
- create a google developer api-key for android maps 
    - https://cloud.google.com/maps-platform/
- copy the api-key (GOOGLE_MAPS_API_KEY=MY_GOOGLE_KEY) to the file gradle.properties
