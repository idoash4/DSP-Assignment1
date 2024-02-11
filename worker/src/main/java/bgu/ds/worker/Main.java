package bgu.ds.worker;
import org.json.JSONObject;

public class Main {
    static SentimentAnalysisHandler sentimentAnalysisHandler = new SentimentAnalysisHandler();
    static NamedEntityRecognitionHandler namedEntityRecognitionHandler = new NamedEntityRecognitionHandler();


    public static void main(String[] args) {
        String review = "{\"id\":\"R14D3WP6J91DCU\",\"link\":\"https://www.amazon.com/gp/customer-reviews/R14D3WP6J91DCU/ref=cm_cr_arp_d_rvw_ttl?ie=UTF8&ASIN=0689835604\",\"title\":\"Five Stars\",\"text\":\"Super cute book. My son Obama loves lifting the flaps.\",\"rating\":0,\"author\":\"Nikki J\",\"date\":\"2017-05-01T21:00:00.000Z\"}";
        JSONObject jsonObject = new JSONObject(review);
        String reviewText = jsonObject.getString("text");
        int rating = jsonObject.getInt("rating");
        int mainSentiment = sentimentAnalysisHandler.findSentiment(review);
        boolean isSarcastic = isSarcasm(rating, mainSentiment);
        String namedEntities = namedEntityRecognitionHandler.getEntities(reviewText);
        System.out.println("Rating: " + rating + ", Sentiment: " + mainSentiment + ", Is Sarcastic?: " + isSarcastic + " ,List: " + namedEntities);
    }

    public static boolean isSarcasm(int rating, int mainSentiment) {
        return Math.abs(rating - mainSentiment) > 2;
    }
}
