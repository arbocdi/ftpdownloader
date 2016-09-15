package kg.megacom.as.ftpdownloader.cfg;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;
import javax.inject.Qualifier;
import lombok.Data;
import lombok.ToString;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

@Root
@Data
public class JobConfig {

    @Element
    private String ftpInitialDir;
    @Element
    private String destFolderPath;
    @Element
    private String filePattern;
    @Element
    private int files;
    @Element
    private String cronSchedule;
    @Element
    private boolean deleteSourceFile = true;
    @Element
    private JobType jobType = JobType.DOWNLOAD;

    @Qualifier
    @Retention(RUNTIME)
    @Target({TYPE, METHOD, FIELD, PARAMETER})
    public static @interface JobConfigPQualifier {

    }

    public enum JobType {

        DOWNLOAD, UPLOAD;
    }
}
