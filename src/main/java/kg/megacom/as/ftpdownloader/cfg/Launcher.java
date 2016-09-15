package kg.megacom.as.ftpdownloader.cfg;

import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author root
 */
@Slf4j
public class Launcher {
    
    public static void main(String[] args) throws Exception {
        final AppServices services = new AppServices();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                services.stop();
            }
        });
        try {
            services.start();
        } catch (Exception ex) {
            log.warn("Cant start FTP donwloader, exiting", ex);
            System.exit(1);
        }
        
    }
}
