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
import java.util.Arrays;
import java.util.List;
import java.lang.Iterable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin("*")
public class TweetController {

    static class Pair implements Comparable<Pair> {
	    public final int index;
	    public final float value;

	    public Pair(int index, float value) {
	        this.index = index;
	        this.value = value;
	    }

	    @Override
	    public int compareTo(Pair other) {
	        //multiplied to -1 as the author need descending sort order
	        return -1 * Float.valueOf(this.value).compareTo(other.value);
	    }
	}
	public static int julianDay(int year, int month, int day) {
		  int a = (14 - month) / 12;
		  int y = year + 4800 - a;
		  int m = month + 12 * a - 3;
		  int jdn = day + (153 * m + 2)/5 + 365*y + y/4 - y/100 + y/400 - 32045;
		  return jdn;
		}
	public static int diff(int y1, int m1, int d1, int y2, int m2, int d2) {
		  return julianDay(y1, m1, d1) - julianDay(y2, m2, d2);
		}

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

        String[] fields = {"Name", "UserName", "Text", "Hashtags", "Title", "Latitude", "Longtitude"}; //Add the fields here

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
        boosts.put(fields[5], 1.0f);
        boosts.put(fields[6], 1.0f);
        //adjust these
       	MultiFieldQueryParser parser2 = new MultiFieldQueryParser(fields, analyzer, boosts);
       	Query q = parser2.parse(query);
        ScoreDoc[] hits = indexSearcher.search(q, topHitCount).scoreDocs;

        String currdate = new Date().toString(); //current date
        String[] splitNow = currdate.split("\\s+"); // turn string to array
    	// [0] = day, [1] = month, [2] = date, [3] = time.=, [ 5] = year
        String split2 = splitNow[2] + ' ' + splitNow[1] + ' ' + splitNow[5];

        SimpleDateFormat format1=new SimpleDateFormat("dd MMM yyyy");
        SimpleDateFormat format2=new SimpleDateFormat("dd MM yyyy");
        try{
    		Date currdate2=format1.parse(split2);
            String currdate3 = format2.format(currdate2);
            splitNow = currdate3.split("\\s+");

            int d1 = Integer.parseInt(splitNow[0]); //date
            int m1 = Integer.parseInt(splitNow[1]); // month
            int y1 = Integer.parseInt(splitNow[2]); // year

            //this chunk is to adjust tweets by time relevancy-------------------------------------------------------------------------
            for (int rank = 0; rank < hits.length; ++rank) {
            	Document hitDoc = indexSearcher.doc(hits[rank].doc); // list of all the docs
            	//System.out.println(hitDoc.get("Date")); //datetime of current iteration
            	//System.out.println(hits[rank].score); //score of current iteration
            	String twttime = hitDoc.get("Date"); // get tweet's date
                String[] splittwt = twttime.split("\\s+"); // turn string to array
                String split2twt = splittwt[2] + ' ' + splittwt[1] + ' ' + splittwt[5];
        		Date currdate2twt=format1.parse(split2twt);
                String currdate3twt = format2.format(currdate2twt);
                splittwt = currdate3twt.split("\\s+");
                int d2 = Integer.parseInt(splittwt[0]); //date
                int m2 = Integer.parseInt(splittwt[1]); // month
                int y2 = Integer.parseInt(splittwt[2]); // year

            	//calculate Julian day distance
            	int dist = diff(y1,m1,d1,y2,m2,d2);
            	int scoremult = 0; //int to multiply our relevance score
            	if (dist <= 1) { //less than 1 day
            		scoremult = 5;
            	} else if (dist <= 3) { // less than 3 day
            		scoremult = 4;
            	} else if (dist <= 7) { // less than 7 day
            		scoremult = 3;
            	} else if (dist <= 30) { // less than 30 day
            		scoremult = 2;
            	} else { //after 30 day
            		scoremult = 1;
            	}
            	hits[rank].score = hits[rank].score*scoremult;
            }
        } catch (Exception e)  {
            e.printStackTrace();
        }
        Pair[] arr = new Pair[hits.length];
        for (int k = 0; k < hits.length; ++k) {
        	arr[k] = new Pair (k, hits[k].score);
        }
        Arrays.sort(arr);
        // System.out.println(hits.length);
        // tweets = new ArrayList<>();
        for (int rank = 0; rank < hits.length; ++rank) {
            Document hitDoc = indexSearcher.doc(hits[rank].doc);
            matches.add(new Tweet(hitDoc.get("UserName"), hitDoc.get("Name"), hitDoc.get("Text"), hitDoc.get("Hashtags"), hitDoc.get("Title"), hitDoc.get("Date")));
            // System.out.println((rank + 1) + " (score:" + hits[rank].score + ") " + hitDoc.get("Date") + " --> " +
            //                    hitDoc.get("Name") + " - " + hitDoc.get("Text"));
            // System.out.println(indexSearcher.explain(query, hits[rank].doc));
            // System.out.println(matches.get(rank).toString());
        }

        return matches;
    }
}
