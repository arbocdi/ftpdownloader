/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kg.megacom.as.ftpdownloader.cfg;

import kg.megacom.as.ftpdownloader.download_job.DownloadJob;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;

/**
 *
 * @author root
 */
public class WeldTestLauncher {
    public static void main(String[] args) throws Exception{
        Weld weld = new Weld();
        WeldContainer container = weld.initialize();
        DownloadJob job = container.select(DownloadJob.class).get();
        System.out.println(job);
    }
}
