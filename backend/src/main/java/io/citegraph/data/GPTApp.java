package io.citegraph.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class GPTApp {
    private static final Logger LOG = LoggerFactory.getLogger(DblpParser.class);
    private static final String GPT_URL = "https://api.openai.com/v1/chat/completions";
    private static final String API_KEY = System.getenv("OPENAI_KEY");
    private static final String GPT_MODEL = "gpt-3.5-turbo";

    private static final Map<String, Boolean> GPT_QA_CACHE = new HashMap<>();

    private static boolean sameOrg(String org1, String org2) {
        org1 = org1.trim();
        org2 = org2.trim();
        if (org1.compareTo(org2) < 0) {
            return sameOrg(org2, org1);
        }
        String key = org1 + "," + org2;
        if (GPT_QA_CACHE.containsKey(key)) {
            return GPT_QA_CACHE.get(key);
        }
        try {
            URL obj = new URL(GPT_URL);
            HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", "Bearer " + API_KEY);
            connection.setRequestProperty("Content-Type", "application/json");
            String prompt = "Do " + org1 + " and " + org2 + " belong to the same (larger) institute? Answer yes or no without explanation.";

            // The request body
            String body = "{\"model\": \"" + GPT_MODEL + "\", \"messages\": [{\"role\": \"user\", \"content\": \"" + prompt + "\"}]}";
            connection.setDoOutput(true);
            OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
            writer.write(body);
            writer.flush();
            writer.close();

            // Response from ChatGPT
            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuffer response = new StringBuffer();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
            br.close();
            LOG.info("Response from OpenAI is {}, org1 = {}, org2 = {}", response, org1, org2);
            boolean ans = response.toString().toLowerCase().contains("yes");
            GPT_QA_CACHE.put(key, ans);
            return ans;
        } catch (Exception e) {
            LOG.error("Fail to call OpenAI API", e);
            return false;
        }
    }

    public static void main(String[] args) {
        System.out.println("Using API key " + API_KEY);
        System.out.println("result = " + sameOrg("university of illinois at chicago", "Corresponding authors. Tel.: +86 13813825166 (Lin Chen).\n"));
    }
}
