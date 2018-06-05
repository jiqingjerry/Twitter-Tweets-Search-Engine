package org.twitter.crawler;

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
//import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import java.io.*;
import org.jsoup.*;
//import twitter4j.*;
import org.json.simple.JSONObject;

import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.lang.Iterable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;


public class index
{

	@SuppressWarnings("deprecation")
	public static void main(String[] args) throws IOException, org.apache.lucene.queryparser.classic.ParseException
	{
		Analyzer analyzer = new StandardAnalyzer();

        Directory directory = FSDirectory.open(Paths.get(args[1]));
        IndexWriterConfig config = new IndexWriterConfig(analyzer).setOpenMode(OpenMode.CREATE);
        IndexWriter indexWriter = new IndexWriter(directory, config);

        String path = args[0];
        try(Stream<Path> paths = Files.walk(Paths.get(path))) {
        	paths.forEach(filePath -> {
        		if(Files.isRegularFile(filePath)) {
        			JSONParser parser = new JSONParser();
        			try
        			{
	//					String filePath = "/home/jiqingjerry/Documents/tweets/num"+i+".json";
						System.out.println("Reading " + filePath);
						JSONArray a = (JSONArray) parser.parse(new FileReader(filePath.toString()));
						for(Object o : a)
						{
							JSONObject tweet = (JSONObject) o; //create the iteration of tweet
							Document doc = new Document(); // create the document
							String name = (String) tweet.get("name");
							String username = (String) tweet.get("username");
							String text = (String) tweet.get("text");
							String date = (String) tweet.get("date");
							String hashtag = (String) tweet.get("hashtags").toString();
							String title = "";
							if(tweet.get("titles") != null) {
								title = (String) tweet.get("titles").toString();
								title = title.replace("[","");
								title = title.replace("]","");
								title = title.replace(","," ");
								title = title.replace("\"","");
							}
							
							hashtag = hashtag.replace("[","");
							hashtag = hashtag.replace("]","");
							hashtag = hashtag.replace(","," ");
							hashtag = hashtag.replace("\"","");
							//check if tweet has geolocation, if not it will be null
							String state = null;
							String country = null;
							String lat = "0";
							String longs = "0";
							String hasgeo = "0"; // 0 indicates tweet doesn't have geolocation

							if ( (String) tweet.get("state") != null) {
								hasgeo = "1"; //1 indicates tweet has geolocation
								state = (String) tweet.get("state");
								country = (String) tweet.get("country");
								lat = (String) tweet.get("lat").toString();
								longs = (String) tweet.get("long").toString();
							}

				            doc.add(new StringField("Name", name, Field.Store.YES));
				            doc.add(new StringField("UserName", username, Field.Store.YES));
				            doc.add(new TextField("Text", text, Field.Store.YES));
				            doc.add(new StringField("Date", date, Field.Store.YES));
				            doc.add(new TextField("Hashtags",hashtag,Field.Store.YES));
				            //hasGeo field to later search for tweets that have GeoLocation
				            doc.add(new StringField("HasGeo",hasgeo,Field.Store.YES));
				            
				            doc.add(new TextField("Title", title, Field.Store.YES));
				            //adds geolocation if it has it
				            if (hasgeo == "1") {
					            doc.add(new StringField("State",state,Field.Store.YES));
					            doc.add(new StringField("Country",country,Field.Store.YES));
					            doc.add(new StringField("Lat",lat,Field.Store.YES));
					            doc.add(new StringField("Long",longs,Field.Store.YES));
				            }

				            //Add more here
				            indexWriter.addDocument(doc);
						}

        			}
					catch (ParseException e)
					{
						e.printStackTrace();
					} catch (FileNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
        		}
        	});
        }catch (IOException e) {
        	e.printStackTrace();
        }
        indexWriter.close();
	}

}
