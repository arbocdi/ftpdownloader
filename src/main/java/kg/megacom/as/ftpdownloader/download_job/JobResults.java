package kg.megacom.as.ftpdownloader.download_job;

import java.io.File;
import static java.lang.annotation.ElementType.*;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;
import java.util.Calendar;
import javax.inject.Qualifier;
import kg.megacom.as.ftpdownloader.job.CalendarConverter;
import lombok.Data;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.convert.AnnotationStrategy;
import org.simpleframework.xml.convert.Convert;
import org.simpleframework.xml.core.Persister;
@Root
@Data
public class JobResults {
    @Element
    @Convert(CalendarConverter.class)
    private Calendar lastDownloadedFile;

    public interface JobResultsStore {

        void save(JobResults jobResults) throws Exception;

        JobResults load() throws Exception;
    }
    
    @Qualifier
    @Retention(RUNTIME)
    @Target({TYPE,METHOD,FIELD,PARAMETER})
    public static @interface JobResultsXMLStoreQualifier{
        
    }

    public static class JobResultsXMLStore implements JobResultsStore {

        protected File jobResultsFile;

        public JobResultsXMLStore(File jobResultsFile) {
            this.jobResultsFile = jobResultsFile;
        }

        @Override
        public synchronized void save(JobResults jobResults) throws Exception {
            Serializer persister = new Persister(new AnnotationStrategy());
            persister.write(jobResults, jobResultsFile);
        }

        @Override
        public synchronized JobResults load() throws Exception {
            Serializer persister = new Persister(new AnnotationStrategy());
            return persister.read(JobResults.class, jobResultsFile);
        }
    }
}
