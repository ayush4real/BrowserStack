import io.github.bonigarcia.wdm.WebDriverManager;
import okhttp3.RequestBody;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.*;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.MediaType;
import okhttp3.Response;

public class Automation {
    WebDriver driver;

    @Before
    public void setup() {
        WebDriverManager.chromedriver().setup();
        driver = new ChromeDriver();
        driver.get("https://elpais.com/");
        new WebDriverWait(driver, Duration.ofSeconds(20)).until(
                wd -> "complete".equals(
                        ((JavascriptExecutor) wd).executeScript("return document.readyState")
                )
        );
    }

    @Test
    public void isTextSpanish(){
//        driver.get("https://elpais.com/");
        String isSpanish = driver.findElement(By.tagName("html")).getAttribute("lang");
        Assert.assertNotNull(isSpanish);
        if(!isSpanish.equals("es-ES")){
            throw new IllegalStateException("This page is not a Spanish, it is in: "+isSpanish );
        }else {
            System.out.println("---------Test for page language---------");
            System.out.println("This page is in Spanish");
        }
        System.out.println("------------------Test 1 completed--------------");
    }

    @Test
    public void scrapeOpinionArticles(){

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(60));

        boolean clicked = false;

        // For my local machine : look for the button by id
        try {
            WebElement button = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("didomi-notice-agree-button")));
            wait.until(ExpectedConditions.elementToBeClickable(button));
            button.click();
            clicked = true;
            System.out.println("Clicked button with id=didomi-notice-agree-button.");
        } catch (Exception e) {
            System.out.println("didomi-notice-agree-button not found or not clickable: " + e.getMessage());
        }

        // If !clicked, I try to find the <a> tag with text containing 'Accept and continue' - Automation Browserstack
        if (!clicked) {
            try {
                WebElement link = wait.until(ExpectedConditions.presenceOfElementLocated(
                        By.xpath("//a[text()='Accept and continue']")
                ));
                wait.until(ExpectedConditions.elementToBeClickable(link));
                link.click();
                clicked = true;
                System.out.println("Clicked <a> tag with 'Accept and continue' text.");
            } catch (Exception e) {
                System.out.println("Fallback <a> element not found or not clickable: " + e.getMessage());
            }
            System.out.println("Clicked <a> tag with 'Accept and continue' text.");
        }

        // After handling consent, click your target div link regardless
        try {
            WebElement opinionLink = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//div[@class='sm _df']//a[@data-mrf-link='https://elpais.com/opinion/']")
            ));
            opinionLink.click();
            System.out.println("Clicked opinion link successfully.");
        } catch (Exception e) {
            System.out.println("Failed to click the opinion link: " + e.getMessage());
        }


        List<WebElement> articles = driver.findElements(By.tagName("article"));
        List<Map<String,String>> articleInfo = new ArrayList<>();

        for(int i=0;i<Math.min(articles.size(),5);i++){
            WebElement article = articles.get(i);
            String sectionName = "";
            String title = "";
            String articleUrl = "";
            String authorDate = "";
            String summary = "";
            String imageUrl = "";

            // 1. Get section name
            try {
                WebElement sectionElement = article.findElement(By.xpath(".//header/a"));
                sectionName = sectionElement.getText().trim();
            } catch (Exception e) {
                sectionName = "No section name present";
            }

            // 2. Get title and
            // 3. Get URL
            try {
                WebElement titleElement = article.findElement(By.xpath(".//header/h2/a"));
                title = titleElement.getText().trim();
                articleUrl = titleElement.getAttribute("href");
            } catch (Exception e) {
                title = "No title present";
                articleUrl = "No articleURL present";
            }

            // 4. Get author_and_date (optional div as it is seen to be missing in some articles I saw)
            try {
                WebElement authorDiv = article.findElement(By.xpath(".//div[@class='c_a']"));
                authorDate = authorDiv.getText().trim();
            } catch (Exception e) {
                authorDate = "No author data available";
            }

            // 5. Get summary paragraph
            try {
                WebElement para = article.findElement(By.xpath(".//p"));
                summary = para.getText().trim();
            } catch (Exception e) {
                summary = "No paragraph available";
            }

            // 6. Get image URL from <figure>
            try {
                WebElement img = article.findElement(By.xpath(".//figure/a/img"));
                imageUrl = img.getAttribute("src");
            } catch (Exception e) {
                imageUrl = "No image URL present or no image present";
            }

            Map<String, String> info = new LinkedHashMap<>();
            info.put("sectionName",sectionName);
            info.put("title",title);
            info.put("articleUrl",articleUrl);
            info.put("authorDate",authorDate);
            info.put("summary",summary);
            info.put("imageUrl",imageUrl);

            if (imageUrl != null &&
                    !imageUrl.isEmpty() &&
                    !imageUrl.startsWith("No image URL")) {
                String filename = "image_" + (i + 1) + ".jpg";
                downloadImage(imageUrl, filename);
            }
            articleInfo.add(info);
        }

