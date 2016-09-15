package kg.megacom.as.ftpdownloader.job;

public interface FileNameStore {

    boolean fileIsPresent(String fileName) throws Exception;

    void addFileName(String fileName) throws Exception;
}
