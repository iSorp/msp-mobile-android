package ch.bfh.ti.these.msp.mavlink.model;

import ch.bfh.ti.these.msp.db.ActionDao;
import ch.bfh.ti.these.msp.models.*;

import java.util.ArrayList;
import java.util.List;

public class Converter {

    public static MavlinkMission convertToUploadItems(Mission m) {
        MavlinkMission mavlinkMission = new MavlinkMission();
        for (Waypoint w: m.getWaypoints()) {
            MavlinkMissionUploadItem wpItem = new MavlinkMissionUploadItem(
                    w.getLongitude(),
                    w.getLatitude(),
                    w.getAltitude()
            );
            // @TODO set speed and delay
            wpItem.setBehavior(5, 2);
            mavlinkMission.addUploadItem(wpItem);
            for (Action a: w.getActions()) {
                MavlinkMissionUploadItem actionItem = new MavlinkMissionUploadItem(
                        w.getLongitude(),
                        w.getLatitude(),
                        w.getAltitude()
                );
                actionItem.setSensor(
                        a.getMavlinkSensor(),
                        a.getMavlinkCommand(),
                        a.getParam3(),
                        a.getParam4()
                );
                mavlinkMission.addUploadItem(actionItem);
            }
        }

        return mavlinkMission;
    }

    public static List<SensorData> convertToSensorData(MavlinkData da, ActionDao dao) {
        List<SensorData> sensorDataList = new ArrayList<>(da.getSensors().size());
        for (MavlinkData.SensorValue sd: da.getSensors()) {
            SensorData sensorData = new SensorData();

            sensorData.setWaypointActionId(dao.getActionId(da.getSeq(), sd.getSensorId(), sd.getCommandId()));
            sensorData.setTime(da.getTime());
            sensorData.setLongitude(da.getX());
            sensorData.setLatitude(da.getY());
            sensorData.setAltitude(da.getZ());
            sensorData.setData(sd.getValue());
        }

        return sensorDataList;
    }
}
