package ch.bfh.ti.these.msp.db;

import androidx.room.TypeConverter;

import java.util.Date;

public class ValueConverters {

    @TypeConverter
    public Date fromTimestamp(Long value) {
        return value == null ? null : new Date(value);
    }

    @TypeConverter
    public Long dateToTimestamp(Date date) {
        return date == null ? null : date.getTime();
    }
}
