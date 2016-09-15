/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kg.megacom.as.ftpdownloader.cfg;

import com.megacom.ashevelev.utils.ConfigUtilI;
import com.megacom.ashevelev.utils.ftp.FTPConfig;
import lombok.Getter;
import net.sf.selibs.utils.inject.Injector;
import org.quartz.Scheduler;

/**
 *
 * @author root
 */
public class Resources {
    
    public static final String QUARTZ_PROPERTIES=ConfigUtilI.CONFIG+"/quartz.properties";
    @Getter
    protected Scheduler scheduler;
    @Getter
    private final Injector injector = new Injector();
    @Getter
    protected FTPConfig ftpConfig;
    
    
}
