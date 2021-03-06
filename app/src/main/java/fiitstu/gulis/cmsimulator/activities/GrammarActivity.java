package fiitstu.gulis.cmsimulator.activities;

import android.Manifest;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.sql.Time;
import java.util.*;

import fiitstu.gulis.cmsimulator.R;
import fiitstu.gulis.cmsimulator.adapters.grammar.RulesAdapter;
import fiitstu.gulis.cmsimulator.adapters.tasks.GrammarTaskAdapter;
import fiitstu.gulis.cmsimulator.database.DataSource;
import fiitstu.gulis.cmsimulator.database.FileHandler;
import fiitstu.gulis.cmsimulator.dialogs.*;
import fiitstu.gulis.cmsimulator.elements.GrammarRule;
import fiitstu.gulis.cmsimulator.elements.GrammarType;
import fiitstu.gulis.cmsimulator.elements.Task;
import fiitstu.gulis.cmsimulator.elements.Timer;
import fiitstu.gulis.cmsimulator.exceptions.NotImplementedException;
import fiitstu.gulis.cmsimulator.models.tasks.grammar_tasks.GrammarTask;
import fiitstu.gulis.cmsimulator.network.ServerController;
import fiitstu.gulis.cmsimulator.network.UrlManager;
import fiitstu.gulis.cmsimulator.network.grammar_tasks.UpdateTimerAsync;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

/**
 * Grammar creation activity.
 * <p>
 * Created by Krisztian Tóth on 29.1.2019.
 */

public class GrammarActivity extends FragmentActivity implements SaveGrammarDialog.SaveDialogListener {

    //log tag
    private static final String TAG = GrammarActivity.class.getName();

    private static final int BACKGROUND_CHANGE_LENGTH = 1000;

    //strings
    private static final String GRAMMAR = "GRAMMAR";
    private static final String SAVE_DIALOG = "SAVE_DIALOG";
    private static final String HELP_DIALOG = "HELP_DIALOG";
    private static final String REGULAR = "Regular";
    private static final String CONTEXT_FREE = "Context-Free";
    private static final String CONTEXT_SENSITIVE = "Context-Sensitive";
    private static final String UNRESTRICTED = "Unrestricted";
    private static final String GRAMMAR_LIST = "GrammarList";
    private static final String GRAMMAR_TYPE = "GrammarType";
    private static final String PIPE = "|";
    private static final String EPSILON = "ε";

    // INTENT EXTRA KEYS
    public static final String EXAMPLE_GRAMMAR_KEY = "EXAMPLE_GRAMMAR";
    public static final String EXAMPLE_NUMBER_KEY = "EXAMPLE_GRAMMAR_NUMBER";
    public static final String PREVIEW_TASK_KEY = "PREVIEW_TASK_KEY";
    public static final String CONFIGURATION_GRAMMAR_KEY = "CONFIGURATION_GRAMMAR_KEY";
    public static final String TASK_SOLVE_GRAMMAR_KEY = "TASK_SOLVE_GRAMMAR_KEY";
    public static final String HAS_TESTS_ENABLED_KEY = "HAS_TEST_ENABLED_KEY";
    public static final String HAS_TIMER_ENABLED = "HAS_TIMER_ENABLED";
    public static final String TIMER_KEY = "TIMER_KEY";
    public static final String TASK_ID_KEY = "TASK_ID_KEY";
    public static final String AVAILABLE_TIME_KEY = "AVAILABLE_TIME_KEY";
    public static final String TASK_NAME_KEY = "TASK_NAME_KEY";
    public static final String TASK_TEXT_KEY = "TASK_TEXT_KEY";
    public static final String REMAINING_TIME_KEY = "REMAINING_TIME_KEY";

    //storage permissions
    public static final int REQUEST_READ_STORAGE = 0;
    public static final int REQUEST_WRITE_STORAGE = 1;

    //variables
    private RulesAdapter rulesAdapter;
    private DataSource dataSource;
    private RecyclerView recyclerView;
    private String filename;
    private int rulesTableSize = 15;
    private Time availableTime;

    private boolean hasTestsEnabled = true;
    private boolean setGrammarTask = false;
    private boolean taskSolving = false;
    private boolean timerRunOut = false;
    private boolean hasTimer = false;
    private boolean previewTask = false;

    private String task_name;
    private String task_text;
    private String remaining_time;

