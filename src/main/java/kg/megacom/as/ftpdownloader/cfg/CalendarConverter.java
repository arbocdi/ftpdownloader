/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kg.megacom.as.ftpdownloader.job;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import org.simpleframework.xml.convert.Converter;
import org.simpleframework.xml.stream.InputNode;
import org.simpleframework.xml.stream.OutputNode;

/**
 *
 * @author root
 */
public class CalendarConverter implements Converter<Calendar> {

    protected DateFormat df;

    public CalendarConverter() {
        df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    @Override
    public synchronized Calendar read(InputNode node) throws Exception {
        Date date = df.parse(node.getValue());
        Calendar cal = new GregorianCalendar();
        cal.setTimeZone(TimeZone.getTimeZone("GMT"));
        cal.setTime(date);
        return cal;
    }

    @Override
    public synchronized void write(OutputNode node, Calendar value) throws Exception {
        node.getAttributes().remove("class");
        node.setValue(df.format(value.getTime()));
    }

}
