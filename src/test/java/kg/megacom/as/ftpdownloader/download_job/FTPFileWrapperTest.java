/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kg.megacom.as.ftpdownloader.download_job;

import java.util.Calendar;
import kg.megacom.as.ftpdownloader.download_job.FTPFileWrapper.FTPTimeParser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author root
 */
public class FTPFileWrapperTest {
    
    public FTPFileWrapperTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    @Test
    public void testParseFtpTimeResponse() throws Exception {
        System.out.println("========FTPFileWrapperTest:testParseFtpTimeResponse==========");
        Calendar cal = FTPTimeParser.parseFtpTimeResponse(FTPTimeParser.RESPONSE_EXAMPLE);
        System.out.println(cal.getTime());
        Assert.assertEquals(11, cal.get(Calendar.HOUR_OF_DAY));
        Assert.assertEquals(3, cal.get(Calendar.MINUTE));
        Assert.assertEquals(54, cal.get(Calendar.SECOND));
    }
    
}
