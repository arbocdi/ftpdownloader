package kg.megacom.as.ftpdownloader.cfg;

import com.megacom.ashevelev.utils.ConfigUtilI;
import com.megacom.ashevelev.utils.ftp.FTPConfig;
import java.io.File;
import java.util.Calendar;
import java.util.GregorianCalendar;
import kg.megacom.as.ftpdownloader.download_job.JobResults;
import kg.megacom.as.ftpdownloader.download_job.JobResults.JobResultsXMLStore;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.convert.AnnotationStrategy;
import org.simpleframework.xml.core.Persister;

/**
 *
 * @author root
 */
public class ConfigGenerator {
    
    public static void main(String[] args) throws Exception {
        Serializer persister = new Persister(new AnnotationStrategy());
        File cfgDir = new File(ConfigUtilI.CONFIG);
        cfgDir.mkdir();
        //JOB_RESULTS_FILE
        JobResults jobResults = new JobResults();
        jobResults.setLastDownloadedFile(new GregorianCalendar());
        jobResults.getLastDownloadedFile().set(Calendar.YEAR, 1970);
        JobResultsXMLStore fobResultsStore = new JobResultsXMLStore(new File(CfgConstants.JOB_RESULTS_FILE));
        fobResultsStore.save(jobResults);
        System.out.println(fobResultsStore.load());
        //JOB_CONFIG_FILE
        JobConfig cfg = new JobConfig();
        cfg.setCronSchedule("* * * * * ?");
        cfg.setDestFolderPath("ftpFiles");
        cfg.setFilePattern("^.*$");
        cfg.setFiles(3);
        cfg.setFtpInitialDir("/ftpFiles");
        cfg.setDeleteSourceFile(false);
        cfg.setJobType(JobConfig.JobType.DOWNLOAD);
        persister.write(cfg, new File(CfgConstants.JOB_CONFIG_FILE));
        System.out.println(persister.read(JobConfig.class, new File(CfgConstants.JOB_CONFIG_FILE)));
        //FTP_CONFIG_FILE
        FTPConfig ftpCfg = new FTPConfig();
        ftpCfg.setHost("127.0.0.1");
        ftpCfg.setPassword("remote");
        ftpCfg.setPort(21);
        ftpCfg.setUsername("remote");
        ftpCfg.setTimeout(null);
        persister.write(ftpCfg, new File(CfgConstants.FTP_CONFIG_FILE));
        System.out.println(persister.read(FTPConfig.class, new File(CfgConstants.FTP_CONFIG_FILE)));
    }
}