//        for(Map<String,String> info:articleInfo){
//            System.out.println(info);
//        }
        String allTitlesCombined="";
        for(int i=0;i<articleInfo.size();i++){
            System.out.println("Info for article "+(i+1));
            System.out.println("-------------");
            System.out.println("Section name: "+articleInfo.get(i).get("sectionName"));
            System.out.println("Title: "+articleInfo.get(i).get("title"));
            System.out.println("Article URL: "+articleInfo.get(i).get("articleUrl"));
            System.out.println("Author name and date posted: "+articleInfo.get(i).get("authorDate"));
            System.out.println("Summary: "+articleInfo.get(i).get("summary"));
            System.out.println("Image URL: "+articleInfo.get(i).get("imageUrl"));
            String titleInEng = translateText(articleInfo.get(i).get("title"),"es","en");
            allTitlesCombined=(allTitlesCombined+" "+titleInEng).trim();
            System.out.println("******");
            System.out.println("Title translated to English: "+titleInEng);
            System.out.println();
        }


//        Finding words that occur more than twice, across translated titles
        System.out.println("Frequency of words across all translated titles combined");
        Map <String,Integer> freq=countFreq(allTitlesCombined);
        int countMoreThan0 = 0;
        for (Map.Entry<String, Integer> entry : freq.entrySet()) {
            if (entry.getValue() > 2) {
                System.out.println(entry.getKey() + " occurred no. of times: " + entry.getValue());
                countMoreThan0++;
            }
        }
        if(countMoreThan0==0){
            System.out.println("No words found occurring more than 2 times");
        }

        System.out.println("------------------Test 2 completed------------------");
    }

    public Map<String,Integer> countFreq(String text){
        Map <String,Integer> freq = new LinkedHashMap<>();
        for(String i: text.trim().split(" ")){
            freq.put(i,freq.getOrDefault(i,0)+1);
        }
        return freq;
    }

    public String translateText(String text, String fromLang, String toLang) {
        OkHttpClient client = new OkHttpClient();

        String jsonBody = String.format("""
    {
      "text": %s,
      "to": "%s",
      "from_lang": "%s"
    }
    """, escapeJson(text), toLang, fromLang == null ? "" : fromLang);

        RequestBody body = RequestBody.create(
                jsonBody,
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url("https://google-api31.p.rapidapi.com/gtranslate")
                .post(body)
                .addHeader("x-rapidapi-key", "4bfb70a04cmshe40dfa268005eefp1e292ejsn1095b8f60f9c")
                .addHeader("x-rapidapi-host", "google-api31.p.rapidapi.com")
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String responseBody = response.body().string();
                // Parse translated_text from JSON response
                String marker = "\"translated_text\":\"";
                int start = responseBody.indexOf(marker);
                if (start >= 0) {
                    start += marker.length();
                    int end = responseBody.indexOf("\"", start);
                    if (end >= 0) {
                        return responseBody.substring(start, end);
                    }
                }
                return "Could not parse translation from response: " + responseBody;
            } else {
                return "Request failed: " + response.code() + " " + response.message();
            }
        } catch (Exception e) {
            System.err.println("Error occurred during translation: " + e.getMessage());
            return "Exception: " + e.getMessage();
        }
    }

    private String escapeJson(String text) {
        return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    public void downloadImage(String imageUrl, String fileName) {
        try {
            // Get folder where the current class .class file is located
            File currentFolder = new File(getClass().getProtectionDomain().getCodeSource().getLocation().toURI());

            // Create the full destination path inside this folder
            File destinationFile = new File(currentFolder, fileName);

            try (InputStream in = new URL(imageUrl).openStream();
                 FileOutputStream out = new FileOutputStream(destinationFile)) {

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }

            System.out.println("Downloaded image to " + destinationFile.getAbsolutePath());
        } catch (Exception e) {
            System.out.println("Error downloading image: " + e.getMessage());
        }
    }

    @After
    public void teardown() {
        driver.quit();
    }
}
