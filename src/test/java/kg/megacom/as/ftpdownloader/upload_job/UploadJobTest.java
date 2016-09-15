/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kg.megacom.as.ftpdownloader.upload_job;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.mchange.v1.util.ArrayUtils;
import com.megacom.ashevelev.utils.ftp.AFTPClient;
import com.megacom.ashevelev.utils.ftp.FTPConfig;
import com.megacom.ashevelev.utils.ftp.FTPUtils;
import java.io.File;
import java.io.IOException;
import java.util.List;
import kg.megacom.as.ftpdownloader.cfg.JobConfig;
import static kg.megacom.as.ftpdownloader.download_job.DownloadJobTest.FTP_CLT_DIR;
import static kg.megacom.as.ftpdownloader.download_job.DownloadJobTest.FTP_SRV_DIR;
import static kg.megacom.as.ftpdownloader.download_job.DownloadJobTest.TEST_FILES_DIR;
import kg.megacom.as.ftpdownloader.download_job.FTPConfigSerializerI;
import kg.megacom.as.ftpdownloader.job.FileNameStore;
import net.sf.selibs.utils.inject.Injector;
import org.apache.commons.io.FileUtils;
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
import org.slf4j.LoggerFactory;

/**
 *
 * @author root
 */
public class UploadJobTest {

    private static FtpServer server;
    private FTPConfig ftpCfg;
    private JobConfig jobCfg;
    private FTPConfigSerializerI.FTPConfigSerializer ftpCfgSerializer;
    private Injector injector;
    private JobExecutionContext ctx;
    private UploadJob job;
    private AFTPClient aftp;

    public UploadJobTest() {

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
        FileUtils.copyDirectory(TEST_FILES_DIR, FTP_CLT_DIR);

        this.ftpCfg = new FTPConfig();
        this.ftpCfg.setHost("127.0.0.1");
        this.ftpCfg.setPort(2354);
        this.ftpCfg.setUsername("admin");
        this.ftpCfg.setPassword("admin");

        jobCfg = new JobConfig();
        jobCfg.setCronSchedule("* * * * * ?");
        jobCfg.setDestFolderPath(FTP_SRV_DIR.getName());
        jobCfg.setFilePattern("^.*$");
        jobCfg.setFiles(2);
        jobCfg.setFtpInitialDir(FTP_CLT_DIR.getAbsolutePath());

        ftpCfgSerializer = new FTPConfigSerializerI.FTPConfigSerializer();

        injector = new Injector();
        injector.addBinding(FTPConfig.class, this.ftpCfg);
        injector.addBinding(FTPConfigSerializerI.class, this.ftpCfgSerializer);
        injector.addBinding(JobConfig.class, this.jobCfg);
        injector.addBinding(FileNameStore.class, Mockito.mock(FileNameStore.class));

        job = new UploadJob();
        this.injector.injectInto(job);

        this.ctx = Mockito.mock(JobExecutionContext.class);
        Scheduler sched = Mockito.mock(Scheduler.class);
        Mockito.when(sched.isShutdown()).thenReturn(false);
        Mockito.when(this.ctx.getScheduler()).thenReturn(sched);

        this.aftp = new AFTPClient(ftpCfg);
        this.aftp.connect();

    }

    @After
    public void tearDown() throws IOException {
        this.aftp.disconnect();
    }

    @Test
    public void testPrepareFileList() throws Exception {
        System.out.println("=============UploadJobTest:testPrepareFileList=================");
        List<File> files = this.job.prepareFileList();
        System.out.println(files);
        Assert.assertEquals(2, files.size());
        Assert.assertEquals("file6.txt", files.get(0).getName());
        Assert.assertEquals("file4.txt", files.get(1).getName());
    }

    @Test
    public void testUploadFile() throws Exception {
        System.out.println("=============UploadJobTest:testUploadFile=================");
        File localFile = new File(FTP_CLT_DIR + "/file6.txt");
        this.aftp.getFtpClient().changeWorkingDirectory(this.jobCfg.getDestFolderPath());
        this.job.uploadFile(aftp, localFile);
        FTPFile file6 = FTPUtils.getFTPFile(this.aftp.getFtpClient(), "file6.txt");
        System.out.println(file6);
        Assert.assertEquals(localFile.length(), file6.getSize());
    }

    @Test
    public void textExecute() throws Exception {
        System.out.println("=============UploadJobTest:textExecute=================");
        this.job.execute(ctx);
        this.aftp.getFtpClient().changeWorkingDirectory(FTP_SRV_DIR.getName());
        FTPFile[] uploadedFiles = this.aftp.getFtpClient().listFiles();
        String ftpFiles = ArrayUtils.stringifyContents(uploadedFiles);
        System.out.println(ftpFiles);
        Assert.assertTrue(ftpFiles.contains("file4"));
        Assert.assertTrue(ftpFiles.contains("file6"));
    }

}
