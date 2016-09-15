package kg.megacom.as.ftpdownloader.download_job;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import lombok.Data;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.net.ftp.FTPFile;

@Data
public class FTPFileWrapper {

    private FTPFile file;
    private Calendar modificationTime;

    public static class FTPTimeParser {

        public static final String RESPONSE_EXAMPLE = "213 20160513110354.000";

        //date format in GMT0 YYYYMMDDhhmmss(.xxx)?

        public static Calendar parseFtpTimeResponse(String response) throws ParseException {
            DateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
            df.setTimeZone(TimeZone.getTimeZone("GMT"));
            Date date = df.parse(response.split(" ")[1]);
            Calendar cal = new GregorianCalendar();
            cal.setTimeZone(TimeZone.getTimeZone("GMT"));
            cal.setTime(date);
            return cal;
        }
    }

}
