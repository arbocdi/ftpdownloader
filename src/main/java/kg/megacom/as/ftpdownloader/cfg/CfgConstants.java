package kg.megacom.as.ftpdownloader.cfg;

import com.megacom.ashevelev.utils.ConfigUtilI;
import java.io.File;

public interface CfgConstants {
    String JOB_RESULTS_FILE=ConfigUtilI.CONFIG+"/jobResults.xml";
    String JOB_CONFIG_FILE=ConfigUtilI.CONFIG+"/jobConfig.xml";
    String FTP_CONFIG_FILE=ConfigUtilI.CONFIG+"/ftpConfig.xml";
    String LOGGER_CONFIG_FILE=ConfigUtilI.CONFIG+"/logger.xml";
    String QUARTZ_FACTORY_FILE=ConfigUtilI.CONFIG+"/quartz.properties";
    File FILE_NAME_STORE=new File(ConfigUtilI.CONFIG+"/processedFiles.txt");
}
