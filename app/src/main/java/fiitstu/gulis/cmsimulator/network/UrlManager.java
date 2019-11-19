package fiitstu.gulis.cmsimulator.network;

import android.net.Uri;
import fiitstu.gulis.cmsimulator.activities.MainActivity;
import fiitstu.gulis.cmsimulator.elements.Task;
import fiitstu.gulis.cmsimulator.models.tasks.automata_tasks.FiniteAutomataTask;
import fiitstu.gulis.cmsimulator.models.tasks.automata_tasks.LinearBoundedAutomataTask;
import fiitstu.gulis.cmsimulator.models.tasks.automata_tasks.PushdownAutomataTask;
import fiitstu.gulis.cmsimulator.models.tasks.automata_type;
import fiitstu.gulis.cmsimulator.models.users.User;
import fiitstu.gulis.cmsimulator.network.users.PasswordManager;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class UrlManager {

    private final static String URI = "http://147.175.160.30:3000";

    // PATHS
    private final static String LOGIN_PATH = "/api/login";
    private final static String CHANGE_PASSWORD_PATH = "/api/user/changePassword";
    private final static String ADD_NEW_USER_PATH = "/api/user/signup";
    private final static String GET_ALL_USERS_PATH = "/api/user/getUsers";
    private final static String SEARCH_USER_PATH = "/api/user/getUsersFiltered";
    private final static String PUBLISH_TASK_PATH = "/api/tasks/upload";
    private final static String ADD_TASK_TO_TABLE = "/api/tasks/add";
    private final static String GET_ALL_AUTOMATA_TASKS_PATH = "/api/tasks/getTasks";

    // LOGIN QUERY KEYS
    private final static String USERNAME_QUERY_KEY = "username";
    private final static String AUTHKEY_QUERY_KEY = "auth_key";

    // SIGN UP QUERY KEYS
    private final static String FIRST_NAME_KEY = "first_name";
    private final static String LAST_NAME_KEY = "last_name";
    private final static String USER_TYPE_KEY = "user_type";

    // CHANGE PASSWORD QUERY KEYS
    private final static String USER_ID_KEY = "user_id";
    private final static String NEW_AUTH_KEY = "new_auth_key";
    private final static String OLD_AUTH_KEY = "auth_key";

    // ADD TASK TO TABLE QUERY KEYS
    private final static String TASK_NAME_KEY = "task_name";
    private final static String TASK_DESCRIPTION_KEY = "task_description";
    private final static String TIME_KEY = "time";
    private final static String ASSIGNER_KEY = "assigner";
    private final static String PUBLIC_INPUT_KEY = "public_input";
    private final static String FILE_NAME_KEY = "file_name";
    private final static String AUTOMATA_TYPE_KEY = "automata_type";


    public URL getLoginUrl(String username, String password)
    {
        PasswordManager passwordManager = new PasswordManager();
        String authkey = passwordManager.getAuthkey(password);

        Uri builtUri = Uri.parse(URI + LOGIN_PATH).buildUpon()
                .appendQueryParameter(USERNAME_QUERY_KEY, username)
                .appendQueryParameter(AUTHKEY_QUERY_KEY, authkey)
                .build();

        URL url = null;

        try
        {
            url = new URL(builtUri.toString());
        }
        catch (MalformedURLException e)
        {
            e.printStackTrace();
        }

        return url;
    }

    public URL getLoginUrlWithAuthkey(String username, String authkey)
    {
        Uri builtUri = Uri.parse(URI + LOGIN_PATH).buildUpon()
                .appendQueryParameter(USERNAME_QUERY_KEY, username)
                .appendQueryParameter(AUTHKEY_QUERY_KEY, authkey)
                .build();

        URL url = null;

        try
        {
            url = new URL(builtUri.toString());
        }
        catch (MalformedURLException e)
        {
            e.printStackTrace();
        }

        return url;
    }

    public URL getAllUsersUrl(String authkey)
    {
        Uri builtUri = Uri.parse(URI + GET_ALL_USERS_PATH).buildUpon()
                .appendQueryParameter(AUTHKEY_QUERY_KEY, authkey)
                .build();

        URL url = null;

        try
        {
            url = new URL(builtUri.toString());
        }
        catch (MalformedURLException e)
        {
            e.printStackTrace();
        }

        return url;
    }

    public URL getSearchedUser(String authkey, String searchedUserLastName)
    {
        Uri builtUri = Uri.parse(URI + SEARCH_USER_PATH).buildUpon()
                .appendQueryParameter(AUTHKEY_QUERY_KEY, authkey)
                .appendQueryParameter(LAST_NAME_KEY, searchedUserLastName)
                .build();

        URL url = null;

        try
        {
            url = new URL(builtUri.toString());
        }
        catch (MalformedURLException e)
        {
            e.printStackTrace();
        }

        return url;
    }

    public URL getChangePasswordUrl(int user_id, String old_password, String new_password)
    {
        PasswordManager passwordManager = new PasswordManager();
        String old_auth = passwordManager.getAuthkey(old_password);
        String new_auth = passwordManager.getAuthkey(new_password);

        Uri builtUri = Uri.parse(URI + CHANGE_PASSWORD_PATH).buildUpon()
                .appendQueryParameter(USER_ID_KEY, Integer.toString(user_id))
                .appendQueryParameter(NEW_AUTH_KEY, new_auth)
                .appendQueryParameter(OLD_AUTH_KEY, old_auth)
                .build();

        URL url = null;

        try
        {
            url = new URL(builtUri.toString());
        }
        catch (MalformedURLException e)
        {
            e.printStackTrace();
        }

        return url;
    }

    public URL getPublishAutomataTaskURL(String file_name)
    {

        Uri builtUri = Uri.parse(URI + PUBLISH_TASK_PATH).buildUpon()
                .appendQueryParameter(FILE_NAME_KEY, file_name)
                .build();

        URL url = null;

        try
        {
            url = new URL(builtUri.toString());
        }
        catch (MalformedURLException e)
        {
            e.printStackTrace();
        }

        return url;
    }

    public URL getFetchAllAutomataTasksUrl(int user_id, String auth_key)
    {

        Uri builtUri = Uri.parse(URI + GET_ALL_AUTOMATA_TASKS_PATH).buildUpon()
                .appendQueryParameter(AUTHKEY_QUERY_KEY, auth_key)
                .appendQueryParameter(USER_ID_KEY, Integer.toString(user_id))
                .build();

        URL url = null;

        try
        {
            url = new URL(builtUri.toString());
        }
        catch (MalformedURLException e)
        {
            e.printStackTrace();
        }

        return url;
    }

    public URL getPushAutomataTaskToTable(Task task, int user_id, int machine_id)
    {
        String type = null;
        switch (machine_id)
        {
            case MainActivity
                    .FINITE_STATE_AUTOMATON:
                type = automata_type.FINITE_AUTOMATA.getApiKey();
                break;
            case MainActivity.PUSHDOWN_AUTOMATON:
                type = automata_type.PUSHDOWN_AUTOMATA.getApiKey();
                break;
            case MainActivity.LINEAR_BOUNDED_AUTOMATON:
                type = automata_type.LINEAR_BOUNDED_AUTOMATA.getApiKey();
                break;
            case MainActivity.TURING_MACHINE:
                type = automata_type.TURING_MACHINE.getApiKey();
                break;
        }
        Uri builtUri = Uri.parse(URI + ADD_TASK_TO_TABLE).buildUpon()
                .appendQueryParameter(TASK_NAME_KEY, task.getTitle())
                .appendQueryParameter(TASK_DESCRIPTION_KEY, task.getText())
                .appendQueryParameter(TIME_KEY, Integer.toString(task.getMinutes()))
                .appendQueryParameter(PUBLIC_INPUT_KEY, Boolean.toString(task.getPublicInputs()))
                .appendQueryParameter(ASSIGNER_KEY, Integer.toString(user_id))
                .appendQueryParameter(FILE_NAME_KEY, task.getTitle() + ".cmst")
                .appendQueryParameter(AUTOMATA_TYPE_KEY, type)
                .build();

        URL url = null;

        try
        {
            url = new URL(builtUri.toString());
        }
        catch (MalformedURLException e)
        {
            e.printStackTrace();
        }

        return url;
    }

    public URL getAddUserUrl(String username, String first_name, String last_name, String authkey, User.user_type user_type)
    {
        PasswordManager passwordManager = new PasswordManager();

        String user_type_string = "";

        switch (user_type)
        {
            case admin:
                user_type_string = "admin";
                break;
            case lector:
                user_type_string = "lector";
                break;
            case student:
                user_type_string = "student";
                break;
        }

        Uri builtUri = Uri.parse(URI + ADD_NEW_USER_PATH).buildUpon()
                .appendQueryParameter(FIRST_NAME_KEY, first_name)
                .appendQueryParameter(LAST_NAME_KEY, last_name)
                .appendQueryParameter(USERNAME_QUERY_KEY, username)
                .appendQueryParameter(AUTHKEY_QUERY_KEY, authkey)
                .appendQueryParameter(USER_TYPE_KEY, user_type_string)
                .build();

        URL builtURL = null;

        try
        {
            builtURL = new URL(builtUri.toString());
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return builtURL;
    }



}
