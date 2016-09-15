package kg.megacom.as.ftpdownloader.download_job;

import com.megacom.ashevelev.utils.ftp.AFTPClient;
import com.megacom.ashevelev.utils.ftp.FTPConfig;
import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import javax.inject.Inject;
import kg.megacom.as.ftpdownloader.cfg.AppServices;
import kg.megacom.as.ftpdownloader.cfg.FTPConfigPQualifier;
import kg.megacom.as.ftpdownloader.cfg.JobConfig;
import kg.megacom.as.ftpdownloader.cfg.JobConfig.JobConfigPQualifier;
import kg.megacom.as.ftpdownloader.download_job.FTPFileWrapper.FTPTimeParser;
import kg.megacom.as.ftpdownloader.download_job.JobResults.JobResultsStore;
import kg.megacom.as.ftpdownloader.download_job.JobResults.JobResultsXMLStoreQualifier;
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
public class DownloadJob implements Job {
    
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
            List<FTPFileWrapper> filesToDownload = this.prepareFileList(ftp);
            if (filesToDownload.isEmpty()) {
                log.info(String.format("No files to download from %s, entering sleep state", this.cfgSerializer.toString(cfg)));
                return;
            }
            log.info(String.format("Found %s files to download from %s", filesToDownload.size(), this.cfgSerializer.toString(cfg)));
            int downloadedFiles = this.downloadFiles(filesToDownload, ftp, this.jobCfg, context);
            log.info(String.format("Downloaded %s files from source, entering sleep state", downloadedFiles));
            
        } catch (Exception ex) {
            log.info(String.format("Error working with %s, entering sleep state.", this.cfgSerializer.toString(cfg)), ex);
        } finally {
            try {
                ftp.disconnect();
            } catch (Exception ignored) {
                
            }
        }
    }
    
    protected List<FTPFileWrapper> prepareFileList(AFTPClient ftp) throws Exception {
        List<FTPFileWrapper> files = new LinkedList();
        ftp.getFtpClient().changeWorkingDirectory(this.jobCfg.getFtpInitialDir());
        FTPFile[] allRemoteFiles = ftp.getFtpClient().listFiles();
        log.debug(String.format("total number of files in dir %s is %s",
                this.jobCfg.getFtpInitialDir(),
                allRemoteFiles.length));
        //apply pattern filter
        for (FTPFile remoteFile : allRemoteFiles) {
            //skip change dir files
            if (remoteFile.getName().length() <= 2) {
                continue;
            }
            //remove file if found in store
            if (this.fileNameStore.fileIsPresent(remoteFile.getName())) {
                continue;
            }
            if (remoteFile.getName().matches(this.jobCfg.getFilePattern())) {
                FTPFileWrapper remoteFileWrapper = new FTPFileWrapper();
                remoteFileWrapper.setFile(remoteFile);
                String modificationTime = ftp.getFtpClient().getModificationTime(remoteFile.getName());
                remoteFileWrapper.setModificationTime(FTPTimeParser.parseFtpTimeResponse(modificationTime));
                files.add(remoteFileWrapper);
            }
        }
        //sort files by modification time
        Collections.sort(files, new FTPFileComparator());
        //drop last file
        if (!files.isEmpty()) {
            files.remove(files.size() - 1);
        }
        //return specified number of files
        List<FTPFileWrapper> xFiles = new LinkedList();
        for (int i = 0; i < files.size(); i++) {
            if (i >= this.jobCfg.getFiles()) {
                break;
            }
            xFiles.add(files.get(i));
        }
        return xFiles;
    }
    
    protected int downloadFiles(List<FTPFileWrapper> files, AFTPClient ftp, JobConfig jobCfg, JobExecutionContext ctx) throws Exception {
        int downloadFiles = 0;
        ftp.getFtpClient().changeWorkingDirectory(jobCfg.getFtpInitialDir());
        for (FTPFileWrapper file : files) {
            if (AppServices.stopped) {
                log.info("DownloadJob was interrupted");
                return downloadFiles;
            }
            //download file
            this.downloadFile(ftp, file, jobCfg.getDestFolderPath());
            //increment download counter
            downloadFiles++;
            log.info(String.format("Downloaded file %s from %s, %s from %s file(s) downloaded",
                    file.getFile().getName(),
                    this.cfgSerializer.toString(cfg),
                    downloadFiles,
                    files.size()));
            //delete remote file if needed
            if (this.jobCfg.isDeleteSourceFile()) {
                
                if (ftp.getFtpClient().deleteFile(file.getFile().getName())) {
                    log.info(String.format("Deleted source file %s from %s", file.getFile().getName(), this.cfgSerializer.toString(cfg)));
                } else {
                    log.warn(String.format("Cant delete source file %s from %s", file.getFile().getName(), this.cfgSerializer.toString(cfg)));
                }
            }
            //update last downloaded file
            this.fileNameStore.addFileName(file.getFile().getName());
            
        }
        return downloadFiles;
    }
    
    protected void downloadFile(AFTPClient ftp, FTPFileWrapper remoteFile, String localFileDir) throws IOException {
        String localFileName = localFileDir + "/" + remoteFile.getFile().getName();
        ftp.downloadFile(localFileName, remoteFile.getFile().getName(), false);
        File localFile = new File(localFileName);
        if (localFile.length() != remoteFile.getFile().getSize()) {
            String msg = String.format("Local file %s size is %s, remote file %s size is %s",
                    localFileName,
                    localFile.length(),
                    remoteFile.getFile().getName(),
                    remoteFile.getFile().getSize());
            localFile.delete();
            throw new IOException(msg);
        }
    }
    
    public static String getFtpFileName(FTPFile file) {
        String[] data = file.getName().split("/");
        return data[data.length - 1];
    }
    
    public static class FTPFileComparator implements Comparator<FTPFileWrapper> {
        
        @Override
        public int compare(FTPFileWrapper o1, FTPFileWrapper o2) {
            return o1.getModificationTime().compareTo(o2.getModificationTime());
        }
        
    }
    
}
