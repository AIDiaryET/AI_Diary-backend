package backend.crawler.kca.component;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.net.URI;
import java.util.Optional;

public class FrameAwareFetcher {
    private static final int TIMEOUT_MS = 15000;

    public static Document fetchFollowingFrames(String url, String referrer, String userAgent) throws Exception {
        return fetch(url, referrer, userAgent, 0, 5);
    }

    private static Document fetch(String url, String ref, String ua, int depth, int maxDepth) throws Exception {
        if (depth > maxDepth) throw new IllegalStateException("Frame depth exceeded");
        Document doc = connect(url, ref, ua).get();

        Element frameset = doc.selectFirst("frameset");
        if (frameset == null) return doc;

        Optional<Element> frame = Optional.ofNullable(frameset.selectFirst("frame[name=mainFrame]"))
                .or(() -> Optional.ofNullable(frameset.selectFirst("frame")))
                .or(() -> Optional.ofNullable(doc.selectFirst("frame")));
        if (frame.isEmpty()) return doc;

        String src = frame.get().attr("src");
        String next = absolutize(url, src);
        return fetch(next, ref, ua, depth+1, maxDepth);
    }

    private static Connection connect(String url, String ref, String ua){
        String norm = url.replace("http://","https://").replace("//www.","//");
        return Jsoup.connect(norm)
                .timeout(TIMEOUT_MS)
                .referrer(ref!=null? ref: "https://www.counselors.or.kr/")
                .userAgent(ua!=null? ua: "Mozilla/5.0 (compatible; KCA-Crawler/1.0)");
    }

    private static String absolutize(String base, String href){
        try { return URI.create(base).resolve(href).toString(); }
        catch (Exception e) { return href; }
    }
}
