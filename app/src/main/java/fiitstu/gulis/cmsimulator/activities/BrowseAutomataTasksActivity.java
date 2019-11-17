package fiitstu.gulis.cmsimulator.activities;

import android.app.ActionBar;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ProgressBar;
import android.widget.Toast;
import fiitstu.gulis.cmsimulator.R;
import fiitstu.gulis.cmsimulator.adapters.tasks.AutomataTaskAdapter;
import fiitstu.gulis.cmsimulator.elements.Task;
import fiitstu.gulis.cmsimulator.models.tasks.automata_tasks.FiniteAutomataTask;
import fiitstu.gulis.cmsimulator.network.ServerController;
import fiitstu.gulis.cmsimulator.network.UrlManager;
import fiitstu.gulis.cmsimulator.network.automata_tasks.AutomataTaskParser;
import org.json.JSONException;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class BrowseAutomataTasksActivity extends FragmentActivity {
    private List<Task> listOfTasks;
    private int user_id;
    private String authkey;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_browse_automata_tasks);

        // menu
        ActionBar actionbar = this.getActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);
        actionbar.setTitle(getString(R.string.available_tasks));

        // fetch intent extras
        user_id = this.getIntent().getIntExtra("USER_ID", 0);
        authkey = this.getIntent().getStringExtra("AUTHKEY");

        getListOfTasks();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = this.getMenuInflater();
        menuInflater.inflate(R.menu.menu_browse_automata_tasks, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }

        return false;
    }

    public void getListOfTasks() {

        class FetchTasksAsync extends AsyncTask<Void, Void, String> {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                showLoadScreen(true);
            }

            @Override
            protected String doInBackground(Void... voids) {
                UrlManager urlManager = new UrlManager();
                URL url = urlManager.getFetchAllAutomataTasksUrl(user_id, authkey);

                ServerController serverController = new ServerController();
                try {
                    return serverController.getResponseFromServer(url);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                return null;
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);

                AutomataTaskParser automataTaskParser = new AutomataTaskParser();
                try {
                    List<Task> taskList = automataTaskParser.getTasksFromJsonArray(s);
                    setListOfTasks(taskList);
                    setRecyclerView();

                } catch (JSONException e) {
                    e.printStackTrace();
                } finally{
                    showLoadScreen(false);
                }
            }
        }

        new FetchTasksAsync().execute();
    }

    public void setListOfTasks(List<Task> listOfTasks) {
        this.listOfTasks = listOfTasks;
    }

    private void setRecyclerView()
    {
        // set recyclerview
        RecyclerView recyclerViewTasks = findViewById(R.id.recyclerview_tasks);
        AutomataTaskAdapter adapter = new AutomataTaskAdapter(listOfTasks, this);

        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        float dpWidth = displayMetrics.widthPixels / displayMetrics.density;
        int noOfColumns = (int) (dpWidth / 180);

        GridLayoutManager layoutManager = new GridLayoutManager(this, noOfColumns);
        recyclerViewTasks.setLayoutManager(layoutManager);
        recyclerViewTasks.setAdapter(adapter);

        Animation showUpAnimation = AnimationUtils.loadAnimation(this, R.anim.item_show_animation);

        recyclerViewTasks.setAnimation(showUpAnimation);
    }

    private void showLoadScreen(boolean value)
    {
        ProgressBar progressBar = findViewById(R.id.progressbar_users);
        RecyclerView recyclerView = findViewById(R.id.recyclerview_tasks);

        progressBar.setVisibility(value ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(value ? View.GONE : View.VISIBLE);
    }

}