package edu.cmu.pocketsphinx;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.anysou.aslogger.ASLogger;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import edu.cmu.pocketsphinx.kit.RecognizerSetupListener;
import edu.cmu.pocketsphinx.kit.RecognizerSetupTask;

/**
 * Created by xiaoyunfei on 2017/6/13.
 */

public class PocketSphinxUtil implements PocketListener {
    private static String TAG = "PocketSphinxUtil";
    private static PocketSphinxUtil pocketSphinxUtil;


    private SpeechRecognizer recognizer;
    private boolean isInit = false;

    private Assets assets;
    private PocketListener pocketListener;

    private String currentName = PocketListener.SEARCH;

    public static void init(Context context) {

        if (pocketSphinxUtil == null) {
            pocketSphinxUtil = new PocketSphinxUtil(context);
        }
    }

    public static PocketSphinxUtil get() throws NullPointerException {
        return pocketSphinxUtil;
    }

    private PocketSphinxUtil(Context context) {

        try {
            assets = new Assets(context);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void startRecord(String name, PocketListener pocketListener) {
        currentName = name;
        this.pocketListener = pocketListener;
        switchSearch();
    }

    public void stopRecord() {
        if (recognizer == null) {
            return;
        }
        try {
            recognizer.stop();
            System.gc();
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public void runRecognizerSetup(final RecognizerSetupListener recognizerSetupListener) {
        if (isInit) {
            recognizerSetupListener.onRecognizerAlreadySetup();
            return;
        }

        RecognizerSetupTask recognizerSetupTask = new RecognizerSetupTask(new RecognizerSetupListener() {
            @Override
            public void onRecognizerAlreadySetup() {
            }

            @Override
            public Exception doInBackGround() {

                try {
                    File assetDir = assets.syncAssets();
                    setupRecognizer(assetDir);
                } catch (IOException e) {
                    return e;
                }
                return null;
            }

            @Override
            public void onRecognizerPrepareError() {
                recognizerSetupListener.onRecognizerPrepareError();
            }

            @Override
            public void onRecognizerPrepareSuccess() {
                isInit = true;
                recognizerSetupListener.onRecognizerPrepareSuccess();
            }
        });
        recognizerSetupTask.execute();

    }


    public void switchSearch() {
        if (recognizer == null) {
            return;
        }
        try {
            recognizer.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }

        recognizer.startListening(currentName);
    }

    private void setupRecognizer(File assetsDir) throws IOException {

        SpeechRecognizerSetup setup = SpeechRecognizerSetup.defaultSetup();

        if (setup == null) {
            Log.e(TAG, "SpeechRecognizerSetup is null");
            return;
        }

          setup.setKeywordThreshold(1e-40f)
                 .setBoolean("-allphone_ci", true)
//          .setString("-keyphrase","你好中天") //forward
          ;

//        setup.setSampleRate(24000);


        File file = new File(assetsDir, "zh-ptm");
        if (!file.exists()) {
            Log.e(TAG, "zh-ptm not found");
            return;
        }
        setup.setAcousticModel(file);

        file = new File(assetsDir, "voice.dic");
        if (!file.exists()) {
            Log.e(TAG, "voice.dic not found");
            return;
        }
        setup.setDictionary(file);
        recognizer = setup.getRecognizer();
//        recognizer.addNgramSearch("ngram_search", new File(assetsDir, "voice.lm"));
        if (recognizer == null) {
            Log.e(TAG, "SpeechRecognizer1  is null");
            return;
        }
        recognizer.addListener(recognitionListener);
        recognizer.addKeyphraseSearch(PocketListener.SEARCH, "你好中天");

        File menuGrammar = new File(assetsDir, "zh_test.gram");
//        recognizer.addGrammarSearch(PocketListener.SEARCH, menuGrammar);

//        recognizer.addNgramSearch(PocketListener.SEARCH, new File(assetsDir, "7058.lm"));

    }

    RecognitionListener recognitionListener = new RecognitionListener() {
        @Override
        public void onBeginningOfSpeech() {
            Log.e(TAG, "onBeginningOfSpeech()");
            onSpeechStart();
        }

        @Override
        public void onEndOfSpeech() {
            String searchName = recognizer.getSearchName();
            Log.e(TAG, "onEndOfSpeech()" + searchName);
        }

        @Override
        public void onPartialResult(Hypothesis hypothesis) {

            if (hypothesis == null) {
                ASLogger.i(TAG, "正在监听，没有发现关键字 ");
                return;
            }
            ASLogger.i(TAG, "onPartialResult()：发现关键字：" + hypothesis.getHypstr());
        }

        @Override
        public void onResult(Hypothesis hypothesis) {
            if (hypothesis == null) {
                Log.e(TAG, "onResult() hypothesis is null ");
                return;
            }

            ASLogger.i(TAG, "onResult()" + hypothesis.getHypstr());
            String string = hypothesis.getHypstr();
            String[] arry = string.split(" ");
            List<String> list = Arrays.asList(arry);
            onSpeechResult(list);
        }

        @Override
        public void onError(Exception e) {
            Log.e(TAG, "onError()" + e.toString());
            onSpeechError(e.toString());
        }

        @Override
        public void onTimeout() {
            Log.e(TAG, "onTimeout()");
        }
    };

    @Override
    public void onSpeechStart() {
        if (pocketListener != null) {
            pocketListener.onSpeechStart();
        }
    }

    @Override
    public void onSpeechResult(List<String> strings) {
        if (pocketListener != null) {
            pocketListener.onSpeechResult(strings);
        }
    }

    @Override
    public void onSpeechError(String error) {
        if (pocketListener != null) {
            pocketListener.onSpeechError(error);
        }
    }
}
