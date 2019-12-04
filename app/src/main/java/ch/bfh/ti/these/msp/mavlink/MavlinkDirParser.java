package ch.bfh.ti.these.msp.mavlink;

import java.util.ArrayList;
import java.util.List;

public class MavlinkDirParser {

    public static int countFiles(String responseString) {
        return parseString(responseString, MavlinkFileType.File).size();
    }

    public static int countSubFolders(String responseString) {
        return parseString(responseString, MavlinkFileType.Folder).size();
    }

    public static int getFiles(String responseString) {
        return parseString(responseString, MavlinkFileType.File).size();
    }

    private static List<String[]> parseString(String responseString, MavlinkFileType typeFilter) {
        List<String[]> result = new ArrayList<>();
        String[] entries = responseString.split("\\\\0");
        for (String entry: entries) {
            MavlinkFileType type = MavlinkFileType.fromString(entry.substring(0, 1));
            // filter types
            if (!type.equals(typeFilter)) {
                continue;
            }

            switch (type) {
                case File:
                    String[] file = entry.split("\\\\t");
                    file[0] = file[0].substring(1);
                    result.add(file);
                    break;
                case Folder:
                    result.add(new String[]{entry.substring(1)});
                    break;
                case Skip:
                    // Ignore
                    break;
            }
        }
        return result;
    }

    enum MavlinkFileType {
        Folder("D"),
        File("F"),
        Skip("S");

        private final String text;

        MavlinkFileType(final String text) {
            this.text = text;
        }

        public static MavlinkFileType fromString(String text) {
            for (MavlinkFileType b : MavlinkFileType.values()) {
                if (b.text.equalsIgnoreCase(text)) {
                    return b;
                }
            }
            return null;
        }
    }
}
