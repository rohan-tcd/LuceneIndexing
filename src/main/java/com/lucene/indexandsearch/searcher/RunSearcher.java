package com.lucene.indexandsearch.searcher;

import com.lucene.indexandsearch.indexer.SMJAnalyzer;
import com.lucene.indexandsearch.utils.Constants;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.*;
import org.apache.lucene.search.similarities.LMSimilarity.CollectionModel;
import org.apache.lucene.store.FSDirectory;

import java.io.*;

import static com.lucene.indexandsearch.searcher.RunSearcher.SimModel.BM25;

public class RunSearcher {

    protected Similarity simfn;
    protected IndexReader reader;
    protected IndexSearcher searcher;
    protected Analyzer analyzer;
    protected QueryParser parser;
    protected CollectionModel colModel;
    protected String fieldsFile;
    protected String qeFile;

    protected enum SimModel {
        BM25, LMD, LMJ
    }

    protected SimModel sim;

    private void setSim(String val) {
        try {
            sim = SimModel.valueOf(val);
        } catch (Exception e) {
            System.out.println("Similarity Function Not Recognized - Setting to Default");
            System.out.println("Possible Similarity Functions are:");
            for (SimModel value : SimModel.values()) {
                System.out.println("<MODELBM25>" + value.name() + "</MODELBM25>");
            }
            sim = BM25;
        }
    }

    public void selectSimilarityFunction(SimModel sim) {
        colModel = null;
        switch (sim) {

            case BM25:
                System.out.println("BM25 Similarity Function");
                simfn = new BM25Similarity(Constants.k, Constants.b);
                break;
            case LMD:
                System.out.println("LM Dirichlet Similarity Function");
                colModel = new LMSimilarity.DefaultCollectionModel();
                simfn = new LMDirichletSimilarity(colModel, Constants.mu);
                break;

            case LMJ:
                System.out.println("LM Jelinek Mercer Similarity Function");
                colModel = new LMSimilarity.DefaultCollectionModel();
                simfn = new LMJelinekMercerSimilarity(colModel, Constants.lam);
                break;
            default:
                System.out.println("Default Similarity Function");
                simfn = new BM25Similarity();

                break;
        }
    }

    public void setParams(String similarityToUse) {


//        setSim(Constants.MODELBM25.toUpperCase());
        setSim(similarityToUse.toUpperCase());

//        System.out.println("Path to index: " + Constants.indexName);
//        System.out.println("Query File: " + Constants.queryFile);
//        System.out.println("Result File: " + Constants.searchResultFile);
//        System.out.println("Model: " + Constants.MODELBM25);
//        System.out.println("Max Results: " + Constants.maxResults);
//        if (sim == BM25) {
//            System.out.println("b value: " + Constants.b);
//            System.out.println("k value: " + Constants.k);
//        }


        analyzer = new SMJAnalyzer();
    }

    public void processQueryFile() {

        //Creating a File object
        File file1 = new File(Constants.resultsDirectoryPath);
        //Creating the directory
        boolean bool = file1.mkdir();
        if(bool){
            System.out.println("Directory created successfully");
        }else{
            System.out.println("Sorry couldn’t create specified directory");
        }
        System.out.println(Constants.CYAN_BOLD_BRIGHT + "Query File..." + Constants.ANSI_RESET);
        try {
            BufferedReader br = new BufferedReader(new FileReader(Constants.queryFile));
            File file = new File(Constants.searchResultFile + "_" + sim);
            FileWriter fw = new FileWriter(file);
            int docCount = 0;
            try {
                String line = br.readLine();
                while (line != null) {
                    docCount++;
                    String[] parts = line.split(" ");
                    String qno = parts[1];
                    String queryTerms = "";
                    if (br.readLine().equalsIgnoreCase(".W")) {
//                        String lineChecker = br.readLine();
                        line = br.readLine();
                        do {
                            String[] queryParts = line.split(" ");
                            for (int i = 0; i < queryParts.length; i++)
                                queryTerms = queryTerms + " " + queryParts[i];
                            line = br.readLine();
                        } while (line != null && !line.startsWith(".I"));
                    }
                    System.out.println("QNo:" + qno);
                    ScoreDoc[] scored = runQuery(qno, queryTerms.trim());

                    int n = Math.min(Constants.maxResults, scored.length);

                    for (int i = 0; i < n; i++) {
                        Document doc = searcher.doc(scored[i].doc);
                        String docno = doc.get("docnum");
                        fw.write(docCount + "\tQ0\t" + docno + "\t" + (i + 1) + "\t" + scored[i].score + "\t" + Constants.runTag);
                        fw.write(System.lineSeparator());
                    }
                    //line = br.readLine();
                }
            } finally {
                br.close();
                fw.close();
            }
            System.out.println(Constants.CYAN_BOLD_BRIGHT + "Total number of queries: " + docCount + Constants.ANSI_RESET);
            System.out.println(Constants.CYAN_BOLD_BRIGHT + "Query Ranking/Result file is stored at: " + Constants.searchResultFile + "_" + sim + Constants.ANSI_RESET);

        } catch (Exception e) {
            System.out.println(" caught a " + e.getClass() +
                    "\n with message: " + e.getMessage());
        }
    }

    public ScoreDoc[] runQuery(String qno, String queryTerms) {
        ScoreDoc[] hits = null;

        System.out.println("Query No.: " + qno + " " + queryTerms);
        try {
            Query query = parser.parse(QueryParser.escape(queryTerms));

            try {
                TopDocs results = searcher.search(query, Constants.maxResults);
                hits = results.scoreDocs;
            } catch (IOException ioe) {
                ioe.printStackTrace();
                System.exit(1);
            }
        } catch (ParseException pe) {
            pe.printStackTrace();
            System.exit(1);
        }
        return hits;
    }

    public RunSearcher(String similarity) {
        System.out.println("Searcher");
        setParams(similarity);
        try {
            reader = DirectoryReader.open(FSDirectory.open(new File(Constants.indexName).toPath()));
            searcher = new IndexSearcher(reader);

            // create similarity function and parameter
            selectSimilarityFunction(sim);
            searcher.setSimilarity(simfn);
            analyzer = new SMJAnalyzer();
            parser = new QueryParser(Constants.FIELD_ALL, analyzer);

        } catch (Exception e) {
            System.out.println(" caught a " + e.getClass() +
                    "\n with message: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        String sim;
        if (args.length != 0) {
            sim = args[0];
        } else {
            System.out.println("Please mention similarity to use or default similarity BM25 would be used.");
            sim = Constants.MODELBM25;
        }
        RunSearcher searcher = new RunSearcher(sim);
        searcher.processQueryFile();
    }
}