package com.mycompany.app;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class App {

    public static void main(String[] args) {
        CdCoverGenerator generator = new CdCoverGenerator();
        generator.run();
    }
}

class CdCoverGenerator {

    private static final String WEB_PAGE = "http://www.papercdcase.com";
    private static final String INPUT_FILE = "data/data.txt";
    private static final String RESULT_DIR = "result";
    private static final String FINAL_NAME = "cd.pdf";

    private WebDriver session;
    private Path downloadPath;

    public void run() {
        try {
            System.out.println("Starting CD cover generation process...");

            List<String> lines = readInputData();
            if (lines.isEmpty()) {
                System.err.println("No data found in input file");
                return;
            }

            setupDownloadDirectory();
            session = createBrowserSession();

            navigateToPage();
            fillCoverDetails(lines);
            submitForm();

            waitForDownloadCompletion();
            renameDownloadedFile();

            System.out.println("CD cover generated successfully!");

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (session != null) {
                session.quit();
            }
        }
    }

    private List<String> readInputData() throws IOException {
        Path path = Paths.get(INPUT_FILE);
        return Files.readAllLines(path);
    }

    private void setupDownloadDirectory() throws IOException {
        downloadPath = Paths.get(RESULT_DIR).toAbsolutePath();
        Files.createDirectories(downloadPath);
        System.out.println("Download directory: " + downloadPath);
    }

    private WebDriver createBrowserSession() {
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", downloadPath.toString());
        prefs.put("download.prompt_for_download", false);
        prefs.put("plugins.always_open_pdf_externally", true);
        prefs.put("safebrowsing.enabled", true);

        ChromeOptions config = new ChromeOptions();
        config.setExperimentalOption("prefs", prefs);
        config.addArguments("--headless");
        config.addArguments("--disable-gpu");
        config.addArguments("--no-sandbox");
        config.addArguments("--disable-dev-shm-usage");
        config.addArguments("--ignore-certificate-errors");

        WebDriver driver = new ChromeDriver(config);
        System.out.println("Browser session created");
        return driver;
    }

    private void navigateToPage() {
        session.get(WEB_PAGE);
        System.out.println("Page loaded: " + WEB_PAGE);
        sleep(2000);
    }

    private void fillCoverDetails(List<String> data) {
        String artist = data.size() > 0 ? data.get(0).trim() : "";
        String title = data.size() > 1 ? data.get(1).trim() : "";

        System.out.println("Artist: " + artist);
        System.out.println("Title: " + title);

        setInputValue("artist", artist);
        setInputValue("title", title);

        int trackNum = 0;
        for (int i = 2; i < data.size() && i < 18; i++) {
            String track = data.get(i).trim();
            if (!track.isEmpty()) {
                trackNum++;
                setInputValue("track" + trackNum, track);
                System.out.println("Track " + trackNum + ": " + track);
            }
        }

        selectOption("template", "jewel");
        selectOption("size", "a4");
    }

    private void setInputValue(String fieldName, String value) {
        try {
            WebElement input = session.findElement(By.name(fieldName));
            input.clear();
            input.sendKeys(value);
        } catch (Exception e) {
            System.out.println("Warning: Could not set field " + fieldName);
        }
    }

    private void selectOption(String groupName, String value) {
        try {
            String xpath = "//input[@name='" + groupName + "' and @value='" + value + "']";
            WebElement radio = session.findElement(By.xpath(xpath));
            radio.click();
        } catch (Exception e) {
            System.out.println("Warning: Could not select " + groupName + "=" + value);
        }
    }

    private void submitForm() {
        try {
            WebElement button = session.findElement(By.name("submit"));
            button.click();
            System.out.println("Form submitted");
        } catch (Exception e) {
            System.err.println("Could not submit form: " + e.getMessage());
        }
    }

    private void waitForDownloadCompletion() {
        System.out.println("Waiting for PDF download to complete...");
        
        File dir = downloadPath.toFile();
        int maxAttempts = 30;
        int attempt = 0;
        
        while (attempt < maxAttempts) {
            File[] files = dir.listFiles();
            if (files == null) break;
            
            boolean downloading = false;
            for (File file : files) {
                String name = file.getName();
                if (name.endsWith(".crdownload") || name.endsWith(".tmp")) {
                    downloading = true;
                    System.out.println("Download in progress: " + name);
                    break;
                }
            }
            
            if (!downloading) {
                File[] pdfFiles = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".pdf"));
                if (pdfFiles != null && pdfFiles.length > 0) {
                    System.out.println("PDF file found: " + pdfFiles[0].getName());
                    return;
                }
            }
            
            sleep(1000);
            attempt++;
        }
        
        System.out.println("Download wait completed after " + attempt + " seconds");
    }

    private void renameDownloadedFile() throws IOException {
        File dir = downloadPath.toFile();
        File[] allFiles = dir.listFiles();
        
        if (allFiles == null || allFiles.length == 0) {
            System.err.println("No files found in download directory");
            return;
        }
        
        System.out.println("Files in download directory:");
        for (File file : allFiles) {
            System.out.println("  - " + file.getName() + " (" + file.length() + " bytes)");
        }
        
        File pdfFile = null;
        for (File file : allFiles) {
            if (file.getName().toLowerCase().endsWith(".pdf") && !file.getName().equals(FINAL_NAME)) {
                pdfFile = file;
                break;
            }
        }
        
        if (pdfFile != null) {
            Path target = downloadPath.resolve(FINAL_NAME);
            Files.move(pdfFile.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("PDF renamed to: " + FINAL_NAME);
            System.out.println("Final file: " + target + " (" + target.toFile().length() + " bytes)");
        } else {
            System.err.println("No PDF file found to rename");
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
