
public class SearchResult{
   String words; // strings matched for this url
   String[] url;   // url matching search query
   long frequency; //number of hits for page
   
   public SearchResult(String w, String[] urls, long freq)
   {
	   words = w;
	   url = urls;
	   frequency = freq;
   }
   
   public String GetWord()
   {
	   return words;
   }
   
   public String[] GetURLs()
   {
	   return url;
   }
   
   public long GetFrequency()
   {
	   return frequency;
   }
}



