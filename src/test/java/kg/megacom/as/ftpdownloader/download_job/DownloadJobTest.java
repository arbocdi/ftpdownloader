/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kg.megacom.as.ftpdownloader.download_job;

import kg.megacom.as.ftpdownloader.download_job.FTPConfigSerializerI;
import kg.megacom.as.ftpdownloader.download_job.FTPFileWrapper;
import kg.megacom.as.ftpdownloader.download_job.JobResults;
import kg.megacom.as.ftpdownloader.download_job.DownloadJob;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.megacom.ashevelev.utils.ftp.AFTPClient;
import com.megacom.ashevelev.utils.ftp.FTPConfig;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import kg.megacom.as.ftpdownloader.cfg.JobConfig;
import kg.megacom.as.ftpdownloader.download_job.FTPConfigSerializerI.FTPConfigSerializer;
import kg.megacom.as.ftpdownloader.download_job.JobResults.JobResultsStore;
import kg.megacom.as.ftpdownloader.download_job.JobResults.JobResultsXMLStore;
import kg.megacom.as.ftpdownloader.job.FileNameStore;
import net.sf.selibs.utils.inject.Injector;
import org.apache.commons.io.FileUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.ClearTextPasswordEncryptor;
import org.apache.ftpserver.usermanager.impl.PropertiesUserManager;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Ignore;
import org.mockito.Mockito;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.convert.AnnotationStrategy;
import org.simpleframework.xml.core.Persister;
import org.slf4j.LoggerFactory;

/**
 *
 * @author root
 */
public class DownloadJobTest {

    public static final File FTP_SRV_DIR = new File("test/srvFiles/dir1");
    public static final File TEST_FILES_DIR = new File("test/testFiles");
    public static final File FTP_CLT_DIR = new File("test/cltFiles");
    public static final File JOB_RESULTS_FILE = new File("test/jobResults.xml");

    static FtpServer server;
    FTPConfig ftpCfg;
    JobConfig jobCfg;
    FTPConfigSerializerI ftpCfgSerializer;
    JobResultsStore jobResultsStore;
    Injector injector;
    JobExecutionContext ctx;
    FileNameStore fileNameStore;

    DownloadJob job;

    public DownloadJobTest() {
    }

    @BeforeClass
    public static void setUpClass() throws FtpException {
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.WARN);

        FtpServerFactory serverFactory = new FtpServerFactory();
        UserManager uman = new PropertiesUserManager(new ClearTextPasswordEncryptor(), new File("test/ftpUsers.properties"), "admin");
        serverFactory.setUserManager(uman);

