/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kg.megacom.as.ftpdownloader.job;

import net.sf.selibs.utils.store.RollingFileRecordStore;
import net.sf.selibs.utils.store.StringRecord;

/**
 *
 * @author root
 */
@FileNameStoreQ
public class FileNameRecordStore implements FileNameStore {
    
    RollingFileRecordStore<String, String> store;
    
    public FileNameRecordStore(RollingFileRecordStore<String, String> store) {
        this.store = store;
    }
    
    @Override
    public boolean fileIsPresent(String fileName) throws Exception {
        return this.store.getRecord(fileName) != null;
    }
    
    @Override
    public void addFileName(String fileName) throws Exception {
        this.store.addRecord(new StringRecord(fileName, "."));
    }
    
}
