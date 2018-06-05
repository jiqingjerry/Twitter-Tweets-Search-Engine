package org.ucr.cs172.jerryzhu.lucenesearcher;

import org.springframework.web.bind.annotation.*;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.DateTools.Resolution;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
// import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.lang.Iterable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin("*")
public class TweetController {
    static List<Tweet> tweets;
    // static String[] args = LuceneSearcherApplication.getArgs();

    @GetMapping("/tweets")
    public List<Tweet> searchTweets (@RequestParam(required=false, defaultValue="") String query) throws IOException, org.apache.lucene.queryparser.classic.ParseException  {
        // System.out.println(args[0]);
        Analyzer analyzer = new StandardAnalyzer();
        String name = System.getProperty("user.name");
        // Now search the index:
        Directory directory = FSDirectory.open(Paths.get("/home/"+name+"/Documents/indexTweets"));
        DirectoryReader indexReader = DirectoryReader.open(directory);

        IndexSearcher indexSearcher = new IndexSearcher(indexReader);

        String[] fields = {"Name", "UserName", "Text", "Hashtags", "Title"}; //Add the fields here

       	int topHitCount = 20;
        //Sort sort = new Sort(SortField.FIELD_SCORE, new SortField("Date", SortField.Type.STRING));
        List<Tweet> matches = new ArrayList<>();
        // for (Tweet tweet : tweets) {
        //     if (tweet.body.contains(query))
        //         matches.add(tweet);
        // }

        Map<String, Float> boosts = new HashMap<>();
        boosts.put(fields[0], .75f);
        boosts.put(fields[1], .75f);
        boosts.put(fields[2], 1.0f);
        boosts.put(fields[3], 1.5f);
        boosts.put(fields[4], 1.0f);
        //adjust these
       	MultiFieldQueryParser parser2 = new MultiFieldQueryParser(fields, analyzer, boosts);
       	Query q = parser2.parse(query);
        ScoreDoc[] hits = indexSearcher.search(q, topHitCount).scoreDocs;

        // System.out.println(hits.length);
        // tweets = new ArrayList<>();
        for (int rank = 0; rank < hits.length; ++rank) {
            Document hitDoc = indexSearcher.doc(hits[rank].doc);
            matches.add(new Tweet(hitDoc.get("UserName"), hitDoc.get("Name"), hitDoc.get("Text"), hitDoc.get("Hashtags"), hitDoc.get("Title")));
            // System.out.println((rank + 1) + " (score:" + hits[rank].score + ") " + hitDoc.get("Date") + " --> " +
            //                    hitDoc.get("Name") + " - " + hitDoc.get("Text"));
            // System.out.println(indexSearcher.explain(query, hits[rank].doc));
            // System.out.println(matches.get(rank).toString());
        }

        return matches;
    }
}