        ListenerFactory factory = new ListenerFactory();
        // set the port of the listener
        factory.setPort(2354);
        // replace the default listener
        serverFactory.addListener("default", factory.createListener());
        // start the server
        server = serverFactory.createServer();
        server.start();
    }

    @AfterClass
    public static void tearDownClass() {
        server.stop();
    }

    @Before
    public void setUp() throws Exception {
        FileUtils.cleanDirectory(FTP_SRV_DIR);
        FileUtils.cleanDirectory(FTP_CLT_DIR);
        FileUtils.copyDirectory(TEST_FILES_DIR, FTP_SRV_DIR);

        Serializer persister = new Persister(new AnnotationStrategy());
        JobResults results = new JobResults();
        Calendar cal = new GregorianCalendar();
        cal.set(Calendar.YEAR, 1970);
        results.setLastDownloadedFile(cal);
        persister.write(results, JOB_RESULTS_FILE);

        this.ftpCfg = new FTPConfig();
        this.ftpCfg.setHost("127.0.0.1");
        this.ftpCfg.setPort(2354);
        this.ftpCfg.setUsername("admin");
        this.ftpCfg.setPassword("admin");

        jobCfg = new JobConfig();
        jobCfg.setCronSchedule("* * * * * ?");
        jobCfg.setDestFolderPath(FTP_CLT_DIR.getAbsolutePath());
        jobCfg.setFilePattern("^.*$");
        jobCfg.setFiles(2);
        jobCfg.setFtpInitialDir(FTP_SRV_DIR.getName());

        ftpCfgSerializer = new FTPConfigSerializer();

        jobResultsStore = new JobResultsXMLStore(JOB_RESULTS_FILE);

        this.fileNameStore = Mockito.mock(FileNameStore.class);

        injector = new Injector();
        injector.addBinding(FTPConfig.class, this.ftpCfg);
        injector.addBinding(FTPConfigSerializerI.class, this.ftpCfgSerializer);
        injector.addBinding(JobConfig.class, this.jobCfg);
        injector.addBinding(JobResultsStore.class, this.jobResultsStore);
        injector.addBinding(FileNameStore.class, this.fileNameStore);

        job = new DownloadJob();
        this.injector.injectInto(job);

        this.ctx = Mockito.mock(JobExecutionContext.class);
        Scheduler sched = Mockito.mock(Scheduler.class);
        Mockito.when(sched.isShutdown()).thenReturn(false);
        Mockito.when(this.ctx.getScheduler()).thenReturn(sched);
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testPrepareFileList2Files() throws Exception {
        System.out.println("========DownloadJobTest:testPrepareFileList2Files============");
        AFTPClient clt = new AFTPClient(this.ftpCfg);
        try {
            clt.connect();
            List<FTPFileWrapper> files = this.job.prepareFileList(clt);
            for (FTPFileWrapper file : files) {
                System.out.println(file.getFile().getName() + " " + file.getModificationTime().getTime());
            }
            Assert.assertEquals(2, files.size());
            Assert.assertEquals("file6.txt", files.get(0).getFile().getName());
            Assert.assertEquals("file4.txt", files.get(1).getFile().getName());
        } finally {
            clt.disconnect();
        }

    }

    @Test
    public void testPrepareFileListPatternFiles() throws Exception {
        System.out.println("========DownloadJobTest:testPrepareFileListPatternFiles============");
        this.jobCfg.setFilePattern("^file[1-4]\\.txt$");
        this.jobCfg.setFiles(5);
        AFTPClient clt = new AFTPClient(this.ftpCfg);
        try {
            clt.connect();
            List<FTPFileWrapper> files = this.job.prepareFileList(clt);
            for (FTPFileWrapper file : files) {
                System.out.println(file.getFile().getName() + " " + file.getModificationTime().getTime());
            }
            Assert.assertEquals(3, files.size());
            Assert.assertTrue(this.fileListContainsName(files, "file1.txt"));
            Assert.assertTrue(this.fileListContainsName(files, "file2.txt"));
            Assert.assertTrue(this.fileListContainsName(files, "file4.txt"));

        } finally {
            clt.disconnect();
        }

    }

    public boolean fileListContainsName(List<FTPFileWrapper> files, String name) {
        for (FTPFileWrapper file : files) {
            if (file.getFile().getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    @Test
    public void testPrepareFileListAllFiles() throws Exception {
        System.out.println("========DownloadJobTest:testPrepareFileListAllFiles============");
        this.jobCfg.setFiles(10);
        AFTPClient clt = new AFTPClient(this.ftpCfg);
        try {
            clt.connect();
            List<FTPFileWrapper> files = this.job.prepareFileList(clt);
            for (FTPFileWrapper file : files) {
                System.out.println(file.getFile().getName() + " " + file.getModificationTime().getTime());
            }
            Assert.assertEquals(5, files.size());
        } finally {
            clt.disconnect();
        }

    }

    @Test
    public void testExecute2Files() throws Exception {
        System.out.println("========DownloadJobTest:testExecute2Files============");
        this.job.execute(ctx);
        //2 oldest files downloaded
        System.out.println(Arrays.toString(FTP_CLT_DIR.listFiles()));
        Assert.assertEquals(2, FTP_CLT_DIR.listFiles().length);
        Assert.assertTrue(this.folderContainsFile(DownloadJobTest.FTP_CLT_DIR, "file6.txt"));
        Assert.assertTrue(this.folderContainsFile(DownloadJobTest.FTP_CLT_DIR, "file4.txt"));
        //2 oldest files deleted from source
        System.out.println(Arrays.toString(FTP_SRV_DIR.listFiles()));
        Assert.assertFalse(this.folderContainsFile(DownloadJobTest.FTP_SRV_DIR, "file6.txt"));
        Assert.assertFalse(this.folderContainsFile(DownloadJobTest.FTP_SRV_DIR, "file4.txt"));
    }

    @Test
    public void testExecute2KeepSourceFiles() throws Exception {
        System.out.println("========DownloadJobTest:testExecute2KeepSourceFiles============");
        this.jobCfg.setDeleteSourceFile(false);
        this.job.execute(ctx);
        //2 oldest files downloaded
        System.out.println(Arrays.toString(FTP_CLT_DIR.listFiles()));
        Assert.assertEquals(2, FTP_CLT_DIR.listFiles().length);
        Assert.assertTrue(this.folderContainsFile(DownloadJobTest.FTP_CLT_DIR, "file6.txt"));
        Assert.assertTrue(this.folderContainsFile(DownloadJobTest.FTP_CLT_DIR, "file4.txt"));
        //2 oldest files deleted from source
        System.out.println(Arrays.toString(FTP_SRV_DIR.listFiles()));
        Assert.assertTrue(this.folderContainsFile(DownloadJobTest.FTP_SRV_DIR, "file6.txt"));
        Assert.assertTrue(this.folderContainsFile(DownloadJobTest.FTP_SRV_DIR, "file4.txt"));
        //jobResults updated
    }

    @Test
    public void testDownloadFile() throws Exception {
        System.out.println("========DownloadJobTest:testDownloadFile============");
        AFTPClient clt = new AFTPClient(this.ftpCfg);
        try {
            clt.connect();
            clt.getFtpClient().changeWorkingDirectory(this.jobCfg.getFtpInitialDir());
            FTPFile[] files = clt.getFtpClient().listFiles();
            for (FTPFile file : files) {
                if (file.getName().equals("file6.txt")) {
                    FTPFileWrapper fileWrapper = new FTPFileWrapper();
                    fileWrapper.setFile(file);
                    job.downloadFile(clt, fileWrapper, this.jobCfg.getDestFolderPath());
                }
            }
        } finally {
            clt.disconnect();
        }
        File localFile = new File(FTP_CLT_DIR, "file6.txt");
        Assert.assertTrue(localFile.exists());

    }

    public boolean folderContainsFile(File folder, String fileName) {
        for (File file : folder.listFiles()) {
            if (file.getName().equals(fileName)) {
                return true;
            }
        }
        return false;
    }

}
