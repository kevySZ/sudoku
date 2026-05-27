package com.kevy.sudoku;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.kevy.sudoku.game.Difficulty;
import com.kevy.sudoku.game.SudokuGenerator;
import com.kevy.sudoku.game.SudokuPuzzle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int MAX_MISTAKES = 3;
    private static final int MAX_HINTS = 3;
    private static final int HINT_COST = 15;
    private static final int INITIAL_COIN_BALANCE = 30;
    private static final int MAX_BEST_RECORDS = 10;
    private static final int CELL_COUNT = 81;
    private static final int BUTTON_SHADOW_TOP_SPACE_DP = 4;
    private static final int BUTTON_SHADOW_BOTTOM_SPACE_DP = 10;
    private static final String PREFS_NAME = "sudoku_game";
    private static final String RECORDS_PREFS_NAME = "sudoku_records";
    private static final String WALLET_PREFS_NAME = "sudoku_wallet";
    private static final String KEY_HAS_SAVED = "has_saved";
    private static final String KEY_DIFFICULTY = "difficulty";
    private static final String KEY_GIVENS = "givens";
    private static final String KEY_SOLUTION = "solution";
    private static final String KEY_CURRENT = "current";
    private static final String KEY_MISTAKES = "mistakes";
    private static final String KEY_HINTS_USED = "hints_used";
    private static final String KEY_ELAPSED = "elapsed";
    private static final String KEY_PAUSED = "paused";
    private static final String KEY_SELECTED_CELL = "selected_cell";
    private static final String KEY_COIN_BALANCE = "coin_balance";
    private static final String KEY_WALLET_INITIALIZED = "wallet_initialized";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final SudokuGenerator generator = new SudokuGenerator();
    private final Runnable timerTick = new Runnable() {
        @Override
        public void run() {
            updateTimerText();
            if (timerRunning) {
                handler.postDelayed(this, 500L);
            }
        }
    };

    private SudokuBoardView boardView;
    private Difficulty selectedHomeDifficulty = Difficulty.BEGINNER;
    private Button[] difficultyButtons;
    private TextView difficultyText;
    private TextView timerText;
    private TextView mistakesText;
    private TextView statusText;
    private TextView homeCoinText;
    private TextView coinText;
    private TextView hintsText;
    private Button pauseButton;
    private Button hintButton;
    private Button continueButton;
    private TextView continueHintText;
    private LinearLayout numberPad;
    private ProgressBar progressBar;

    private SudokuPuzzle puzzle;
    private Difficulty difficulty = Difficulty.BEGINNER;
    private int[] currentValues = new int[CELL_COUNT];
    private int selectedCell = -1;
    private int mistakes = 0;
    private int hintsUsed = 0;
    private boolean paused = false;
    private boolean gameOver = false;
    private boolean generating = false;
    private boolean screenVisible = false;
    private boolean showingGame = false;
    private boolean timerRunning = false;
    private long elapsedBeforeStartMs = 0L;
    private long timerStartedAtMs = 0L;
    private int generationToken = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializeWalletIfNeeded();
        showHome();
    }

    @Override
    protected void onResume() {
        super.onResume();
        screenVisible = true;
        startTimer();
    }

    @Override
    protected void onPause() {
        pauseTimer();
        saveGameIfNeeded();
        screenVisible = false;
        super.onPause();
    }

    @Override
    protected void onStop() {
        saveGameIfNeeded();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private void showHome() {
        pauseTimer();
        saveGameIfNeeded();
        showingGame = false;

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(color(R.color.sudoku_background));
        allowShadowOverflow(scrollView);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
        root.setPadding(dp(24), dp(28), dp(24), dp(40));
        allowShadowOverflow(root);
        scrollView.addView(root, new ScrollView.LayoutParams(-1, -2));
        setContentView(scrollView);

        SudokuLogoView logoView = new SudokuLogoView(this);
        root.addView(logoView, new LinearLayout.LayoutParams(dp(92), dp(92)));

        TextView title = new TextView(this);
        title.setText(R.string.app_name);
        title.setTextSize(36);
        title.setGravity(Gravity.CENTER);
        title.setTextColor(color(R.color.sudoku_text));
        title.setTypeface(title.getTypeface(), Typeface.BOLD);
        title.setPadding(0, dp(16), 0, 0);
        root.addView(title, new LinearLayout.LayoutParams(-1, -2));

        TextView subtitle = new TextView(this);
        subtitle.setText(R.string.home_subtitle);
        subtitle.setTextSize(16);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setTextColor(color(R.color.sudoku_muted));
        subtitle.setPadding(0, dp(8), 0, dp(24));
        root.addView(subtitle, new LinearLayout.LayoutParams(-1, -2));

        homeCoinText = new TextView(this);
        homeCoinText.setTextSize(15);
        homeCoinText.setGravity(Gravity.CENTER);
        homeCoinText.setTextColor(color(R.color.sudoku_primary_dark));
        homeCoinText.setTypeface(homeCoinText.getTypeface(), Typeface.BOLD);
        homeCoinText.setPadding(dp(14), dp(6), dp(14), dp(6));
        setRoundedBackground(homeCoinText, color(R.color.sudoku_surface_alt), 0, dp(18));
        LinearLayout.LayoutParams homeCoinParams = new LinearLayout.LayoutParams(-2, -2);
        homeCoinParams.bottomMargin = dp(20);
        root.addView(homeCoinText, homeCoinParams);
        updateCoinTexts();

        TextView difficultyLabel = sectionLabel(getString(R.string.difficulty_label));
        root.addView(difficultyLabel, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout difficultyGrid = new LinearLayout(this);
        difficultyGrid.setOrientation(LinearLayout.VERTICAL);
        difficultyGrid.setPadding(0, 0, 0, dp(4));
        allowShadowOverflow(difficultyGrid);
        root.addView(difficultyGrid, new LinearLayout.LayoutParams(-1, -2));
        difficultyButtons = new Button[Difficulty.values().length];
        for (int row = 0; row < 2; row++) {
            LinearLayout line = new LinearLayout(this);
            line.setOrientation(LinearLayout.HORIZONTAL);
            line.setGravity(Gravity.CENTER);
            allowShadowOverflow(line);
            LinearLayout.LayoutParams lineParams = new LinearLayout.LayoutParams(-1, dp(60));
            lineParams.bottomMargin = row == 1 ? dp(8) : 0;
            difficultyGrid.addView(line, lineParams);
            for (int col = 0; col < 2; col++) {
                int index = row * 2 + col;
                Difficulty item = Difficulty.values()[index];
                Button button = new Button(this);
                button.setText(difficultyName(item));
                button.setTextSize(16);
                button.setAllCaps(false);
                button.setOnClickListener(v -> {
                    selectedHomeDifficulty = item;
                    difficulty = item;
                    updateDifficultyButtons();
                });
                difficultyButtons[index] = button;
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(46), 1f);
                params.setMargins(
                        col == 0 ? 0 : dp(6),
                        dp(BUTTON_SHADOW_TOP_SPACE_DP),
                        col == 0 ? dp(6) : 0,
                        dp(BUTTON_SHADOW_BOTTOM_SPACE_DP));
                line.addView(button, params);
            }
        }
        selectedHomeDifficulty = difficulty;
        updateDifficultyButtons();

        Button startButton = new Button(this);
        startButton.setText(R.string.start_game);
        startButton.setTextSize(18);
        styleButton(startButton, true);
        startButton.setOnClickListener(v -> {
            difficulty = selectedHomeDifficulty;
            showGameLayout();
            startNewGame(difficulty);
        });
        LinearLayout.LayoutParams startParams = new LinearLayout.LayoutParams(-1, dp(52));
        startParams.topMargin = dp(24);
        startParams.bottomMargin = dp(8);
        root.addView(startButton, startParams);

        continueButton = new Button(this);
        continueButton.setText(R.string.continue_game);
        continueButton.setTextSize(18);
        styleButton(continueButton, false);
        continueButton.setOnClickListener(v -> continueSavedGame());
        LinearLayout.LayoutParams continueParams = new LinearLayout.LayoutParams(-1, dp(52));
        continueParams.topMargin = dp(4);
        continueParams.bottomMargin = dp(8);
        root.addView(continueButton, continueParams);

        Button recordsButton = new Button(this);
        recordsButton.setText(R.string.best_records);
        recordsButton.setTextSize(18);
        styleButton(recordsButton, false);
        recordsButton.setOnClickListener(v -> showBestRecords());
        LinearLayout.LayoutParams recordsParams = new LinearLayout.LayoutParams(-1, dp(52));
        recordsParams.topMargin = dp(4);
        recordsParams.bottomMargin = dp(8);
        root.addView(recordsButton, recordsParams);

        continueHintText = new TextView(this);
        continueHintText.setTextSize(14);
        continueHintText.setGravity(Gravity.CENTER);
        continueHintText.setTextColor(color(R.color.sudoku_muted));
        continueHintText.setPadding(0, dp(12), 0, 0);
        root.addView(continueHintText, new LinearLayout.LayoutParams(-1, -2));

        updateContinueState();
    }

    private void showGameLayout() {
        showingGame = true;

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(16), dp(14), dp(16), dp(40));
        allowShadowOverflow(root);
        root.setBackgroundColor(color(R.color.sudoku_background));
        setContentView(root);

        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(0, dp(2), 0, dp(10));
        root.addView(topBar, new LinearLayout.LayoutParams(-1, -2));

        difficultyText = new TextView(this);
        difficultyText.setTextSize(22);
        difficultyText.setTextColor(color(R.color.sudoku_text));
        difficultyText.setTypeface(difficultyText.getTypeface(), Typeface.BOLD);
        topBar.addView(difficultyText, new LinearLayout.LayoutParams(0, -2, 1f));

        coinText = new TextView(this);
        coinText.setTextSize(14);
        coinText.setGravity(Gravity.CENTER);
        coinText.setTextColor(color(R.color.sudoku_primary_dark));
        coinText.setTypeface(coinText.getTypeface(), Typeface.BOLD);
        coinText.setPadding(dp(10), dp(6), dp(10), dp(6));
        setRoundedBackground(coinText, color(R.color.sudoku_surface_alt), 0, dp(18));
        LinearLayout.LayoutParams coinParams = new LinearLayout.LayoutParams(-2, -2);
        coinParams.rightMargin = dp(8);
        topBar.addView(coinText, coinParams);

        timerText = new TextView(this);
        timerText.setText(R.string.timer_initial);
        timerText.setTextSize(18);
        timerText.setGravity(Gravity.CENTER);
        timerText.setTextColor(color(R.color.sudoku_primary_dark));
        timerText.setTypeface(timerText.getTypeface(), Typeface.BOLD);
        timerText.setPadding(dp(14), dp(6), dp(14), dp(6));
        setRoundedBackground(timerText, color(R.color.sudoku_surface_alt), 0, dp(18));
        topBar.addView(timerText, new LinearLayout.LayoutParams(-2, -2));

        boardView = new SudokuBoardView(this);
        boardView.setOnCellSelectedListener(index -> {
            if (puzzle == null || generating || paused || gameOver) {
                selectedCell = -1;
                boardView.setSelectedCell(-1);
                boardView.setHighlightedValue(0);
                return;
            }
            selectedCell = index;
            boardView.setSelectedCell(index);
            boardView.setHighlightedValue(currentValues[index]);
            statusText.setText(currentValues[index] == 0
                    ? getString(R.string.input_number)
                    : getString(R.string.same_number_highlighted));
            updateControls();
            saveGameIfNeeded();
        });
        root.addView(boardView, new LinearLayout.LayoutParams(-1, 0, 1f));

        progressBar = new ProgressBar(this);
        progressBar.setIndeterminate(true);
        progressBar.setVisibility(View.GONE);
        root.addView(progressBar, new LinearLayout.LayoutParams(dp(40), dp(40)));

        numberPad = new LinearLayout(this);
        numberPad.setOrientation(LinearLayout.HORIZONTAL);
        numberPad.setGravity(Gravity.CENTER);
        numberPad.setPadding(0, dp(10), 0, dp(10));
        allowShadowOverflow(numberPad);
        root.addView(numberPad, new LinearLayout.LayoutParams(-1, -2));
        for (int i = 1; i <= 9; i++) {
            final int value = i;
            Button button = new Button(this);
            button.setText(String.valueOf(i));
            button.setTextSize(18);
            styleNumberButton(button);
            button.setOnClickListener(v -> handleNumber(value));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(44), 1f);
            params.setMargins(
                    dp(2),
                    dp(BUTTON_SHADOW_TOP_SPACE_DP),
                    dp(2),
                    dp(BUTTON_SHADOW_BOTTOM_SPACE_DP));
            numberPad.addView(button, params);
        }

        LinearLayout actionBar = new LinearLayout(this);
        actionBar.setOrientation(LinearLayout.HORIZONTAL);
        actionBar.setGravity(Gravity.CENTER);
        actionBar.setPadding(0, dp(BUTTON_SHADOW_TOP_SPACE_DP), 0, dp(BUTTON_SHADOW_BOTTOM_SPACE_DP));
        allowShadowOverflow(actionBar);
        LinearLayout.LayoutParams actionBarParams = new LinearLayout.LayoutParams(-1, -2);
        actionBarParams.bottomMargin = dp(4);
        root.addView(actionBar, actionBarParams);

        pauseButton = new Button(this);
        pauseButton.setText(R.string.pause);
        styleButton(pauseButton, false);
        pauseButton.setOnClickListener(v -> togglePause());
        actionBar.addView(pauseButton, actionButtonParams(0, 4));

        hintButton = new Button(this);
        hintButton.setText(R.string.hint);
        styleButton(hintButton, true);
        hintButton.setOnClickListener(v -> handleHint());
        actionBar.addView(hintButton, actionButtonParams(1, 4));

        Button newGameButton = new Button(this);
        newGameButton.setText(R.string.new_game);
        styleButton(newGameButton, false);
        newGameButton.setOnClickListener(v -> startNewGame(difficulty));
        actionBar.addView(newGameButton, actionButtonParams(2, 4));

        Button homeButton = new Button(this);
        homeButton.setText(R.string.home);
        styleButton(homeButton, false);
        homeButton.setOnClickListener(v -> showHome());
        actionBar.addView(homeButton, actionButtonParams(3, 4));

        mistakesText = new TextView(this);
        mistakesText.setTextSize(17);
        mistakesText.setGravity(Gravity.CENTER);
        mistakesText.setTextColor(color(R.color.sudoku_text));
        mistakesText.setPadding(0, dp(10), 0, dp(2));
        root.addView(mistakesText, new LinearLayout.LayoutParams(-1, -2));

        hintsText = new TextView(this);
        hintsText.setTextSize(14);
        hintsText.setGravity(Gravity.CENTER);
        hintsText.setTextColor(color(R.color.sudoku_muted));
        hintsText.setPadding(0, 0, 0, dp(2));
        root.addView(hintsText, new LinearLayout.LayoutParams(-1, -2));

        statusText = new TextView(this);
        statusText.setTextSize(14);
        statusText.setGravity(Gravity.CENTER);
        statusText.setTextColor(color(R.color.sudoku_muted));
        statusText.setMinHeight(dp(34));
        root.addView(statusText, new LinearLayout.LayoutParams(-1, -2));
    }

    private void startNewGame(Difficulty newDifficulty) {
        if (!showingGame) {
            showGameLayout();
        }
        clearSavedGame();
        difficulty = newDifficulty;
        generationToken++;
        int token = generationToken;
        generating = true;
        gameOver = false;
        paused = false;
        mistakes = 0;
        hintsUsed = 0;
        selectedCell = -1;
        puzzle = null;
        currentValues = new int[CELL_COUNT];
        stopTimer();
        elapsedBeforeStartMs = 0L;
        updateTimerText();
        updateMistakesText();
        updateHintsText();
        updateCoinTexts();
        updateDifficultyText();
        updateControls();
        statusText.setText(getString(R.string.generating_puzzle, difficultyName(difficulty)));
        progressBar.setVisibility(View.VISIBLE);
        boardView.setWarningCell(-1);
        boardView.setSelectedCell(-1);
        boardView.setHighlightedValue(0);
        boardView.setBoard(new int[CELL_COUNT], currentValues);

        new Thread(() -> {
            SudokuPuzzle generated = generator.generate(newDifficulty);
            runOnUiThread(() -> {
                if (token != generationToken) {
                    return;
                }
                puzzle = generated;
                currentValues = generated.getGivens();
                generating = false;
                progressBar.setVisibility(View.GONE);
                renderPuzzleState();
                statusText.setText(R.string.choose_cell_hint);
                saveGameIfNeeded();
                startTimer();
            });
        }, "SudokuGenerator").start();
    }

    private void continueSavedGame() {
        if (!loadSavedGameIntoState()) {
            updateContinueState();
            return;
        }
        showGameLayout();
        renderPuzzleState();
        statusText.setText(paused ? getString(R.string.paused) : getString(R.string.restored_saved_game));
        startTimer();
    }

    private void renderPuzzleState() {
        if (puzzle == null || boardView == null) {
            return;
        }
        boardView.setBoard(puzzle.getGivens(), currentValues);
        boardView.setWarningCell(-1);
        boardView.setSelectedCell(selectedCell);
        boardView.setHighlightedValue(selectedCell >= 0 ? currentValues[selectedCell] : 0);
        updateDifficultyText();
        updateTimerText();
        updateMistakesText();
        updateHintsText();
        updateCoinTexts();
        updateControls();
    }

    private void handleNumber(int value) {
        if (puzzle == null || selectedCell < 0 || generating || paused || gameOver) {
            return;
        }
        if (currentValues[selectedCell] != 0) {
            return;
        }
        boardView.setWarningCell(-1);
        if (puzzle.isCorrect(selectedCell, value)) {
            currentValues[selectedCell] = value;
            boardView.setBoard(puzzle.getGivens(), currentValues);
            boardView.setHighlightedValue(value);
            statusText.setText(R.string.correct_input);
            updateControls();
            saveGameIfNeeded();
            if (puzzle.isSolved(currentValues)) {
                finishWithWin();
            }
        } else {
            mistakes++;
            boardView.setWarningCell(selectedCell);
            statusText.setText(R.string.wrong_input);
            updateMistakesText();
            saveGameIfNeeded();
            if (mistakes >= MAX_MISTAKES) {
                finishWithLoss();
            }
        }
    }

    private void handleHint() {
        if (puzzle == null || generating || paused || gameOver) {
            return;
        }
        if (hintsUsed >= MAX_HINTS) {
            statusText.setText(R.string.hint_limit_status);
            updateControls();
            return;
        }
        if (getCoinBalance() < HINT_COST) {
            statusText.setText(R.string.hint_no_coin_status);
            updateControls();
            return;
        }
        int hintCell = findHintCell();
        if (hintCell < 0) {
            statusText.setText(R.string.hint_no_empty_status);
            updateControls();
            return;
        }

        spendCoins(HINT_COST);
        hintsUsed++;
        selectedCell = hintCell;
        int hintValue = puzzle.getSolution()[hintCell];
        currentValues[hintCell] = hintValue;
        boardView.setWarningCell(-1);
        boardView.setSelectedCell(hintCell);
        boardView.setBoard(puzzle.getGivens(), currentValues);
        boardView.setHighlightedValue(hintValue);
        statusText.setText(getString(R.string.hint_used_status, hintValue, MAX_HINTS - hintsUsed));
        updateControls();
        updateHintsText();
        updateCoinTexts();
        saveGameIfNeeded();
        if (puzzle.isSolved(currentValues)) {
            finishWithWin();
        }
    }

    private int findHintCell() {
        if (selectedCell >= 0 && selectedCell < CELL_COUNT && currentValues[selectedCell] == 0) {
            return selectedCell;
        }
        for (int i = 0; i < CELL_COUNT; i++) {
            if (currentValues[i] == 0) {
                return i;
            }
        }
        return -1;
    }

    private void togglePause() {
        if (puzzle == null || generating || gameOver) {
            return;
        }
        if (paused) {
            paused = false;
            statusText.setText(R.string.game_continues);
            startTimer();
        } else {
            paused = true;
            statusText.setText(R.string.paused);
            pauseTimer();
        }
        updateControls();
        saveGameIfNeeded();
    }

    private void finishWithWin() {
        gameOver = true;
        pauseTimer();
        long completedElapsedMs = getElapsedMs();
        boolean newBestRecord = updateBestRecord(difficulty, completedElapsedMs);
        int rewardCoins = rewardForDifficulty(difficulty);
        addCoins(rewardCoins);
        clearSavedGame();
        updateControls();
        updateCoinTexts();
        statusText.setText(R.string.win_status);
        new AlertDialog.Builder(this)
                .setTitle(R.string.win_title)
                .setMessage(newBestRecord
                        ? getString(R.string.win_message_new_record,
                        formatTime(completedElapsedMs), mistakes, MAX_MISTAKES, difficultyName(difficulty), rewardCoins)
                        : getString(R.string.win_message, formatTime(completedElapsedMs), mistakes, MAX_MISTAKES, rewardCoins))
                .setPositiveButton(R.string.new_game, (dialog, which) -> startNewGame(difficulty))
                .setNegativeButton(R.string.close, null)
                .show();
    }

    private void finishWithLoss() {
        gameOver = true;
        pauseTimer();
        clearSavedGame();
        updateControls();
        statusText.setText(R.string.loss_status);
        new AlertDialog.Builder(this)
                .setTitle(R.string.loss_title)
                .setMessage(R.string.loss_message)
                .setPositiveButton(R.string.retry_game, (dialog, which) -> startNewGame(difficulty))
                .setNegativeButton(R.string.close, null)
                .show();
    }

    private void updateControls() {
        if (boardView == null || numberPad == null || pauseButton == null || hintButton == null) {
            return;
        }
        boolean playable = puzzle != null && !generating && !paused && !gameOver;
        int assistedValue = getBoxCompletionAssistedValue();
        boardView.setInputEnabled(playable);
        numberPad.setEnabled(playable);
        for (int i = 0; i < numberPad.getChildCount(); i++) {
            Button child = (Button) numberPad.getChildAt(i);
            int value = i + 1;
            boolean completed = isDigitCompleted(value);
            if (assistedValue > 0) {
                boolean isAnswer = value == assistedValue;
                if (completed && !isAnswer) {
                    child.setText("");
                    child.setVisibility(View.GONE);
                    child.setEnabled(false);
                    child.setAlpha(1f);
                    styleNumberButton(child);
                    continue;
                }
                child.setVisibility(View.VISIBLE);
                child.setText(isAnswer ? String.valueOf(value) : "");
                child.setEnabled(playable && isAnswer);
                child.setAlpha(isAnswer ? 1f : 0.45f);
                if (isAnswer) {
                    styleSuggestedNumberButton(child);
                } else {
                    styleNumberButton(child);
                }
            } else {
                child.setText(String.valueOf(value));
                child.setVisibility(completed ? View.GONE : View.VISIBLE);
                child.setEnabled(playable && !completed);
                child.setAlpha(1f);
                styleNumberButton(child);
            }
        }
        pauseButton.setEnabled(puzzle != null && !generating && !gameOver);
        pauseButton.setText(paused ? getString(R.string.resume) : getString(R.string.pause));
        boolean hintAvailable = playable && hintsUsed < MAX_HINTS && findHintCell() >= 0;
        hintButton.setEnabled(hintAvailable);
        hintButton.setAlpha(hintAvailable && getCoinBalance() >= HINT_COST ? 1f : 0.55f);
    }

    private int getBoxCompletionAssistedValue() {
        if (!isBoxCompletionAssistEnabled() || selectedCell < 0 || selectedCell >= CELL_COUNT
                || currentValues[selectedCell] != 0) {
            return 0;
        }
        int boxRow = selectedCell / 27;
        int boxCol = (selectedCell % 9) / 3;
        int startRow = boxRow * 3;
        int startCol = boxCol * 3;
        int emptyCount = 0;
        boolean[] seen = new boolean[10];
        for (int row = startRow; row < startRow + 3; row++) {
            for (int col = startCol; col < startCol + 3; col++) {
                int value = currentValues[row * 9 + col];
                if (value == 0) {
                    emptyCount++;
                } else if (value >= 1 && value <= 9) {
                    seen[value] = true;
                }
            }
        }
        if (emptyCount != 1) {
            return 0;
        }
        for (int value = 1; value <= 9; value++) {
            if (!seen[value]) {
                return value;
            }
        }
        return 0;
    }

    private boolean isBoxCompletionAssistEnabled() {
        return difficulty == Difficulty.BEGINNER || difficulty == Difficulty.INTERMEDIATE;
    }

    private void updateContinueState() {
        if (continueButton == null || continueHintText == null) {
            return;
        }
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean hasSaved = prefs.getBoolean(KEY_HAS_SAVED, false);
        continueButton.setEnabled(hasSaved);
        continueButton.setAlpha(hasSaved ? 1f : 0.55f);
        if (hasSaved) {
            Difficulty savedDifficulty = readDifficulty(prefs.getString(KEY_DIFFICULTY, Difficulty.BEGINNER.name()));
            long savedElapsed = prefs.getLong(KEY_ELAPSED, 0L);
            int savedMistakes = prefs.getInt(KEY_MISTAKES, 0);
            continueHintText.setText(getString(R.string.saved_game_summary,
                    difficultyName(savedDifficulty), formatTime(savedElapsed), savedMistakes, MAX_MISTAKES));
        } else {
            continueHintText.setText(R.string.no_saved_game);
        }
    }

    private void showBestRecords() {
        pauseTimer();
        saveGameIfNeeded();
        showingGame = false;

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(30), dp(24), dp(40));
        allowShadowOverflow(root);
        root.setBackgroundColor(color(R.color.sudoku_background));
        setContentView(root);

        TextView title = new TextView(this);
        title.setText(R.string.best_records);
        title.setTextSize(30);
        title.setTextColor(color(R.color.sudoku_text));
        title.setTypeface(title.getTypeface(), Typeface.BOLD);
        root.addView(title, new LinearLayout.LayoutParams(-1, -2));

        TextView subtitle = new TextView(this);
        subtitle.setText(R.string.records_subtitle);
        subtitle.setTextSize(15);
        subtitle.setTextColor(color(R.color.sudoku_muted));
        subtitle.setPadding(0, dp(6), 0, dp(18));
        root.addView(subtitle, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout table = new LinearLayout(this);
        table.setOrientation(LinearLayout.VERTICAL);
        table.setPadding(dp(16), dp(8), dp(16), dp(8));
        setRoundedBackground(table, color(R.color.sudoku_surface), color(R.color.sudoku_border), dp(18));
        root.addView(table, new LinearLayout.LayoutParams(-1, -2));

        for (Difficulty item : Difficulty.values()) {
            addRecordRow(table, item);
        }

        Button backButton = new Button(this);
        backButton.setText(R.string.back_home);
        backButton.setTextSize(18);
        styleButton(backButton, true);
        backButton.setOnClickListener(v -> showHome());
        LinearLayout.LayoutParams backParams = new LinearLayout.LayoutParams(-1, dp(52));
        backParams.topMargin = dp(20);
        backParams.bottomMargin = dp(8);
        root.addView(backButton, backParams);
    }

    private boolean updateBestRecord(Difficulty recordDifficulty, long elapsedMs) {
        if (recordDifficulty == null || elapsedMs <= 0L) {
            return false;
        }
        List<Long> records = getBestRecords(recordDifficulty);
        long oldBest = records.isEmpty() ? 0L : records.get(0);
        records.add(elapsedMs);
        Collections.sort(records);
        while (records.size() > MAX_BEST_RECORDS) {
            records.remove(records.size() - 1);
        }
        saveBestRecords(recordDifficulty, records);
        return oldBest <= 0L || elapsedMs < oldBest;
    }

    private long getBestRecord(Difficulty recordDifficulty) {
        List<Long> records = getBestRecords(recordDifficulty);
        return records.isEmpty() ? 0L : records.get(0);
    }

    private String bestRecordKey(Difficulty recordDifficulty) {
        return "best_" + recordDifficulty.name();
    }

    private String bestRecordsKey(Difficulty recordDifficulty) {
        return "best_list_" + recordDifficulty.name();
    }

    private List<Long> getBestRecords(Difficulty recordDifficulty) {
        List<Long> records = new ArrayList<>();
        if (recordDifficulty == null) {
            return records;
        }
        SharedPreferences prefs = getSharedPreferences(RECORDS_PREFS_NAME, MODE_PRIVATE);
        String encoded = prefs.getString(bestRecordsKey(recordDifficulty), null);
        if (encoded != null && !encoded.isEmpty()) {
            String[] parts = encoded.split(",");
            for (String part : parts) {
                try {
                    long value = Long.parseLong(part);
                    if (value > 0L) {
                        records.add(value);
                    }
                } catch (NumberFormatException ignored) {
                    // Ignore corrupted local record entries and keep the valid ones.
                }
            }
        } else {
            long legacyBest = prefs.getLong(bestRecordKey(recordDifficulty), 0L);
            if (legacyBest > 0L) {
                records.add(legacyBest);
            }
        }
        Collections.sort(records);
        while (records.size() > MAX_BEST_RECORDS) {
            records.remove(records.size() - 1);
        }
        return records;
    }

    private void saveBestRecords(Difficulty recordDifficulty, List<Long> records) {
        if (recordDifficulty == null) {
            return;
        }
        StringBuilder builder = new StringBuilder();
        int limit = Math.min(records == null ? 0 : records.size(), MAX_BEST_RECORDS);
        for (int i = 0; i < limit; i++) {
            long value = records.get(i);
            if (value <= 0L) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(',');
            }
            builder.append(value);
        }
        SharedPreferences.Editor editor = getSharedPreferences(RECORDS_PREFS_NAME, MODE_PRIVATE).edit();
        editor.putString(bestRecordsKey(recordDifficulty), builder.toString());
        editor.putLong(bestRecordKey(recordDifficulty), limit > 0 ? records.get(0) : 0L);
        editor.apply();
    }

    private void addRecordRow(LinearLayout table, Difficulty item) {
        long bestTime = getBestRecord(item);
        boolean hasRecord = bestTime > 0L;
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(12), 0, dp(12));
        row.setClickable(hasRecord);
        row.setEnabled(hasRecord);
        if (hasRecord) {
            row.setOnClickListener(v -> showBestRecordDetails(item));
        }
        table.addView(row, new LinearLayout.LayoutParams(-1, -2));

        TextView name = new TextView(this);
        name.setText(difficultyName(item));
        name.setTextSize(18);
        name.setTextColor(color(R.color.sudoku_text));
        name.setTypeface(name.getTypeface(), Typeface.BOLD);
        row.addView(name, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView time = new TextView(this);
        time.setText(hasRecord ? formatTime(bestTime) : getString(R.string.no_record));
        time.setTextSize(18);
        time.setGravity(Gravity.END);
        time.setTextColor(hasRecord ? color(R.color.sudoku_primary_dark) : color(R.color.sudoku_muted));
        row.addView(time, new LinearLayout.LayoutParams(0, -2, 1f));

        if (hasRecord) {
            TextView indicator = new TextView(this);
            indicator.setText(R.string.record_drilldown_indicator);
            indicator.setTextSize(20);
            indicator.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
            indicator.setTextColor(color(R.color.sudoku_muted));
            indicator.setPadding(dp(8), 0, 0, 0);
            row.addView(indicator, new LinearLayout.LayoutParams(dp(22), -2));
        }
    }

    private void showBestRecordDetails(Difficulty item) {
        pauseTimer();
        saveGameIfNeeded();
        showingGame = false;

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(color(R.color.sudoku_background));
        allowShadowOverflow(scrollView);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(30), dp(24), dp(40));
        allowShadowOverflow(root);
        root.setBackgroundColor(color(R.color.sudoku_background));
        scrollView.addView(root, new ScrollView.LayoutParams(-1, -2));
        setContentView(scrollView);

        TextView title = new TextView(this);
        title.setText(getString(R.string.record_detail_title, difficultyName(item)));
        title.setTextSize(30);
        title.setTextColor(color(R.color.sudoku_text));
        title.setTypeface(title.getTypeface(), Typeface.BOLD);
        root.addView(title, new LinearLayout.LayoutParams(-1, -2));

        TextView subtitle = new TextView(this);
        subtitle.setText(R.string.record_detail_subtitle);
        subtitle.setTextSize(15);
        subtitle.setTextColor(color(R.color.sudoku_muted));
        subtitle.setPadding(0, dp(6), 0, dp(18));
        root.addView(subtitle, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout table = new LinearLayout(this);
        table.setOrientation(LinearLayout.VERTICAL);
        table.setPadding(dp(16), dp(8), dp(16), dp(8));
        setRoundedBackground(table, color(R.color.sudoku_surface), color(R.color.sudoku_border), dp(18));
        root.addView(table, new LinearLayout.LayoutParams(-1, -2));

        List<Long> records = getBestRecords(item);
        if (records.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(R.string.record_detail_empty);
            empty.setTextSize(17);
            empty.setGravity(Gravity.CENTER);
            empty.setTextColor(color(R.color.sudoku_muted));
            empty.setPadding(0, dp(18), 0, dp(18));
            table.addView(empty, new LinearLayout.LayoutParams(-1, -2));
        } else {
            for (int i = 0; i < records.size(); i++) {
                addRecordDetailRow(table, i + 1, records.get(i));
            }
        }

        Button backButton = new Button(this);
        backButton.setText(R.string.back_records);
        backButton.setTextSize(18);
        styleButton(backButton, true);
        backButton.setOnClickListener(v -> showBestRecords());
        LinearLayout.LayoutParams backParams = new LinearLayout.LayoutParams(-1, dp(52));
        backParams.topMargin = dp(20);
        backParams.bottomMargin = dp(8);
        root.addView(backButton, backParams);
    }

    private void addRecordDetailRow(LinearLayout table, int rank, long elapsedMs) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(12), 0, dp(12));
        table.addView(row, new LinearLayout.LayoutParams(-1, -2));

        TextView rankText = new TextView(this);
        rankText.setText(getString(R.string.record_rank, rank));
        rankText.setTextSize(17);
        rankText.setTextColor(rank == 1 ? color(R.color.sudoku_primary_dark) : color(R.color.sudoku_muted));
        rankText.setTypeface(rankText.getTypeface(), Typeface.BOLD);
        row.addView(rankText, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView time = new TextView(this);
        time.setText(formatTime(elapsedMs));
        time.setTextSize(18);
        time.setGravity(Gravity.END);
        time.setTextColor(color(R.color.sudoku_text));
        time.setTypeface(time.getTypeface(), Typeface.BOLD);
        row.addView(time, new LinearLayout.LayoutParams(0, -2, 1f));
    }

    private TextView sectionLabel(String text) {
        TextView label = new TextView(this);
        label.setText(text);
        label.setTextSize(15);
        label.setTextColor(color(R.color.sudoku_muted));
        label.setTypeface(label.getTypeface(), Typeface.BOLD);
        label.setPadding(0, 0, 0, dp(10));
        return label;
    }

    private void updateDifficultyButtons() {
        if (difficultyButtons == null) {
            return;
        }
        for (Difficulty item : Difficulty.values()) {
            Button button = difficultyButtons[item.ordinal()];
            boolean selected = item == selectedHomeDifficulty;
            button.setTextColor(selected ? color(R.color.white) : color(R.color.sudoku_text));
            setRoundedBackground(button,
                    selected ? color(R.color.sudoku_primary) : color(R.color.sudoku_surface),
                    selected ? color(R.color.sudoku_primary) : color(R.color.sudoku_border),
                    dp(14));
        }
    }

    private LinearLayout.LayoutParams actionButtonParams(int index, int total) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(46), 1f);
        params.setMargins(index == 0 ? 0 : dp(3), 0, index == total - 1 ? 0 : dp(3), 0);
        return params;
    }

    private void styleButton(Button button, boolean primary) {
        button.setAllCaps(false);
        button.setTextColor(primary ? color(R.color.white) : color(R.color.sudoku_text));
        setRoundedBackground(button,
                primary ? color(R.color.sudoku_primary) : color(R.color.sudoku_surface),
                primary ? color(R.color.sudoku_primary) : color(R.color.sudoku_border),
                dp(16));
    }

    private void styleNumberButton(Button button) {
        button.setAllCaps(false);
        button.setTextColor(color(R.color.sudoku_primary_dark));
        setRoundedBackground(button, color(R.color.sudoku_surface), color(R.color.sudoku_number_border), dp(12));
    }

    private void styleSuggestedNumberButton(Button button) {
        button.setAllCaps(false);
        button.setTextColor(color(R.color.sudoku_text));
        setRoundedBackground(button, color(R.color.sudoku_accent), color(R.color.sudoku_primary), dp(12));
    }

    private void setRoundedBackground(View view, int fillColor, int strokeColor, int radiusPx) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fillColor);
        drawable.setCornerRadius(radiusPx);
        if (strokeColor != 0) {
            drawable.setStroke(dp(1), strokeColor);
        }
        view.setBackground(drawable);
    }

    private void allowShadowOverflow(ViewGroup viewGroup) {
        viewGroup.setClipToPadding(false);
        viewGroup.setClipChildren(false);
    }

    private void updateDifficultyText() {
        if (difficultyText != null) {
            difficultyText.setText(difficultyName(difficulty));
        }
    }

    private String difficultyName(Difficulty value) {
        switch (value) {
            case BEGINNER:
                return getString(R.string.difficulty_beginner);
            case INTERMEDIATE:
                return getString(R.string.difficulty_intermediate);
            case ADVANCED:
                return getString(R.string.difficulty_advanced);
            case NIGHTMARE:
                return getString(R.string.difficulty_nightmare);
            default:
                return getString(R.string.difficulty_beginner);
        }
    }

    private void updateMistakesText() {
        if (mistakesText != null) {
            mistakesText.setText(getString(R.string.mistakes_text, mistakes, MAX_MISTAKES));
        }
    }

    private void updateHintsText() {
        if (hintsText != null) {
            hintsText.setText(getString(R.string.hints_text, hintsUsed, MAX_HINTS));
        }
    }

    private void updateCoinTexts() {
        int balance = getCoinBalance();
        if (homeCoinText != null) {
            homeCoinText.setText(getString(R.string.coin_balance, balance));
        }
        if (coinText != null) {
            coinText.setText(getString(R.string.coin_balance, balance));
        }
    }

    private void initializeWalletIfNeeded() {
        SharedPreferences prefs = getSharedPreferences(WALLET_PREFS_NAME, MODE_PRIVATE);
        if (!prefs.getBoolean(KEY_WALLET_INITIALIZED, false)) {
            prefs.edit()
                    .putBoolean(KEY_WALLET_INITIALIZED, true)
                    .putInt(KEY_COIN_BALANCE, INITIAL_COIN_BALANCE)
                    .apply();
        }
    }

    private int getCoinBalance() {
        initializeWalletIfNeeded();
        return Math.max(0, getSharedPreferences(WALLET_PREFS_NAME, MODE_PRIVATE)
                .getInt(KEY_COIN_BALANCE, INITIAL_COIN_BALANCE));
    }

    private void addCoins(int amount) {
        if (amount <= 0) {
            return;
        }
        setCoinBalance(getCoinBalance() + amount);
    }

    private void spendCoins(int amount) {
        if (amount <= 0) {
            return;
        }
        setCoinBalance(Math.max(0, getCoinBalance() - amount));
    }

    private void setCoinBalance(int balance) {
        getSharedPreferences(WALLET_PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_WALLET_INITIALIZED, true)
                .putInt(KEY_COIN_BALANCE, Math.max(0, balance))
                .apply();
    }

    private int rewardForDifficulty(Difficulty value) {
        switch (value) {
            case BEGINNER:
                return 20;
            case INTERMEDIATE:
                return 35;
            case ADVANCED:
                return 55;
            case NIGHTMARE:
                return 80;
            default:
                return 20;
        }
    }

    private void startTimer() {
        if (timerRunning || !shouldTimerRun()) {
            return;
        }
        timerRunning = true;
        timerStartedAtMs = SystemClock.elapsedRealtime();
        handler.removeCallbacks(timerTick);
        handler.post(timerTick);
    }

    private void pauseTimer() {
        if (!timerRunning) {
            return;
        }
        elapsedBeforeStartMs = getElapsedMs();
        timerRunning = false;
        handler.removeCallbacks(timerTick);
        updateTimerText();
    }

    private void stopTimer() {
        timerRunning = false;
        handler.removeCallbacks(timerTick);
    }

    private long getElapsedMs() {
        if (timerRunning) {
            return elapsedBeforeStartMs + SystemClock.elapsedRealtime() - timerStartedAtMs;
        }
        return elapsedBeforeStartMs;
    }

    private void updateTimerText() {
        if (timerText != null) {
            timerText.setText(formatTime(getElapsedMs()));
        }
    }

    private boolean shouldTimerRun() {
        return showingGame && screenVisible && puzzle != null && !generating && !paused && !gameOver;
    }

    private boolean isDigitCompleted(int value) {
        if (value < 1 || value > 9) {
            return false;
        }
        int count = 0;
        for (int currentValue : currentValues) {
            if (currentValue == value) {
                count++;
            }
        }
        return count >= 9;
    }

    private void saveGameIfNeeded() {
        if (puzzle == null || generating || gameOver) {
            return;
        }
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putBoolean(KEY_HAS_SAVED, true);
        editor.putString(KEY_DIFFICULTY, difficulty.name());
        editor.putString(KEY_GIVENS, encodeArray(puzzle.getGivens()));
        editor.putString(KEY_SOLUTION, encodeArray(puzzle.getSolution()));
        editor.putString(KEY_CURRENT, encodeArray(currentValues));
        editor.putInt(KEY_MISTAKES, mistakes);
        editor.putInt(KEY_HINTS_USED, hintsUsed);
        editor.putLong(KEY_ELAPSED, getElapsedMs());
        editor.putBoolean(KEY_PAUSED, paused);
        editor.putInt(KEY_SELECTED_CELL, selectedCell);
        editor.apply();
    }

    private boolean loadSavedGameIntoState() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        if (!prefs.getBoolean(KEY_HAS_SAVED, false)) {
            return false;
        }
        int[] givens = decodeArray(prefs.getString(KEY_GIVENS, null));
        int[] solution = decodeArray(prefs.getString(KEY_SOLUTION, null));
        int[] current = decodeArray(prefs.getString(KEY_CURRENT, null));
        if (givens == null || solution == null || current == null) {
            clearSavedGame();
            return false;
        }
        try {
            difficulty = readDifficulty(prefs.getString(KEY_DIFFICULTY, Difficulty.BEGINNER.name()));
            puzzle = new SudokuPuzzle(givens, solution, difficulty);
            currentValues = current;
            mistakes = Math.max(0, Math.min(MAX_MISTAKES, prefs.getInt(KEY_MISTAKES, 0)));
            hintsUsed = Math.max(0, Math.min(MAX_HINTS, prefs.getInt(KEY_HINTS_USED, 0)));
            elapsedBeforeStartMs = Math.max(0L, prefs.getLong(KEY_ELAPSED, 0L));
            paused = prefs.getBoolean(KEY_PAUSED, false);
            selectedCell = prefs.getInt(KEY_SELECTED_CELL, -1);
            if (selectedCell < -1 || selectedCell >= CELL_COUNT) {
                selectedCell = -1;
            }
            generating = false;
            gameOver = false;
            stopTimer();
            return true;
        } catch (IllegalArgumentException e) {
            clearSavedGame();
            return false;
        }
    }

    private void clearSavedGame() {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().clear().apply();
    }

    private Difficulty readDifficulty(String value) {
        try {
            return Difficulty.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException e) {
            return Difficulty.BEGINNER;
        }
    }

    private String encodeArray(int[] values) {
        StringBuilder builder = new StringBuilder(CELL_COUNT);
        for (int i = 0; i < CELL_COUNT; i++) {
            int value = values != null && i < values.length ? values[i] : 0;
            builder.append((char) ('0' + Math.max(0, Math.min(9, value))));
        }
        return builder.toString();
    }

    private int[] decodeArray(String encoded) {
        if (encoded == null || encoded.length() != CELL_COUNT) {
            return null;
        }
        int[] values = new int[CELL_COUNT];
        for (int i = 0; i < CELL_COUNT; i++) {
            char c = encoded.charAt(i);
            if (c < '0' || c > '9') {
                return null;
            }
            values[i] = c - '0';
        }
        return values;
    }

    private String formatTime(long elapsedMs) {
        long totalSeconds = elapsedMs / 1000L;
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return String.format(Locale.US, "%02d:%02d", minutes, seconds);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private int color(int resId) {
        return getColor(resId);
    }
}
