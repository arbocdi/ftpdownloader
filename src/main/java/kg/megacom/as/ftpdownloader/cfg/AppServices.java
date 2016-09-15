package kg.megacom.as.ftpdownloader.cfg;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import java.io.File;
import kg.megacom.as.ftpdownloader.cfg.JobConfig.JobType;
import kg.megacom.as.ftpdownloader.download_job.DownloadJob;
import kg.megacom.as.ftpdownloader.upload_job.UploadJob;
import lombok.extern.slf4j.Slf4j;
import net.sf.selibs.utils.inject.CdiContext;
import net.sf.selibs.utils.inject.QuartzWeldJobFactory;
import net.sf.selibs.utils.service.AbstractService;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import org.slf4j.LoggerFactory;

/**
 *
 * @author root
 */
@Slf4j
public class AppServices implements AbstractService {
    
    public static volatile boolean stopped = false;
    
    protected Scheduler scheduler;
    protected Weld weld;
    protected WeldContainer container;
    protected JobDetail downloadJob;
    protected JobDetail uploadJob;
    
    protected void startLogger() {
        /*
         Logback relies on a configuration library called Joran, part of
         logback-core. Logback's default configuration mechanism invokes
         JoranConfigurator on the default configuration file it finds on the
         class path. If you wish to override logback's default configuration
         mechanism for whatever reason, you can do so by invoking
         JoranConfigurator directly. 
        
         This application fetches the LoggerContext currently in effect, 
         creates a new JoranConfigurator, sets the context on which it will operate, 
         resets the logger context, and then finally asks the configurator 
         to configure the context using the configuration file passed as a parameter to the application. 
         Internal status data is printed in case of warnings or errors. 
         Note that for multi-step configuration, context.reset() invocation should be omitted.
         */
        // assume SLF4J is bound to logback in the current environment
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        
        try {
            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(context);
            // Call context.reset() to clear any previous configuration, e.g. default 
            // configuration. For multi-step configuration, omit calling context.reset().
            context.reset();
            configurator.doConfigure(CfgConstants.LOGGER_CONFIG_FILE);
        } catch (JoranException je) {
            // StatusPrinter will handle this
        }
        StatusPrinter.printInCaseOfErrorsOrWarnings(context);
    }
    
    protected void scheduleDownloadJob(Scheduler sched, JobConfig jobConfig) throws Exception {
        
        downloadJob = JobBuilder.newJob(DownloadJob.class).
                withIdentity("downloadJob", "group1")
                .build();
        CronTrigger tg = TriggerBuilder.newTrigger().
                withIdentity("downloadJobTrigger", "group1").
                withSchedule(CronScheduleBuilder.cronSchedule(jobConfig.getCronSchedule()).withMisfireHandlingInstructionDoNothing())
                .build();
        sched.scheduleJob(downloadJob, tg);
    }
    
    protected void scheduleUploadJob(Scheduler sched, JobConfig jobConfig) throws Exception {
        
        uploadJob = JobBuilder.newJob(UploadJob.class).
                withIdentity("uploadJob", "group1")
                .build();
        CronTrigger tg = TriggerBuilder.newTrigger().
                withIdentity("uploadJobTrigger", "group1").
                withSchedule(CronScheduleBuilder.cronSchedule(jobConfig.getCronSchedule()).withMisfireHandlingInstructionDoNothing())
                .build();
        sched.scheduleJob(this.uploadJob, tg);
    }
    
    protected void startWeld() throws Exception {
        System.getProperties().setProperty(Weld.SHUTDOWN_HOOK_SYSTEM_PROPERTY, "false");
        this.weld = new Weld();
        this.weld.property(Weld.SHUTDOWN_HOOK_SYSTEM_PROPERTY, false);
        this.container = weld.initialize();
    }
    
    @Override
    public void start() throws Exception {
        stopped = false;
        this.startLogger();
        log.info("=========FTPDownloader starting===========");
        this.startWeld();
        //schedule jobs
        StdSchedulerFactory sf = new StdSchedulerFactory();
        sf.initialize(CfgConstants.QUARTZ_FACTORY_FILE);
        this.scheduler = sf.getScheduler();
        this.scheduler.setJobFactory(new QuartzWeldJobFactory(this.container));
        
        Serializer persister = new Persister();
        JobConfig jobConfig = persister.read(JobConfig.class, new File(CfgConstants.JOB_CONFIG_FILE));
        
        if (jobConfig.getJobType() == JobType.DOWNLOAD) {
            this.scheduleDownloadJob(scheduler, jobConfig);
        } else {
            this.scheduleUploadJob(scheduler, jobConfig);
        }
        this.scheduler.start();
    }
    
    @Override
    public void stop() {
        log.info("=======FTPDownloader stopping===========");
        stopped = true;
        //shutdown scheduler
        try {
            this.scheduler.shutdown(true);
        } catch (SchedulerException ex) {
            log.warn("Cant properly stop scheduler", ex);
        }
        try {
            //stop weld
            this.container.shutdown();
            this.weld.shutdown();
        } catch (Exception ex) {
            log.warn("Cant properly stop weld", ex);
        }
        log.info("=======FTPDownloader stopped===========");
        try {
            Thread.sleep(1000);
        } catch (Exception ignored) {
        }
        //stop logger
        LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
        ctx.stop();
    }
    
    @Override
    public void join() throws InterruptedException {
    }
    
}
