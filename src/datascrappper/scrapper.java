package datascrappper;

import org.jsoup.Jsoup;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.URL;
import java.net.HttpURLConnection;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class scrapper {

    private static final String BASE_URL = "https://papers.nips.cc";

    public static void main(String[] args) {
        try {
           
            Document mainPage = Jsoup.connect(BASE_URL).get();
            Elements yearLinks = mainPage.select("a[href^=/paper_files/paper/]");

            List<String> years = new ArrayList<>();
            for (Element yearLink : yearLinks) {
                String yearText = yearLink.text();
                
                String year = extractYear(yearText);
                if (year != null) {
                    years.add(year);
                }
            }

         
            Collections.sort(years, Collections.reverseOrder());

           
            int limit = Math.min(5, years.size());

           
            for (int i = 0; i < limit; i++) {
                String year = years.get(i);
                String yearUrl = BASE_URL + "/papers/" + year;  
                System.out.println("Fetching papers for year: " + year);

               
                Document yearPage = Jsoup.connect(yearUrl).get();

             
                Elements paperLinks = yearPage.select("a[href^=/paper_files/paper/][title]");
                List<String> pdfLinks = new ArrayList<>();

                for (Element paperLink : paperLinks) {
                    String paperUrl = BASE_URL + paperLink.attr("href");
                    pdfLinks.add(paperUrl);
                }

              
                for (String pdfLink : pdfLinks) {
                    downloadPdf(pdfLink);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

   
    private static String extractYear(String yearText) {
       
        Pattern pattern = Pattern.compile("\\d{4}");
        Matcher matcher = pattern.matcher(yearText);
        if (matcher.find()) {
            return matcher.group(0);  
        }
        return null; 
    }

  
    private static void downloadPdf(String pdfUrl) {
        try {
          
            URL url = new URL(pdfUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

          
            String fileName = getFileNameFromPaperPage(pdfUrl);
            if (fileName == null || fileName.isEmpty()) {
                fileName = getFileNameFromUrl(pdfUrl); 
            }

            try (InputStream in = connection.getInputStream();
                 BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(fileName))) {

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }

                System.out.println("Downloaded: " + fileName);
            }
        } catch (IOException e) {
            System.err.println("Failed to download PDF: " + pdfUrl);
            e.printStackTrace();
        }
    }

  
    private static String getFileNameFromPaperPage(String pdfUrl) {
        try {
             
            Document paperPage = Jsoup.connect(pdfUrl).get();

            String title = paperPage.title();  

            if (title == null || title.isEmpty()) {
                title = paperPage.select("meta[name=citation_title]").attr("content");
            }

         
            if (title != null && !title.isEmpty()) {
                return sanitizeFileName(title) + ".pdf"; 
            }
        } catch (IOException e) {
            System.err.println("Failed to fetch paper page for filename extraction: " + pdfUrl);
            e.printStackTrace();
        }
        return null; 
    }

    private static String getFileNameFromUrl(String pdfUrl) {
        String fileName = pdfUrl.substring(pdfUrl.lastIndexOf("/") + 1);
        return sanitizeFileName(fileName);
    }

 
    private static String sanitizeFileName(String fileName) {
      
        fileName = fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
        return fileName;
    }
}