    private int task_id = -1;

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem tests = menu.findItem(R.id.menu_grammar_test);
        if (setGrammarTask)
            tests.setVisible(true);
        if (taskSolving) {
            MenuItem save = menu.findItem(R.id.menu_save_task);
            MenuItem publish = menu.findItem(R.id.menu_submit_task);
            MenuItem showTask = menu.findItem(R.id.menu_show_task);

            save.setVisible(true);
            publish.setVisible(true);
            showTask.setVisible(true);
            if (hasTestsEnabled)
                tests.setVisible(true);
            else tests.setVisible(false);
        }

        return true;
    }

    /**
     * Defines the usage of all the buttons in the activity
     *
     * @param savedInstaceState Bundle of arguments passed to this activity
     */
    @Override
    public void onCreate(Bundle savedInstaceState) {
        super.onCreate(savedInstaceState);
        setContentView(R.layout.activity_grammar);

        Log.v(TAG, "onCreate initialization started");

        //menu
        final ActionBar actionBar = this.getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(R.string.grammar_definition);

        Intent thisIntent = this.getIntent();
        setGrammarTask = thisIntent.getBooleanExtra(CONFIGURATION_GRAMMAR_KEY, false);
        hasTestsEnabled = thisIntent.getBooleanExtra(HAS_TESTS_ENABLED_KEY, true);
        previewTask = thisIntent.getBooleanExtra(PREVIEW_TASK_KEY, false);

        task_name = thisIntent.getStringExtra(TASK_NAME_KEY);
        task_text = thisIntent.getStringExtra(TASK_TEXT_KEY);
        remaining_time = thisIntent.getStringExtra(REMAINING_TIME_KEY);

        //recycler view creation
        recyclerView = findViewById(R.id.recyclerViewRules);
        rulesAdapter = new RulesAdapter(rulesTableSize);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(rulesAdapter);
        ViewCompat.setNestedScrollingEnabled(recyclerView, false);
        recyclerView.setItemViewCacheSize(rulesTableSize);

        int grammarExampleToLoad;

        //dataSource initialization
        dataSource = DataSource.getInstance();
        dataSource.open();

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();

        // EXAMPLE GRAMMAR
        if (intent.getBooleanExtra(EXAMPLE_GRAMMAR_KEY, false)) {
            grammarExampleToLoad = intent.getIntExtra(EXAMPLE_NUMBER_KEY, 0);
            prepareExmaple(grammarExampleToLoad);
        }

        // IF TASK CONFIGURATION
        if (setGrammarTask) {
            loadTask();
        }

        if (previewTask) {
            FileHandler fileHandler = new FileHandler(FileHandler.Format.CMSG);

            String filePath = this.getApplicationInfo().dataDir + "/grammarTask.cmsg";
            try {
                fileHandler.loadFile(filePath);
                dataSource.open();
                dataSource.globalDrop();
                fileHandler.getData(dataSource);
                List<GrammarRule> grammarRuleList = dataSource.getGrammarRuleFullExtract();
                filename = filePath.substring(filePath.lastIndexOf("/") + 1, filePath.lastIndexOf("."));
                rulesAdapter = new RulesAdapter(rulesTableSize);
                rulesAdapter.setGrammarRuleList(grammarRuleList);
                recyclerView.setAdapter(rulesAdapter);
                recyclerView.setItemViewCacheSize(rulesTableSize);
            } catch (Exception e) {
                Toast.makeText(this, R.string.file_not_loaded, Toast.LENGTH_SHORT).show();
            }
        }

        //test
        Button grammarTestButton = findViewById(R.id.button_grammar_test);
        grammarTestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<GrammarRule> grammarRuleList = simplifyRules();
                if (hasStartingNonTerminal(grammarRuleList)) {
                    Intent nextActivityIntent = new Intent(GrammarActivity.this, SimulationGrammarActivity.class);
                    nextActivityIntent.putExtra(GRAMMAR_LIST, (Serializable) grammarRuleList);
                    nextActivityIntent.putExtra(GRAMMAR_TYPE, (Serializable) checkGrammarType(grammarRuleList));
                    startActivity(nextActivityIntent);
                } else {
                    Toast.makeText(GrammarActivity.this, R.string.grammar_missing_start, Toast.LENGTH_SHORT).show();
                }
            }
        });

        //grammar type
        Button grammarTypeButton = findViewById(R.id.button_grammar_grammarType);
        grammarTypeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView grammarTypeGrammarTypeTextView = findViewById(R.id.textView_grammar_type_grammar_type);
                TextView grammarTypeDefinitionTypeTextView = findViewById(R.id.textView_grammar_type_definition);
                TextView grammarTypeNonTerminalsTypeTextView = findViewById(R.id.textView_grammar_type_non_terminals);
                TextView grammarTypeTerminalsTypeTextView = findViewById(R.id.textView_grammar_type_terminals);

                List<GrammarRule> simplifiedList = simplifyRules();
                if (hasStartingNonTerminal(simplifiedList)) {
                    GrammarType grammarType = new GrammarType(checkGrammarType(simplifiedList), getNonTerminals(simplifiedList), getTerminals(simplifiedList));

                    String definitionString = "G = (" + grammarType.getDefinition() + ")";
                    String nonTerminalsString = "N = {" + grammarType.getNon_terminals() + "}";
                    String terminalsString = "T = {" + grammarType.getTerminals() + "}";

                    grammarTypeGrammarTypeTextView.setText(grammarType.getGrammar_type());
                    grammarTypeDefinitionTypeTextView.setText(definitionString);
                    grammarTypeNonTerminalsTypeTextView.setText(nonTerminalsString);
                    grammarTypeTerminalsTypeTextView.setText(terminalsString);

                } else {
                    Toast.makeText(GrammarActivity.this, R.string.grammar_missing_start, Toast.LENGTH_SHORT).show();
                }

                ConstraintLayout grammarMainLayout = findViewById(R.id.linearLayout_grammar);
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(grammarMainLayout.getWindowToken(), 0);
                }
            }
        });

        // IF TASK SOLVING
        if (intent.getBooleanExtra(TASK_SOLVE_GRAMMAR_KEY, false)) {
            taskSolving = true;
            FrameLayout grammarDefinitions = findViewById(R.id.framelayout_grammar_definition);
            grammarDefinitions.setVisibility(View.GONE);
            task_id = intent.getIntExtra(TASK_ID_KEY, -1);
            hasTimer = intent.getBooleanExtra(HAS_TIMER_ENABLED, false);
            if (hasTimer) {
                availableTime = (Time) intent.getSerializableExtra(AVAILABLE_TIME_KEY);
                Time time = (Time) intent.getSerializableExtra(TIMER_KEY);
                Timer timer;

                timer = Timer.getInstance(time);
                timer.setOnTickListener(new Timer.OnTickListener() {
                    @Override
                    public void onTick(long millisUntilFinished) {
                        int hours = (int) (millisUntilFinished / 3600000);
                        int minutes = (int) ((millisUntilFinished - (hours * 3600000)) / 60000);
                        int seconds = (int) ((millisUntilFinished - (hours * 3600000) - (minutes * 60000)) / 1000);

                        if (hours == 0 && minutes <= 4 && !timerRunOut) {
                            timerRunOut = true;

                            final int s_dark = getColor(R.color.primary_color_dark);
                            final int s_normal = getColor(R.color.primary_color);
                            final int s_light = getColor(R.color.primary_color_light);

                            final int t_dark = getColor(R.color.in_progress_dark);
                            final int t_normal = getColor(R.color.in_progress_top_bar);
                            final int t_light = getColor(R.color.in_progress_bottom_bar);

                            changeActivityBackgroundColor(s_dark, s_normal, s_light, t_dark, t_normal, t_light);
                        }


                        String timerText = String.format("%02d:%02d:%02d", hours, minutes, seconds);

                        actionBar.setTitle(timerText);
                    }
                });
                timer.setOnTimeRunOutListener(new Timer.OnTimeRunOutListener() {
                    @Override
                    public void onTimeRunOut() {
                        // COMPLETED: HANDLE TIMER RUN OUT
                        handleTimer();
                        //BrowseAutomataTasksActivity.adapter.setTaskStatus(task.getTask_id(), Task.TASK_STATUS.TOO_LATE);
                        AlertDialog timeRunOutAlert = new AlertDialog.Builder(GrammarActivity.this)
                                .setTitle(R.string.time_ran_out_title)
                                .setMessage(R.string.time_ran_out_message)
                                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        GrammarActivity.this.finish();
                                    }
                                })
                                .setCancelable(false)
                                .create();


                        timeRunOutAlert.show();
                    }
                });
                if (!Timer.isSet())
                    timer.startTimer();
            }
            loadTask();
            grammarTestButton.setVisibility(View.GONE);
            grammarTypeButton.setVisibility(View.GONE);
        }

        if (intent.getBooleanExtra(CONFIGURATION_GRAMMAR_KEY, false)) {
            grammarTestButton.setVisibility(View.GONE);
        }

        final Button pipeButton = findViewById(R.id.button_grammar_pipe);
        pipeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleTimer();
                rulesAdapter.insertSpecialChar(PIPE);
            }
        });

        final Button epsilonButton = findViewById(R.id.button_grammar_epsilon);
        epsilonButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleTimer();
                rulesAdapter.insertSpecialChar(EPSILON);
            }
        });

        final Button addRowButton = findViewById(R.id.button_grammar_add_row);
        addRowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleTimer();
                rulesAdapter.addRow();
            }
        });

        Log.i(TAG, "onCreate initialized");
    }

    private void handleTimer() {
        if (hasTimer) {
            Timer timer = Timer.getInstance(null);
            Time time = timer.getCurrentTime();

            long elapsed = (availableTime.getTime() - time.getTime());

            int elapsedHours = (int) (elapsed / 3600000);
            int elapsedMinutes = (int) ((elapsed - (elapsedHours * 3600000)) / 60000);
            int elapsedSeconds = (int) ((elapsed - (elapsedHours * 3600000) - (elapsedMinutes * 60000)) / 1000);


            final String sTime = String.format("%02d:%02d:%02d", elapsedHours, elapsedMinutes, elapsedSeconds);
            final Time elapsedTime = Time.valueOf(sTime);

            new UpdateTimerAsync().execute(String.valueOf(task_id), String.valueOf(TaskLoginActivity.loggedUser.getUser_id()), elapsedTime.toString());
            if (GrammarTaskAdapter.runningTask != null) {
                GrammarTaskAdapter.runningTask.setRemaining_time(time);
            }
        }
    }

    private void loadTask() {
        List<GrammarRule> grammarRuleList = dataSource.getGrammarRuleFullExtract();
        rulesAdapter = new RulesAdapter(rulesTableSize);
        rulesAdapter.setGrammarRuleList(grammarRuleList);
        recyclerView.setAdapter(rulesAdapter);
        recyclerView.setItemViewCacheSize(rulesTableSize);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = this.getMenuInflater();
        menuInflater.inflate(R.menu.menu_grammar, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        handleTimer();
        DataSource dataSource = DataSource.getInstance();
        dataSource.open();
        saveRules();
        List<GrammarRule> grammarRuleList = dataSource.getGrammarRuleFullExtract();
        List<String> testList = dataSource.getGrammarTests();
        FileHandler fileHandler = new FileHandler(FileHandler.Format.CMSG);
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.menu_show_task:
                // TODO: IMPLEMENT TASK DIALOG
                TaskAssignmentDialog taskAssignmentDialog = TaskAssignmentDialog.newInstance(task_name ,task_text);
                FragmentManager fm1 = this.getSupportFragmentManager();
                taskAssignmentDialog.show(fm1, TAG);
                return true;
            case R.id.menu_save_task:
                try {
                    fileHandler.setData(grammarRuleList, testList);
                    fileHandler.writeFile(Integer.toString(task_id));
                } catch (ParserConfigurationException | TransformerException | IOException e) {
                    e.printStackTrace();
                }

                String filePath = FileHandler.PATH + "/" + Integer.toString(task_id) + FileHandler.Format.CMSG.getExtension();
                final File file = new File(filePath);

                class SaveTaskAsync extends AsyncTask<Void, Void, String> {
                    @Override
                    protected String doInBackground(Void... voids) {
                        UrlManager urlManager = new UrlManager();
                        ServerController serverController = new ServerController();
                        URL url = urlManager.getSaveGrammarURL(TaskLoginActivity.loggedUser.getUser_id(), Integer.toString(task_id) + FileHandler.Format.CMSG.getExtension());
                        String output = null;

                        output = serverController.doPostRequest(url, file);
                        return output;
                    }

                    @Override
                    protected void onPostExecute(String s) {
                        if (s == null || s.isEmpty()) {
                            Toast.makeText(GrammarActivity.this, R.string.generic_error, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(GrammarActivity.this, R.string.save_complete, Toast.LENGTH_SHORT).show();
                        }
                    }
                }

                new SaveTaskAsync().execute();

                return true;
            case R.id.menu_submit_task:
                String grammarType = checkGrammarType(grammarRuleList);

                SubmitGrammarTaskDialog submitGrammarTaskDialog = new SubmitGrammarTaskDialog(grammarType, task_id);
                FragmentManager fragmentManager = this.getSupportFragmentManager();
                submitGrammarTaskDialog.show(fragmentManager, TAG);
                return true;
            case R.id.menu_grammar_test:
                dataSource.open();

                Intent grammarTest = new Intent(this, GrammarTestingActivity.class);
                grammarTest.putExtra(GrammarTestingActivity.CONFIGURATION_MODE, setGrammarTask);
                grammarTest.putExtra(GrammarTestingActivity.SOLVE_MODE, taskSolving);
                grammarTest.putExtra(GrammarTestingActivity.GRAMMAR_TYPE, checkGrammarType(grammarRuleList));
                startActivity(grammarTest);
                return true;
            case R.id.menu_grammar_help:
                FragmentManager fm = getSupportFragmentManager();
                GuideFragment guideFragment = GuideFragment.newInstance(GuideFragment.GRAMMAR);
                guideFragment.show(fm, HELP_DIALOG);
                return true;
            case R.id.menu_save:
                if (Build.VERSION.SDK_INT > 15
                        && ContextCompat.checkSelfPermission(GrammarActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(GrammarActivity.this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            REQUEST_WRITE_STORAGE);
                } else {
                    if (filename == null)
                        filename = GRAMMAR;
                    showSaveGrammarDialog(false);
                }
                return true;
            case R.id.menu_load:
                if (Build.VERSION.SDK_INT > 15
                        && ContextCompat.checkSelfPermission(GrammarActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(GrammarActivity.this,
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            REQUEST_READ_STORAGE);
                } else {
                    loadGrammar();
                }
                return true;
            case R.id.menu_new_grammar:
                GrammarActivity.this.finish();
                Intent nextActivityIntent = new Intent(GrammarActivity.this, GrammarActivity.class);
                startActivity(nextActivityIntent);
                return true;
        }

        return false;
    }

    /**
     * Handles the confirmation window when exiting the activity
     */
    @Override
    public void onBackPressed() {
        Log.v(TAG, "onBackPressed method started");
        handleTimer();
        if (!setGrammarTask && !taskSolving) {
            TaskDialog.setStatusText(null);
            new AlertDialog.Builder(this)
                    .setTitle(R.string.warning)
                    .setMessage(R.string.grammar_confirmation)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            if (Build.VERSION.SDK_INT > 15
                                    && ContextCompat.checkSelfPermission(GrammarActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                                ActivityCompat.requestPermissions(GrammarActivity.this,
                                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                        REQUEST_WRITE_STORAGE);
                            } else {
                                if (filename == null)
                                    filename = GRAMMAR;
                                showSaveGrammarDialog(true);
                            }
                        }
                    })
                    .setNeutralButton(R.string.cancel, null)
                    .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dataSource.open();
                            dataSource.globalDrop();
                            dataSource.close();
                            GrammarActivity.this.finish();
                            GrammarActivity.super.onBackPressed();
                        }
                    })
                    .show();


        } else if (taskSolving) {
            AlertDialog alertDialog = new AlertDialog.Builder(this)
                    .setTitle(android.R.string.cancel)
                    .setMessage(R.string.cancel_task)
                    .setNeutralButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (hasTimer) {
                                Timer timer = Timer.getInstance(null);
                                timer.pauseTimer();
                                Timer.deleteTimer();
                            }
                            GrammarActivity.this.finish();
                        }
                    }).create();

            alertDialog.show();
        } else {
            saveRules();
            super.onBackPressed();
        }
    }

    private void saveRules() {
        List<GrammarRule> grammarRuleList = rulesAdapter.getGrammarRuleList();
        DataSource dataSource = DataSource.getInstance();
        dataSource.open();
        List<String> tests = dataSource.getGrammarTests();
        dataSource.globalDrop();
        for (GrammarRule rule : grammarRuleList) {
            dataSource.addGrammarRule(rule.getGrammarLeft(), rule.getGrammarRight());
        }
        for (String test : tests) {
            dataSource.addGrammarTest(test);
        }
    }

    /**
     * Method for checking the type of the defined grammar. The input list of grammar rules is iterated and on the base of
     * various conditions the type of the grammar is determined within the Chomsky hierarchy. ArrayList is used for easy
     * iteration and the GrammarRule object is used for easy access to the left and right side of the rule.
     *
     * @param simplifiedList a list of all the defined rules - each rule in a separate row
     * @return the type of the grammar as string
     */
    private String checkGrammarType(List<GrammarRule> simplifiedList) {
        int grammarType = 0;

        for (GrammarRule grammarRule : simplifiedList) {
            if (grammarRule.getGrammarLeft() != null && grammarRule.getGrammarRight() != null) {
                if (grammarRule.getGrammarLeft().length() == 1 && isUpperCase(grammarRule.getGrammarLeft())) {
                    if (grammarRule.getGrammarRight().length() == 1 && isLowerCase(grammarRule.getGrammarRight())) {
                        grammarType = grammarType < 1 ? 1 : grammarType;
                    } else if (grammarRule.getGrammarRight().length() == 2 && isOneUpperOneLower(grammarRule.getGrammarRight())) {
                        grammarType = grammarType < 1 ? 1 : grammarType;
                    } else {
                        grammarType = grammarType < 2 ? 2 : grammarType;
                    }
                } else {
                    if (grammarRule.getGrammarLeft().length() <= grammarRule.getGrammarRight().length()) {
                        grammarType = grammarType < 3 ? 3 : grammarType;
                    } else {
                        grammarType = grammarType < 4 ? 4 : grammarType;
                    }
                }
            }
        }

        return grammarType == 1 ? REGULAR : (grammarType == 2) ? CONTEXT_FREE : (grammarType == 3) ? CONTEXT_SENSITIVE : UNRESTRICTED;
    }

    /**
     * Method used for extracting all the unique terminal symbols from the rules. ArrayList is used for easy
     * iteration and the GrammarRule object is used for easy access to the left and right side of the rule.
     *
     * @param simplifiedList a list of all the defined rules - each rule in a separate row
     * @return all the terminal symbols as string
     */
    private String getTerminals(List<GrammarRule> simplifiedList) {
        Set<Character> terminals = new HashSet<>();

        for (GrammarRule grammarRule : simplifiedList) {
            if (grammarRule.getGrammarLeft() != null && grammarRule.getGrammarRight() != null) {
                for (int i = 0; i < grammarRule.getGrammarLeft().length(); i++) {
                    char currentCharLeft = grammarRule.getGrammarLeft().charAt(i);
                    if (Character.isLowerCase(currentCharLeft) || Character.isDigit(currentCharLeft)) {
                        terminals.add(currentCharLeft);
                    }
                }
                for (int i = 0; i < grammarRule.getGrammarRight().length(); i++) {
                    char currentCharRight = grammarRule.getGrammarRight().charAt(i);
                    if (Character.isLowerCase(currentCharRight) || Character.isDigit(currentCharRight)) {
                        terminals.add(currentCharRight);
                    }
                }

            }
        }

        return terminals.toString().substring(1, terminals.toString().length() - 1);
    }

    /**
     * Method used for extracting all the unique non-terminal symbols from the rules. ArrayList is used for easy
     * iteration and the GrammarRule object is used for easy access to the left and right side of the rule.
     *
     * @param simplifiedList a list of all the defined rules - each rule in a separate row
     * @return all the non-terminal symbols as string
     */
    private String getNonTerminals(List<GrammarRule> simplifiedList) {
        Set<Character> non_terminals = new HashSet<>();

        for (GrammarRule grammarRule : simplifiedList) {
            if (grammarRule.getGrammarLeft() != null && grammarRule.getGrammarRight() != null) {
                for (int i = 0; i < grammarRule.getGrammarLeft().length(); i++) {
                    char currentCharLeft = grammarRule.getGrammarLeft().charAt(i);
                    if (Character.isUpperCase(currentCharLeft)) {
                        non_terminals.add(currentCharLeft);
                    }
                }
                for (int i = 0; i < grammarRule.getGrammarRight().length(); i++) {
                    char currentCharRight = grammarRule.getGrammarRight().charAt(i);
                    if (Character.isUpperCase(currentCharRight)) {
                        non_terminals.add(currentCharRight);
                    }
                }
            }
        }
        return non_terminals.toString().substring(1, non_terminals.toString().length() - 1);
    }

    /**
     * Method for simplifying the list of grammar rules. All the other methods require each rule in a separate row.
     * This method separates the rules to different rows if the contain PIPE and removes all the unnecessary whitespaces.
     *
     * @return the list of grammar rules with each rule in a different row
     */
    private List<GrammarRule> simplifyRules() {
        List<GrammarRule> simplifiedRuleList = new ArrayList<>();

        for (GrammarRule grammarRule : rulesAdapter.getGrammarRuleList()) {
            if (grammarRule.getGrammarLeft() != null && grammarRule.getGrammarRight() != null) {
                String rule = grammarRule.getGrammarRight();
                rule = rule.replaceAll("\\s+", "");
                if (rule.contains(PIPE)) {
                    String[] splitRule = rule.split("\\|");
                    for (String newRule : splitRule) {
                        GrammarRule newGrammarRule = new GrammarRule(grammarRule.getGrammarLeft(), newRule);
                        simplifiedRuleList.add(newGrammarRule);
                    }
                } else {
                    GrammarRule newGrammarRule = new GrammarRule(grammarRule.getGrammarLeft(), grammarRule.getGrammarRight());
                    simplifiedRuleList.add(newGrammarRule);
                }
            }
        }

        return simplifiedRuleList;
    }

    private boolean isUpperCase(String rule) {
        return rule.equals(rule.toUpperCase());
    }

    private boolean isLowerCase(String rule) {
        return rule.equals(rule.toLowerCase());
    }

    private boolean isOneUpperOneLower(String rule) {
        return (isUpperCase(rule.substring(0, 1)) && isLowerCase(rule.substring(1, 2))) || (isLowerCase(rule.substring(0, 1)) && isUpperCase(rule.substring(1, 2)));
    }

    /**
     * Method for handling the file selection the loading the grammar rules from an external file.
     * The rules are loaded to an ArrayList of grammar rules, because this data structure is used in every other method.
     */
    private void loadGrammar() {
        FileSelector fileSelector = new FileSelector();
        fileSelector.setFileSelectedListener(new FileSelector.FileSelectedListener() {
            @Override
            public void onFileSelected(String filePath, FileHandler.Format format) {
                try {
                    FileHandler fileHandler = new FileHandler(format);
                    fileHandler.loadFile(filePath);
                    dataSource.open();
                    dataSource.globalDrop();
                    fileHandler.getData(dataSource);
                    List<GrammarRule> grammarRuleList = dataSource.getGrammarRuleFullExtract();
                    filename = filePath.substring(filePath.lastIndexOf("/") + 1, filePath.lastIndexOf("."));
                    rulesAdapter = new RulesAdapter(rulesTableSize);
                    rulesAdapter.setGrammarRuleList(grammarRuleList);
                    recyclerView.setAdapter(rulesAdapter);
                    recyclerView.setItemViewCacheSize(rulesTableSize);

                } catch (Exception e) {
                    Log.e(TAG, "File was not loaded", e);
                    Toast.makeText(GrammarActivity.this, getResources().getString(R.string.file_not_loaded), Toast.LENGTH_SHORT).show();
                    dataSource.globalDrop();
                    dataSource.close();
                    finish();
                }

            }
        });
        fileSelector.setExceptionListener(new FileSelector.ExceptionListener() {
            @Override
            public void onException(Exception e) {
                Toast.makeText(GrammarActivity.this, getResources().getString(R.string.file_not_loaded), Toast.LENGTH_SHORT).show();
            }
        });
        fileSelector.selectFile(this);
    }

    /**
     * Method for opening the dialog box for saving the grammar
     *
     * @param exit a boolean indicating if the user wants to close the activity after saving
     */
    private void showSaveGrammarDialog(boolean exit) {
        SaveGrammarDialog saveGrammarDialog = SaveGrammarDialog.newInstance(filename, null, exit);
        saveGrammarDialog.show(getFragmentManager(), SAVE_DIALOG);
    }

    /**
     * Method for handling the save of the defined grammar to an external file.
     *
     * @param filename name of the created file
     * @param format   format of the created file
     * @param exit     a boolean indicating if the user wants to close the activity after saving
     */
    @Override
    public void saveDialogClick(String filename, FileHandler.Format format, boolean exit) {
        this.filename = filename;
        try {
            FileHandler fileHandler = new FileHandler(format);
            List<GrammarRule> grammarRuleList = simplifyRules();

            dataSource.open();
            List<String> tests = dataSource.getGrammarTests();
            if (tests.size() > 0)
                fileHandler.setData(grammarRuleList, tests);
            else fileHandler.setData(grammarRuleList);
            fileHandler.writeFile(filename);

            Toast.makeText(this, FileHandler.PATH + "/" + filename + format.getExtension() + " " + getResources().getString(R.string.save_succ), Toast.LENGTH_SHORT).show();
            SaveGrammarDialog saveGrammarDialog = (SaveGrammarDialog) getFragmentManager().findFragmentByTag(SAVE_DIALOG);
            if (saveGrammarDialog != null) {
                saveGrammarDialog.dismiss();
            }

            if (exit) {
                dataSource.globalDrop();
                GrammarActivity.this.finish();
                GrammarActivity.super.onBackPressed();
            }
        } catch (Exception e) {
            Log.e(TAG, "File was not saved", e);
            Toast.makeText(this, getResources().getString(R.string.file_not_saved), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Method for checking if the rules have a starting non-terminal symbol
     *
     * @param grammarRuleList list of rules
     * @return boolean indicating if the grammar contains a starting non-terminal symbol
     */
    public boolean hasStartingNonTerminal(List<GrammarRule> grammarRuleList) {
        for (GrammarRule grammarRule : grammarRuleList) {
            if (grammarRule.getGrammarLeft().equals("S")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Method for handling and loading any example grammar.
     * The rules are loaded to an ArrayList of grammar rules, because this data structure is used in every other method.
     *
     * @param grammarExampleToLoad id of the example grammar to be loaded
     */
    private void prepareExmaple(int grammarExampleToLoad) {
        String file = null;
        switch (grammarExampleToLoad) {
            case MainActivity.EXAMPLE_GRAMMAR1:
                file = FileHandler.GrammarExamples.GREG_AN;
                break;
            case MainActivity.EXAMPLE_GRAMMAR2:
                file = FileHandler.GrammarExamples.GREG_3KPlus1;
                break;
            case MainActivity.EXAMPLE_GRAMMAR3:
                file = FileHandler.GrammarExamples.GREG_End01Or10;
                break;
            case MainActivity.EXAMPLE_GRAMMAR4:
                file = FileHandler.GrammarExamples.LCF_ANBN;
                break;
            case MainActivity.EXAMPLE_GRAMMAR5:
                file = FileHandler.GrammarExamples.LCF_ANBN_CNF;
                break;
            case MainActivity.EXAMPLE_GRAMMAR6:
                file = FileHandler.GrammarExamples.LCF_WWR;
                break;
            case MainActivity.EXAMPLE_GRAMMAR7:
                file = FileHandler.GrammarExamples.LCS_ANBNCN;
                break;
        }
        try {
            FileHandler fileHandler = new FileHandler(FileHandler.Format.CMSG);
            fileHandler.loadAsset(getAssets(), file);
            fileHandler.getData(dataSource);
            List<GrammarRule> grammarRuleList = dataSource.getGrammarRuleFullExtract();
            filename = file;
            rulesAdapter = new RulesAdapter(rulesTableSize);
            rulesAdapter.setGrammarRuleList(grammarRuleList);
            recyclerView.setAdapter(rulesAdapter);
            recyclerView.setItemViewCacheSize(rulesTableSize);

        } catch (Exception e) {
            Log.e(TAG, "File was not loaded", e);
            Toast.makeText(GrammarActivity.this, getResources().getString(R.string.file_not_loaded), Toast.LENGTH_SHORT).show();
            dataSource.globalDrop();
            dataSource.close();
            finish();
        }
    }

    private void updateStatusBarColor(int s_dark, int t_dark) {
        ValueAnimator animator = ValueAnimator.ofObject(new ArgbEvaluator(), s_dark, t_dark);
        animator.setDuration(BACKGROUND_CHANGE_LENGTH);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                GrammarActivity.this.getWindow().setStatusBarColor((int) animation.getAnimatedValue());
            }
        });
        animator.start();
    }

    private void updateNavigationBarColor(int s_dark, int t_dark) {
        ValueAnimator animator = ValueAnimator.ofObject(new ArgbEvaluator(), s_dark, t_dark);
        animator.setDuration(BACKGROUND_CHANGE_LENGTH);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                GrammarActivity.this.getWindow().setNavigationBarColor((int) animation.getAnimatedValue());
            }
        });
        animator.start();
    }


    private void updateActionBarColor(int s_normal, int t_normal) {
        ValueAnimator animator = ValueAnimator.ofObject(new ArgbEvaluator(), s_normal, t_normal);
        animator.setDuration(BACKGROUND_CHANGE_LENGTH);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                GrammarActivity.this.getActionBar().setBackgroundDrawable(new ColorDrawable((int) animation.getAnimatedValue()));
            }
        });
        animator.start();
    }

    private void updateInnerViewsColor(int s_light, int t_light) {
        ValueAnimator animator = ValueAnimator.ofObject(new ArgbEvaluator(), s_light, t_light);

        final Button epsilon = findViewById(R.id.button_grammar_epsilon);
        final Button pipe = findViewById(R.id.button_grammar_pipe);
        final Button add = findViewById(R.id.button_grammar_add_row);

        animator.setDuration(BACKGROUND_CHANGE_LENGTH);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {


                final int currentColorValue = (int) animation.getAnimatedValue();

                int[][] states = new int[][]{
                        new int[]{0}
                };

                int[] color = new int[]{
                        currentColorValue
                };
                ColorStateList list = new ColorStateList(states, color);

                epsilon.getBackground().setColorFilter(currentColorValue, PorterDuff.Mode.MULTIPLY);
                pipe.getBackground().setColorFilter(currentColorValue, PorterDuff.Mode.MULTIPLY);
                add.getBackground().setColorFilter(currentColorValue, PorterDuff.Mode.MULTIPLY);

            }
        });
        animator.start();
    }


    private void changeActivityBackgroundColor(int s_dark, int s_normal, int s_light, int t_dark, int t_normal, int t_light) {
        updateStatusBarColor(s_dark, t_dark);
        updateActionBarColor(s_normal, t_normal);
        updateNavigationBarColor(s_dark, t_dark);
        updateInnerViewsColor(s_light, t_light);
    }
}
