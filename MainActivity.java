package com.example.feedbackclassifier;

import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.feedbackclassifier.databinding.ActivityMainBinding;
import com.google.android.material.tabs.TabLayout;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private List<String> feedbackLines = new ArrayList<>();

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private static final double SIMILARITY_THRESHOLD = 0.1;
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "a", "about", "above", "after", "again", "against", "all", "am", "an", "and", "any", "are", "aren't", "as", "at",
            "be", "because", "been", "before", "being", "below", "between", "both", "but", "by",
            "can", "can't", "cannot", "could", "couldn't", "did", "didn't", "do", "does", "doesn't", "doing", "don't", "down", "during",
            "each", "few", "for", "from", "further", "had", "hadn't", "has", "hasn't", "have", "haven't", "having", "he", "he'd",
            "he'll", "he's", "her", "here", "here's", "hers", "herself", "him", "himself", "his", "how", "how's",
            "i", "i'd", "i'll", "i'm", "i've", "if", "in", "into", "is", "isn't", "it", "it's", "its", "itself",
            "let's", "me", "more", "most", "mustn't", "my", "myself", "no", "nor", "of", "off", "on", "once", "only",
            "or", "other", "ought", "our", "ours", "ourselves", "out", "over", "own",
            "same", "shan't", "she", "she'd", "she'll", "she's", "should", "shouldn't", "so", "some", "such",
            "than", "that", "that's", "the", "their", "theirs", "them", "themselves", "then", "there", "there's", "these",
            "they", "they'd", "they'll", "they're", "they've", "this", "those", "through", "to", "too",
            "under", "until", "up", "very", "was", "wasn't", "we", "we'd", "we'll", "we're", "we've", "were",
            "weren't", "what", "what's", "when", "when's", "where", "where's", "which", "while", "who", "who's",
            "whom", "why", "why's", "with", "won't", "would", "wouldn't",
            "you", "you'd", "you'll", "you're", "you've", "your", "yours", "yourself", "yourselves",
            "get", "got", "go", "just", "like", "make", "really", "see", "say", "said", "sent"
    ));

    private final Set<String> NEGATION_WORDS = new HashSet<>(Arrays.asList(
            "not", "no", "never", "n't", "cannot", "isn't", "wasn't", "aren't",
            "weren't", "haven't", "hasn't", "hadn't", "doesn't", "don't", "didn't",
            "won't", "wouldn't", "shan't", "shouldn't", "mightn't", "mustn't", "never"
    ));

    private final List<String> positiveTrainingSentences = new ArrayList<>(Arrays.asList(
            "This product is amazing and I absolutely love its features.", "The customer service was excellent and very helpful.",
            "I am very happy with my purchase, it exceeded my expectations.", "What a fantastic experience, I would highly recommend this to everyone.",
            "The quality is superb and it's great value for money.", "Fast delivery and the item was in perfect condition.",
            "I'm thoroughly impressed with the performance.", "User interface is intuitive and easy to navigate.",
            "This app has made my life so much easier.", "Kudos to the development team for this brilliant tool.",
            "Five stars, would definitely buy again!", "The support staff were incredibly responsive and solved my issue quickly.",
            "It works flawlessly and smoothly.", "Great value for the price, very affordable.",
            "The design is sleek and modern.", "I appreciate the regular updates and new features.",
            "Exactly what I was looking for, thank you!", "A wonderful piece of software, very reliable.",
            "Setup was a breeze, got it running in minutes.", "The documentation is clear and helpful.",
            "This is a game-changer for my workflow.", "No complaints whatsoever, everything is perfect.",
            "Solid performance, no crashes or bugs encountered.", "I am delighted with this product.",
            "The features are very comprehensive and useful.", "It's surprisingly powerful for such a simple interface.",
            "Highly satisfied with the outcome.", "The best investment I've made this year.",
            "Exceptional quality and craftsmanship.", "Customer support went above and beyond.",
            "I love the new update, it's fantastic!", "This is by far the best app of its kind.",
            "So glad I found this, it's a lifesaver.", "The experience was pleasant from start to finish.",
            "Absolutely brilliant, couldn't ask for more.", "The product arrived earlier than expected, which was a great surprise.",
            "Everything works as advertised, if not better.", "I'm a very satisfied customer and will return.",
            "The team clearly cares about their users.", "Wonderful job, keep up the great work!",
            "This tool is incredibly efficient.", "A pleasure to use every day.", "The results are consistently excellent.",
            "I find it very user-friendly.", "This has significantly improved my productivity.", "Top-notch service and product.",
            "An outstanding solution for my needs.", "I have nothing but good things to say.", "The attention to detail is commendable.",
            "Well worth the money spent.", "I feel very positive about this purchase.", "It's a relief to find something that works so well.",
            "My colleagues are also impressed.", "The positive reviews are spot on.", "This made a difficult task much simpler.",
            "I am genuinely happy with this.", "It's a joy to interact with this product.", "The performance is consistently reliable.",
            "I'm giving this a glowing recommendation.", "Truly a five-star experience.", "It's easy to see why this is so popular.",
            "The value proposition is excellent.", "I couldn't be happier with my choice.", "This is exactly what I needed.",
            "The benefits are immediately obvious.", "A truly exceptional item.", "My expectations were not just met, but surpassed.",
            "I am thoroughly enjoying this.", "This product stands out from the competition.", "It's clear a lot of thought went into this.",
            "The positive aspects far outweigh any minor issues.", "I'm very pleased and would recommend it to friends.",
            "The functionality is superb.", "This is not bad, actually.", "The delivery was surprisingly fast.", "Excellent value for money."
    ));

    private final List<String> negativeTrainingSentences = new ArrayList<>(Arrays.asList(
            "The product broke after just one week, very poor quality.", "Customer support was terrible and unhelpful with my issue.",
            "I am extremely disappointed with this purchase, it was a waste of money.", "This is the worst service I have ever received.",
            "The item arrived damaged and the company is difficult to deal with.", "The customer service was terrible and very slow.",
            "Unfortunately, the software is full of bugs and very confusing to use.", "The app crashes frequently, making it unusable.",
            "Very frustrating experience, I regret buying this.", "The interface is clunky and difficult to understand.",
            "Performance is sluggish and unresponsive at times.", "This was a complete waste of time and money.",
            "Support took days to get back to me and offered no real solution.", "Full of glitches and errors, not ready for release.",
            "The instructions are unclear and poorly written.", "I found several critical bugs within the first hour.",
            "Not worth the price, there are better alternatives.", "The advertised features are misleading or don't work.",
            "I encountered many problems trying to set it up.", "This is incredibly disappointing, I expected much more.",
            "The battery drain is unacceptable when using this app.", "I will be requesting a refund immediately.",
            "Avoid this product at all costs.", "It's not user-friendly at all.",
            "The updates seem to make things worse, not better.", "I'm very unhappy with the quality.",
            "Too complicated for what it does.", "This is a piece of junk, don't buy it.",
            "The company doesn't seem to care about its customers.", "I'm fed up with the constant issues.",
            "A truly awful experience from start to finish.", "The product is defective and poorly made.",
            "I would not recommend this to anyone.", "The customer service agent was rude and unhelpful.",
            "This app is a nightmare to navigate.", "It failed to deliver on its promises.",
            "I am deeply dissatisfied with this purchase.", "The system is unstable and unreliable.",
            "This has caused me nothing but headaches.", "A very bad decision to buy this.",
            "The quality is shockingly poor.", "I feel cheated and ripped off.",
            "There are too many flaws to list.", "This product is fundamentally broken.",
            "I have never been so disappointed in a purchase.", "The negative reviews are all true.",
            "It's frustratingly slow and inefficient.", "This is an absolute disaster.",
            "I cannot find a single good thing to say about it.", "The features are lacking and poorly implemented.",
            "I demand a full refund for this terrible product.", "It's completely useless for my needs.",
            "The user experience is abysmal.", "I wish I had never bought this.",
            "This product is a major letdown.", "The software is riddled with problems.",
            "I'm extremely annoyed by the lack of support.", "This feels like a scam.",
            "It's far too expensive for such low quality.", "I regret spending money on this.",
            "The app consistently fails to perform basic tasks.", "This is by far the worst app I have ever used.",
            "I am having a lot of trouble with this system.", "It's very buggy and crashes often.",
            "The learning curve is unnecessarily steep and confusing.", "I'm not satisfied with the product at all.",
            "I encountered an issue with the setup, it was confusing.", "The quality is poor.", "Not good at all.",
            "I will never buy this again.", "The app has a lot of errors."
    ));

    private final List<String> neutralTrainingSentences = new ArrayList<>(Arrays.asList(
            "The product is okay, it does what it says on the tin.", "The delivery time was average for this type of item.",
            "It's an acceptable solution, nothing special but it works.", "The features are standard and meet basic requirements.",
            "My experience was neither good nor bad, just fine.", "This is a standard product with no outstanding features.",
            "The application was installed successfully.", "User registration requires an email address.",
            "The current version is 2.1.3.", "This item is available in three colors: red, green, and blue.",
            "The settings menu can be accessed from the top right corner.", "Processing the file took several seconds to complete.",
            "The feedback has been submitted for review by the team.", "It meets the basic criteria outlined in the documentation.",
            "The software is compatible with Android 10 and above.", "This is a factual statement about the product's specifications.",
            "The price point is in the mid-range for similar items.", "It has the usual options one would expect from this type of software.",
            "The process is straightforward and follows a logical sequence.", "No strong feelings either way about this particular feature.",
            "It performs as advertised, no more, no less than expected.", "The data is displayed in a tabular format with sortable columns.",
            "This component handles data input from the user form.", "The default background color of the main screen is blue.",
            "There are five steps in the user onboarding wizard.", "User confirmed the action by clicking the 'Proceed' button.",
            "The system will undergo scheduled maintenance tonight.", "Please refer to the user manual for more details.",
            "The item weighs approximately 2 kilograms.", "This model was released in the third quarter of last year.",
            "The warranty period is one year from the date of purchase.", "Data synchronization occurs every 15 minutes.",
            "The report was generated on the specified date.", "This is simply an observation without judgment.",
            "The file format required is CSV.", "Login requires a username and password.",
            "The maximum file size is 10MB.", "This information is provided for your reference.",
            "The conference call is scheduled for 3 PM.", "There are multiple ways to achieve this outcome.",
            "The program executed without any error messages.", "This is a description of the object.",
            "The quick brown fox jumps over the lazy dog.", "She sells seashells by the seashore.",
            "The dimensions are 10cm by 5cm by 2cm.", "The button is located on the left side of the screen.",
            "This feature is currently in beta testing.", "The update will be rolled out next week.",
            "User is viewing the dashboard.", "The list contains ten items.",
            "This is a standard procedure.", "The server response time was 200ms.",
            "Notification preferences can be adjusted.", "The account was created on January 1st.",
            "The payment was processed.", "This text serves as a placeholder.",
            "The meeting has been rescheduled.", "Input is validated before submission.",
            "The manual provides instructions for assembly.", "This is a neutral statement about the weather.",
            "It's an okay product, not bad but not great either.", "This is fine."
    ));

    private static final Pattern TOKENIZE_PATTERN = Pattern.compile("[^\\w']+");

    private final ActivityResultLauncher<String[]> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) {
                    handleSelectedFile(uri);
                } else {
                    Toast.makeText(this, "No file selected.", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        binding.goodFeedbackScrollView.setBackgroundColor(Color.parseColor("#E6FFE6"));
        binding.goodFeedbackArea.setTextColor(Color.parseColor("#006400"));

        binding.badFeedbackScrollView.setBackgroundColor(Color.parseColor("#FFEEEE"));
        binding.badFeedbackArea.setTextColor(Color.parseColor("#8B0000"));

        binding.neutralFeedbackScrollView.setBackgroundColor(Color.parseColor("#F0F5FA"));
        binding.neutralFeedbackArea.setTextColor(Color.parseColor("#3C3C5A"));

        setupTabs();

        binding.loadFileButton.setOnClickListener(v -> loadFile());
        binding.processButton.setOnClickListener(v -> processFeedback());

        updateStatus("Status: Ready", false);
        showSelectedFeedbackView(0);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void setupTabs() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("👍 Good (0)"));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("👎 Bad (0)"));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("😐 Neutral (0)"));

        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                showSelectedFeedbackView(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }

    private void showSelectedFeedbackView(int position) {
        binding.goodFeedbackScrollView.setVisibility(position == 0 ? View.VISIBLE : View.GONE);
        binding.badFeedbackScrollView.setVisibility(position == 1 ? View.VISIBLE : View.GONE);
        binding.neutralFeedbackScrollView.setVisibility(position == 2 ? View.VISIBLE : View.GONE);

        final ScrollView scrollViewToScroll;
        switch (position) {
            case 0:
                scrollViewToScroll = binding.goodFeedbackScrollView;
                break;
            case 1:
                scrollViewToScroll = binding.badFeedbackScrollView;
                break;
            case 2:
                scrollViewToScroll = binding.neutralFeedbackScrollView;
                break;
            default:
                scrollViewToScroll = null;
                break;
        }

        if (scrollViewToScroll != null) {
            scrollViewToScroll.post(() -> scrollViewToScroll.fullScroll(View.FOCUS_UP));
        }
    }

    private void loadFile() {
        filePickerLauncher.launch(new String[]{"text/plain"});
    }

    private void handleSelectedFile(Uri uri) {
        String fileName = getFileName(uri);
        binding.selectedFileLabel.setText("File: " + fileName);
        binding.selectedFileLabel.setTextColor(ContextCompat.getColor(this, com.google.android.material.R.color.material_on_surface_emphasis_high_type));

        clearResultsExceptOriginal();
        updateStatus("Status: Reading file...", false);

        executorService.execute(() -> {
            try {
                List<String> lines = readFileContent(uri);
                mainHandler.post(() -> {
                    feedbackLines = lines;
                    binding.originalFeedbackArea.setText(String.join("\n", feedbackLines));
                    binding.processButton.setEnabled(!feedbackLines.isEmpty());
                    updateStatus("Status: File loaded. Ready to process.", false);
                    if (!feedbackLines.isEmpty()) {
                        binding.originalFeedbackScrollView.post(() -> binding.originalFeedbackScrollView.fullScroll(View.FOCUS_UP));
                    }
                });
            } catch (IOException e) {
                mainHandler.post(() -> {
                    binding.selectedFileLabel.setText("Error loading file.");
                    binding.selectedFileLabel.setTextColor(ContextCompat.getColor(this, com.google.android.material.R.color.design_default_color_error));
                    updateStatus("Status: Error reading file: " + e.getMessage(), true);
                    binding.processButton.setEnabled(false);
                    Toast.makeText(MainActivity.this, "Error reading file: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void clearResultsExceptOriginal() {
        binding.goodFeedbackArea.setText("");
        binding.badFeedbackArea.setText("");
        binding.neutralFeedbackArea.setText("");
        updateTabCounts(0, 0, 0);
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme() != null && uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                // Ignored
            }
        }
        if (result == null) {
            result = uri.getPath();
            if (result != null) {
                int cut = result.lastIndexOf('/');
                if (cut != -1) {
                    result = result.substring(cut + 1);
                }
            }
        }
        return result != null ? result : "Selected File";
    }

    private List<String> readFileContent(Uri uri) throws IOException {
        List<String> lines = new ArrayList<>();
        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            if (inputStream == null) {
                throw new IOException("Unable to open input stream for URI: " + uri);
            }
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        return lines;
    }

    private void clearClassifiedResultsOnly() {
        binding.goodFeedbackArea.setText("");
        binding.badFeedbackArea.setText("");
        binding.neutralFeedbackArea.setText("");
        updateTabCounts(0, 0, 0);
    }

    private void processFeedback() {
        if (feedbackLines.isEmpty()) {
            Toast.makeText(this, "No feedback loaded to process.", Toast.LENGTH_SHORT).show();
            return;
        }

        clearClassifiedResultsOnly();
        updateStatus("Status: Processing...", false);
        binding.processButton.setEnabled(false);
        binding.loadFileButton.setEnabled(false);

        executorService.execute(() -> {
            StringBuilder goodFeedback = new StringBuilder();
            StringBuilder badFeedback = new StringBuilder();
            StringBuilder neutralFeedback = new StringBuilder();
            final int[] goodCount = {0};
            final int[] badCount = {0};
            final int[] neutralCount = {0};
            final int totalLines = feedbackLines.size();

            for (int i = 0; i < totalLines; i++) {
                String line = feedbackLines.get(i);
                if (line.trim().isEmpty()) continue;

                String sentiment = classifySentiment(line);
                switch (sentiment) {
                    case "GOOD":
                        goodFeedback.append(line).append("\n\n");
                        goodCount[0]++;
                        break;
                    case "BAD":
                        badFeedback.append(line).append("\n\n");
                        badCount[0]++;
                        break;
                    case "NEUTRAL":
                    default:
                        neutralFeedback.append(line).append("\n\n");
                        neutralCount[0]++;
                        break;
                }

                if (i % 20 == 0 || i == totalLines - 1) {
                    String currentGood = goodFeedback.toString();
                    String currentBad = badFeedback.toString();
                    String currentNeutral = neutralFeedback.toString();
                    int currentGoodC = goodCount[0];
                    int currentBadC = badCount[0];
                    int currentNeutralC = neutralCount[0];
                    int progress = i + 1;

                    mainHandler.post(() -> {
                        binding.goodFeedbackArea.setText(currentGood.trim());
                        binding.badFeedbackArea.setText(currentBad.trim());
                        binding.neutralFeedbackArea.setText(currentNeutral.trim());
                        updateTabCounts(currentGoodC, currentBadC, currentNeutralC);
                        updateStatus("Status: Processing... (" + progress + "/" + totalLines + ")", false);
                    });
                }
                try {
                    Thread.sleep(2);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            mainHandler.post(() -> {
                binding.goodFeedbackArea.setText(goodFeedback.toString().trim());
                binding.badFeedbackArea.setText(badFeedback.toString().trim());
                binding.neutralFeedbackArea.setText(neutralFeedback.toString().trim());

                binding.goodFeedbackScrollView.post(() -> binding.goodFeedbackScrollView.fullScroll(View.FOCUS_UP));
                binding.badFeedbackScrollView.post(() -> binding.badFeedbackScrollView.fullScroll(View.FOCUS_UP));
                binding.neutralFeedbackScrollView.post(() -> binding.neutralFeedbackScrollView.fullScroll(View.FOCUS_UP));

                updateTabCounts(goodCount[0], badCount[0], neutralCount[0]);
                updateStatus("Status: Processing complete. Good: " + goodCount[0] +
                        ", Bad: " + badCount[0] + ", Neutral: " + neutralCount[0], false);
                binding.processButton.setEnabled(true);
                binding.loadFileButton.setEnabled(true);
            });
        });
    }

    private void updateTabCounts(int good, int bad, int neutral) {
        TabLayout.Tab goodTab = binding.tabLayout.getTabAt(0);
        if (goodTab != null) goodTab.setText("👍 Good (" + good + ")");
        TabLayout.Tab badTab = binding.tabLayout.getTabAt(1);
        if (badTab != null) badTab.setText("👎 Bad (" + bad + ")");
        TabLayout.Tab neutralTab = binding.tabLayout.getTabAt(2);
        if (neutralTab != null) neutralTab.setText("😐 Neutral (" + neutral + ")");
    }

    private void updateStatus(String message, boolean isError) {
        binding.statusLabel.setText(message);
        binding.statusLabel.setTextColor(
                ContextCompat.getColor(this, isError ?
                        com.google.android.material.R.color.design_default_color_error :
                        com.google.android.material.R.color.material_on_surface_emphasis_medium)
        );
    }

    private Set<String> tokenizeSentence(String sentence) {
        if (sentence == null || sentence.trim().isEmpty()) {
            return new HashSet<>();
        }
        String[] words = TOKENIZE_PATTERN.split(sentence.toLowerCase());
        Set<String> tokens = new HashSet<>();
        for (String word : words) {
            if (!word.isEmpty() && !STOP_WORDS.contains(word)) {
                tokens.add(word);
            }
        }
        return tokens;
    }

    private double jaccardSimilarity(Set<String> set1, Set<String> set2) {
        if (set1 == null || set2 == null || set1.isEmpty() || set2.isEmpty()) {
            return 0.0;
        }

        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);

        return (double) intersection.size() / union.size();
    }

    private String classifySentiment(String text) {
        Set<String> inputWords = tokenizeSentence(text);
        if (inputWords.isEmpty()) return "NEUTRAL";

        double maxPositiveSimilarity = 0.0;
        for (String trainSentence : positiveTrainingSentences) {
            maxPositiveSimilarity = Math.max(maxPositiveSimilarity, jaccardSimilarity(inputWords, tokenizeSentence(trainSentence)));
        }

        double maxNegativeSimilarity = 0.0;
        for (String trainSentence : negativeTrainingSentences) {
            maxNegativeSimilarity = Math.max(maxNegativeSimilarity, jaccardSimilarity(inputWords, tokenizeSentence(trainSentence)));
        }

        double maxNeutralSimilarity = 0.0;
        for (String trainSentence : neutralTrainingSentences) {
            maxNeutralSimilarity = Math.max(maxNeutralSimilarity, jaccardSimilarity(inputWords, tokenizeSentence(trainSentence)));
        }

        // --- Negation Handling ---
        boolean hasNegation = false;
        String[] originalWords = text.toLowerCase().split("\\s+");
        for (String word : originalWords) {
            if (NEGATION_WORDS.contains(word)) {
                hasNegation = true;
                break;
            }
        }

        if (hasNegation) {
            maxNegativeSimilarity += 0.2; // Boost negative score if a negation word is found
        }
        // --- End of Negation Handling ---

        // --- Classification Logic ---
        if (maxPositiveSimilarity > maxNegativeSimilarity && maxPositiveSimilarity > maxNeutralSimilarity && maxPositiveSimilarity >= SIMILARITY_THRESHOLD) {
            return "GOOD";
        }

        if (maxNegativeSimilarity > maxPositiveSimilarity && maxNegativeSimilarity > maxNeutralSimilarity && maxNegativeSimilarity >= SIMILARITY_THRESHOLD) {
            return "BAD";
        }

        // Fallback Logic
        if (maxPositiveSimilarity > maxNegativeSimilarity && maxPositiveSimilarity > maxNeutralSimilarity) {
            return "GOOD";
        } else if (maxNegativeSimilarity > maxPositiveSimilarity && maxNegativeSimilarity > maxNeutralSimilarity) {
            return "BAD";
        } else {
            return "NEUTRAL";
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }
    }
}