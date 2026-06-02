package com.mycompany.app;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import javax.net.ssl.*;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

public class App {
    
    private static final String TARGET_SITE = "http://www.papercdcase.com";
    private static final String INPUT_DATA_PATH = "data/data.txt";
    private static final String OUTPUT_FOLDER = "result";
    private static final String OUTPUT_FILENAME = "cd.pdf";
    
    public static void main(String[] args) {
        System.out.println("=== Paper CD Case Generator ===");
        
        try {
            List<String> discInfo = readDiscData(INPUT_DATA_PATH);
            
            WebDriver webBrowser = startBrowser();
            populateForm(webBrowser, discInfo);
            
            String pdfUrl = webBrowser.getCurrentUrl();
            System.out.println("PDF URL: " + pdfUrl);
            
            fetchAndSavePdf(pdfUrl, OUTPUT_FOLDER, OUTPUT_FILENAME);
            
            webBrowser.quit();
            System.out.println("=== Process completed successfully ===");
            
        } catch (Exception ex) {
            System.err.println("Error occurred: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    private static List<String> readDiscData(String path) throws IOException {
        System.out.println("Reading data from: " + path);
        List<String> lines = Files.readAllLines(Paths.get(path));
        
        List<String> cleaned = new ArrayList<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                cleaned.add(trimmed);
            }
        }
        
        System.out.println("Loaded " + cleaned.size() + " lines");
        return cleaned;
    }
    
    private static WebDriver startBrowser() {
        ChromeOptions browserConfig = new ChromeOptions();
        browserConfig.addArguments("--headless");
        browserConfig.addArguments("--no-sandbox");
        browserConfig.addArguments("--disable-dev-shm-usage");
        browserConfig.addArguments("--disable-gpu");
        browserConfig.addArguments("--window-size=1920,1080");
        browserConfig.setAcceptInsecureCerts(true);
        
        WebDriver driver = new ChromeDriver(browserConfig);
        System.out.println("Browser started");
        
        driver.get(TARGET_SITE);
        System.out.println("Site opened: " + TARGET_SITE);
        
        return driver;
    }
    
    private static void populateForm(WebDriver driver, List<String> data) {
        System.out.println("Populating form...");
        
        String artistName = data.size() > 0 ? data.get(0) : "Unknown Artist";
        String albumTitle = data.size() > 1 ? data.get(1) : "Unknown Album";
        
        System.out.println("Artist: " + artistName);
        System.out.println("Album: " + albumTitle);
        
        WebElement artistInput = driver.findElement(By.name("artist"));
        artistInput.clear();
        artistInput.sendKeys(artistName);
        
        WebElement titleInput = driver.findElement(By.name("title"));
        titleInput.clear();
        titleInput.sendKeys(albumTitle);
        
        int trackIndex = 0;
        for (int i = 2; i < data.size() && i < 18; i++) {
            trackIndex++;
            String trackTitle = data.get(i);
            
            String inputName = "track" + trackIndex;
            WebElement trackInput = driver.findElement(By.name(inputName));
            trackInput.clear();
            trackInput.sendKeys(trackTitle);
            
            System.out.println("Track " + trackIndex + ": " + trackTitle);
        }
        
        selectRadioOption(driver, "template", "jewel");
        selectRadioOption(driver, "size", "a4");
        
        WebElement generateButton = driver.findElement(By.name("submit"));
        generateButton.click();
        System.out.println("Form submitted");
        
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private static void selectRadioOption(WebDriver driver, String paramName, String paramValue) {
        try {
            String cssQuery = "input[name='" + paramName + "'][value='" + paramValue + "']";
            WebElement radioBtn = driver.findElement(By.cssSelector(cssQuery));
            radioBtn.click();
            System.out.println("Selected: " + paramName + "=" + paramValue);
        } catch (Exception e) {
            System.out.println("Warning: Could not select " + paramName + "=" + paramValue);
        }
    }
    
    private static void fetchAndSavePdf(String fileUrl, String outputDir, String fileName) {
        try {
            Files.createDirectories(Paths.get(outputDir));
            
            String fullPath = outputDir + File.separator + fileName;
            System.out.println("Downloading to: " + fullPath);
            
            disableSslVerification();
            
            URL url = new URL(fileUrl);
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);
            
            int statusCode = connection.getResponseCode();
            if (statusCode != HttpsURLConnection.HTTP_OK) {
                throw new IOException("Server returned HTTP code: " + statusCode);
            }
            
            try (InputStream inputStream = connection.getInputStream();
                 FileOutputStream outputStream = new FileOutputStream(fullPath)) {
                
                byte[] buffer = new byte[8192];
                int bytesRead;
                long totalDownloaded = 0;
                
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    totalDownloaded += bytesRead;
                }
                
                System.out.println("Downloaded " + totalDownloaded + " bytes");
            }
            
            System.out.println("PDF saved successfully: " + fullPath);
            
        } catch (Exception e) {
            System.err.println("Download failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void disableSslVerification() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
            };
            
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            
            HostnameVerifier allHostsValid = (hostname, session) -> true;
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
            
        } catch (Exception e) {
            System.err.println("Warning: Could not disable SSL verification: " + e.getMessage());
        }
    }
}
