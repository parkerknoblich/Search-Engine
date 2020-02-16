package search.analyzers;

import datastructures.concrete.ChainedHashSet;
import datastructures.concrete.KVPair;
import datastructures.concrete.dictionaries.ChainedHashDictionary;
import datastructures.interfaces.IDictionary;
import datastructures.interfaces.IList;
import datastructures.interfaces.ISet;
//import misc.exceptions.NotYetImplementedException;
import search.models.Webpage;

import java.net.URI;

/**
 * This class is responsible for computing how "relevant" any given document is
 * to a given search query.
 *
 * See the spec for more details.
 */
public class TfIdfAnalyzer {
    // This field must contain the IDF score for every single word in all
    // the documents.
    private IDictionary<String, Double> idfScores;

    // This field must contain the TF-IDF vector for each webpage you were given
    // in the constructor.
    //
    // We will use each webpage's page URI as a unique key.
    private IDictionary<URI, IDictionary<String, Double>> documentTfIdfVectors;

    // Feel free to add extra fields and helper methods.
    private IDictionary<URI, Double> normOfDocTfIdfVectors;

    public TfIdfAnalyzer(ISet<Webpage> webpages) {
        // Implementation note: We have commented these method calls out so your
        // search engine doesn't immediately crash when you try running it for the
        // first time.
        //
        // You should uncomment these lines when you're ready to begin working
        // on this class.

        this.idfScores = this.computeIdfScores(webpages);
        this.documentTfIdfVectors = this.computeAllDocumentTfIdfVectors(webpages);
        this.normOfDocTfIdfVectors = new ChainedHashDictionary<>();
        for (KVPair<URI, IDictionary<String, Double>> pair : documentTfIdfVectors) {
            normOfDocTfIdfVectors.put(pair.getKey(), norm(pair.getValue()));
        }
    }

    // Note: this method, strictly speaking, doesn't need to exist. However,
    // we've included it so we can add some unit tests to help verify that your
    // constructor correctly initializes your fields.
    public IDictionary<URI, IDictionary<String, Double>> getDocumentTfIdfVectors() {
        return this.documentTfIdfVectors;
    }

    // Note: these private methods are suggestions or hints on how to structure your
    // code. However, since they're private, you're not obligated to implement exactly
    // these methods: feel free to change or modify these methods however you want. The
    // important thing is that your 'computeRelevance' method ultimately returns the
    // correct answer in an efficient manner.

    /**
     * Return a dictionary mapping every single unique word found
     * in every single document to their IDF score.
     */
    private IDictionary<String, Double> computeIdfScores(ISet<Webpage> pages) {
        double size = pages.size();
        IDictionary<String, Double> temp = new ChainedHashDictionary<>();
        for (Webpage site : pages) {
            ISet<String> uniqueWords = new ChainedHashSet<>();
            for (String word : site.getWords()) {
                uniqueWords.add(word);
            }
            for (String indUniqueWord : uniqueWords) {
                if (!temp.containsKey(indUniqueWord)) {
                    temp.put(indUniqueWord, 1.0);
                } else {
                    temp.put(indUniqueWord, temp.get(indUniqueWord) + 1);
                }
            }
        }
        IDictionary<String, Double> result = new ChainedHashDictionary<>();
        for (KVPair<String, Double> pair : temp) {
            double iDF = Math.log(size / pair.getValue());
            result.put(pair.getKey(), iDF);
        }
        return result;
    }

    /**
     * Returns a dictionary mapping every unique word found in the given list
     * to their term frequency (TF) score.
     *
     * The input list represents the words contained within a single document.
     */
    private IDictionary<String, Double> computeTfScores(IList<String> words) {
        double size = words.size();
        IDictionary<String, Double> result = new ChainedHashDictionary<>();
        for (String word : words) {
            if (!result.containsKey(word)) {
                result.put(word, 1.0 / size);
            } else {
                result.put(word, (result.get(word) * size + 1) / size);
            }
        }
        return result;
    }

    /**
     * See spec for more details on what this method should do.
     */
    private IDictionary<URI, IDictionary<String, Double>> computeAllDocumentTfIdfVectors(ISet<Webpage> pages) {
        // Hint: this method should use the idfScores field and
        // call the computeTfScores(...) method.
        IDictionary<URI, IDictionary<String, Double>> result =
                new ChainedHashDictionary<URI, IDictionary<String, Double>>();
        for (Webpage site : pages) {
            IList<String> allWords = site.getWords();
            IDictionary<String, Double> tFScores = computeTfScores(allWords);
            IDictionary<String, Double> temp = new ChainedHashDictionary<>();
            for (KVPair<String, Double> pair : tFScores) {
                String word = pair.getKey();
                double vector = pair.getValue() * idfScores.get(word);
                temp.put(word, vector);
            }
            result.put(site.getUri(), temp);
        }
        return result;
    }

    /**
     * Returns the cosine similarity between the TF-IDF vector for the given query and the
     * URI's document.
     *
     * Precondition: the given uri must have been one of the uris within the list of
     *               webpages given to the constructor.
     */
    public Double computeRelevance(IList<String> query, URI pageUri) {
        // Note: The pseudocode we gave you is not very efficient. When implementing,
        // this method, you should:
        //
        // 1. Figure out what information can be precomputed in your constructor.
        //    Add a third field containing that information.
        //
        // 2. See if you can combine or merge one or more loops.
        IDictionary<String, Double> uriVector = documentTfIdfVectors.get(pageUri);
        IDictionary<String, Double> queryVector = computeQueryVector(query);
        double numerator = 0.0;
        for (KVPair<String, Double> pair : queryVector) {
            double docWordScore = 0.0;
            String word = pair.getKey();
            if (uriVector.containsKey(word)) {
                docWordScore = uriVector.get(word);
            }
            double queryWordScore = queryVector.get(word);
            numerator += docWordScore * queryWordScore;
        }
        double denominator = normOfDocTfIdfVectors.get(pageUri) * norm(queryVector);
        if (denominator != 0.0) {
            return numerator / denominator;
        } else {
            return 0.0;
        }
    }

    private IDictionary<String, Double> computeQueryVector(IList<String> query) {
        IDictionary<String, Double> result = new ChainedHashDictionary<>();
        ISet<String> uniqueWords = new ChainedHashSet<>();
        for (String word : query) {
            uniqueWords.add(word);
        }
        IDictionary<String, Double> tfScores = computeTfScores(query);
        for (String word : uniqueWords) {
            if (idfScores.containsKey(word)) {
                result.put(word, tfScores.get(word) * idfScores.get(word));
            }
        }
        return result;
    }

    private double norm(IDictionary<String, Double> vector) {
        double output = 0.0;
        for (KVPair<String, Double> pair : vector) {
            double score = pair.getValue();
            output += score * score;
        }
        return Math.sqrt(output);
    }

}
