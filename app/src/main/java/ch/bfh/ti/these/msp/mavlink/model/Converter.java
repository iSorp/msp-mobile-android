package ch.bfh.ti.these.msp.mavlink.model;

import ch.bfh.ti.these.msp.models.Action;
import ch.bfh.ti.these.msp.models.Mission;
import ch.bfh.ti.these.msp.models.WayPoint;

public class Converter {

    public static MavlinkMission convertToUploadItems(Mission m) {
        MavlinkMission mavlinkMission = new MavlinkMission();
        for (WayPoint w: m.getWayPoints()) {
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
                        a.getSensor(),
                        a.getCommand(),
                        a.getParam3(),
                        a.getParam4()
                );
                mavlinkMission.addUploadItem(actionItem);
            }
        }

        return mavlinkMission;
    }
}
