package kg.megacom.as.ftpdownloader.upload_job;

import kg.megacom.as.ftpdownloader.download_job.*;
import com.megacom.ashevelev.utils.ftp.AFTPClient;
import com.megacom.ashevelev.utils.ftp.FTPConfig;
import com.megacom.ashevelev.utils.ftp.FTPUtils;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import javax.inject.Inject;
import kg.megacom.as.ftpdownloader.cfg.AppServices;
import kg.megacom.as.ftpdownloader.cfg.FTPConfigPQualifier;
import kg.megacom.as.ftpdownloader.cfg.JobConfig;
import kg.megacom.as.ftpdownloader.cfg.JobConfig.JobConfigPQualifier;
import kg.megacom.as.ftpdownloader.job.FileNameStore;
import kg.megacom.as.ftpdownloader.job.FileNameStoreQ;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTPFile;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.InterruptableJob;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.UnableToInterruptJobException;

/**
 *
 * @author root
 */
@Slf4j
@DisallowConcurrentExecution
@ToString
public class UploadJob implements Job {

    @Inject
    @FTPConfigPQualifier
    protected FTPConfig cfg;
    @Inject
    protected FTPConfigSerializerI cfgSerializer;
    @Inject
    @JobConfigPQualifier
    protected JobConfig jobCfg;
    @Inject
    @FileNameStoreQ
    protected FileNameStore fileNameStore;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        AFTPClient ftp = new AFTPClient(this.cfg);
        try {
            ftp.connect();
            log.info(String.format("Successfully connected to %s", this.cfgSerializer.toString(cfg)));
            List<File> filesToUpload = this.prepareFileList();
            if (filesToUpload.isEmpty()) {
                log.info(String.format("No files to upload to %s, entering sleep state", this.cfgSerializer.toString(cfg)));
                return;
            }
            log.info(String.format("Found %s files to upload to %s", filesToUpload.size(), this.cfgSerializer.toString(cfg)));
            int downloadedFiles = this.uploadFiles(filesToUpload, ftp, this.jobCfg, context);
            log.info(String.format("Uploaded %s files from source, entering sleep state", downloadedFiles));

        } catch (Exception ex) {
            log.info(String.format("Error working with %s, entering sleep state.", this.cfgSerializer.toString(cfg)), ex);
        } finally {
            try {
                ftp.disconnect();
            } catch (Exception ignored) {

            }
        }
    }

    protected List<File> prepareFileList() throws Exception {
        List<File> files = new LinkedList();
        File initialDir = new File(this.jobCfg.getFtpInitialDir());
        File[] initialFiles = initialDir.listFiles();
        log.debug(String.format("total number of files in dir %s is %s",
                this.jobCfg.getFtpInitialDir(),
                initialFiles.length));
        //apply pattern filter
        for (File file : initialFiles) {
            //skip change dir files
            if (file.getName().length() <= 2) {
                continue;
            }
            if (file.getName().matches(this.jobCfg.getFilePattern()) && !this.fileNameStore.fileIsPresent(file.getName())) {
                files.add(file);
            }
        }
        //sort files by modification time
        Collections.sort(files, new LocalFileComparator());
        //return specified number of files
        List<File> xFiles = new LinkedList();
        for (int i = 0; i < files.size(); i++) {
            if (i >= this.jobCfg.getFiles()) {
                break;
            }
            xFiles.add(files.get(i));
        }
        return xFiles;
    }

    protected int uploadFiles(List<File> filesToUpload, AFTPClient ftp, JobConfig jobCfg, JobExecutionContext ctx) throws Exception {
        int uploadedFiles = 0;
        ftp.getFtpClient().changeWorkingDirectory(jobCfg.getDestFolderPath());
        for (File file : filesToUpload) {
            if (AppServices.stopped) {
                log.info("UploadJob was interrupted");
                return uploadedFiles;
            }
            //download file
            this.uploadFile(ftp, file);
            //increment upload counter
            uploadedFiles++;
            log.info(String.format("Uploaded file %s to %s, %s from %s file(s) uploaded",
                    file.getName(),
                    this.cfgSerializer.toString(cfg),
                    uploadedFiles,
                    filesToUpload.size()));
            //delete local file if needed
            if (this.jobCfg.isDeleteSourceFile()) {
                file.delete();
            }
            //add to uploaded list
            this.fileNameStore.addFileName(file.getName());

        }
        return uploadedFiles;
    }

    protected void uploadFile(AFTPClient ftp, File localFile) throws IOException {
        ftp.uploadFile(localFile.getAbsolutePath(), localFile.getName());
        FTPFile remoteFile = FTPUtils.getFTPFile(ftp.getFtpClient(), localFile.getName());
        if (localFile.length() != remoteFile.getSize()) {
            String msg = String.format("Local file %s size is %s, remote file %s size is %s",
                    localFile,
                    localFile.length(),
                    remoteFile.getName(),
                    remoteFile.getSize());
            ftp.getFtpClient().dele(localFile.getName());
            throw new IOException(msg);
        }
    }


    public static class LocalFileComparator implements Comparator<File> {

        @Override
        public int compare(File o1, File o2) {
            return ((Long) o1.lastModified()).compareTo((Long) o2.lastModified());
        }

    }

}
