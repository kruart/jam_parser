package parser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

class HtmlParser {

    public static String getHtml(String url) throws IOException {
        return Jsoup.connect(url).get().html();
    }

    public static List<String> getLinksFromDivs(String html) {
        Document doc = Jsoup.parse(html);
        Elements divs = doc.select("div#catalog_item");
        return divs.stream()
                .map(div -> div.getElementsByTag("a").attr("href"))
                .collect(Collectors.toList());
    }

    public static String getNumOfPages(String html) {
        Document doc = Jsoup.parse(html);
        String href = doc.select("div#catalog_pager")
                .last()
                .getElementsByTag("a")
                .last()
                .attr("href");

        // https://jam.ua/picks?list=21  => 21
        return href.substring(href.lastIndexOf("=") + 1);
    }

    public static Map<String, List<String>> getImageLinks(String html) {
        List<String> imgLinks = new ArrayList<>();
        String domain = "https://jam.ua";

        Document doc = Jsoup.parse(html);
        String title = getTextFromElement(doc, "div#item_header_name > h1");
        if (!doc.select("div.img-item-gallery").isEmpty()) {
            imgLinks.addAll(getImgLinksFromGallery(doc, domain));
        } else {
            imgLinks.add(getMainImageLink(doc, domain));
        }
//        return Map.of(title, imgLinks); //java 9
        return Collections.unmodifiableMap(new HashMap<String, List<String>>() {{ put(title, imgLinks);}});
    }

    public static String getTextFromElement(Document doc, String element) {
        return doc.selectFirst(element).text();
    }

    private static String getMainImageLink(Document doc, String domain) {
        String beginningImageLink = "/files/";
        Element parentDiv = doc.selectFirst("div#item_image_wrapper");
        String itemImg = parentDiv.select("div#item_img > a > div").attr("style");
        int startIndex = itemImg.indexOf(beginningImageLink);
        String link = itemImg.split("'\\)")[0];

        return domain + itemImg.substring(startIndex, link.length());   //full link
    }

    private static List<String> getImgLinksFromGallery(Document doc, String domain) {
        return doc.select("div.img-item-block > a").stream()
                .map(a -> domain + a.attr("href"))
                .collect(Collectors.toList());
    }

    public static void downloadImages(Map<String, List<String>> data) throws IOException {
        String parentFolder = "./images/";

        for (Map.Entry<String, List<String>> entry : data.entrySet()) {
            for (String link : entry.getValue()) {
                System.out.println(Thread.currentThread() + " start working on " + entry.getKey() + " " + link);
                Path path = Paths.get(parentFolder + entry.getKey());

                if (!Files.exists(path)) {
                    Files.createDirectories(path);
                }

                String fullPath = path.toString() + File.separator + getImgName(link);

                try(InputStream in = new URL(link).openStream()){
                    Files.copy(in, Paths.get(fullPath), StandardCopyOption.REPLACE_EXISTING);
                }

                System.out.println(fullPath + " is downloaded.");
            }
        }
    }

    private static String getImgName(String link) {
        return link.substring(link.lastIndexOf("/") + 1);
    }
}
