package kg.megacom.as.ftpdownloader.cfg;

import com.megacom.ashevelev.utils.ConfigUtilI;
import com.megacom.ashevelev.utils.ftp.FTPConfig;
import java.io.File;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import kg.megacom.as.ftpdownloader.cfg.JobConfig.JobConfigPQualifier;
import kg.megacom.as.ftpdownloader.download_job.JobResults.JobResultsStore;
import kg.megacom.as.ftpdownloader.download_job.JobResults.JobResultsXMLStore;
import kg.megacom.as.ftpdownloader.download_job.JobResults.JobResultsXMLStoreQualifier;
import kg.megacom.as.ftpdownloader.job.FileNameRecordStore;
import kg.megacom.as.ftpdownloader.job.FileNameStore;
import kg.megacom.as.ftpdownloader.job.FileNameStoreQ;
import net.sf.selibs.utils.store.RollingFileRecordStore;
import net.sf.selibs.utils.store.StringRecordParser;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.convert.AnnotationStrategy;
import org.simpleframework.xml.core.Persister;

@ApplicationScoped
public class ProducerBean {

    @ApplicationScoped
    @Produces
    @JobResultsXMLStoreQualifier
    public JobResultsStore createJobResultsXMLStore() {
        return new JobResultsXMLStore(new File(CfgConstants.JOB_RESULTS_FILE));
    }

    @ApplicationScoped
    @Produces
    @JobConfigPQualifier
    public JobConfig createJobConfig() {
        try {
            Serializer persister = new Persister(new AnnotationStrategy());
            return persister.read(JobConfig.class, new File(CfgConstants.JOB_CONFIG_FILE));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    @ApplicationScoped
    @Produces
    @FTPConfigPQualifier
    public FTPConfig createFTPConfig() {
        try {
            Serializer persister = new Persister();
            return persister.read(FTPConfig.class, new File(CfgConstants.FTP_CONFIG_FILE));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    @ApplicationScoped
    @Produces
    @FileNameStoreQ
    public FileNameStore createFileNameStore() {
        RollingFileRecordStore<String,String> recordStore = new RollingFileRecordStore(CfgConstants.FILE_NAME_STORE, new StringRecordParser());
        recordStore.setMaxRecords(20000);
        return new FileNameRecordStore(recordStore);
    }

}
