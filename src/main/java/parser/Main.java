package parser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/*
* Html parser for jam.ua site
* */
public class Main {
    private static int currentPage = 1;
    private static int numOfPages = 1;

    public static void main(String[] args) throws IOException {
        List<String> links = new ArrayList<>();

        try {
            while (currentPage <= numOfPages) {
                String url = String.format("https://jam.ua/picks?list=%d", currentPage);
                String html = HtmlParser.getHtml(url);
                links.addAll(HtmlParser.getLinksFromDivs(html));

                if (numOfPages == 1) {
                    numOfPages = Integer.parseInt(HtmlParser.getNumOfPages(html));
                }

                currentPage++;
            }
        } catch (Exception e) {
            System.out.println("Something went wrong...");
        }
        System.out.println(links.size());
        links.parallelStream().forEach(link -> {
            try {
                String html = HtmlParser.getHtml(link);
                Map<String, List<String>> imgLinks = HtmlParser.getImageLinks(html); //<title, img links>
                HtmlParser.downloadImages(imgLinks);

            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        String html = null;
        try {
            html = HtmlParser.getHtml("https://jam.ua/martin-picks-18a0050-1pick-pack-073");
            Map<String, List<String>> imgLinks = HtmlParser.getImageLinks(html);
            HtmlParser.downloadImages(imgLinks);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
