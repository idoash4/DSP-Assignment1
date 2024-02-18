package bgu.ds.local;

import bgu.ds.common.AWS;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Main {
    final static AWS aws = AWS.getInstance();
    public static void main(String[] args) {// args = [inFilePath, outFilePath, tasksPerWorker, -t (terminate, optional)]
        try {
            setup();
            aws.createInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
        String htmlContent = createHTMLContent();
        String filePath = "BLaalbaasg";
        createHTMLFile(htmlContent, filePath);

    }

    //Create Buckets, Create Queues, Upload JARs to S3
    private static void setup() {
        System.out.println("[DEBUG] Create bucket if not exist.");
        aws.createBucketIfNotExists(aws.bucketName);
    }

    public static void createHTMLFile(String htmlContent, String filePath) {
        try {
            File file = new File(filePath);
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(htmlContent);
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static String createHTMLContent()
    {
        //needed some reviewsFile with Title, link, sentiment(for color), entities, is sarcasem)
        Object[][] example = {
                {"titleOfProduct", "http://link1", 2, "No Entities!","False"},
                {"titleOfProduct", "http://link2", 2, "No Entities!","True"},
                {"titleOfProduct", "http://link3", 4, "No Entities!","True"},
                {"titleOfProduct", "http://link4", 0, "[Yagel:Person, Ekron:LOCATION]","True"}
        };
        String colors[] = {"darkred", "red", "black", "lightgreen", "darkgreen"}; // [0] -> very negetive color ... [4] -> very positive
        String HTMLString = "<html>\n" +
                            "<head>\n" +
                            "<title>Sarcasm Analysis</title>\n" +
                            "</head>\n" +
                            "<body>\n" +
                            "$body" +
                            "</body>\n" +
                            "</html>";
        String productTitle = "TitleExample";

        String htmlBody = "<h3>"+ productTitle +"</h2>\n<ul>\n";
        for (Object[] review : example) {
            htmlBody += "<li><a href=\"" + (String) review[1]+"\"";
            htmlBody += " style=\"color:" + colors[(int) review[2]] + ";\"> ";;
            htmlBody += (String) review[1] + "</a>\n";
            htmlBody += "<p> Entities: " + (String) review[3] +"</p>\n";
            htmlBody += "<p> Sarcasm detected? <b>" + (String) review[4] +"</b></p>\n";
            htmlBody += "</li>\n";
        }
        htmlBody += "</ul>\n";
        HTMLString = HTMLString.replace("$body", htmlBody);
        return HTMLString;

    }
}
