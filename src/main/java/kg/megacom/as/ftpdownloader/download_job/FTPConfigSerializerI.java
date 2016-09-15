package kg.megacom.as.ftpdownloader.download_job;

import com.megacom.ashevelev.utils.ftp.FTPConfig;


public interface FTPConfigSerializerI {

    String toString(FTPConfig cfg);

    public static class FTPConfigSerializer implements FTPConfigSerializerI {

        @Override
        public String toString(FTPConfig cfg) {
            return String.format(" sever = %s:%s, user = %s", cfg.getHost(), cfg.getPort(), cfg.getUsername());
        }
    }
}
